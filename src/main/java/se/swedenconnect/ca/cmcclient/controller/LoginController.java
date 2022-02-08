/*
 * Copyright (c) 2021-2022.  Agency for Digital Government (DIGG)
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
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import se.swedenconnect.ca.cmcclient.configuration.EmbeddedLogo;
import se.swedenconnect.ca.cmcclient.configuration.HtmlServiceInfo;

import javax.servlet.http.HttpSession;
import java.util.Map;

/**
 * Web controller for the login page
 *
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
 */
@Slf4j
@Controller
public class LoginController {

  private final Map<String, EmbeddedLogo> logoMap;
  private final HtmlServiceInfo htmlServiceInfo;
  @Value("${ca-client.config.bootstrap-css}") String bootstrapCss;

  @Autowired
  public LoginController(Map<String, EmbeddedLogo> logoMap, HttpSession httpSession, HtmlServiceInfo htmlServiceInfo) {
    this.logoMap = logoMap;
    this.htmlServiceInfo = htmlServiceInfo;
  }

  @GetMapping("/login")
  public String login(Model model){

    model.addAttribute("bootstrapCss", bootstrapCss);
    model.addAttribute("logoMap", logoMap);
    model.addAttribute("htmlInfo", htmlServiceInfo);

    return "login-page";
  }
}
