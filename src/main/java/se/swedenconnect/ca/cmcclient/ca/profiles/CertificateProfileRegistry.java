package se.swedenconnect.ca.cmcclient.ca.profiles;

import se.swedenconnect.ca.cmcclient.configuration.profile.CertificateProfileProperties;

import java.util.Map;

/**
 * Description
 *
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
 */
public interface CertificateProfileRegistry {

  /**
   * Get the current certificate profile from the registry
   * @return map of certificate profiles
   */
  Map<String, CertificateProfile> getCertificateProfileMap();

  /**
   * Add a new certificate profile to the registry
   * @param name name of the certificate profile
   * @param certificateProfile certificate profile
   */
  void registerCertificateProfile(String name, CertificateProfile certificateProfile);



}
