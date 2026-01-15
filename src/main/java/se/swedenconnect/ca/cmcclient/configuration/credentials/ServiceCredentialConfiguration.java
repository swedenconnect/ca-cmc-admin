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
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ResourceUtils;
import se.swedenconnect.ca.cmcclient.configuration.LenientResourceResolver;
import se.swedenconnect.ca.cmcclient.utils.CertificateUtils;
import se.swedenconnect.security.credential.BasicCredential;
import se.swedenconnect.security.credential.KeyStoreCredential;
import se.swedenconnect.security.credential.PkiCredential;
import se.swedenconnect.security.credential.pkcs11.FilePkcs11Configuration;
import se.swedenconnect.security.credential.pkcs11.Pkcs11Configuration;
import se.swedenconnect.security.credential.pkcs11.Pkcs11Credential;
import se.swedenconnect.security.credential.pkcs11.SunPkcs11CertificatesAccessor;
import se.swedenconnect.security.credential.pkcs11.SunPkcs11PrivateKeyAccessor;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
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
  public Map<ServiceCredential, PkiCredential> serviceCredentialMap(KeyCredentialProperties keyCredentialProperties, LenientResourceResolver resourceLoader) throws Exception {
    log.info("Setting up service credential configuration");
    Map<ServiceCredential, PkiCredential> serviceCredentialMap = new EnumMap<>(ServiceCredential.class);

    final Map<ServiceCredential, ServiceCredentialParams> credentialPropMap = keyCredentialProperties.getCredentials();
    if (credentialPropMap == null || credentialPropMap.isEmpty()) {
      return serviceCredentialMap;
    }

    if (credentialPropMap.containsKey(ServiceCredential.service)) {
      serviceCredentialMap.put(ServiceCredential.service, getCredential(credentialPropMap.get(ServiceCredential.service), keyCredentialProperties, resourceLoader));
      log.info("Added key credential for Service signing");
    }
    if (credentialPropMap.containsKey(ServiceCredential.cmc)) {
      serviceCredentialMap.put(ServiceCredential.cmc, getCredential(credentialPropMap.get(ServiceCredential.cmc), keyCredentialProperties, resourceLoader));
      log.info("Added key credential for CMC signing");
    }
    return serviceCredentialMap;
  }

  private PkiCredential getCredential(ServiceCredentialParams serviceCredentialParams, KeyCredentialProperties keyCredentialProperties, LenientResourceResolver resourceLoader) throws Exception {

    final Credentialtype credentialtype = serviceCredentialParams.getCredentialtype();
    final InputStream keyResourceIs = resourceLoader.getResource(serviceCredentialParams.getKeyLocation()).getInputStream();
    final char[] password = serviceCredentialParams.getPassword().toCharArray();
    final String alias = serviceCredentialParams.getAlias();
    KeyStore keyStore = null;

    switch (credentialtype) {
    case jks:
      keyStore = KeyStore.getInstance("JKS");
      keyStore.load(keyResourceIs, password);
      return new KeyStoreCredential(keyStore, alias, password);
    case pkcs12:
      keyStore = KeyStore.getInstance("PKCS12");
      keyStore.load(keyResourceIs, password);
      return new KeyStoreCredential(keyStore, alias, password);
    case pkcs11:
      final Pkcs11Credential p11Credential =
          getPkcs11Credential(keyCredentialProperties, alias, password);
      log.info("Created PKCS11 credential");
      return p11Credential;
    case pem:
      PEMKey pemKey = new PEMKey(resourceLoader.getResource(serviceCredentialParams.getKeyLocation()),
        serviceCredentialParams.getPassword());
      BasicCredential pemCredential = new BasicCredential(
        CertificateUtils.decodeCertificate(new FileInputStream(serviceCredentialParams.getCertLocation())),
        pemKey.privateKey
      );
      log.info("Created PEM credential");
      return pemCredential;
    }
    throw new IOException("Unable to create credential");
  }

  private static Pkcs11Credential getPkcs11Credential(final KeyCredentialProperties keyCredentialProperties,
      final String alias, final char[] password) {
    final String pkcs11configLocation = keyCredentialProperties.getPkcs11configLocation();
    if (pkcs11configLocation == null) {
      throw new IllegalArgumentException("PKCS11 provider must be set for PKCS 11 key sources");
    }
    Pkcs11Configuration pkcs11Configuration = new FilePkcs11Configuration(pkcs11configLocation);
    return new Pkcs11Credential(
        pkcs11Configuration, alias, password,
        new SunPkcs11PrivateKeyAccessor(), new SunPkcs11CertificatesAccessor());
  }

}
