package doa.apps.collector.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import doa.apps.collector.service.FileTransferService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/transfer")
@RequiredArgsConstructor
public class FileTransferController {

    private final FileTransferService fileTransferService;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        return ResponseEntity.ok(fileTransferService.getHealthStatus());
    }

    @PostMapping("/enable")
    public ResponseEntity<String> enableService() {
        fileTransferService.enableService();
        return ResponseEntity.ok("File transfer service enabled");
    }

    @PostMapping("/disable") 
    public ResponseEntity<String> disableService() {
        fileTransferService.disableService();
        return ResponseEntity.ok("File transfer service disabled");
    }

    @PostMapping("/trigger")
    public ResponseEntity<String> triggerTransfer() {
        fileTransferService.triggerManualTransfer();
        return ResponseEntity.ok("Manual transfer triggered");
    }

    @PostMapping("/reset-stats")
    public ResponseEntity<String> resetStatistics() {
        fileTransferService.resetStatistics();
        return ResponseEntity.ok("Statistics reset");
    }
}