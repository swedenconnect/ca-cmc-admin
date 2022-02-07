package se.swedenconnect.ca.cmcclient.ca.profiles.impl;

import org.bouncycastle.asn1.x509.KeyUsage;
import se.swedenconnect.ca.cmc.api.CMCCertificateModelBuilder;
import se.swedenconnect.ca.cmcclient.ca.profiles.*;
import se.swedenconnect.ca.engine.ca.models.cert.extension.impl.CertificatePolicyModel;
import se.swedenconnect.ca.engine.ca.models.cert.extension.impl.simple.BasicConstraintsModel;
import se.swedenconnect.ca.engine.ca.models.cert.extension.impl.simple.KeyUsageModel;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Basic CA certificate profile
 *
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
 */
public class CACertificateProfile extends AbstractCertificateProfile {

  public CACertificateProfile(String templatePage, List<AttrReqParameter> attrReqParameters,
    List<SubjectAlltNameReqParameter> subjectAlltNameReqParameters,
    List<EKUReqParameter> ekuReqParameters, List<OtherReqParameters> otherReqParameters, Map<String, String> fixedValueMap) {
    super(templatePage, attrReqParameters, subjectAlltNameReqParameters, ekuReqParameters, otherReqParameters, fixedValueMap);
  }

  public CACertificateProfile(List<AttrReqParameter> attrReqParameters) {
    this("cert-request", attrReqParameters, null, null, null, null);
  }

  public CACertificateProfile() {
    this( "cert-request",
      Arrays.asList(
        AttrReqParameter.commonName,
        AttrReqParameter.organizationName,
        AttrReqParameter.orgUnitName,
        AttrReqParameter.orgIdentifier,
        AttrReqParameter.country
      ), null, null, null, null
    );
  }

  /**
   * The name of the page returned to provide user input
   *
   * @return page template name
   */
  @Override public String getHtmlTamplatePage() {
    return "cert-request";
  }

  @Override protected void doProfileUpdates(CMCCertificateModelBuilder certificateModelBuilder, PublicKey publicKey,
    Map<String, String[]> requestParameters) {
    certificateModelBuilder
      .basicConstraints(new BasicConstraintsModel(true, true))
      .certificatePolicy(new CertificatePolicyModel(true))
      .keyUsage(new KeyUsageModel(KeyUsage.keyCertSign + KeyUsage.cRLSign))
      .includeAki(true)
      .includeSki(true);
  }
}
