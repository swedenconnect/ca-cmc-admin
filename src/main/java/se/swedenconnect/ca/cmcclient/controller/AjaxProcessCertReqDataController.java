package se.swedenconnect.ca.cmcclient.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import se.swedenconnect.ca.cmcclient.ca.PublicKeyValidator;
import se.swedenconnect.ca.cmcclient.ca.profiles.CertificateProfileRegistry;
import se.swedenconnect.ca.cmcclient.ca.request.RequestData;
import se.swedenconnect.ca.cmcclient.ca.request.RequestDataResult;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * Description
 *
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
 */
@Profile("base")
@RestController
public class AjaxProcessCertReqDataController {

  private static final ObjectMapper objectMapper = new ObjectMapper();
  private final PublicKeyValidator publicKeyValidator;
  private final CertificateProfileRegistry certificateProfileRegistry;

  @Autowired
  public AjaxProcessCertReqDataController(PublicKeyValidator publicKeyValidator, CertificateProfileRegistry certificateProfileRegistry) {
    this.publicKeyValidator = publicKeyValidator;
    this.certificateProfileRegistry = certificateProfileRegistry;
  }

  @RequestMapping("/processCertReqData")
  public String processCertReqData(@RequestParam("certRequestInputText") String certRequestInputText, @RequestParam(name = "profile", required = false) String profile) throws
    JsonProcessingException {

    Map<String, String> fixedValueMap = profile != null && certificateProfileRegistry.getCertificateProfileMap().containsKey(profile)
      ? certificateProfileRegistry.getCertificateProfileMap().get(profile).getFixedValueMap()
      : new HashMap<>();

    RequestData requestData = new RequestData(certRequestInputText, publicKeyValidator, fixedValueMap);
    RequestDataResult requestDataResult = requestData.getRequestDataResult();
    return objectMapper.writeValueAsString(requestDataResult);
  }

}
