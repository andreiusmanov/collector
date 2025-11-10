package doa.apps.collector;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FileCollectorApplication {
    public static void main(String[] args) {
        SpringApplication.run(FileCollectorApplication.class, args);
    }
}