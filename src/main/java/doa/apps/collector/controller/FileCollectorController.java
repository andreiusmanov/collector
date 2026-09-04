package doa.apps.collector.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import doa.apps.collector.service.FileCollectorService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/collector")
@RequiredArgsConstructor
public class FileCollectorController {

    private final FileCollectorService fileCollectorService;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        return ResponseEntity.ok(fileCollectorService.getHealthStatus());
    }

    @PostMapping("/enable")
    public ResponseEntity<String> enableService() {
        fileCollectorService.enableService();
        return ResponseEntity.ok("File collector service enabled");
    }

    @PostMapping("/disable") 
    public ResponseEntity<String> disableService() {
        fileCollectorService.disableService();
        return ResponseEntity.ok("File collector service disabled");
    }

    @PostMapping("/trigger")
    public ResponseEntity<String> triggerCollector() {
        fileCollectorService.triggerManualCollection();
        return ResponseEntity.ok("Manual collector triggered");
    }

    @PostMapping("/reset-stats")
    public ResponseEntity<String> resetStatistics() {
        fileCollectorService.resetStatistics();
        return ResponseEntity.ok("Statistics reset");
    }
}