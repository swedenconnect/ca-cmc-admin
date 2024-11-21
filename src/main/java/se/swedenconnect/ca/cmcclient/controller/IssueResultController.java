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

package se.swedenconnect.ca.cmcclient.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import se.idsec.utils.printcert.PrintCertificate;
import se.swedenconnect.ca.cmc.CMCException;
import se.swedenconnect.ca.cmc.api.client.CMCClient;
import se.swedenconnect.ca.cmc.api.data.CMCResponse;
import se.swedenconnect.ca.cmcclient.authz.CurrentUser;
import se.swedenconnect.ca.cmcclient.configuration.EmbeddedLogo;
import se.swedenconnect.ca.cmcclient.configuration.HtmlServiceInfo;
import se.swedenconnect.ca.cmcclient.configuration.cmc.CMCProperties;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigInteger;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;

/**
 * Controller for issue result display
 *
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
 */
@Slf4j
@Controller
public class IssueResultController {

  private final Map<String, CMCClient> cmcClientMap;
  private final CMCProperties cmcProperties;
  private final Map<String, EmbeddedLogo> logoMap;
  private final HttpSession httpSession;
  private final HtmlServiceInfo htmlServiceInfo;
  @Value("${ca-client.config.bootstrap-css}") String bootstrapCss;

  @Autowired
  public IssueResultController(Map<String, CMCClient> cmcClientMap,
    CMCProperties cmcProperties, Map<String, EmbeddedLogo> logoMap, HttpSession httpSession,
    HtmlServiceInfo htmlServiceInfo) {
    this.cmcClientMap = cmcClientMap;
    this.cmcProperties = cmcProperties;
    this.logoMap = logoMap;
    this.httpSession = httpSession;
    this.htmlServiceInfo = htmlServiceInfo;
  }

  @RequestMapping("/issue-result")
  public String getIssuerResultPage(HttpServletRequest servletRequest, Model model, Authentication authentication,
    @RequestParam("instance") String instance) throws CertificateException, IOException, CMCException {

    // Get current user
    CurrentUser currentUser = new CurrentUser(authentication);
    model.addAttribute("currentUser", currentUser);

    if (!cmcClientMap.containsKey(instance) || !currentUser.isAuthorizedFor(instance)) {
      log.debug(!cmcClientMap.containsKey(instance)
        ? "Request to non existent CA instance - redirect to no-found"
        :"User not authorized - redirect to no-found");
      return "redirect:not-found";
    }

    BigInteger certSerial = (BigInteger) httpSession.getAttribute("certSerialNumber-" + instance);
    if (certSerial == null){
      log.debug("Certificate not found");
      return "redirect:request?instance=" + instance;
    }

    model.addAttribute("instance", instance);
    model.addAttribute("bootstrapCss", bootstrapCss);
    model.addAttribute("logoMap", logoMap);
    model.addAttribute("htmlInfo", htmlServiceInfo);
    model.addAttribute("cmcConfig", cmcProperties.getInstance().get(instance));
    //model.addAttribute("basicConfig", basicConfig);

    final CMCClient cmcClient = cmcClientMap.get(instance);
    final CMCResponse getCertResponse = cmcClient.getIssuedCertificate(certSerial);
    final List<X509Certificate> returnCertificates = getCertResponse.getReturnCertificates();

    if (returnCertificates == null || returnCertificates.size() != 1) {
      log.debug("Certificate not found");
      return ControllerUtils.getErrorPage(model, "Certificate not found", instance, htmlServiceInfo, bootstrapCss, logoMap);
    }

    PrintCertificate printCertificate = new PrintCertificate(returnCertificates.get(0));
    model.addAttribute("cert", printCertificate);

    return "issue-result";
  }

}
