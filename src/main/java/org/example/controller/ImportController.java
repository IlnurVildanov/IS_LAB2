package org.example.controller;

import org.example.dto.ImportHistoryDTO;
import org.example.dto.ImportProgressDTO;
import org.example.entity.ImportHistory;
import org.example.service.ImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/import")
@CrossOrigin(origins = "*")
public class ImportController {

    @Autowired
    private ImportService importService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "userName", defaultValue = "user") String userName,
            @RequestParam(value = "isAdmin", defaultValue = "false") Boolean isAdmin) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || (!fileName.toLowerCase().endsWith(".csv") && !fileName.toLowerCase().endsWith(".json"))) {
            return ResponseEntity.badRequest().body(Map.of("error", "File must be CSV or JSON"));
        }

        try {
            ImportHistory history = importService.importFileAsync(file, userName, isAdmin);

            Map<String, Object> response = new HashMap<>();
            response.put("importId", history.getId());
            response.put("fileName", fileName);
            response.put("status", "IN_PROGRESS");
            response.put("message", "Import started");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/upload-multiple")
    public ResponseEntity<?> uploadMultipleFiles(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "userName", defaultValue = "user") String userName,
            @RequestParam(value = "isAdmin", defaultValue = "false") Boolean isAdmin) {

        if (files.length == 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "No files provided"));
        }

        if (files.length > 5) {
            return ResponseEntity.badRequest().body(Map.of("error", "Maximum 5 files allowed"));
        }

        List<Map<String, Object>> imports = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;

            String fileName = file.getOriginalFilename();
            if (fileName == null || (!fileName.toLowerCase().endsWith(".csv") && !fileName.toLowerCase().endsWith(".json"))) {
                continue;
            }

            try {
                ImportHistory history = importService.importFileAsync(file, userName, isAdmin);

                Map<String, Object> importInfo = new HashMap<>();
                importInfo.put("importId", history.getId());
                importInfo.put("fileName", fileName);
                importInfo.put("status", "IN_PROGRESS");
                importInfo.put("message", "Import started");
                imports.add(importInfo);
            } catch (Exception e) {
                Map<String, Object> importInfo = new HashMap<>();
                importInfo.put("fileName", fileName);
                importInfo.put("status", "FAILED");
                importInfo.put("error", e.getMessage());
                imports.add(importInfo);
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("imports", imports);
        response.put("message", "Imports started");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/progress/{importId}")
    public ResponseEntity<ImportProgressDTO> getProgress(@PathVariable Long importId) {
        ImportProgressDTO progress = importService.getProgress(importId);
        if (progress == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(progress);
    }

    @GetMapping("/history")
    public ResponseEntity<List<ImportHistoryDTO>> getImportHistory(
            @RequestParam(value = "userName", defaultValue = "user") String userName) {
        List<ImportHistoryDTO> history = importService.getImportHistory(userName);
        return ResponseEntity.ok(history);
    }

    @DeleteMapping("/history")
    public ResponseEntity<?> clearImportHistory(
            @RequestParam(value = "userName", defaultValue = "user") String userName) {
        try {
            importService.clearImportHistory(userName);
            return ResponseEntity.ok(Map.of("message", "Import history cleared successfully"));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Only admin can clear import history"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}