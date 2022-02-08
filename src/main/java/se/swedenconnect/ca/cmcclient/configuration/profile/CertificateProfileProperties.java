/*
 * Copyright (c) 2022.  Agency for Digital Government (DIGG)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
 * Properties data class for defining certificate profiles.
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
    private Boolean includeSki;
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
