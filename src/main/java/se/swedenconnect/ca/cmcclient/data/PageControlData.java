/*
 * Copyright 2024.  Agency for Digital Government (DIGG)
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

package se.swedenconnect.ca.cmcclient.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import se.swedenconnect.ca.engine.ca.repository.SortBy;

/**
 * Data communicated back to the page setup. THis class is used by Thymeleaf to setup web-pages
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
