# Data Analyst -- Security & Code Quality Review

**Reviewer:** reviewer (sonnet)
**Date:** 2026-06-07
**Scope:** backend/src (Java), python-service (Python), frontend/src (TypeScript/Vue)

---

## CRITICAL -- Must Fix

### C-1. Arbitrary Python Code Execution via Column Name Injection

**File:** `backend/src/main/java/com/dataanalyst/service/AnalysisService.java`, lines 72-84
**Category:** Code Injection

`generateChartData()` builds pandas code by interpolating column names from the uploaded file directly into a `String.format()` template:

```java
code = String.format(
    "grouped = df.groupby('%s')['%s'].sum()...\n",
    nameField, valueField);  // <-- user-controlled column names
```

An attacker crafts a CSV with a column named `'); import os; os.system('whoami'); ('` and the generated code becomes arbitrary command execution.

**Fix:** Sanitize column names before interpolation (allow only `[a-zA-Z0-9_\s]`), or pass column names as data rather than embedding them in code strings. Better yet, use a structured execution API instead of code generation.

---

### C-2. File Path Injection in Python Executor (f-string interpolation)

**File:** `python-service/executor.py`, lines 38-43
**Category:** Command Injection / Path Traversal

The `file_path` parameter is interpolated directly into the generated Python script via f-string:

```python
wrapper = f"""
...
    df = pd.read_excel('{file_path}', nrows=100000)
...
"""
```

A `file_path` like `'); import subprocess; subprocess.call(['rm','-rf','/']); ('` breaks out of the string literal and executes arbitrary code. The forbidden-keyword check (lines 10-15) only runs on the user `code`, not on `file_path`.

**Fix:** Escape or validate `file_path` before interpolation. Use `repr()` or pass it as a variable rather than embedding it in the source string. Example:

```python
# Pass as environment variable or stdin, not string interpolation
import os
os.environ['DATA_FILE'] = file_path
```

---

### C-3. Weak Code Sandbox -- Trivially Bypassable

**File:** `python-service/executor.py`, lines 9-15
**Category:** Insufficient Security Control

The keyword blocklist is trivially bypassed:

- `'import os'` bypass: `__import__('o'+'s')`, `vars()['__builtins__']['__imp'+'ort__']('os')`
- `'open('` bypass: `getattr(__builtins__, 'op'+'en')('/etc/passwd')`
- `'exec('` bypass: `eval('exec("import os; os.system(\\"id\\")")')`
- `'__import__'` bypass: `importlib.import_module()`, `sys.modules` manipulation
- Network access is unrestricted: `import urllib.request` or `import httpx` can exfiltrate data
- No filesystem restrictions beyond the keyword check

The code runs as a subprocess with full OS privileges and network access.

**Fix:** Implement real sandboxing. Options (in order of increasing security):
1. Use `RestrictedPython` or `PyPy Sandbox`
2. Run in a Docker container with `--network=none`, read-only filesystem, no capabilities
3. Use `seccomp` / `AppArmor` profiles
4. At minimum: parse the AST with `ast.parse()` and whitelist allowed node types instead of string matching

---

### C-4. Database Credentials Hardcoded in docker-compose.yml

**File:** `docker-compose.yml`, line 28
**Category:** Credential Exposure

```yaml
- SPRING_DATASOURCE_PASSWORD=laluna157
```

Database password is committed in plain text. This file is typically version-controlled.

**Fix:** Use environment variable substitution (already done for `DEEPSEEK_API_KEY`), a `.env` file (gitignored), or Docker secrets.

---

### C-5. Server-Side File Path Traversal on Upload

**File:** `backend/src/main/java/com/dataanalyst/controller/FileController.java`, line 35
**Category:** Path Traversal

```java
String filename = file.getOriginalFilename();  // user-controlled
String filePath = UPLOAD_DIR + System.currentTimeMillis() + "_" + filename;
```

`getOriginalFilename()` returns the raw client-supplied filename. A filename like `../../etc/cron.d/backdoor` would write outside the upload directory.

**Fix:** Sanitize the filename:

```java
String filename = Paths.get(file.getOriginalFilename()).getFileName().toString();
// Validate: only allow alphanumeric, dots, hyphens, underscores
if (!filename.matches("[a-zA-Z0-9._-]+")) {
    throw new IllegalArgumentException("Invalid filename");
}
```

---

### C-6. No Server-Side File Size Limit

**File:** `backend/src/main/java/com/dataanalyst/controller/FileController.java`, line 29
**Category:** Denial of Service

No `spring.servlet.multipart.max-file-size` is configured. The frontend mentions "50MB" in UI text but nothing enforces it server-side. An attacker can upload multi-GB files to exhaust disk or memory.

**Fix:** Add to `application.properties`:

```properties
spring.servlet.multipart.max-file-size=50MB
spring.servlet.multipart.max-request-size=50MB
```

---

## WARNINGS -- Should Fix

### W-1. Unsafe JSON Parsing from URL Query Parameter (XSS vector)

**File:** `frontend/src/views/Analysis.vue`, line 115
**Category:** XSS

```ts
data.value = JSON.parse(queryData)  // queryData from route.query.data
```

The analysis result is passed via URL query parameter and parsed without validation. If the backend stores attacker-controlled data (e.g., column names, LLM-generated summaries) that include HTML/script tags, and they are rendered via `{{ data.summary }}` (line 13), Vue's template interpolation escapes them. However, the `v-html` pattern is one refactor away, and the URL itself can be extremely long (browser limits ~2MB).

Additionally, the Upload.vue (line 102) serializes the entire analysis result into the URL:

```ts
router.push({ name: 'analysis', query: { data: JSON.stringify(result) } })
```

This leaks data in browser history, referer headers, and server logs.

**Fix:** Always use the `route.params.id` path (file ID from DB) instead of passing data via query string. Return the file ID from the upload endpoint.

---

### W-2. ClassCastException on Invalid/Missing Request Fields

**File:** `backend/src/main/java/com/dataanalyst/controller/QAController.java`, line 28
**Category:** Error Handling / Crash

```java
Long fileId = ((Number) request.get("fileId")).longValue();
```

If `fileId` is missing from the request body, `request.get("fileId")` returns `null` and `.longValue()` throws `NullPointerException`. If it's a string, `ClassCastException`.

**Fix:** Use a typed DTO with `@Valid` annotations:

```java
public class QARequest {
    @NotNull private Long fileId;
    @NotBlank private String question;
    // getters/setters
}
```

---

### W-3. NullPointerException in AnalysisService.analyzeFile()

**File:** `backend/src/main/java/com/dataanalyst/service/AnalysisService.java`, lines 49-50
**Category:** Null Safety

```java
int rowCount = ((Number) profile.get("row_count")).intValue();
int colCount = ((Number) profile.get("col_count")).intValue();
```

If the Python service returns a response missing these fields (e.g., after an error), this throws NPE. Same risk on line 40: `profile.get("charts")` could be null (handled) but `profile.get("columns")` on line 51 is not.

**Fix:** Add null checks before casting:

```java
Number rowCountNum = (Number) profile.get("row_count");
int rowCount = rowCountNum != null ? rowCountNum.intValue() : 0;
```

---

### W-4. LLM API Response Not Validated

**File:** `backend/src/main/java/com/dataanalyst/service/LLMService.java`, lines 52-55
**Category:** Error Handling

```java
List<Map<String, Object>> choices = (List<Map<String, Object>>) respMap.get("choices");
Map<String, Object> message = (Map<String, Object>>) choices.get(0).get("message");
return (String) message.get("content");
```

No null checks. If the LLM returns an unexpected structure (rate limit response, empty choices), this throws NPE or IndexOutOfBoundsException with no useful error message.

**Fix:** Validate the response structure before accessing nested fields.

---

### W-5. Unsafe f-string Path Interpolation in Profiler

**File:** `python-service/profiler.py`, line 7-14
**Category:** Path Traversal / Injection

`profile_file()` accepts a raw `file_path` string with no validation. It does not verify the path exists, is within the expected directory, or is a file (not a symlink to `/etc/passwd`).

**Fix:** Validate the path:

```python
import pathlib
def profile_file(file_path: str) -> dict:
    p = pathlib.Path(file_path).resolve()
    allowed_dir = pathlib.Path("/tmp/data-analyst-uploads").resolve()
    if not str(p).startswith(str(allowed_dir)):
        raise ValueError("File path outside allowed directory")
    if not p.exists():
        raise FileNotFoundError(f"File not found: {file_path}")
```

---

### W-6. No Authentication or Authorization

**Files:** All controllers, `init-db.sql`
**Category:** Authentication

- All API endpoints are publicly accessible with no authentication.
- `user_id` is hardcoded to `1` in `AnalysisService.java` line 55.
- Any user can access any file's profile, QA history, or upload arbitrary files.

**Fix:** Implement at minimum a session-based auth or API key. Even a simple `X-Api-Key` header check is better than nothing.

---

### W-7. Python Service Exposed Without Authentication

**File:** `docker-compose.yml`, `python-service/main.py`
**Category:** Network Security

The Python service binds to all interfaces on port 8001 with no authentication. Any machine on the network can call `/execute` to run arbitrary code.

**Fix:** Bind to localhost only (`127.0.0.1:8001:8001` in docker-compose), or add an API key / shared secret between backend and Python service.

---

### W-8. Error Messages Leak Internal Details

**Files:**
- `backend/src/main/java/com/dataanalyst/controller/FileController.java`, line 46
- `backend/src/main/java/com/dataanalyst/controller/QAController.java`, line 42
- `python-service/main.py`, lines 23-24

**Category:** Information Disclosure

Exception messages are returned directly to the client:

```java
error.put("error", e.getMessage());
```

This can expose file paths, database details, stack traces, and internal architecture.

**Fix:** Return generic error messages to clients; log details server-side:

```java
log.error("Upload failed", e);
error.put("error", "File processing failed. Please try again.");
```

---

### W-9. No Timeout or Memory Limit for File Profiling

**File:** `python-service/profiler.py`, line 10
**Category:** Resource Exhaustion / DoS

```python
df = pd.read_excel(file_path, nrows=50000)
```

50,000 rows of a wide Excel file (hundreds of columns) can consume gigabytes of RAM. No timeout is set. A crafted file could OOM the service.

**Fix:** Add a column limit and memory check:

```python
df = pd.read_excel(file_path, nrows=50000)
if len(df.columns) > 200:
    df = df.iloc[:, :200]  # cap columns
```

---

### W-10. ECharts Event Listener Memory Leak

**File:** `frontend/src/views/Analysis.vue`, line 163
**Category:** Memory Leak

```ts
window.addEventListener('resize', () => chartInstance.resize())
```

A new resize listener is added for every chart, every time `renderCharts()` is called, and never removed. On page navigation or re-render, listeners accumulate.

**Fix:** Store references and clean up in `onUnmounted`:

```ts
import { onUnmounted } from 'vue'
const resizeHandlers: (() => void)[] = []
// ... in renderCharts:
const handler = () => chartInstance.resize()
window.addEventListener('resize', handler)
resizeHandlers.push(handler)
onUnmounted(() => {
  resizeHandlers.forEach(h => window.removeEventListener('resize', h))
})
```

---

## SUGGESTIONS -- Nice to Have

### S-1. Use Typed DTOs Instead of Raw Maps

**Files:** `QAController.java`, `AnalysisService.java`, `QAService.java`
**Category:** Code Quality

Controllers and services pass `Map<String, Object>` everywhere. This makes the code fragile, hard to refactor, and prone to runtime ClassCastExceptions.

**Suggestion:** Create DTOs for API requests/responses. Spring Boot auto-serializes them.

---

### S-2. OkHttp Client Should Have Connection Pooling Config

**File:** `backend/src/main/java/com/dataanalyst/config/LLMConfig.java`, line 26-30
**Category:** Performance

The default `OkHttpClient` connection pool settings are fine for low traffic, but explicit configuration is better for production:

```java
return new OkHttpClient.Builder()
    .readTimeout(Duration.ofSeconds(60))
    .callTimeout(Duration.ofSeconds(60))
    .connectionPool(new ConnectionPool(5, 1, TimeUnit.MINUTES))
    .build();
```

---

### S-3. QAEndpoint Returns Generated Code to Client

**File:** `backend/src/main/java/com/dataanalyst/service/QAService.java`, line 53
**Category:** Security (defense-in-depth)

The LLM-generated pandas code is returned in the API response and stored in the database:

```java
response.put("code", code);
```

If the code is ever displayed in the frontend without escaping, it's an XSS vector. Also, exposing the code reveals the LLM's "reasoning" which may be undesirable.

**Suggestion:** Consider omitting `code` from the client response, or sanitize it before returning.

---

### S-4. Hardcoded LIMIT 20 on Queries

**Files:** `AnalysisService.java` line 90, `QAService.java` lines 83, 93
**Category:** Code Quality

Query limits are hardcoded. If the UI changes to show more items, the backend must be modified.

**Suggestion:** Accept `limit` and `offset` as parameters from the client.

---

### S-5. Missing Input Validation on File ID

**File:** `backend/src/main/java/com/dataanalyst/controller/FileController.java`, line 57

The `@PathVariable Long id` is auto-parsed by Spring. A non-numeric value returns a 400, but there's no explicit validation that `id > 0`.

---

### S-6. Use `v-html` Guard Pattern for Dynamic Content

**File:** `frontend/src/views/Analysis.vue`

Currently safe (uses `{{ }}` interpolation), but consider adding a DOMPurify-like sanitizer as a project-wide policy in case `v-html` is introduced later.

---

### S-7. Consider Using a Proper Code Template for Pandas Generation

**File:** `backend/src/main/java/com/dataanalyst/service/AnalysisService.java`, lines 72-81

The String.format approach for generating pandas code is fragile. Consider using a template engine (e.g., Jinja2 on the Python side) or a structured execution API that accepts column names and aggregation functions as parameters rather than generating raw code.

---

## Summary

| Severity | Count | Key Themes |
|----------|-------|------------|
| Critical | 6 | Code injection via column names, path traversal, weak sandbox, hardcoded creds |
| Warning  | 10 | Null safety, no auth, resource exhaustion, memory leaks, info disclosure |
| Suggestion | 7 | Typed DTOs, connection pooling, input validation |

**Top 3 Actions:**
1. Fix the column name injection in `AnalysisService.generateChartData()` (C-1) and the `file_path` injection in `executor.py` (C-2) -- these are the most exploitable.
2. Implement real sandboxing for code execution (C-3) -- the current blocklist provides false security.
3. Move hardcoded credentials out of `docker-compose.yml` (C-4) and add basic authentication (W-6).
