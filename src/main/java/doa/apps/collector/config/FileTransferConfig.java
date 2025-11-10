package doa.apps.collector.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "file-fetcher")
public class FileTransferConfig {
    private long checkIntervalMs = 60000;
    private int maxFilesPerBatch = 100; // Default value
    private List<TransferPair> transferPairs = new ArrayList<>();

    @Data
    public static class TransferPair {
        private String source;
        private String target;
        private String archive;
        private String filePattern = "*.txt";
    }
}