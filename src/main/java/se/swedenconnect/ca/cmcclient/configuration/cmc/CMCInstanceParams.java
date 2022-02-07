package se.swedenconnect.ca.cmcclient.configuration.cmc;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuration parameters for CMC connection to a CA instance.
 *
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CMCInstanceParams {

  private String requestUrl;
  private String responseCertificateLocation;
  private String caCertificateLocation;
  private String name;
  private String description;
  private Integer index;
  private String[] policy;
  private String profile;
}
