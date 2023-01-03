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

package se.swedenconnect.ca.cmcclient.ca.profiles;

import java.util.Map;

/**
 * Interface for providing a certificate profile registry. The Certificate profile registry allows registration of certificate profiles
 * for use withing the application. A service extending this project can load a bean based in this interface to add its own certificate
 * profiles.
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
