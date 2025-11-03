package br.com.acdev.revenium;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import br.com.acdev.revenium.config.TestcontainersConfiguration;

@SpringBootTest(classes = ReveniumApplication.class)
@Import(TestcontainersConfiguration.class)
class ReveniumApplicationTests {

	@Test
	void contextLoads() {
	}

}
