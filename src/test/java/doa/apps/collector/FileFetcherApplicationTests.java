package doa.apps.collector;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "file-fetcher.check-interval-ms=5000")
class FileCollectorApplicationTests {

	@Test
	void contextLoads() {
	}

}
