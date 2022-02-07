package se.swedenconnect.ca.cmcclient.ca.profiles;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Description
 *
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
 */
@AllArgsConstructor
@Getter
public enum OtherReqParameters {
  otherParamsPolicy("Certificate policy"),
  otherParamsKeyUsage("Key usage");

  private final String inputLabel;
}
