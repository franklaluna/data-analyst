package com.dataanalyst.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisService.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final PythonClient pythonClient;
    private final LLMService llmService;
    private final JdbcTemplate jdbc;

    public AnalysisService(PythonClient pythonClient, LLMService llmService, JdbcTemplate jdbc) {
        this.pythonClient = pythonClient;
        this.llmService = llmService;
        this.jdbc = jdbc;
    }

    public Map<String, Object> analyzeFile(String filePath, String filename, Long projectId) throws Exception {
        // 1. Call Python to profile the file
        Map<String, Object> profile = pythonClient.analyze(filePath);
        log.info("Profiled file: rows={}, cols={}", profile.get("row_count"), profile.get("col_count"));

        // 2. Generate AI summary
        String summary = llmService.generateAnalysisSummary(profile);
        profile.put("summary", summary);

        // 3. Generate ECharts data for suggested charts
        List<Map<String, Object>> charts = (List<Map<String, Object>>) profile.get("charts");
        if (charts != null) {
            for (Map<String, Object> chart : charts) {
                Object chartData = generateChartData(chart, filePath);
                chart.put("data", chartData);
            }
        }

        // 4. Save to database
        int rowCount = ((Number) profile.get("row_count")).intValue();
        int colCount = ((Number) profile.get("col_count")).intValue();
        String columnsJson = mapper.writeValueAsString(profile.get("columns"));
        String profileJson = mapper.writeValueAsString(profile);

        jdbc.update(
            "INSERT INTO da_files (user_id, filename, file_path, row_count, col_count, columns_json, profile_json, project_id) VALUES (1, ?, ?, ?, ?, ?, ?, ?)",
            filename, filePath, rowCount, colCount, columnsJson, profileJson, projectId
        );
        Long fileId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        profile.put("id", fileId);

        return profile;
    }

    /**
     * Escape column name for use in Python string literals.
     * Preserves the original column name while preventing code injection.
     */
    private static String escapePythonString(String col) {
        if (col == null) return null;
        if (col.isEmpty()) return "_col";
        // Escape backslashes first, then single quotes
        return col.replace("\\", "\\\\").replace("'", "\\'");
    }

    @SuppressWarnings("unchecked")
    private Object generateChartData(Map<String, Object> chart, String filePath) throws Exception {
        String type = (String) chart.get("type");
        String xAxis = escapePythonString((String) chart.get("x_axis"));
        String yAxis = escapePythonString((String) chart.get("y_axis"));
        String nameField = escapePythonString((String) chart.get("name_field"));
        String valueField = escapePythonString((String) chart.get("value_field"));

        String code;

        if ("heatmap".equals(type)) {
            // Correlation heatmap
            List<String> fields = (List<String>) chart.get("fields");
            if (fields == null || fields.size() < 3) return null;
            StringBuilder fieldList = new StringBuilder("[");
            for (int i = 0; i < fields.size(); i++) {
                if (i > 0) fieldList.append(", ");
                fieldList.append("'").append(escapePythonString(fields.get(i))).append("'");
            }
            fieldList.append("]");
            code = String.format(
                "corr = df[%s].corr()\n" +
                "n = len(corr)\n" +
                "result = [[i, j, round(float(corr.iloc[i, j]), 2)] for i in range(n) for j in range(n)]\n",
                fieldList.toString());
        } else if ("pie".equals(type)) {
            if ("count".equals(valueField)) {
                // Count-based pie (e.g., call direction distribution)
                code = String.format(
                    "grouped = df['%s'].value_counts().head(10)\n" +
                    "result = [{'name': str(k), 'value': int(v)} for k, v in grouped.items()]\n",
                    nameField);
            } else {
                code = String.format(
                    "grouped = df.groupby('%s')['%s'].sum().sort_values(ascending=False).head(10)\n" +
                    "result = [{'name': str(k), 'value': float(v)} for k, v in grouped.items()]\n",
                    nameField, valueField);
            }
        } else if ("count".equals(yAxis)) {
            // Count-based bar/line (e.g., call volume, top contacts)
            code = String.format(
                "grouped = df['%s'].value_counts().head(10)\n" +
                "result = [{'x': str(k), 'y': int(v)} for k, v in grouped.items()]\n",
                xAxis);
        } else {
            code = String.format(
                "grouped = df.groupby('%s')['%s'].sum()\n" +
                "result = [{'x': str(k), 'y': float(v)} for k, v in grouped.items()]\n",
                xAxis, yAxis);
        }

        Map<String, Object> execResult = pythonClient.executeCode(code, filePath);
        if (execResult.containsKey("error")) {
            log.warn("Chart data generation failed: {}", execResult.get("error"));
            return null;
        }
        return execResult.get("result");
    }

    public List<Map<String, Object>> listFiles() {
        return jdbc.queryForList(
            "SELECT f.id, f.filename, f.row_count, f.col_count, f.created_at, f.project_id, p.name AS project_name " +
            "FROM da_files f LEFT JOIN da_projects p ON f.project_id = p.id " +
            "ORDER BY f.created_at DESC LIMIT 20"
        );
    }

    public List<Map<String, Object>> listFiles(Long projectId) {
        return jdbc.queryForList(
            "SELECT f.id, f.filename, f.row_count, f.col_count, f.created_at, f.project_id, p.name AS project_name " +
            "FROM da_files f LEFT JOIN da_projects p ON f.project_id = p.id " +
            "WHERE f.project_id = ? ORDER BY f.created_at DESC",
            projectId
        );
    }

    public Map<String, Object> getFileProfile(Long fileId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT f.profile_json, f.project_id, p.name AS project_name " +
            "FROM da_files f LEFT JOIN da_projects p ON f.project_id = p.id " +
            "WHERE f.id = ?",
            fileId
        );
        if (rows.isEmpty()) {
            return null;
        }
        String profileJson = (String) rows.get(0).get("profile_json");
        if (profileJson == null) {
            return null;
        }
        try {
            Map<String, Object> profile = mapper.readValue(profileJson, Map.class);
            profile.put("project_id", rows.get(0).get("project_id"));
            profile.put("project_name", rows.get(0).get("project_name"));
            return profile;
        } catch (Exception e) {
            log.error("Failed to parse profile JSON", e);
            return null;
        }
    }
}
