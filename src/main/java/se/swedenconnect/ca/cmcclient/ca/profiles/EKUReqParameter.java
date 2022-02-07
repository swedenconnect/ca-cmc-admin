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
