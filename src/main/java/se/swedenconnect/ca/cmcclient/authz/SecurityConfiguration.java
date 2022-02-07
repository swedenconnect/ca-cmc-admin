/*
 * Copyright (c) 2021. Agency for Digital Government (DIGG)
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.provisioning.InMemoryUserDetailsManagerConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter;

import java.util.Map;

/**
 * Configuration of Spring Boot security for the CA service
 *
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
 */
@Configuration
@EnableWebSecurity
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

  private static final String NO_PASSWORD_DECODER = "{noop}";

  @Autowired
  private UserProperties userProperties;

  @Override protected void configure(AuthenticationManagerBuilder auth) throws Exception {
    InMemoryUserDetailsManagerConfigurer<AuthenticationManagerBuilder> userDetailsManagerConfigurer = auth
      .inMemoryAuthentication();

    Map<String, UserProperties.User> userMap = userProperties.getUser();
    userMap.keySet().stream().forEach(name -> {
      UserProperties.User user = userMap.get(name);
      userDetailsManagerConfigurer.withUser(name)
        .password(NO_PASSWORD_DECODER + user.getPassword())
        .roles(user.getRole());
    });
  }

  @Override protected void configure(HttpSecurity http) throws Exception {
    http
      .authorizeRequests()
      .antMatchers(HttpMethod.GET, "/js/**").permitAll()
      .antMatchers(HttpMethod.GET, "/css/**").permitAll()
      .antMatchers(HttpMethod.GET, "/img/**").permitAll()
      .antMatchers(HttpMethod.GET, "/favicon/**").permitAll()
      .antMatchers(HttpMethod.GET, "/webjars/**").permitAll()
      .anyRequest().authenticated()
      .and()
      .formLogin()
      .loginPage("/login")
      .loginProcessingUrl("/auth")
      .usernameParameter("j_username")
      .passwordParameter("j_password")
      .permitAll()
      .and()
      .logout()
      .logoutUrl("/logout")
      .deleteCookies("JSESSIONID")
      .permitAll()
      .and()
      .csrf().disable()
      .headers().frameOptions().disable()
      .addHeaderWriter(new XFrameOptionsHeaderWriter(XFrameOptionsHeaderWriter.XFrameOptionsMode.SAMEORIGIN));
  }

}
