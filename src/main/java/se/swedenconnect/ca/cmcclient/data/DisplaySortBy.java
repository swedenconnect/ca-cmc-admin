package se.swedenconnect.ca.cmcclient.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import se.swedenconnect.ca.engine.ca.repository.SortBy;

/**
 * Description
 *
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
 */
@AllArgsConstructor
@Getter
public enum DisplaySortBy {
  issueDate("Issue date", SortBy.issueDate),
  serialNumber("Cert serial", SortBy.serialNumber);

  private String displayName;
  private SortBy sortBy;

}
