package poc.globo.globostreaming;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import poc.globo.globostreaming.config.TestConfig;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
class GlobostreamingApplicationTests {

	@Test
	void contextLoads() {
	}

}
