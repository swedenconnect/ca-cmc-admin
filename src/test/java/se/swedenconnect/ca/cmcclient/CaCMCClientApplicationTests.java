package se.swedenconnect.ca.cmcclient;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(locations = "classpath:application-test.properties")
@SpringBootTest
@ActiveProfiles("base")
class CaCMCClientApplicationTests {

  @Test
  void contextLoads() {
  }

}
