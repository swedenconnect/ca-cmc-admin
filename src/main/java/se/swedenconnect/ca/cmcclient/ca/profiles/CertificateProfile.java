/*
 * Copyright (c) 2022.  Agency for Digital Government (DIGG)
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

package se.swedenconnect.ca.cmcclient.ca.profiles;

import se.swedenconnect.ca.cmc.api.CMCCertificateModelBuilder;
import se.swedenconnect.ca.engine.ca.models.cert.CertNameModel;

import java.security.PublicKey;
import java.util.List;
import java.util.Map;

/**
 * Interface for a certificate profile that determines the content of issued certificates
 *
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
 */
public interface CertificateProfile {

  /**
   * This function appends the certificate request model with request for certificate extensions based on input data
   * @param certificateModelBuilder the base certificate model builder used to build the certificate model
   * @param publicKey the public key of the certificate
   * @param requestParameters HttpServlet Request parameter map
   */
  void appendCertificateModel(CMCCertificateModelBuilder certificateModelBuilder, PublicKey publicKey, Map<String, String[]> requestParameters);

  /**
   * Obtain the certificate name model based on the input parameters from the parameterMap provided by the user input page
   * @param parameterMap HttpServletRequest parameter map provided from the user input page certificate request
   * @return certificate name model
   */
  CertNameModel<?> getCertNameModel(Map<String, String[]> parameterMap);

  /**
   * The name of the page returned to provide user input
   * @return page template name
   */
  String getHtmlTemplatePage();

  /**
   * Get request parameters for attributes that should be provided to the certificate request page
   * @return list of attribute request parameters
   */
  List<AttrReqParameter> getAttributeRequestParameters();

  /**
   * Get request parameters for subject alternative names that should be provided to the certificate request page
   * @return list of subject alternative name request parameters
   */
  List<SubjectAlltNameReqParameter> getSubjectAltNameRequestParameters();

  /**
   * Get request parameters for Extended Key Usage (EKU) identifiers that should be provided to the certificate request page
   * @return list of EKU request parameters
   */
  List<EKUReqParameter> getEKURequestParameters();

  /**
   * List of other request parameters that should be provided in the context of the request process
   * @return list of other request parameters
   */
  List<OtherReqParameters> getOtherRequestParameters();

  /**
   * Get a map over request properties that have fixed input values (Like country must have the value SE)
   * @return fixed value map
   */
  Map<String, String> getFixedValueMap();
}
