package se.swedenconnect.ca.cmcclient.configuration.cmc;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Properties configuration for CMC connections to configured CA instances.
 *
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
 */
@Configuration
@ConfigurationProperties(prefix = "ca-client.config.cmc")
@Data
public class CMCProperties {

  String algorithm;
  Map<String, CMCInstanceParams> instance;

}
