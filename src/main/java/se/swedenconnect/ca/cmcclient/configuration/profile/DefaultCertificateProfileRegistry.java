/*
 * Copyright (c) 2022-2023.  Agency for Digital Government (DIGG)
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import se.swedenconnect.ca.cmcclient.ca.profiles.CertificateProfile;
import se.swedenconnect.ca.cmcclient.ca.profiles.CertificateProfileRegistry;
import se.swedenconnect.ca.cmcclient.ca.profiles.impl.PropertyBasedCertificateProfile;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Provides a certificate profile registry. This registry can be loaded as a bean and extended with new registered certificate profiles.
 * A registered certificate profile defines the content of certificates issued according to this profile.
 *
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
 */
@Slf4j
@Component
public class DefaultCertificateProfileRegistry implements CertificateProfileRegistry {

  private final Map<String, CertificateProfile> certificateProfileMap;

  @Autowired
  public DefaultCertificateProfileRegistry(Map<String, CertificateProfileProperties.Profile> propertyProfileDataMap)
    throws JsonProcessingException {
    certificateProfileMap = new HashMap<>();
    registerCertificateProfile("null", null);
    final Set<String> propProfileNames = propertyProfileDataMap.keySet();
    for (String propProfileName : propProfileNames){
      registerCertificateProfile(propProfileName, new PropertyBasedCertificateProfile(propertyProfileDataMap.get(propProfileName),"cert-request"));
      log.info("Registered certificate profile: {}", propProfileName);
      if (log.isDebugEnabled()){
        ObjectMapper objectMapper = new ObjectMapper();
        log.debug("Profile configuration: {}", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(propertyProfileDataMap.get(propProfileName)));
      }
    }
  }

  /** {@inheritDoc} */
  @Override public Map<String, CertificateProfile> getCertificateProfileMap() {
    return certificateProfileMap;
  }

  /** {@inheritDoc} */
  @Override public void registerCertificateProfile(String name, CertificateProfile certificateProfile) {
    log.info("Registered certificate profile {} implementing {}", name, certificateProfile == null ? "null" : certificateProfile.getClass());
    certificateProfileMap.put(name, certificateProfile);
  }

}
