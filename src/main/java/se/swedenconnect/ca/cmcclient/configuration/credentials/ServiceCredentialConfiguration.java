/*
 * Copyright 2024.  Agency for Digital Government (DIGG)
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

package se.swedenconnect.ca.cmcclient.configuration.credentials;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.ResourceUtils;
import se.swedenconnect.ca.cmcclient.utils.CertificateUtils;
import se.swedenconnect.security.credential.BasicCredential;
import se.swedenconnect.security.credential.KeyStoreCredential;
import se.swedenconnect.security.credential.PkiCredential;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.Provider;
import java.security.Security;
import java.util.EnumMap;
import java.util.Map;

/**
 * Bean for providing credentials for enumerated services
 *
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
 */
@Configuration
@Slf4j
public class ServiceCredentialConfiguration {

  @Bean
  public Map<ServiceCredential, PkiCredential> serviceCredentialMap(KeyCredentialProperties keyCredentialProperties) throws Exception {
    log.info("Setting up service credential configuration");
    Map<ServiceCredential, PkiCredential> serviceCredentialMap = new EnumMap<>(ServiceCredential.class);

    final Map<ServiceCredential, ServiceCredentialParams> credentialPropMap = keyCredentialProperties.getCredentials();
    if (credentialPropMap == null || credentialPropMap.isEmpty()) {
      return serviceCredentialMap;
    }

    Provider pkcs11Provider = null;
    if (StringUtils.isNotBlank(keyCredentialProperties.getPkcs11configLocation())) {
      pkcs11Provider = Security.getProvider("SunPKCS11");
      pkcs11Provider = pkcs11Provider.configure(keyCredentialProperties.getPkcs11configLocation());
      Security.addProvider(pkcs11Provider);
      log.info("Setting up service to use PKCS11 provider {}", pkcs11Provider.getName());
    } else {
      log.info("No PKCS11 provider is configured. No HSM usage");
    }

    if (credentialPropMap.containsKey(ServiceCredential.service)) {
      serviceCredentialMap.put(ServiceCredential.service, getCredential(credentialPropMap.get(ServiceCredential.service), pkcs11Provider));
      log.info("Added key credential for Service signing");
    }
    if (credentialPropMap.containsKey(ServiceCredential.cmc)) {
      serviceCredentialMap.put(ServiceCredential.cmc, getCredential(credentialPropMap.get(ServiceCredential.cmc), pkcs11Provider));
      log.info("Added key credential for CMC signing");
    }
    return serviceCredentialMap;
  }

  private PkiCredential getCredential(ServiceCredentialParams serviceCredentialParams, Provider pkcs11Provider) throws Exception {

    final Credentialtype credentialtype = serviceCredentialParams.getCredentialtype();
    switch (credentialtype) {

    case jks:
    case pkcs12:
      KeyStoreCredential keyStoreCredential = new KeyStoreCredential(
        getResource(serviceCredentialParams.getKeyLocation()),
        credentialtype.name().toUpperCase(),
        serviceCredentialParams.getPassword().toCharArray(),
        serviceCredentialParams.getAlias(),
        serviceCredentialParams.getPassword().toCharArray()
      );
      keyStoreCredential.init();
      log.info("Created Keystore credential");
      return keyStoreCredential;
    case pkcs11:
      KeyStoreCredential p11Credential = new KeyStoreCredential(
        null, "PKCS11", pkcs11Provider.getName(),
        serviceCredentialParams.getPassword().toCharArray(),
        serviceCredentialParams.getAlias(), null
      );
      p11Credential.init();
      log.info("Created PKCS11 credential");
      return p11Credential;
    case pem:
      PEMKey pemKey = new PEMKey(getResource(serviceCredentialParams.getKeyLocation()),
        serviceCredentialParams.getPassword());
      BasicCredential pemCredential = new BasicCredential(
        CertificateUtils.decodeCertificate(new FileInputStream(serviceCredentialParams.getCertLocation())),
        pemKey.privateKey
      );
      pemCredential.init();
      log.info("Created PEM credential");
      return pemCredential;
    }
    throw new IOException("Unable to create credential");
  }

  private Resource getResource(String location) throws FileNotFoundException {
    return new FileSystemResource(ResourceUtils.getFile(location));
  }

}
