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

package se.swedenconnect.ca.cmcclient.ca.profiles.impl;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import se.swedenconnect.ca.cmc.api.CMCCertificateModelBuilder;
import se.swedenconnect.ca.cmcclient.ca.profiles.*;
import se.swedenconnect.ca.cmcclient.configuration.cmc.CMCProperties;
import se.swedenconnect.ca.cmcclient.configuration.profile.CertificateProfileProperties;
import se.swedenconnect.ca.engine.ca.models.cert.extension.impl.CertificatePolicyModel;
import se.swedenconnect.ca.engine.ca.models.cert.extension.impl.simple.BasicConstraintsModel;
import se.swedenconnect.ca.engine.ca.models.cert.extension.impl.simple.ExtendedKeyUsageModel;
import se.swedenconnect.ca.engine.ca.models.cert.extension.impl.simple.KeyUsageModel;

import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Provides a certificate profile based on certificate profile configuration properties
 *
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
 */
@Slf4j
public class PropertyBasedCertificateProfile extends AbstractCertificateProfile {
  CertificateProfileProperties.Profile profilePropertiesData;
  private final CMCProperties cmcProperties;

  public PropertyBasedCertificateProfile(CertificateProfileProperties.Profile profilePropertiesData, String templatePage,
      CMCProperties cmcProperties) {
    super(
      templatePage,
      profilePropertiesData.getRequestAttributes(),
      profilePropertiesData.getRequestSubjAltNames(),
      getExtendedKeyUsage(profilePropertiesData.getRequestEku(), cmcProperties),
      profilePropertiesData.getRequestOther(),
      profilePropertiesData.getRequestFixedValue()
    );
    this.profilePropertiesData = profilePropertiesData;
    this.cmcProperties = cmcProperties;
    setCriticalAltNameExt(profilePropertiesData.getSubjAltNameCritical());
    setCriticalEKUExt(profilePropertiesData.getEkuCritical());
    setCriticalCertificatePolicyExt(profilePropertiesData.getPolicyCritical());
  }

  private static List<ExtendedKeyUsage> getExtendedKeyUsage(final List<String> requestEku, final CMCProperties cmcProperties) {
    List<ExtendedKeyUsage> extendedKeyUsages = new ArrayList<>();
    if (requestEku == null || requestEku.isEmpty()) {
      return extendedKeyUsages;
    }
    final List<CMCProperties.ExtendedKeyUsageProperty> customEkuList = cmcProperties.getCustomEku();
    for (String ekuName: requestEku) {
      ExtendedKeyUsage extendedKeyUsage = Arrays.stream(DefaultEKUReqParameter.values())
          .filter(defaultEKUReqParameter -> defaultEKUReqParameter.name().equalsIgnoreCase(ekuName))
          .map(defaultEKUReqParameter -> new ExtendedKeyUsage(
              defaultEKUReqParameter.getEku(),
              defaultEKUReqParameter.name(),
              defaultEKUReqParameter.getInputLabel()))
          .findFirst()
          .orElse(null);
      if (extendedKeyUsage != null) {
        extendedKeyUsages.add(extendedKeyUsage);
        continue;
      }
      if (customEkuList == null || customEkuList.isEmpty()) {
        throw new IllegalArgumentException("Configured eku tag " + ekuName + " is not defined - Define it as custom EKU in application.properties");
      }
      extendedKeyUsage = customEkuList.stream()
          .filter(extendedKeyUsageProperty -> extendedKeyUsageProperty.getTag().equals(ekuName))
          .map(extendedKeyUsageProperty -> new ExtendedKeyUsage(
              KeyPurposeId.getInstance(new ASN1ObjectIdentifier(extendedKeyUsageProperty.getOid())),
              extendedKeyUsageProperty.getTag(),
              extendedKeyUsageProperty.getDescription()))
          .findFirst()
          .orElseThrow(() -> new IllegalArgumentException("Configured eku tag " + ekuName + " is not defined - Define it as custom EKU in application.properties"));
      if (extendedKeyUsage.getTag() == null || !extendedKeyUsage.getTag().startsWith("eku")) {
        throw new IllegalArgumentException("Custom eku tag " + ekuName + " has illegal tag. The tag MUST start with 'eku' - Rename custom EKU tag in application.properties");
      }
      if (extendedKeyUsage.getDescription() == null || extendedKeyUsage.getDescription().isEmpty()) {
        throw new IllegalArgumentException("Custom eku tag " + ekuName + " has no description");
      }
      extendedKeyUsages.add(extendedKeyUsage);
    }
    return extendedKeyUsages;
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
      .includeSki(profilePropertiesData.getIncludeSki())
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
        getExtendedKeyUsage(profilePropertiesData.getEku(), cmcProperties).stream()
          .map(ExtendedKeyUsage::getEku)
          .toArray(KeyPurposeId[]::new)
      ));
    }
  }
}
