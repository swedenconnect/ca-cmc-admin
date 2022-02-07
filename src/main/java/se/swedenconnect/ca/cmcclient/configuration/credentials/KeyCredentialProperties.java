package se.swedenconnect.ca.cmcclient.configuration.credentials;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Properties configuration for key sources
 *
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
 */
@Configuration
@ConfigurationProperties(prefix = "ca-client.config.keys")
@Data
public class KeyCredentialProperties {

  private String pkcs11configLocation;
  private Map<ServiceCredential, ServiceCredentialParams> credentials;

}
