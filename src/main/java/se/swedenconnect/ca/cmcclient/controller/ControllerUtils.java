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

package se.swedenconnect.ca.cmcclient.controller;

import org.springframework.ui.Model;
import se.swedenconnect.ca.cmcclient.configuration.EmbeddedLogo;
import se.swedenconnect.ca.cmcclient.configuration.HtmlServiceInfo;

import java.util.Map;

/**
 * Common functions supporting web controllers
 *
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
 */
public class ControllerUtils {

  public static String getErrorPage(Model model, String errorMessage, String instance, HtmlServiceInfo htmlServiceInfo, String bootstrapCss, Map<String, EmbeddedLogo> logoMap) {
    model.addAttribute("htmlInfo", htmlServiceInfo);
    model.addAttribute("bootstrapCss", bootstrapCss);
    model.addAttribute("logoMap", logoMap);
    model.addAttribute("errorMessage", errorMessage);
    model.addAttribute("returnUrl", "admin?instance=" + instance);
    return "general-error";
  }

}
