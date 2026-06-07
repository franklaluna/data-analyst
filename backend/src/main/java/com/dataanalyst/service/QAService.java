package com.dataanalyst.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class QAService {

    private static final Logger log = LoggerFactory.getLogger(QAService.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final PythonClient pythonClient;
    private final LLMService llmService;
    private final JdbcTemplate jdbc;

    public QAService(PythonClient pythonClient, LLMService llmService, JdbcTemplate jdbc) {
        this.pythonClient = pythonClient;
        this.llmService = llmService;
        this.jdbc = jdbc;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> ask(Long fileId, String question) throws Exception {
        // 1. Get file info
        Map<String, Object> fileRow = jdbc.queryForMap(
            "SELECT file_path, columns_json, profile_json FROM da_files WHERE id = ?", fileId
        );
        String filePath = (String) fileRow.get("file_path");
        String columnsJson = (String) fileRow.get("columns_json");
        String profileJson = (String) fileRow.get("profile_json");

        List<Map<String, Object>> columns = mapper.readValue(columnsJson, List.class);
        Map<String, Object> profile = mapper.readValue(profileJson, Map.class);
        List<Map<String, Object>> sampleData = (List<Map<String, Object>>) profile.get("sample_data");

        // 2. Generate pandas code via LLM
        String code = llmService.generatePandasCode(question, columns, sampleData);
        log.info("Generated code for question: {}", question);
        log.debug("Generated code:\n{}", code);

        // 3. Execute code in Python sandbox
        Map<String, Object> execResult = pythonClient.executeCode(code, filePath);

        // 4. Build response
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("question", question);
        response.put("code", code);

        if (execResult.containsKey("error")) {
            response.put("answer", "分析出错: " + execResult.get("error"));
            response.put("chart", null);
        } else {
            // Ask LLM to summarize the result
            String resultJson = mapper.writeValueAsString(execResult.get("result"));
            String summaryPrompt = "用户问题：" + question + "\n分析结果：" + resultJson +
                "\n\n请用中文简洁地总结这个分析结果，不超过 100 字。";
            String answer = llmService.chat("你是数据分析助手，用中文简洁总结分析结果。", summaryPrompt);
            response.put("answer", answer);
            response.put("result", execResult.get("result"));
            response.put("chart", execResult.get("chart"));
        }

        // 5. Save to history
        Long sessionId = getOrCreateSession(fileId);
        String chartJson = response.get("chart") != null ? mapper.writeValueAsString(response.get("chart")) : null;
        jdbc.update(
            "INSERT INTO da_qa_history (session_id, question, answer, chart_json, code_generated) VALUES (?, ?, ?, ?, ?)",
            sessionId, question, (String) response.get("answer"), chartJson, code
        );

        return response;
    }

    private Long getOrCreateSession(Long fileId) {
        List<Map<String, Object>> sessions = jdbc.queryForList(
            "SELECT id FROM da_sessions WHERE file_id = ? ORDER BY created_at DESC LIMIT 1", fileId
        );
        if (!sessions.isEmpty()) {
            return ((Number) sessions.get(0).get("id")).longValue();
        }
        jdbc.update("INSERT INTO da_sessions (file_id) VALUES (?)", fileId);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    public List<Map<String, Object>> getHistory(Long fileId) {
        return jdbc.queryForList(
            "SELECT q.question, q.answer, q.chart_json, q.created_at " +
            "FROM da_qa_history q " +
            "JOIN da_sessions s ON q.session_id = s.id " +
            "WHERE s.file_id = ? ORDER BY q.created_at DESC LIMIT 20", fileId
        );
    }
}
