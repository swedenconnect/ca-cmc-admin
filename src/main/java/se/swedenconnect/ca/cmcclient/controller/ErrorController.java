package se.swedenconnect.ca.cmcclient.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import se.swedenconnect.ca.cmcclient.configuration.EmbeddedLogo;
import se.swedenconnect.ca.cmcclient.configuration.HtmlServiceInfo;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Controller
public class ErrorController {

    private static final String HTTP_ERROR_PAGE = "http-error";
    private static final String ERROR_MESSAGE = "message";
    private static final String ERROR_CODE = "errorCode";

    private final HtmlServiceInfo htmlServiceInfo;

    @Value("${server.servlet.context-path}") String contextPath;
    @Value("${ca-client.config.bootstrap-css}") String bootstrapCss;

    private ApplicationEventPublisher applicationEventPublisher;
    private final Map<String, EmbeddedLogo> logoMap;

    @Autowired
    public ErrorController(Map<String, EmbeddedLogo> logoMap, HtmlServiceInfo htmlServiceInfo) {
        this.logoMap = logoMap;
        this.htmlServiceInfo = htmlServiceInfo;
    }

    @RequestMapping("/400-redirect")
    public String errorRedirect400(){
        return "redirect:/bad-request";
    }

    @RequestMapping("/404-redirect")
    public String errorRedirect404(){
        return "redirect:/not-found";
    }

    @RequestMapping("/500-redirect")
    public String errorRedirect500(){
        return "redirect:/internal-error";
    }

    @RequestMapping("/not-found")
    public String get404Error(Model model) {
        model.addAttribute(ERROR_MESSAGE, "Requested service or page is not available");
        model.addAttribute(ERROR_CODE, "404");
        model.addAttribute("logoMap", logoMap);
        model.addAttribute("bootstrapCss", bootstrapCss);
        model.addAttribute("htmlInfo", htmlServiceInfo);
        return HTTP_ERROR_PAGE;
    }

    @RequestMapping("/bad-request")
    public String get400Error(Model model, HttpServletRequest request) {
        model.addAttribute(ERROR_MESSAGE, "Illegal Request for service");
        model.addAttribute(ERROR_CODE, "400");
        model.addAttribute("logoMap", logoMap);
        model.addAttribute("bootstrapCss", bootstrapCss);
        model.addAttribute("htmlInfo", htmlServiceInfo);
        return HTTP_ERROR_PAGE;
    }

    @RequestMapping("/internal-error")
    public String get500Error(Model model) {
        model.addAttribute(ERROR_MESSAGE, "The request generated an internal error");
        model.addAttribute(ERROR_CODE, "500");
        model.addAttribute("logoMap", logoMap);
        model.addAttribute("bootstrapCss", bootstrapCss);
        model.addAttribute("htmlInfo", htmlServiceInfo);
        return HTTP_ERROR_PAGE;
    }

}
