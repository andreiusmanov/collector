package doa.apps.collector.service;

import doa.apps.collector.config.FileTransferConfig;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class FileTransferService {

    private final FileTransferConfig config;
    private final ApplicationEventPublisher eventPublisher;
    private final AtomicBoolean enabled = new AtomicBoolean(true);
    private LocalDateTime lastRun;
    private int totalProcessed = 0;
    private int totalErrors = 0;

    @PostConstruct
    public void init() {
        log.info("Transfer pairs configured: {}, Max files per batch: {}",
                config.getTransferPairs() != null ? config.getTransferPairs().size() : "null",
                config.getMaxFilesPerBatch());
    }

    public FileTransferService(FileTransferConfig config, ApplicationEventPublisher eventPublisher) {
        this.config = config;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(fixedDelayString = "${file-fetcher.check-interval-ms}")
    public void transferAllFiles() {
        if (!enabled.get()) {
            log.debug("File transfer service is disabled");
            return;
        }

        log.info("Starting file transfer cycle for {} pairs (max {} files per batch)",
                config.getTransferPairs().size(), config.getMaxFilesPerBatch());
        lastRun = LocalDateTime.now();

        int cycleProcessed = 0;
        int cycleErrors = 0;

        for (FileTransferConfig.TransferPair pair : config.getTransferPairs()) {
            TransferResult result = transferFilesForPair(pair);
            cycleProcessed += result.processedCount;
            cycleErrors += result.errorCount;
        }

        totalProcessed += cycleProcessed;
        totalErrors += cycleErrors;

        log.info("File transfer cycle completed. Processed: {}, errors: {}", cycleProcessed, cycleErrors);
    }

    // REST Control Methods

    public Map<String, Object> getHealthStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("serviceEnabled", enabled.get());
        status.put("lastRun", lastRun);
        status.put("totalProcessed", totalProcessed);
        status.put("totalErrors", totalErrors);
        status.put("transferPairsCount", config.getTransferPairs().size());
        status.put("checkIntervalMs", config.getCheckIntervalMs());
        status.put("maxFilesPerBatch", config.getMaxFilesPerBatch());

        // Check source directory accessibility
        Map<String, Boolean> sourceAccess = new HashMap<>();
        for (FileTransferConfig.TransferPair pair : config.getTransferPairs()) {
            sourceAccess.put(pair.getSource(), Files.exists(Paths.get(pair.getSource())));
        }
        status.put("sourceAccessibility", sourceAccess);

        return status;
    }

    public void enableService() {
        enabled.set(true);
        log.info("File transfer service enabled");
    }

    public void disableService() {
        enabled.set(false);
        log.info("File transfer service disabled");
    }

    public void triggerManualTransfer() {
        log.info("Manual transfer triggered");
        transferAllFiles();
    }

    public void resetStatistics() {
        totalProcessed = 0;
        totalErrors = 0;
        log.info("Statistics reset");
    }

    // Updated file transfer method with batch size limit
    private TransferResult transferFilesForPair(FileTransferConfig.TransferPair pair) {
        int processedCount = 0;
        int errorCount = 0;

        try {
            Path sourcePath = Paths.get(pair.getSource());
            Path targetPath = Paths.get(pair.getTarget());
            Path archivePath = Paths.get(pair.getArchive());

            Files.createDirectories(targetPath);
            Files.createDirectories(archivePath);

            // Collect matching files first
            List<Path> matchingFiles = new ArrayList<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(sourcePath, pair.getFilePattern())) {
                for (Path sourceFile : stream) {
                    if (Files.isRegularFile(sourceFile) && !Files.isHidden(sourceFile)) {
                        matchingFiles.add(sourceFile);
                    }
                }
            }

            log.debug("Found {} matching files in {}, processing up to {}",
                    matchingFiles.size(), pair.getSource(), config.getMaxFilesPerBatch());

            // Process files up to the batch limit
            int filesToProcess = Math.min(matchingFiles.size(), config.getMaxFilesPerBatch());
            for (int i = 0; i < filesToProcess; i++) {
                Path sourceFile = matchingFiles.get(i);
                try {
                    Path targetFile = targetPath.resolve(sourceFile.getFileName());
                    Path archiveFile = archivePath.resolve(sourceFile.getFileName());

                    Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
                    Files.move(sourceFile, archiveFile, StandardCopyOption.REPLACE_EXISTING);

                    processedCount++;
                    log.debug("Transferred: {} → {}", sourceFile.getFileName(), targetFile);

                } catch (Exception e) {
                    log.error("Failed to process {}: {}", sourceFile.getFileName(), e.getMessage());
                    errorCount++;
                }
            }

            if (matchingFiles.size() > config.getMaxFilesPerBatch()) {
                log.info("Batch limit reached for {}. {} files remaining for next cycle",
                        pair.getSource(), matchingFiles.size() - config.getMaxFilesPerBatch());
            }

        } catch (IOException e) {
            log.error("Failed to access directories for pair {}: {}", pair.getSource(), e.getMessage());
            errorCount++;
        }

        return new TransferResult(processedCount, errorCount);
    }

    private record TransferResult(int processedCount, int errorCount) {
    }
}