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

import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import se.swedenconnect.ca.cmc.api.CMCCertificateModelBuilder;
import se.swedenconnect.ca.cmcclient.ca.profiles.*;
import se.swedenconnect.ca.cmcclient.ca.request.RequestData;
import se.swedenconnect.ca.engine.ca.models.cert.AttributeTypeAndValueModel;
import se.swedenconnect.ca.engine.ca.models.cert.CertNameModel;
import se.swedenconnect.ca.engine.ca.models.cert.extension.impl.CertificatePolicyModel;
import se.swedenconnect.ca.engine.ca.models.cert.extension.impl.simple.ExtendedKeyUsageModel;
import se.swedenconnect.ca.engine.ca.models.cert.impl.ExplicitCertNameModel;

import java.security.PublicKey;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Provides the core functions of a certificate profile
 *
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
 */
public abstract class AbstractCertificateProfile implements CertificateProfile {

  /** Decides if SubjectAltName extensions will be critical. Default false */
  @Setter protected boolean criticalAltNameExt = false;
  /** Decides if EKU extensions will be critical. Default true */
  @Setter protected boolean criticalEKUExt = true;
  @Setter protected boolean criticalCertificatePolicyExt = false;

  @Setter private List<AttrReqParameter> attrReqParameters;
  @Setter private List<SubjectAlltNameReqParameter> subjectAlltNameReqParameters;
  @Setter private List<EKUReqParameter> ekuReqParameters;
  @Setter private List<OtherReqParameters> otherReqParameters;
  @Setter private Map<String, String> fixedValueMap;

  @Setter private List<String> certificatePolicylist;

  private final String templatePage;

  public AbstractCertificateProfile(String templatePage, List<AttrReqParameter> attrReqParameters,
    List<SubjectAlltNameReqParameter> subjectAlltNameReqParameters,
    List<EKUReqParameter> ekuReqParameters, List<OtherReqParameters> otherReqParameters, Map<String, String> fixedValueMap) {
    this.templatePage = templatePage;
    this.attrReqParameters = attrReqParameters;
    this.subjectAlltNameReqParameters = subjectAlltNameReqParameters;
    this.ekuReqParameters = ekuReqParameters;
    this.otherReqParameters = otherReqParameters;
    this.fixedValueMap = fixedValueMap;
  }

  /**
   * This function appends the certificate request model with request for certificate extensions based on input data
   *
   * @param certificateModelBuilder the base certificate model builder used to build the certificate model
   * @param publicKey               the public key of the certificate
   * @param requestParameters       Certificate request parameters
   */
  @Override public void appendCertificateModel(CMCCertificateModelBuilder certificateModelBuilder, PublicKey publicKey,
    Map<String, String[]> requestParameters) {

    requestParameters = requestParameters == null ? new HashMap<>() : requestParameters;

    List<String> policyList = certificatePolicylist == null ? new ArrayList<>() : new ArrayList<>(certificatePolicylist);
    List<String> reqParamPolicyList = requestParameters.containsKey(OtherReqParameters.otherParamsPolicy.name())
      ? Arrays.asList(requestParameters.get(OtherReqParameters.otherParamsPolicy.name()))
      : new ArrayList<>();
    policyList.addAll(reqParamPolicyList);
    removeDuplicates(policyList);
    if (!policyList.isEmpty()) {
      final ASN1ObjectIdentifier[] policyOIDArray = policyList.stream()
        .map(ASN1ObjectIdentifier::new)
        .toArray(ASN1ObjectIdentifier[]::new);
      certificateModelBuilder.certificatePolicy(new CertificatePolicyModel(criticalCertificatePolicyExt, policyOIDArray));
    }

    // Include subjectAltNames
    final Map<String, String> fixedValueMap = getFixedValueMap();
    Map<Integer, List<String>> altNameMap = new HashMap<>();
    for (SubjectAlltNameReqParameter subjAltName : SubjectAlltNameReqParameter.values()) {
      // Add an Alt name if the alt name appears in the request parameters or is among the fixed value set
      if (requestParameters.containsKey(subjAltName.name()) || fixedValueMap.containsKey(subjAltName.name())) {
        final String[] valueArray = fixedValueMap.containsKey(subjAltName.name())
          ? new String[] { fixedValueMap.get(subjAltName.name()) }
          : requestParameters.get(subjAltName.name());
        List<String> altNameValueList = new ArrayList<>();
        for (String arrayVal : valueArray) {
          final String[] split = arrayVal.split(",");
          for (String splitVal : split) {
            altNameValueList.add(splitVal.trim());
          }
        }
        altNameMap.put(subjAltName.getGeneralNameIndex(), altNameValueList);
      }
    }
    if (!altNameMap.isEmpty()) {
      certificateModelBuilder.subjectAltNames(criticalAltNameExt, altNameMap);
    }

    // Include EKUs
    List<KeyPurposeId> ekuList = new ArrayList<>();
    for (EKUReqParameter ekuReqParameter : EKUReqParameter.values()) {
      if (requestParameters.containsKey(ekuReqParameter.name())) {
        ekuList.add(ekuReqParameter.getEku());
      }
    }
    if (!ekuList.isEmpty()) {
      certificateModelBuilder.extendedKeyUsage(new ExtendedKeyUsageModel(criticalEKUExt, ekuList.toArray(KeyPurposeId[]::new)));
    }

    //Now make any additional updates to the certificate model
    doProfileUpdates(certificateModelBuilder, publicKey, requestParameters);
  }

  private void removeDuplicates(List<String> valueList) {
    Set<String> valueSet = new LinkedHashSet<>(valueList);
    valueList.clear();
    valueList.addAll(valueSet);
  }

  protected abstract void doProfileUpdates(CMCCertificateModelBuilder certificateModelBuilder, PublicKey publicKey,
    Map<String, String[]> requestParameters);

  @Override public CertNameModel<?> getCertNameModel(Map<String, String[]> httpReqParameterMap) {
    ExplicitCertNameModel certNameModel = new ExplicitCertNameModel();
    certNameModel.setRdnSequence(new ArrayList<>());

    if (httpReqParameterMap == null || httpReqParameterMap.isEmpty()) {
      return certNameModel;
    }
    final List<AttrReqParameter> supportedAttributeList = AttrReqParameter.getPrioritySortedList();
    final Map<String, String> fixedValueMap = getFixedValueMap();
    for (AttrReqParameter attrParam : supportedAttributeList) {
      if (httpReqParameterMap.containsKey(attrParam.name()) || fixedValueMap.containsKey(attrParam.name())) {
        final String[] valueArray = (fixedValueMap.containsKey(attrParam.name()))
          ? new String[] { fixedValueMap.get(attrParam.name()) }
          : httpReqParameterMap.get(attrParam.name());
        if (valueArray != null && valueArray.length > 0) {
          addName(certNameModel, attrParam.getAttributeOid(), valueArray);
        }
      }
    }
    return certNameModel;
  }

  protected void addName(ExplicitCertNameModel certNameModel, ASN1ObjectIdentifier attrOid, String... value)
    throws IllegalArgumentException {
    try {
      final List<AttributeTypeAndValueModel> attributeTypeAndValues = Arrays.stream(value)
        .map(RequestData::validateString)
        .filter(StringUtils::isNotBlank)
        .map(s -> new AttributeTypeAndValueModel(attrOid, s))
        .collect(Collectors.toList());
      if (!attributeTypeAndValues.isEmpty()) {
        certNameModel.getNameData().add(attributeTypeAndValues);
      }
    }
    catch (Exception ex) {
      throw new IllegalArgumentException("Subject name error: " + ex.getMessage());
    }
  }

  /**
   * The name of the page returned to provide user input
   *
   * @return page template name
   */
  @Override public String getHtmlTemplatePage() {
    return templatePage;
  }

  /**
   * Get request parameters for attributes that should be provided to the certificate request page
   *
   * @return list of attribute request parameters
   */
  @Override public List<AttrReqParameter> getAttributeRequestParameters() {
    return attrReqParameters == null ? new ArrayList<>() : attrReqParameters;
  }

  /**
   * Get request parameters for subject alternative names that should be provided to the certificate request page
   *
   * @return list of subject alternative name request parameters
   */
  @Override public List<SubjectAlltNameReqParameter> getSubjectAltNameRequestParameters() {
    return subjectAlltNameReqParameters == null ? new ArrayList<>() : subjectAlltNameReqParameters;
  }

  /**
   * Get request parameters for Extended Key Usage (EKU) identifiers that should be provided to the certificate request page
   *
   * @return list of EKU request parameters
   */
  @Override public List<EKUReqParameter> getEKURequestParameters() {
    return ekuReqParameters == null ? new ArrayList<>() : ekuReqParameters;
  }

  /**
   * List of other request parameters that should be provided in the context of the request process
   *
   * @return list of other request parameters
   */
  @Override public List<OtherReqParameters> getOtherRequestParameters() {
    return otherReqParameters == null ? new ArrayList<>() : otherReqParameters;
  }

  /**
   * List of other request parameters that should be provided in the context of the request process
   *
   * @return list of other request parameters
   */
  @Override public Map<String, String> getFixedValueMap() {
    return fixedValueMap == null ? new HashMap<>() : fixedValueMap;
  }
}
