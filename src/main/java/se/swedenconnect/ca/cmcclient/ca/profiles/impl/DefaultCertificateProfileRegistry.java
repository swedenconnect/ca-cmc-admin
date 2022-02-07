package se.swedenconnect.ca.cmcclient.ca.profiles.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import se.swedenconnect.ca.cmcclient.ca.profiles.*;
import se.swedenconnect.ca.cmcclient.ca.profiles.impl.CACertificateProfile;
import se.swedenconnect.ca.cmcclient.ca.profiles.impl.DefaultClientCertProfile;
import se.swedenconnect.ca.cmcclient.ca.profiles.impl.TLSClientCertProfile;
import se.swedenconnect.ca.cmcclient.configuration.profile.CertificateProfileProperties;

import java.util.*;

/**
 * Description
 *
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
 */
@Slf4j
@Component
@Profile("base")
public class DefaultCertificateProfileRegistry implements CertificateProfileRegistry {

  private Map<String, CertificateProfile> certificateProfileMap;

  private static final Map<String, String> fixedSECountryValues;

  static {
    fixedSECountryValues = new HashMap<>();
    fixedSECountryValues.put(AttrReqParameter.country.name(), "SE");
  }

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


  /**
   * Get the current certificate profile from the registry
   *
   * @return map of certificate profiles
   */
  @Override public Map<String, CertificateProfile> getCertificateProfileMap() {
    return certificateProfileMap;
  }

  @Override public void registerCertificateProfile(String name, CertificateProfile certificateProfile) {
    log.info("Registered certificate profile {} implementing {}", name, certificateProfile == null ? "null" : certificateProfile.getClass());
    certificateProfileMap.put(name, certificateProfile);
  }

}
