package se.swedenconnect.ca.cmcclient.controller;

import org.springframework.ui.Model;
import se.swedenconnect.ca.cmcclient.configuration.EmbeddedLogo;
import se.swedenconnect.ca.cmcclient.configuration.HtmlServiceInfo;

import java.util.Map;

/**
 * Description
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
