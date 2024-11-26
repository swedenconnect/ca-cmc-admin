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

package se.swedenconnect.ca.cmcclient.authz;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.util.Map;

/**
 * Configuration of Spring Boot security for the CA service
 *
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
 */
@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

  private static final String NO_PASSWORD_DECODER = "{noop}";

  @Autowired
  private UserProperties userProperties;

  @Bean
  public UserDetailsService userDetailsService() {
    InMemoryUserDetailsManager userDetailsService = new InMemoryUserDetailsManager();

    Map<String, UserProperties.User> userMap = userProperties.getUser();
    userMap.keySet().stream().forEach(name -> {
      UserProperties.User user = userMap.get(name);
      userDetailsService.createUser(User.withUsername(name)
        .password(NO_PASSWORD_DECODER + user.getPassword())
        .roles(user.getRole())
          .build());
    });
    return userDetailsService;
  }

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
      .authorizeHttpRequests(authorize -> authorize
        .requestMatchers(
          new AntPathRequestMatcher("/js/**", HttpMethod.GET.toString()),
          new AntPathRequestMatcher("/css/**", HttpMethod.GET.toString()),
          new AntPathRequestMatcher("/img/**", HttpMethod.GET.toString()),
          new AntPathRequestMatcher("/favicon/**", HttpMethod.GET.toString()),
          new AntPathRequestMatcher("/webjars/**", HttpMethod.GET.toString())
        )
        .permitAll()
        .anyRequest().authenticated()
      )
      .formLogin(formlogin -> formlogin
        .loginPage("/login")
        .loginProcessingUrl("/auth")
        .usernameParameter("j_username")
        .passwordParameter("j_password")
        .permitAll()
      )
      .logout(logout -> logout
        .logoutUrl("/logout")
        .deleteCookies("JSESSIONID")
        .permitAll()
      )
      .csrf(AbstractHttpConfigurer::disable)
      .headers(headers -> headers
        .addHeaderWriter(new XFrameOptionsHeaderWriter(XFrameOptionsHeaderWriter.XFrameOptionsMode.SAMEORIGIN)));

    return http.build();
  }

}
