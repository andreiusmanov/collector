package doa.apps.collector.service;

import doa.apps.collector.config.FileCollectorConfig;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class FileCollectorService {

    private final FileCollectorConfig config;
    private final ApplicationEventPublisher eventPublisher;
    private final AtomicBoolean enabled = new AtomicBoolean(true);
    private LocalDateTime lastRun;
    private int totalProcessed = 0;
    private int totalErrors = 0;

    @PostConstruct
    public void init() {
        log.info("Collector pairs configured: {}",
                config.getCollectors() != null ? config.getCollectors().size() : "null");
    }

    public FileCollectorService(FileCollectorConfig config, ApplicationEventPublisher eventPublisher) {
        this.config = config;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(fixedDelayString = "${parserfile-collector.check-interval-ms}")
    public void collectAllFiles() {
        if (!enabled.get()) {
            log.debug("File collector service is disabled");
            return;
        }

        log.info("Starting file collection cycle for {} pairs",
                config.getCollectors().size());
        lastRun = LocalDateTime.now();

        int cycleProcessed = 0;
        int cycleErrors = 0;

        for (FileCollectorConfig.CollectorPair pair : config.getCollectors()) {
            CollectionResult result = collectFilesForPair(pair);
            cycleProcessed += result.processedCount;
            cycleErrors += result.errorCount;
        }

        totalProcessed += cycleProcessed;
        totalErrors += cycleErrors;

        log.info("File collection cycle completed. Processed: {}, errors: {}", cycleProcessed, cycleErrors);
    }

    // REST Control Methods

    public Map<String, Object> getHealthStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("serviceEnabled", enabled.get());
        status.put("lastRun", lastRun);
        status.put("totalProcessed", totalProcessed);
        status.put("totalErrors", totalErrors);
        status.put("collectorPairsCount", config.getCollectors().size());
        status.put("checkIntervalMs", config.getCheckIntervalMs());
        status.put("maxDays", config.getMaxDays());

        // Check source directory accessibility
        Map<String, Boolean> sourceAccess = new HashMap<>();
        for (FileCollectorConfig.CollectorPair pair : config.getCollectors()) {
            sourceAccess.put(pair.getSource(), Files.exists(Paths.get(pair.getSource())));
        }
        status.put("sourceAccessibility", sourceAccess);

        return status;
    }

    public void enableService() {
        enabled.set(true);
        log.info("File collector service enabled");
    }

    public void disableService() {
        enabled.set(false);
        log.info("File collector service disabled");
    }

    public void triggerManualCollection() {
        log.info("Manual collection triggered");
        collectAllFiles();
    }

    public void resetStatistics() {
        totalProcessed = 0;
        totalErrors = 0;
        log.info("Statistics reset");
    }

    // File collection method without day age filter
    private CollectionResult collectFilesForPair(FileCollectorConfig.CollectorPair pair) {
        int processedCount = 0;
        int errorCount = 0;

        try {
            Path sourcePath = Paths.get(pair.getSource());
            Path targetPath = Paths.get(pair.getTarget());
            Path archivePath = Paths.get(pair.getArchive());

            Files.createDirectories(targetPath);
            Files.createDirectories(archivePath);

            // Collect matching files (no age limit)
            List<Path> matchingFiles = new ArrayList<>();

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(sourcePath)) {
                for (Path sourceFile : stream) {
                    if (!Files.isRegularFile(sourceFile)) {
                        continue;
                    }

                    if (Files.isHidden(sourceFile)) {
                        continue;
                    }

                    String name = sourceFile.getFileName().toString();
                    if (name.toLowerCase().endsWith(".txt")) {
                        matchingFiles.add(sourceFile);
                    }
                }
            }

            log.debug("Found {} matching files in {}",
                    matchingFiles.size(), pair.getSource());

            // Process all matching files
            for (Path sourceFile : matchingFiles) {
                try {
                    Path targetFile = targetPath.resolve(sourceFile.getFileName());
                    Path archiveFile = archivePath.resolve(sourceFile.getFileName());

                    Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
                    Files.move(sourceFile, archiveFile, StandardCopyOption.REPLACE_EXISTING);

                    processedCount++;
                    log.debug("Collected: {} → {}", sourceFile.getFileName(), targetFile);

                } catch (Exception e) {
                    log.error("Failed to process {}: {}", sourceFile.getFileName(), e.getMessage());
                    errorCount++;
                }
            }

        } catch (IOException e) {
            log.error("Failed to access directories for pair {}: {}", pair.getSource(), e.getMessage());
            errorCount++;
        }

        return new CollectionResult(processedCount, errorCount);
    }

    private record CollectionResult(int processedCount, int errorCount) {
    }
}
