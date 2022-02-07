package se.swedenconnect.ca.cmcclient.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import se.swedenconnect.ca.engine.ca.repository.SortBy;

/**
 * Description
 *
 *         pageCookieData = JSON.stringify({
 *             size: pageSize,
 *             sort: sortBy,
 *             page: 0,
 *             descending: false
 *
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageCookie {

  private int size;
  private SortBy sort;
  private int page;
  private boolean descending;

}
