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

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x509.KeyPurposeId;

/**
 * Enumeration of EKUs that may be requested at the certificate issuing page
 *
 * This includes Covid certificate purpose OIDs for compatibility with the Covid project CSCA
 *   test(new ASN1ObjectIdentifier("1.3.6.1.4.1.1847.2021.1.1"), 1),
 *   vaccinations(new ASN1ObjectIdentifier("1.3.6.1.4.1.1847.2021.1.2"), 2),
 *   recovery(new ASN1ObjectIdentifier("1.3.6.1.4.1.1847.2021.1.3"), 3),
 *
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
 */
@AllArgsConstructor
@Getter
public enum EKUReqParameter {

  ekuServerAuth(KeyPurposeId.id_kp_serverAuth, "TLS Server authentication"),
  ekuClientAuth(KeyPurposeId.id_kp_clientAuth, "TLS Client authentication"),
  ekuTimeStamping(KeyPurposeId.id_kp_timeStamping, "Time stamping"),
  ekuOCSPSigning(KeyPurposeId.id_kp_OCSPSigning, "OCSP signing"),
  ekuCovidTest(KeyPurposeId.getInstance(new ASN1ObjectIdentifier("1.3.6.1.4.1.1847.2021.1.1")), "Covid test result certificates"),
  ekuCovidVaccination(KeyPurposeId.getInstance(new ASN1ObjectIdentifier("1.3.6.1.4.1.1847.2021.1.2")), "Covid vaccination certificates"),
  ekuCovidRecovery(KeyPurposeId.getInstance(new ASN1ObjectIdentifier("1.3.6.1.4.1.1847.2021.1.3")), "Covid recovery certificates");

  public final KeyPurposeId eku;
  public final String inputLabel;
}
