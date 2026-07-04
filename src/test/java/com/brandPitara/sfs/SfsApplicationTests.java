package com.brandPitara.sfs;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = SfsApplicationTests.TestApplication.class)
@ActiveProfiles("test")
class SfsApplicationTests {

	@Test
	void contextLoads() {
	}

	@SpringBootConfiguration
	static class TestApplication {
	}

}
