package se.swedenconnect.ca.cmcclient.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import se.swedenconnect.ca.engine.ca.repository.SortBy;

/**
 * Description
 *
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PageControlData {

  int page;
  int pageSize;
  int numberOfPages;
  SortBy sortBy;
  boolean descending;

}
