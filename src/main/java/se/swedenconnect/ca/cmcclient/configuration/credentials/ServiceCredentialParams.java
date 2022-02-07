package se.swedenconnect.ca.cmcclient.configuration.credentials;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Parameters provided as configuration data for each service credential.
 *
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServiceCredentialParams {

  private Credentialtype credentialtype;
  private String alias;
  private String password;
  private String certLocation;
  private String keyLocation;

}
