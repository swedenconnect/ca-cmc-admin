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

package se.swedenconnect.ca.cmcclient.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import se.swedenconnect.ca.cmcclient.authz.CurrentUser;
import se.swedenconnect.ca.cmcclient.configuration.EmbeddedLogo;
import se.swedenconnect.ca.cmcclient.configuration.HtmlServiceInfo;
import se.swedenconnect.ca.cmcclient.configuration.cmc.CMCInstanceParams;
import se.swedenconnect.ca.cmcclient.configuration.cmc.CMCProperties;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * Web controller for the main service page
 *
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
 */
@Slf4j
@Controller
public class CAClientMainController {

  private final CMCProperties cmcProperties;
  private final Map<String, EmbeddedLogo> logoMap;
  private final HtmlServiceInfo htmlServiceInfo;
  @Value("${ca-client.config.bootstrap-css}") String bootstrapCss;

  @Autowired
  public CAClientMainController(CMCProperties cmcProperties, HtmlServiceInfo htmlServiceInfo,
    Map<String, EmbeddedLogo> logoMap) {
    this.cmcProperties = cmcProperties;
    this.logoMap = logoMap;
    this.htmlServiceInfo = htmlServiceInfo;
  }

  @RequestMapping("/main")
  public String mainPageRedirect(){
    return "redirect:/";
  }

  @RequestMapping("/")
  public String mainPage(HttpServletRequest servletRequest, Model model, Authentication authentication){

    // Get current user
    CurrentUser currentUser = new CurrentUser(authentication);
    model.addAttribute("currentUser", currentUser);
    final Map<String, CMCInstanceParams> cmcInstanceConfig = cmcProperties.getInstance();
    List<String> sortedInstances = new ArrayList<>(cmcInstanceConfig.keySet());
    Collections.sort(sortedInstances, Comparator.comparingInt(instance -> cmcInstanceConfig.get(instance).getIndex()));

    // Set base model attributes
    model.addAttribute("bootstrapCss", bootstrapCss);
    model.addAttribute("logoMap", logoMap);
    model.addAttribute("cmcConfig", cmcInstanceConfig);
    model.addAttribute("htmlInfo", htmlServiceInfo);
    model.addAttribute("sortedInstances", sortedInstances);

    return "main-page";
  }


}
