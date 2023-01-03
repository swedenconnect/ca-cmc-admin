/*
 * Copyright (c) 2021-2023.  Agency for Digital Government (DIGG)
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

package se.swedenconnect.ca.cmcclient.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.cert.X509CertificateHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import se.idsec.utils.printcert.PrintCertificate;
import se.swedenconnect.ca.cmc.CMCException;
import se.swedenconnect.ca.cmc.api.client.CMCClient;
import se.swedenconnect.ca.cmc.api.data.CMCResponse;
import se.swedenconnect.ca.cmc.model.admin.response.CAInformation;
import se.swedenconnect.ca.cmcclient.authz.CurrentUser;
import se.swedenconnect.ca.cmcclient.ca.PublicKeyValidator;
import se.swedenconnect.ca.cmcclient.ca.request.RequestData;
import se.swedenconnect.ca.cmcclient.ca.request.RequestDataResult;
import se.swedenconnect.ca.cmcclient.data.CertContentDisplayData;
import se.swedenconnect.ca.cmcclient.utils.CAServiceUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.math.BigInteger;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;

/**
 * Rest controller handling Ajax requests from web pages
 *
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
 */
@Slf4j
@RestController
public class AjaxController {

  private static final ObjectMapper objectMapper = new ObjectMapper();
  private final PublicKeyValidator publicKeyValidator;
  private final Map<String, CMCClient> cmcClientMap;
  @Value("${ca-client.config.verbose-cert-print}") boolean verboseCertPrint;

  @Autowired
  public AjaxController(PublicKeyValidator publicKeyValidator, Map<String, CMCClient> cmcClientMap) {
    this.publicKeyValidator = publicKeyValidator;
    this.cmcClientMap = cmcClientMap;
  }

  @RequestMapping("/getCertData")
  public String getCertContentData(HttpServletRequest servletRequest, @RequestParam("serialNumber") String serialNumberHexStr,
    @RequestParam("instance") String instance, Authentication authentication)
    throws CertificateException, IOException, CMCException {

    // Get and validate CA service
    if (!cmcClientMap.containsKey(instance)) {
      log.debug("Request for illegal instance - aborting ajax request and returning null");
      return null;
    }
    final CMCClient cmcClient = cmcClientMap.get(instance);

    // Get current user and enforce authorization
    CurrentUser currentUser = new CurrentUser(authentication);
    if (!currentUser.isAuthorizedFor(instance)){
      log.debug("User not authorized - abort ajax request and return null");
      return null;
    }

    final CMCResponse cmcResponse = cmcClient.getIssuedCertificate(new BigInteger(serialNumberHexStr, 16));
    final List<X509Certificate> returnCertificates = cmcResponse.getReturnCertificates();

    if (returnCertificates == null || returnCertificates.size() != 1) {
      log.debug("Ajax request for certificate - Certificate not found");
      return "";
    }

    PrintCertificate printCertificate = new PrintCertificate(returnCertificates.get(0));
    CertContentDisplayData displayData = new CertContentDisplayData();
    displayData.setCertHtml(printCertificate.toHtml(verboseCertPrint));
    displayData.setPem(printCertificate.toPEM().replaceAll("\n", "<br>"));
    return objectMapper.writeValueAsString(displayData);
  }

  @RequestMapping("/getChainCertData")
  public String getCaChainCert(HttpServletRequest servletRequest, @RequestParam("idx") int idx,
    @RequestParam("instance") String instance, Authentication authentication)
    throws IOException, CertificateException, CMCException {

    // Get and validate CA service
    if (!cmcClientMap.containsKey(instance)) {
      log.debug("Request for illegal instance - aborting ajax request and returning null");
      return null;
    }

    // Get current user and enforce authorization
    CurrentUser currentUser = new CurrentUser(authentication);
    if (!currentUser.isAuthorizedFor(instance)){
      log.debug("User not authorized - abort ajax request and return null");
      return null;
    }

    final CMCClient cmcClient = cmcClientMap.get(instance);
    final CAInformation caInformation = cmcClient.getCAInformation(true);

    byte[] targetCertificate;
    if (idx == -1){
      try {
        targetCertificate = caInformation.getOcspCertificate();
      } catch (Exception ex) {
        log.debug("Ajax request for certificate - Certificate not found {}", ex.getMessage());
        return "";
      }
    } else {
      final List<X509CertificateHolder> caChain = CAServiceUtils.getCaChain(caInformation);
      if ((idx + 1) > caChain.size() || idx < 0) {
        log.debug("Ajax request for certificate - Certificate not found");
        return "";
      }
      targetCertificate = caChain.get(idx).getEncoded();
    }

    PrintCertificate printCertificate = new PrintCertificate(targetCertificate);
    CertContentDisplayData displayData = new CertContentDisplayData();
    displayData.setCertHtml(printCertificate.toHtml(verboseCertPrint));
    displayData.setPem(printCertificate.toPEM().replaceAll("\n", "<br>"));
    return objectMapper.writeValueAsString(displayData);
  }

}
