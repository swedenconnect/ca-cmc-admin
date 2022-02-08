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

package se.swedenconnect.ca.cmcclient.utils;

import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.x509.CRLReason;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import se.swedenconnect.ca.cmc.model.admin.response.CAInformation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility functions for this CA service
 *
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
 */
public class CAServiceUtils {

  public static String getAttributeStringValue(ASN1Encodable value) {
    if (value instanceof DERPrintableString) {
      return ((DERPrintableString) value).getString();
    }
    if (value instanceof DERUTF8String) {
      return ((DERUTF8String) value).getString();
    }
    if (value instanceof DERIA5String) {
      return ((DERIA5String) value).getString();
    }
    if (value instanceof ASN1GeneralizedTime) {
      return ((ASN1GeneralizedTime) value).getTimeString().substring(0, 8);
    }
    return value.toString();
  }

  /**
   * The CRLReason enumeration.
   *    CRLReason ::= ENUMERATED {
   *     unspecified             (0),
   *     keyCompromise           (1),
   *     cACompromise            (2),
   *     affiliationChanged      (3),
   *     superseded              (4),
   *     cessationOfOperation    (5),
   *     certificateHold         (6),
   *     removeFromCRL           (8),
   *     privilegeWithdrawn      (9),
   *     aACompromise           (10)
   *    }
   * @param reason revocation reason code
   * @return display string for the reason code
   */
  public static String getRevocationReasonString (Integer reason) {
    if (reason == null) {
      return "";
    }
    switch (reason) {
    case CRLReason.unspecified:
      return "Unspecified";
    case CRLReason.keyCompromise:
      return "Key compromise";
    case CRLReason.cACompromise:
      return "CA Compromise";
    case CRLReason.affiliationChanged:
      return "Affiliation changed";
    case CRLReason.superseded:
      return "Superseded";
    case CRLReason.cessationOfOperation:
      return "Operation ceased";
    case CRLReason.certificateHold:
      return "On hold";
    case CRLReason.removeFromCRL:
      return "Remove from CRL";
    case CRLReason.privilegeWithdrawn:
      return "withdrawn";
    case CRLReason.aACompromise:
      return "AA Compromise";
    default:
      return "Unknown";
    }
  }

  public static X509CertificateHolder getOcspCert(File configFolder, String instance) throws IOException, CertificateEncodingException {
    File certDir = new File(configFolder , "instances/"+ instance+"/certs");
    if (certDir.exists()){
      Optional<File> ocspCertFile = Arrays.stream(certDir.listFiles((dir, name) -> name.endsWith("ocsp.crt"))).findFirst();
      if (ocspCertFile.isPresent()) {
        X509CertificateHolder ocspIssuerCert = new JcaX509CertificateHolder(
          Objects.requireNonNull(
            getCertOrNull(ocspCertFile.get())));
        return ocspIssuerCert;
      }
    }
    return null;
  }

  public static X509Certificate getCertOrNull(File certFile) {
    try {
      return CertificateUtils.decodeCertificate(new FileInputStream(certFile));
    } catch (Exception ex){
      return null;
    }
  }

  public static X509Certificate getCertOrNull(byte[] certbytes) {
    try {
      return CertificateUtils.decodeCertificate(certbytes);
    }
    catch (Exception ex) {
      return null;
    }
  }

  public static X509CertificateHolder getCertHolderOrNull(X509Certificate cert){
    try {
      return new JcaX509CertificateHolder(cert);
    } catch (Exception ex){
      return null;
    }
  }

  public static List<X509CertificateHolder> getCaChain(CAInformation caInformation){
    if (caInformation == null || caInformation.getCertificateChain() == null){
      return new ArrayList<>();
    }
    return caInformation.getCertificateChain().stream()
      .map(CAServiceUtils::getCertOrNull)
      .filter(Objects::nonNull)
      .map(CAServiceUtils::getCertHolderOrNull)
      .filter(Objects::nonNull)
      .collect(Collectors.toList());
  }

}
