package se.swedenconnect.ca.cmcclient.configuration.profile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import se.swedenconnect.ca.cmcclient.ca.profiles.*;

import java.util.List;
import java.util.Map;

/**
 * Description
 *
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
 */
@Configuration
@ConfigurationProperties(prefix = "ca-client.certificate-profiles")
@Data
public class CertificateProfileProperties {

  Profile defaultValue;
  Map<String, Profile> profile;


  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class Profile {
    private List<AttrReqParameter> requestAttributes;
    private List<SubjectAlltNameReqParameter> requestSubjAltNames;
    private List<EKUReqParameter> requestEku;
    private List<OtherReqParameters> requestOther;
    private Map<String, String> requestFixedValue;

    private Boolean includeAki;
    private Boolean inlcudeSki;
    private Boolean includeCrlDp;
    private Boolean includeOcspUrl;
    private List<String> policy;
    private Boolean anyPolicy;
    private Boolean policyCritical;
    private List<EKUReqParameter> eku;
    private Boolean ekuCritical;
    private Boolean ca;
    private Boolean bcCritical;
    private List<KeyUsageType> keyUsages;
    private Boolean keyUsageCritical;
    private Boolean subjAltNameCritical;
  }


  public enum KeyUsageType{
    sign, encrypt, nr, ca
  }

}
