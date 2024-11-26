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

package se.swedenconnect.ca.cmcclient.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ResourceUtils;
import se.swedenconnect.ca.cmc.api.client.CMCClient;
import se.swedenconnect.ca.cmc.api.client.impl.DefaultCMCClient;
import se.swedenconnect.ca.cmcclient.ca.CaRepositoryCollector;
import se.swedenconnect.ca.cmcclient.configuration.cmc.CMCInstanceParams;
import se.swedenconnect.ca.cmcclient.configuration.cmc.CMCProperties;
import se.swedenconnect.ca.cmcclient.configuration.credentials.ServiceCredential;
import se.swedenconnect.ca.cmcclient.http.GenericHttpConnector;
import se.swedenconnect.ca.cmcclient.utils.CertificateUtils;
import se.swedenconnect.security.credential.BasicCredential;
import se.swedenconnect.security.credential.PkiCredential;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Creating Beans related to TLS processing and cross certification
 *
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
 */
@Slf4j
@Configuration
public class CAClientServiceConfiguration {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Bean
  public Map<String, CMCClient> cmcClientMap(Map<ServiceCredential, PkiCredential> serviceCredentials,
    CMCProperties cmcProperties)
    throws FileNotFoundException, CertificateException, MalformedURLException, NoSuchAlgorithmException,
    OperatorCreationException,
    JsonProcessingException {
    log.info("Setting up CMC clients for CA instances:");
    Map<String, CMCClient> cmcClientMap = new HashMap<>();
    if (cmcProperties.getInstance() == null || cmcProperties.getInstance().isEmpty()) {
      log.warn("No CA instances found in the CMC client configuration.");
      return cmcClientMap;
    }
    final String cmcAlgorithm = cmcProperties.getAlgorithm();
    final Set<String> instanceIdList = cmcProperties.getInstance().keySet();
    for (String instanceId : instanceIdList) {
      log.info("Adding CMC client for instance: {}", instanceId);
      final CMCInstanceParams cmcInstanceParams = cmcProperties.getInstance().get(instanceId);
      X509Certificate responseCert = CertificateUtils.decodeCertificate(
        new FileInputStream(ResourceUtils.getFile(cmcInstanceParams.getResponseCertificateLocation())));
      X509Certificate caCert = CertificateUtils.decodeCertificate(
        new FileInputStream(ResourceUtils.getFile(cmcInstanceParams.getCaCertificateLocation())));
      PkiCredential cmcClientCredential = new BasicCredential(
        serviceCredentials.get(ServiceCredential.cmc).getCertificate(),
        serviceCredentials.get(ServiceCredential.cmc).getPrivateKey());
      CMCClient cmcClient = new DefaultCMCClient(
        cmcInstanceParams.getRequestUrl(),
        cmcClientCredential,
        cmcAlgorithm,
        responseCert, caCert
      );
      cmcClientMap.put(instanceId, cmcClient);
      if (log.isDebugEnabled()) {
        log.debug("CMC instance configuration: {}",
          OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(cmcInstanceParams));
      }
    }
    return cmcClientMap;
  }

  @Bean CaRepositoryCollector caRepositoryCollector(
    @Value("${ca-client.config.http.connectTimeout}") int connectTimeout,
    @Value("${ca-client.config.http.readTimeout}") int readTimeout
  ) {
    return new CaRepositoryCollector(new GenericHttpConnector(connectTimeout, readTimeout));
  }

  @Bean
  Map<String, EmbeddedLogo> logoMap(
    ResourceLoader resourceLoader,
    @Value("${ca-client.config.logo}") String logoLocation,
    @Value("${ca-client.config.icon}") String iconLocation
  ) throws Exception {
    log.info("Service logo obtained from: {}", logoLocation);
    log.info("Icon logo obtained from {}", iconLocation);
    Map<String, EmbeddedLogo> logoMap = new HashMap<>();
    logoMap.put("logo", new EmbeddedLogo(logoLocation, resourceLoader));
    logoMap.put("icon", new EmbeddedLogo(iconLocation, resourceLoader));
    return logoMap;
  }

  @Bean(name = "BasicServiceConfig")
  BasicServiceConfig basicServiceConfig(
    @Value("${ca-client.config.data-directory}") String configLocation,
    @Value("${ca-client.config.base-url}") String serviceBaseUrl,
    @Value("${server.servlet.context-path:#{null}}") String serviceContextPath
  ) {
    Security.insertProviderAt(new BouncyCastleProvider(), 1);
    log.info("Available crypto providers: {}", String.join(",", Arrays.stream(Security.getProviders())
      .map(Provider::getName)
      .collect(Collectors.toList())));
    final Provider bcProvider = Security.getProvider("BC");
    log.info("Bouncycastle version: {}", bcProvider.getVersionStr());
    log.info("JRE Path: {}", System.getProperty("java.home"));

    BasicServiceConfig basicServiceConfig = new BasicServiceConfig();
    if (StringUtils.isNotBlank(configLocation)) {
      basicServiceConfig.setDataStoreLocation(new File(
        configLocation.endsWith("/")
          ? configLocation.substring(0, configLocation.length() - 1)
          : configLocation
      ));
    }
    else {
      basicServiceConfig.setDataStoreLocation(new File(System.getProperty("user.dir"), "target/temp/ca-config"));
      if (!basicServiceConfig.getDataStoreLocation().exists()) {
        basicServiceConfig.getDataStoreLocation().mkdirs();
      }
    }
    basicServiceConfig.setServiceUrl(serviceContextPath == null
      ? serviceBaseUrl
      : serviceBaseUrl + serviceContextPath);
    basicServiceConfig.setServiceHostUrl(serviceBaseUrl);
    log.info("Service URL: {}", basicServiceConfig.getServiceUrl());
    log.info("Service storage location: {}", basicServiceConfig.getDataStoreLocation());
    return basicServiceConfig;
  }

}
