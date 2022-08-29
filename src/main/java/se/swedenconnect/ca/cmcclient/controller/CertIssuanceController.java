/*
 * Copyright (c) 2021-2022.  Agency for Digital Government (DIGG)
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
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.util.encoders.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import se.swedenconnect.ca.cmc.api.CMCCertificateModelBuilder;
import se.swedenconnect.ca.cmc.api.client.CMCClient;
import se.swedenconnect.ca.cmc.api.data.CMCFailType;
import se.swedenconnect.ca.cmc.api.data.CMCResponse;
import se.swedenconnect.ca.cmc.api.data.CMCResponseStatus;
import se.swedenconnect.ca.cmc.api.data.CMCStatusType;
import se.swedenconnect.ca.cmcclient.authz.CurrentUser;
import se.swedenconnect.ca.cmcclient.ca.PublicKeyValidator;
import se.swedenconnect.ca.cmcclient.ca.profiles.CertificateProfile;
import se.swedenconnect.ca.cmcclient.ca.profiles.CertificateProfileRegistry;
import se.swedenconnect.ca.cmcclient.ca.profiles.OtherReqParameters;
import se.swedenconnect.ca.cmcclient.ca.request.RequestData;
import se.swedenconnect.ca.cmcclient.ca.request.RequestDataResult;
import se.swedenconnect.ca.cmcclient.configuration.EmbeddedLogo;
import se.swedenconnect.ca.cmcclient.configuration.HtmlServiceInfo;
import se.swedenconnect.ca.cmcclient.configuration.cmc.CMCInstanceParams;
import se.swedenconnect.ca.cmcclient.configuration.cmc.CMCProperties;
import se.swedenconnect.ca.cmcclient.configuration.profile.CertificateProfileProperties;
import se.swedenconnect.ca.engine.ca.models.cert.CertNameModel;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * Web controller for services related to certificate issuance
 *
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
 */
@Slf4j
@Controller
@Profile("base")
public class CertIssuanceController {

  private final Map<String, CMCClient> cmcClientMap;
  private final CMCProperties cmcProperties;
  private final Map<String, EmbeddedLogo> logoMap;
  private final CertificateProfileRegistry certificateProfileRegistry;
  private final PublicKeyValidator publicKeyValidator;
  private final HttpSession httpSession;
  private final HtmlServiceInfo htmlServiceInfo;
  private final Map<String, CertificateProfileProperties.Profile> propertyProfileDataMap;
  @Value("${ca-client.config.bootstrap-css}") String bootstrapCss;

  @Autowired
  public CertIssuanceController(Map<String, CMCClient> cmcClientMap, CMCProperties cmcProperties,
    Map<String, EmbeddedLogo> logoMap, CertificateProfileRegistry certificateProfileRegistry,
    PublicKeyValidator publicKeyValidator, HttpSession httpSession, HtmlServiceInfo htmlServiceInfo,
    Map<String, CertificateProfileProperties.Profile> propertyProfileDataMap) {
    this.cmcClientMap = cmcClientMap;
    this.cmcProperties = cmcProperties;
    this.logoMap = logoMap;
    this.publicKeyValidator = publicKeyValidator;
    this.httpSession = httpSession;
    this.htmlServiceInfo = htmlServiceInfo;
    this.certificateProfileRegistry = certificateProfileRegistry;
    this.propertyProfileDataMap = propertyProfileDataMap;
  }

  @RequestMapping("/request")
  public String certificateRequest(HttpServletRequest servletRequest, @RequestParam("instance") String instance, Model model,
    Authentication authentication) {

    // Get current user
    CurrentUser currentUser = new CurrentUser(authentication);
    model.addAttribute("currentUser", currentUser);

    // Validate that instance exists and that current user is authorized to manage this instance
    if (!cmcClientMap.containsKey(instance) || !currentUser.isAuthorizedFor(instance)) {
      log.debug(!cmcClientMap.containsKey(instance)
        ? "Request to non existent CA instance - redirect to no-found"
        : "User not authorized - redirect to no-found");
      return "redirect:not-found";
    }

    final CMCInstanceParams cmcInstanceParams = cmcProperties.getInstance().get(instance);
    final String profile = cmcInstanceParams.getProfile();
    if (!certificateProfileRegistry.getCertificateProfileMap().containsKey(profile)) {
      log.error("The profile configuration for instance {} does not have a defined profile: value {}", instance, profile);
      return "redirect:not-found";
    }
    final CertificateProfile certificateProfile = certificateProfileRegistry.getCertificateProfileMap().get(profile);

    model.addAttribute("instance", instance);
    model.addAttribute("bootstrapCss", bootstrapCss);
    model.addAttribute("logoMap", logoMap);
    model.addAttribute("cmcConfig", cmcProperties.getInstance().get(instance));
    model.addAttribute("htmlInfo", htmlServiceInfo);
    model.addAttribute("certificateProfile", certificateProfile);
    model.addAttribute("profile", profile);

    return certificateProfile.getHtmlTemplatePage();
  }

  @PostMapping("/issue")
  public String issueCertificate(Model model, Authentication authentication,
    HttpServletRequest servletRequest,
    @RequestParam(value = "certRequest", required = false) String certRequest,
    @RequestParam(value = "instance") String instance
  ) {

    // Get current user
    CurrentUser currentUser = new CurrentUser(authentication);
    model.addAttribute("currentUser", currentUser);

    // Validate that instance exists and that current user is authorized to manage this instance
    if (!cmcClientMap.containsKey(instance) || !currentUser.isAuthorizedFor(instance)) {
      log.debug(!cmcClientMap.containsKey(instance)
        ? "Request to non existent CA instance - redirect to no-found"
        : "User not authorized - redirect to no-found");
      return "redirect:not-found";
    }

    final CMCClient cmcClient = cmcClientMap.get(instance);
    final CMCInstanceParams cmcInstanceParams = cmcProperties.getInstance().get(instance);
    final String profile = cmcInstanceParams.getProfile();
    if (!certificateProfileRegistry.getCertificateProfileMap().containsKey(profile)) {
      log.error("The profile configuration for instance {} does not have a defined profile: value {}", instance, profile);
      return "redirect:not-found";
    }
    final CertificateProfile certificateProfile = certificateProfileRegistry.getCertificateProfileMap().get(profile);

    RequestData requestData = new RequestData(certRequest, publicKeyValidator);
    RequestDataResult requestDataResult = requestData.getRequestDataResult();

    // We have a request. Make an audit log event

    model.addAttribute("instance", instance);
    model.addAttribute("bootstrapCss", bootstrapCss);
    model.addAttribute("logoMap", logoMap);
    model.addAttribute("htmlInfo", htmlServiceInfo);
    model.addAttribute("cmcConfig", cmcInstanceParams);
    //model.addAttribute("basicConfig", basicConfig);

    if (requestDataResult.getErrorMessage() != null) {
      log.debug("Certificate request error - {}", requestDataResult.getErrorMessage());
      return ControllerUtils.getErrorPage(model, requestDataResult.getErrorMessage(), instance, htmlServiceInfo, bootstrapCss, logoMap);
    }

    try {
      final Map<String, String[]> parameterMap = extendParameterMap(servletRequest.getParameterMap(), cmcInstanceParams);
      CertNameModel nameModel = certificateProfile.getCertNameModel(parameterMap);
      boolean includeCrlDp = true;
      boolean includeOcspUrl = true;
      if (propertyProfileDataMap.containsKey(profile)){
        final CertificateProfileProperties.Profile profilePropData = propertyProfileDataMap.get(profile);
        includeCrlDp = profilePropData.getIncludeCrlDp();
        includeOcspUrl = profilePropData.getIncludeOcspUrl();
      }
      final CMCCertificateModelBuilder certificateModelBuilder = cmcClient.getCertificateModelBuilder(requestData.getPublicKey(),
        nameModel, includeCrlDp, includeOcspUrl);
      certificateProfile.appendCertificateModel(certificateModelBuilder, requestData.getPublicKey(), parameterMap);

      final CMCResponse cmcResponse = cmcClient.issueCertificate(certificateModelBuilder.build());
      final CMCResponseStatus responseStatus = cmcResponse.getResponseStatus();
      if (!responseStatus.getStatus().equals(CMCStatusType.success)) {
        final CMCFailType failType = responseStatus.getFailType();
        final String failTypeMessage = responseStatus.getMessage() != null ? ", Message: " + responseStatus.getMessage() : "";
        String message = "Status: " + responseStatus.getStatus().name() + ", Failtype: " + failType.name() + failTypeMessage;
        return ControllerUtils.getErrorPage(model, message, instance, htmlServiceInfo, bootstrapCss, logoMap);
      }
      final X509Certificate issuedCert = cmcResponse.getReturnCertificates().get(0);
      X509CertificateHolder certificateHolder = new JcaX509CertificateHolder(issuedCert);

      httpSession.setAttribute("certSerialNumber-" + instance, certificateHolder.getSerialNumber());

      log.info("Certificate issued to {}", certificateHolder.getSubject().toString());
      if (log.isTraceEnabled()) {
        log.trace("Issued Certificate: {}", Base64.toBase64String(certificateHolder.getEncoded()));
      }
    }
    catch (Exception ex) {
      return ControllerUtils.getErrorPage(model, ex.getMessage(), instance, htmlServiceInfo, bootstrapCss, logoMap);
    }

    return "redirect:issue-result?instance=" + instance;
  }

  private Map<String, String[]> extendParameterMap(Map<String, String[]> originalParameterMap, CMCInstanceParams cmcInstanceParams) {
    Map<String, String[]> updatedParameterMap = originalParameterMap != null
      ? new HashMap<>(originalParameterMap)
      : new HashMap<>();
    final String[] policy = cmcInstanceParams.getPolicy();
    if (policy != null && policy.length > 0) {
      List<String> policyList = new ArrayList<>(Arrays.asList(policy));
      if (updatedParameterMap.containsKey(OtherReqParameters.otherParamsPolicy.name())){
        final String[] reqPolicyArray = updatedParameterMap.get(OtherReqParameters.otherParamsPolicy.name());
        if (reqPolicyArray != null && reqPolicyArray.length > 0){
          policyList.addAll(Arrays.asList(reqPolicyArray));
        }
      }
      updatedParameterMap.put(OtherReqParameters.otherParamsPolicy.name(), policyList.toArray(String[]::new));
    }
    return updatedParameterMap;
  }

}
