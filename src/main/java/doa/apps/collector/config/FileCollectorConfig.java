package doa.apps.collector.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "parserfile-collector")
public class FileCollectorConfig {
    private long checkIntervalMs = 60000;
    private int maxDays = 5; // Replaces maxFilesPerBatch for age filtering
    private List<CollectorPair> collectors = new ArrayList<>();

    @Data
    public static class CollectorPair {
        private String source;
        private String target;
        private String archive;
        private String filePattern = "*.txt";
    }
}