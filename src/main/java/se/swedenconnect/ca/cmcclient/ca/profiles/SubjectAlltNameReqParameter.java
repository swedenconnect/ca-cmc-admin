package se.swedenconnect.ca.cmcclient.ca.profiles;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bouncycastle.asn1.x509.GeneralName;

/**
 * Enumeration of Subject alt name request parameters supported at the request input page
 *
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
 */
@AllArgsConstructor
@Getter
public enum SubjectAlltNameReqParameter {

  altNameDnsName(GeneralName.dNSName, "DNS name"),
  altNameEmail(GeneralName.rfc822Name, "E-mail adress");

  private final int generalNameIndex;
  private final String inputLabel;

}
