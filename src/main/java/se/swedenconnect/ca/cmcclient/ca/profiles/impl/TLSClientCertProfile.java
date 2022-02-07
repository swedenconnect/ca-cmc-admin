package se.swedenconnect.ca.cmcclient.ca.profiles.impl;

import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import se.swedenconnect.ca.cmc.api.CMCCertificateModelBuilder;
import se.swedenconnect.ca.cmcclient.ca.profiles.AttrReqParameter;
import se.swedenconnect.ca.cmcclient.ca.profiles.EKUReqParameter;
import se.swedenconnect.ca.cmcclient.ca.profiles.OtherReqParameters;
import se.swedenconnect.ca.cmcclient.ca.profiles.SubjectAlltNameReqParameter;
import se.swedenconnect.ca.engine.ca.models.cert.extension.impl.simple.BasicConstraintsModel;
import se.swedenconnect.ca.engine.ca.models.cert.extension.impl.simple.ExtendedKeyUsageModel;
import se.swedenconnect.ca.engine.ca.models.cert.extension.impl.simple.KeyUsageModel;

import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Basic user certificate profile
 *
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
 */
public class TLSClientCertProfile extends AbstractCertificateProfile {

  public TLSClientCertProfile(String templatePage, List<AttrReqParameter> attrReqParameters,
    List<SubjectAlltNameReqParameter> subjectAlltNameReqParameters,
    List<EKUReqParameter> ekuReqParameters, List<OtherReqParameters> otherReqParameters, Map<String, String> fixedValueMap) {
    super(templatePage, attrReqParameters, subjectAlltNameReqParameters, ekuReqParameters, otherReqParameters, fixedValueMap);
  }

  public TLSClientCertProfile(List<AttrReqParameter> attrReqParameters, String... certificatePolicy) {
    this("cert-request", attrReqParameters, null, null, null, null);
    if (certificatePolicy != null && certificatePolicy.length >0){
      setCertificatePolicylist(Arrays.asList(certificatePolicy));
    }
  }
  public TLSClientCertProfile(String... certificatePolicy) {
    this( "cert-request",
      Arrays.asList(
        AttrReqParameter.commonName,
        AttrReqParameter.givenName,
        AttrReqParameter.surname,
        AttrReqParameter.serialNumber,
        AttrReqParameter.organizationName,
        AttrReqParameter.orgUnitName,
        AttrReqParameter.orgIdentifier,
        AttrReqParameter.country
      ), null, null, null, null
    );
    if (certificatePolicy != null && certificatePolicy.length >0){
      setCertificatePolicylist(Arrays.asList(certificatePolicy));
    }
  }

  @Override protected void doProfileUpdates(CMCCertificateModelBuilder certificateModelBuilder, PublicKey publicKey,
    Map<String, String[]> requestParameters) {
    boolean keyEncipherment = publicKey instanceof RSAPublicKey;
    int keyUsage = KeyUsage.digitalSignature
      + (keyEncipherment ? KeyUsage.keyEncipherment : KeyUsage.keyAgreement);

    certificateModelBuilder
      .basicConstraints(new BasicConstraintsModel(false, false))
      .includeAki(true)
      .includeSki(true)
      .keyUsage(new KeyUsageModel(keyUsage))
      .extendedKeyUsage(new ExtendedKeyUsageModel(true, KeyPurposeId.id_kp_clientAuth));
  }
}
