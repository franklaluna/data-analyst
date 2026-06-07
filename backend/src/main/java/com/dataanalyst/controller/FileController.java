package com.dataanalyst.controller;

import com.dataanalyst.service.AnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private static final Logger log = LoggerFactory.getLogger(FileController.class);
    private static final String UPLOAD_DIR = "/tmp/data-analyst-uploads/";

    private final AnalysisService analysisService;

    public FileController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) {
        try {
            File uploadDir = new File(UPLOAD_DIR);
            if (!uploadDir.exists()) uploadDir.mkdirs();

            String rawName = file.getOriginalFilename();
            String filename = Paths.get(rawName != null ? rawName : "upload.dat").getFileName().toString();
            // Strip dangerous characters (path separators, null bytes) but keep CJK, punctuation, spaces
            filename = filename.replaceAll("[/\\\\\0]+", "");
            if (filename.isEmpty() || filename.equals(".") || filename.equals("..")) {
                filename = "upload_" + System.currentTimeMillis() + ".dat";
            }
            String filePath = UPLOAD_DIR + System.currentTimeMillis() + "_" + filename;
            file.transferTo(new File(filePath));

            log.info("Uploaded file: {} -> {}", filename, filePath);

            Map<String, Object> result = analysisService.analyzeFile(filePath, filename);
            result.put("filename", filename);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Upload failed", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listFiles() {
        return ResponseEntity.ok(analysisService.listFiles());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getFile(@PathVariable Long id) {
        Map<String, Object> profile = analysisService.getFileProfile(id);
        if (profile == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(profile);
    }
}
