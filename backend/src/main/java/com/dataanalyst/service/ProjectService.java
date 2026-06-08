package com.dataanalyst.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);

    private final JdbcTemplate jdbc;

    public ProjectService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> listProjects() {
        return jdbc.queryForList(
            "SELECT p.id, p.name, p.description, p.created_at, " +
            "(SELECT COUNT(*) FROM da_files f WHERE f.project_id = p.id) AS file_count " +
            "FROM da_projects p WHERE p.user_id = 1 ORDER BY p.created_at DESC"
        );
    }

    public Long createProject(String name, String description) {
        jdbc.update(
            "INSERT INTO da_projects (user_id, name, description) VALUES (1, ?, ?)",
            name, description
        );
        Long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        log.info("Created project: id={}, name={}", id, name);
        return id;
    }

    public Map<String, Object> getProject(Long projectId) {
        List<Map<String, Object>> results = jdbc.queryForList(
            "SELECT p.id, p.name, p.description, p.created_at, p.updated_at, " +
            "(SELECT COUNT(*) FROM da_files f WHERE f.project_id = p.id) AS file_count " +
            "FROM da_projects p WHERE p.id = ? AND p.user_id = 1",
            projectId
        );
        return results.isEmpty() ? null : results.get(0);
    }

    public List<Map<String, Object>> getProjectFiles(Long projectId) {
        return jdbc.queryForList(
            "SELECT id, filename, row_count, col_count, created_at FROM da_files " +
            "WHERE project_id = ? ORDER BY created_at DESC",
            projectId
        );
    }

    public int updateProject(Long projectId, String name, String description) {
        int rows = jdbc.update(
            "UPDATE da_projects SET name = ?, description = ? WHERE id = ? AND user_id = 1",
            name, description, projectId
        );
        log.info("Updated project: id={}, rows={}", projectId, rows);
        return rows;
    }

    public int deleteProject(Long projectId) {
        int rows = jdbc.update(
            "DELETE FROM da_projects WHERE id = ? AND user_id = 1",
            projectId
        );
        log.info("Deleted project: id={}, rows={}", projectId, rows);
        return rows;
    }
}
