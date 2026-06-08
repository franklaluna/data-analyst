package com.dataanalyst.controller;

import com.dataanalyst.service.ProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private static final Logger log = LoggerFactory.getLogger(ProjectController.class);

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listProjects() {
        return ResponseEntity.ok(projectService.listProjects());
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createProject(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        String description = body.get("description");
        if (name == null || name.trim().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "name is required");
            return ResponseEntity.badRequest().body(error);
        }
        Long id = projectService.createProject(name.trim(), description);
        Map<String, Object> result = new HashMap<>();
        result.put("id", id);
        result.put("name", name.trim());
        result.put("description", description);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getProject(@PathVariable Long id) {
        Map<String, Object> project = projectService.getProject(id);
        if (project == null) {
            return ResponseEntity.notFound().build();
        }
        List<Map<String, Object>> files = projectService.getProjectFiles(id);
        project.put("files", files);
        return ResponseEntity.ok(project);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateProject(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String name = body.get("name");
        String description = body.get("description");
        if (name == null || name.trim().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "name is required");
            return ResponseEntity.badRequest().body(error);
        }
        int rows = projectService.updateProject(id, name.trim(), description);
        if (rows == 0) {
            return ResponseEntity.notFound().build();
        }
        Map<String, Object> result = new HashMap<>();
        result.put("id", id);
        result.put("name", name.trim());
        result.put("description", description);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteProject(@PathVariable Long id) {
        int rows = projectService.deleteProject(id);
        if (rows == 0) {
            return ResponseEntity.notFound().build();
        }
        Map<String, Object> result = new HashMap<>();
        result.put("deleted", true);
        result.put("id", id);
        return ResponseEntity.ok(result);
    }
}
