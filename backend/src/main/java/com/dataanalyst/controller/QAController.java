package com.dataanalyst.controller;

import com.dataanalyst.service.QAService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/qa")
public class QAController {

    private static final Logger log = LoggerFactory.getLogger(QAController.class);

    private final QAService qaService;

    public QAController(QAService qaService) {
        this.qaService = qaService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> ask(@RequestBody Map<String, Object> request) {
        try {
            Long fileId = ((Number) request.get("fileId")).longValue();
            String question = (String) request.get("question");

            if (question == null || question.trim().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "问题不能为空");
                return ResponseEntity.badRequest().body(error);
            }

            Map<String, Object> result = qaService.ask(fileId, question);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("QA failed", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/history/{fileId}")
    public ResponseEntity<List<Map<String, Object>>> history(@PathVariable Long fileId) {
        return ResponseEntity.ok(qaService.getHistory(fileId));
    }
}
