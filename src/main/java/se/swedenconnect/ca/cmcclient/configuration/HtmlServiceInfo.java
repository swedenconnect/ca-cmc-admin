/*
 * Copyright (c) 2021-2023.  Agency for Digital Government (DIGG)
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

package se.swedenconnect.ca.cmcclient.configuration;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration info for service information used in HTML pages
 *
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
 */
@Component
@ConfigurationProperties(prefix = "ca-client.service-info")
@Data
@ToString
public class HtmlServiceInfo {

  /** Title heading of the service home page */
  private String homePageTitle;
  /** The prefix of the title of all HTML pages - A suitable suffix is specified in each HTML page */
  private String htmlTitlePrefix;
  /** The description meta tag content of HTML pages */
  private String htmlDescription;
  /** The Author meta tag of HTML pages */
  private String htmlAuthor;

}
