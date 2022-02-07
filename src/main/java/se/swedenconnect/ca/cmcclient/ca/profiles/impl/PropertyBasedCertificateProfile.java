package se.swedenconnect.ca.cmcclient.ca.profiles.impl;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import se.swedenconnect.ca.cmc.api.CMCCertificateModelBuilder;
import se.swedenconnect.ca.cmcclient.ca.profiles.*;
import se.swedenconnect.ca.cmcclient.configuration.profile.CertificateProfileProperties;
import se.swedenconnect.ca.engine.ca.models.cert.extension.impl.CertificatePolicyModel;
import se.swedenconnect.ca.engine.ca.models.cert.extension.impl.simple.BasicConstraintsModel;
import se.swedenconnect.ca.engine.ca.models.cert.extension.impl.simple.ExtendedKeyUsageModel;
import se.swedenconnect.ca.engine.ca.models.cert.extension.impl.simple.KeyUsageModel;

import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Description
 *
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
 */
public class PropertyBasedCertificateProfile extends AbstractCertificateProfile {
  CertificateProfileProperties.Profile profilePropertiesData;

  public PropertyBasedCertificateProfile(CertificateProfileProperties.Profile profilePropertiesData, String templatePage) {
    super(
      templatePage,
      profilePropertiesData.getRequestAttributes(),
      profilePropertiesData.getRequestSubjAltNames(),
      profilePropertiesData.getRequestEku(),
      profilePropertiesData.getRequestOther(),
      profilePropertiesData.getRequestFixedValue()
    );
    this.profilePropertiesData = profilePropertiesData;
    setCriticalAltNameExt(profilePropertiesData.getSubjAltNameCritical());
    setCriticalEKUExt(profilePropertiesData.getEkuCritical());
    setCriticalCertificatePolicyExt(profilePropertiesData.getPolicyCritical());
  }

  @Override protected void doProfileUpdates(CMCCertificateModelBuilder certificateModelBuilder, PublicKey publicKey,
    Map<String, String[]> requestParameters) {

    int keyUsageVal = 0;
    final List<CertificateProfileProperties.KeyUsageType> keyUsages = profilePropertiesData.getKeyUsages();
    for (CertificateProfileProperties.KeyUsageType keyUsageType : keyUsages){
      switch (keyUsageType) {

      case sign:
        keyUsageVal += KeyUsage.digitalSignature;
        break;
      case encrypt:
        int encryptVal = publicKey instanceof RSAPublicKey ? KeyUsage.keyEncipherment : KeyUsage.keyAgreement;
        keyUsageVal += encryptVal;
        break;
      case nr:
        keyUsageVal += KeyUsage.nonRepudiation;
        break;
      case ca:
        keyUsageVal += KeyUsage.keyCertSign + KeyUsage.cRLSign;
        break;
      }
    }

    //Set basic constraints
    certificateModelBuilder
      .basicConstraints(new BasicConstraintsModel(profilePropertiesData.getCa(), profilePropertiesData.getBcCritical()))
      .includeAki(profilePropertiesData.getIncludeAki())
      .includeSki(profilePropertiesData.getInlcudeSki())
      .keyUsage(new KeyUsageModel(keyUsageVal, profilePropertiesData.getKeyUsageCritical()));

    // If policy is set in profile properties, then override any previous settings
    if (profilePropertiesData.getAnyPolicy()){
      certificateModelBuilder.certificatePolicy(new CertificatePolicyModel(profilePropertiesData.getPolicyCritical(), CertificatePolicyModel.ANY_POLICY));
    } else {
      if (profilePropertiesData.getPolicy() != null && !profilePropertiesData.getPolicy().isEmpty()){
        certificateModelBuilder.certificatePolicy(new CertificatePolicyModel(
          profilePropertiesData.getPolicyCritical(),
          profilePropertiesData.getPolicy().stream()
            .map(ASN1ObjectIdentifier::new)
            .toArray(ASN1ObjectIdentifier[]::new)));
      }
    }

    // If EKU settings are set, then override any input settings
    if (profilePropertiesData.getEku() != null && !profilePropertiesData.getEku().isEmpty()){
      certificateModelBuilder.extendedKeyUsage(new ExtendedKeyUsageModel(
        profilePropertiesData.getEkuCritical(),
        profilePropertiesData.getEku().stream()
          .map(EKUReqParameter::getEku)
          .toArray(KeyPurposeId[]::new)
      ));
    }
  }
}
