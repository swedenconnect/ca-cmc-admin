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

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.cmc.CMCStatus;
import org.bouncycastle.asn1.x500.AttributeTypeAndValue;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.CRLReason;
import org.bouncycastle.cert.X509CertificateHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import se.idsec.utils.printcert.PrintCertificate;
import se.swedenconnect.ca.cmc.CMCException;
import se.swedenconnect.ca.cmc.api.client.CMCClient;
import se.swedenconnect.ca.cmc.api.client.CMCResponseExtract;
import se.swedenconnect.ca.cmc.api.data.CMCResponse;
import se.swedenconnect.ca.cmc.model.admin.response.CAInformation;
import se.swedenconnect.ca.cmc.model.admin.response.CertificateData;
import se.swedenconnect.ca.cmcclient.authz.CurrentUser;
import se.swedenconnect.ca.cmcclient.ca.profiles.CertificateProfile;
import se.swedenconnect.ca.cmcclient.ca.profiles.CertificateProfileRegistry;
import se.swedenconnect.ca.cmcclient.configuration.EmbeddedLogo;
import se.swedenconnect.ca.cmcclient.configuration.HtmlServiceInfo;
import se.swedenconnect.ca.cmcclient.configuration.cmc.CMCInstanceParams;
import se.swedenconnect.ca.cmcclient.configuration.cmc.CMCProperties;
import se.swedenconnect.ca.cmcclient.data.CertDisplayData;
import se.swedenconnect.ca.cmcclient.data.PageControlData;
import se.swedenconnect.ca.cmcclient.data.PageCookie;
import se.swedenconnect.ca.cmcclient.utils.CAServiceUtils;
import se.swedenconnect.ca.cmcclient.utils.CertificateUtils;
import se.swedenconnect.ca.engine.ca.attribute.CertAttributes;
import se.swedenconnect.ca.engine.ca.repository.SortBy;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Web controller for admin task requests to a CA instance
 *
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
 */
@Slf4j
@Controller
public class AdminController {

  private final static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
  private final static Random RNG = new SecureRandom();
  private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final CMCProperties cmcProperties;
  private final Map<String, CMCClient> cmcClientMap;
  private final Map<String, EmbeddedLogo> logoMap;
  private final HttpSession httpSession;
  private final HtmlServiceInfo htmlServiceInfo;
  private final CertificateProfileRegistry certificateProfileRegistry;
  @Value("${ca-client.config.bootstrap-css}") String bootstrapCss;
  @Value("${ca-client.config.page-sizes}") int[] pageSizes;
  @Value("${ca-client.config.page-size-default-index:0}") int pageSizeDefaultIndex;

  @Autowired
  public AdminController(Map<String, CMCClient> cmcClientMap, CMCProperties cmcProperties, CertificateProfileRegistry certificateProfileRegistry,
    Map<String, EmbeddedLogo> logoMap, HtmlServiceInfo htmlServiceInfo, HttpSession httpSession) {
    this.cmcClientMap = cmcClientMap;
    this.cmcProperties = cmcProperties;
    this.logoMap = logoMap;
    this.httpSession = httpSession;
    this.htmlServiceInfo = htmlServiceInfo;
    this.certificateProfileRegistry = certificateProfileRegistry;
  }

  @RequestMapping("/admin")
  public String adminPage(HttpServletRequest servletRequest, @RequestParam("instance") String instance, Model model,
    Authentication authentication, @CookieValue(value = "justValidCerts", required = false) String justValidCertsCookie,
    @CookieValue(value = "pageControlCookie", required = false) String pageCookieVal)
    throws CMCException {

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

    boolean justValidCerts = !(justValidCertsCookie != null && justValidCertsCookie.equalsIgnoreCase("false"));
    final CMCClient cmcClient = cmcClientMap.get(instance);
    final CAInformation caInformation = cmcClient.getCAInformation(true);
    final PageControlData pageControlData = getPageControlData(pageCookieVal, caInformation, justValidCerts);

    CMCResponse listCertResponse = cmcClient.listCertificates(
      pageControlData.getPageSize(),
      pageControlData.getPage(),
      pageControlData.getSortBy(),
      justValidCerts,
      pageControlData.isDescending());
    final List<CertificateData> certificateDataList = CMCResponseExtract.extractCertificateData(listCertResponse);

    List<CertDisplayData> certDisplayDataList = certificateDataList.stream()
      .map(this::getDisplayData)
      .filter(Objects::nonNull)
      .filter(certDisplayData -> {
        if (justValidCerts) {
          return !certDisplayData.isRevoked();
        }
        return true;
      })
      .collect(Collectors.toList());

    model.addAttribute("instance", instance);
    model.addAttribute("bootstrapCss", bootstrapCss);
    model.addAttribute("logoMap", logoMap);
    model.addAttribute("cmcConfig", cmcProperties.getInstance().get(instance));
    model.addAttribute("certList", certDisplayDataList);
    model.addAttribute("ocspCertSubject", getOCSPEntitySubjectDn(caInformation));
    model.addAttribute("htmlInfo", htmlServiceInfo);
    model.addAttribute("justValidCerts", justValidCerts);
    model.addAttribute("page", pageControlData);
    model.addAttribute("pageSizes", pageSizes);
    model.addAttribute("certCount", caInformation.getCertificateCount());
    model.addAttribute("nonRevokedCount", caInformation.getValidCertificateCount());
    model.addAttribute("certificateProfile", certificateProfile);

    // Add a unique revoke key that need to be returned in order to perform revocation of a certificate.
    String revokeKey = new BigInteger(64, RNG).toString(16);
    httpSession.setAttribute("revokeKey", revokeKey);
    model.addAttribute("revokeKey", revokeKey);

    // Finally. Get CA cert chain
    List<X509CertificateHolder> caChain = CAServiceUtils.getCaChain(caInformation);
    model.addAttribute("caChain", caChain);
    return "admin-page";

  }

  private PageControlData getPageControlData(String pageCookieVal, CAInformation caInformation, boolean justValidCerts) {

    int numberOfCerts = justValidCerts ? caInformation.getValidCertificateCount() : caInformation.getCertificateCount();
    PageCookie pageCookie;
    try {
      pageCookie = OBJECT_MAPPER.readValue(pageCookieVal, PageCookie.class);
    }
    catch (Exception ex) {
      pageCookie = null;
      log.debug("Unable to parse page cookie with val {}", pageCookieVal);
    }

    int pageSize = pageSizes[pageSizeDefaultIndex];
    if (pageCookie != null) {
      PageCookie finalPageCookie = pageCookie;
      pageSize = Arrays.stream(pageSizes)
        .filter(value -> value == finalPageCookie.getSize())
        .findFirst()
        .orElse(pageSizes[pageSizeDefaultIndex]);
    }

    int pages = (int) Math.ceil((double) numberOfCerts / (double) pageSize);

    if (pageCookie == null) {
      return PageControlData.builder()
        .page(0)
        .numberOfPages(pages)
        .pageSize(pageSize)
        .descending(false)
        .sortBy(SortBy.issueDate)
        .build();
    }

    int page = pageCookie.getPage() < 0
      ? 0
      : Math.min(pageCookie.getPage(), Math.max(pages - 1, 0));

    PageControlData pageControlData = PageControlData.builder()
      .page(page)
      .numberOfPages(pages)
      .pageSize(pageSize)
      .descending(pageCookie.isDescending())
      .sortBy(pageCookie.getSort() != null ? pageCookie.getSort() : SortBy.issueDate)
      .build();

    return pageControlData;
  }

  private String getOCSPEntitySubjectDn(CAInformation caInformation) {
    try {
      return CertificateUtils.decodeCertificate(caInformation.getOcspCertificate()).getSubjectX500Principal().toString();
    }
    catch (Exception ex) {
      return null;
    }
  }

  @RequestMapping("/revoke")
  public String revokeCertificate(HttpServletRequest servletRequest, @RequestParam("instance") String instance, Model model,
    Authentication authentication, @RequestParam("reason") int reason,
    @RequestParam("serialNumber") String serialNumberHex, @RequestParam("revokeKey") String revokeKey)
    throws CMCException {

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

    BigInteger certSerial;
    try {
      certSerial = new BigInteger(serialNumberHex, 16);
    }
    catch (Exception ex) {
      log.debug("Revocation request for illegal serial number - {}", ex.getMessage());
      return "redirect:/bad-request";
    }
    Date revocationTime = new Date();

    model.addAttribute("instance", instance);
    model.addAttribute("bootstrapCss", bootstrapCss);
    model.addAttribute("logoMap", logoMap);
    model.addAttribute("cmcConfig", cmcProperties.getInstance().get(instance));
    //model.addAttribute("basicConfig", basicConfig);

    String sessionRevokeKey = (String) httpSession.getAttribute("revokeKey");
    if (sessionRevokeKey == null || !sessionRevokeKey.equals(revokeKey)) {
      log.warn("Unauthorized revocation attempt - wrong revocation key");
      return ControllerUtils.getErrorPage(model,
        "Revocation request rejected due to session error - This may be caused by page reload", instance,
        htmlServiceInfo, bootstrapCss, logoMap);
    }

    // Clear revoke key to prevent double posting
    httpSession.removeAttribute("revokeKey");

    final List<X509Certificate> returnCertificates = cmcClient.getIssuedCertificate(certSerial).getReturnCertificates();

    if (returnCertificates == null || returnCertificates.size() != 1) {
      log.debug("Revocation request for non existent certificate - Request rejected");
      return ControllerUtils.getErrorPage(model, "Revocation request rejected - Requested certificate does not exist in database",
        instance, htmlServiceInfo, bootstrapCss, logoMap);
    }

    final CMCResponse cmcResponse = cmcClient.revokeCertificate(certSerial, reason, new Date());

    final CMCStatus cmcStatus = cmcResponse.getResponseStatus().getStatus().getCmcStatus();
    if (cmcStatus.equals(CMCStatus.success)) {
      log.info("Revoked certificate with serial number {}", certSerial);
    }
    else {
      log.debug("Revocation request for revoked certificate failed");
      return ControllerUtils.getErrorPage(model, "Revocation request rejected - Error response from CA",
        instance, htmlServiceInfo, bootstrapCss, logoMap);
    }

    return "redirect:admin?instance=" + instance;
  }

  private CertDisplayData getDisplayData(CertificateData certificateRecord) {
    try {
      CertDisplayData cdd = new CertDisplayData();
      PrintCertificate cert = new PrintCertificate(certificateRecord.getCertificate());
      addNameDataToResult(cert.getSubject(), cdd);
      cdd.setIssueDate(DATE_FORMAT.format(cert.getNotBefore()));
      cdd.setExpiryDate(DATE_FORMAT.format(cert.getNotAfter()));
      cdd.setSerialNumber(cert.getSerialNumber());
      cdd.setRevoked(certificateRecord.isRevoked());
      cdd.setOnHold(certificateRecord.isRevoked() && CRLReason.certificateHold == certificateRecord.getRevocationReason());
      cdd.setExpired(cert.getNotAfter().before(new Date()));
      cdd.setReason(CAServiceUtils.getRevocationReasonString(certificateRecord.getRevocationReason()));
      cdd.setRevocationDate(certificateRecord.isRevoked()
        ? DATE_FORMAT.format(new Date(certificateRecord.getRevocationDate()))
        : "");
      return cdd;
    }
    catch (Exception ex) {
      log.error("Error parsing certificate from CA database", ex);
      return null;
    }
  }

  private void addNameDataToResult(X500Name name, CertDisplayData cdd) {
    cdd.setSubjectDn(name.toString());
    RDN[] rdNs = name.getRDNs();
    for (RDN rdn : rdNs) {
      AttributeTypeAndValue[] typesAndValues = rdn.getTypesAndValues();
      for (AttributeTypeAndValue atav : typesAndValues) {
        ASN1ObjectIdentifier attrOid = atav.getType();
        if (attrOid.equals(CertAttributes.CN)) {
          cdd.setCn(CAServiceUtils.getAttributeStringValue(atav.getValue()));
        }
        if (attrOid.equals(CertAttributes.O)) {
          cdd.setO(CAServiceUtils.getAttributeStringValue(atav.getValue()));
        }
        if (attrOid.equals(CertAttributes.OU)) {
          cdd.setOu(CAServiceUtils.getAttributeStringValue(atav.getValue()));
        }
        if (attrOid.equals(CertAttributes.C)) {
          cdd.setC(CAServiceUtils.getAttributeStringValue(atav.getValue()));
        }
        if (attrOid.equals(CertAttributes.ORGANIZATION_IDENTIFIER)) {
          cdd.setOrgId(CAServiceUtils.getAttributeStringValue(atav.getValue()));
        }
      }
    }
  }

}
