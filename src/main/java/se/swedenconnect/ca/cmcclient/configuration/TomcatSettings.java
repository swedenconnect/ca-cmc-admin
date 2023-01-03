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

import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.Connector;
import org.apache.commons.lang3.StringUtils;
import org.apache.coyote.ajp.AbstractAjpProtocol;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

/**
 * Tomcat settings for this service
 */
@Slf4j
@Configuration
public class TomcatSettings {
    @Value("${tomcat.ajp.port}") int ajpPort;
    @Value("${tomcat.ajp.remoteauthentication:#{false}}") String remoteAuthentication;
    @Value("${tomcat.ajp.enabled}") boolean tomcatAjpEnabled;
    @Value("${tomcat.ajp.secret:#{null}}") String ajpSecret;

    @Bean
    public ConfigurableServletWebServerFactory servletContainer() {

        TomcatServletWebServerFactory webServerFactory = new TomcatServletWebServerFactory();
        if (tomcatAjpEnabled) {
            Connector connector = new Connector("AJP/1.3");
            // The following 2 lines needs to be added to allow requests from remote web server as of Spring boot 2.3.x
            connector.setProperty("address","0.0.0.0");
            connector.setProperty("allowedRequestAttributesPattern",".*");
            connector.setPort(ajpPort);
            connector.setSecure(false);
            connector.setAllowTrace(false);
            connector.setScheme("http");
            final AbstractAjpProtocol protocol = (AbstractAjpProtocol) connector.getProtocolHandler();
            if (StringUtils.isBlank(ajpSecret)){
                log.info("Setting up tomcat AJP without secret");
                connector.setSecure(false);
                protocol.setSecretRequired(false);
            } else {
                log.info("Setting up tomcat AJP with secret in secure mode");
                connector.setSecure(true);
                protocol.setSecret(ajpSecret);
            }
            webServerFactory.addAdditionalTomcatConnectors(connector);
        }

        webServerFactory.addErrorPages(
          new ErrorPage(HttpStatus.NOT_FOUND, "/404-redirect"),
          new ErrorPage(HttpStatus.BAD_REQUEST, "/400-redirect"),
          new ErrorPage(HttpStatus.METHOD_NOT_ALLOWED, "/400-redirect"),
          new ErrorPage(HttpStatus.FORBIDDEN, "/400-redirect"),
          new ErrorPage(HttpStatus.INTERNAL_SERVER_ERROR, "/500-redirect")
        );
        return webServerFactory;
    }
}
