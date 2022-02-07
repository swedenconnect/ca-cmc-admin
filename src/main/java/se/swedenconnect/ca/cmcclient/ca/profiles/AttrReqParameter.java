package se.swedenconnect.ca.cmcclient.ca.profiles;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import se.swedenconnect.ca.engine.ca.attribute.CertAttributes;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Enumeration of supported subject field attributes that may appear in the certificate subject name request page
 *
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
 */
@AllArgsConstructor
@Getter
public enum AttrReqParameter {
  commonName(5, CertAttributes.CN, "Common name"),
  givenName(7,CertAttributes.GIVENNAME, "Given name"),
  surname(6,CertAttributes.SURNAME, "Surname"),
  serialNumber(4, CertAttributes.SERIALNUMBER, "Serial number"),
  organizationName(1,CertAttributes.O, "Organization name"),
  orgUnitName(2, CertAttributes.OU, "Organizational unit name"),
  orgIdentifier(3, CertAttributes.ORGANIZATION_IDENTIFIER, "Organization identifier"),
  country(0, CertAttributes.C, "Country"),
  title(10, CertAttributes.T, "Title"),
  locality(11, CertAttributes.L, "Locality");

  private final int index;
  private final ASN1ObjectIdentifier attributeOid;
  private final String inputLabel;

  public static List<AttrReqParameter> getPrioritySortedList(){
    return Arrays.stream(values())
      .sorted(Comparator.comparingInt(AttrReqParameter::getIndex))
      .collect(Collectors.toList());
  }

}
