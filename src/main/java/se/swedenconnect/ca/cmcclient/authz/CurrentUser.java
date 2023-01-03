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

package se.swedenconnect.ca.cmcclient.authz;

import lombok.Getter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Representation of the current authenticated user in a session
 *
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
 */
public class CurrentUser {

  @Getter private String name;
  @Getter private List<String> instanceList;

  /**
   * Constructor
   * @param authentication the user authentication data
   */
  public CurrentUser(Authentication authentication) {
    Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
    UserDetails userDetails = (UserDetails) authentication.getPrincipal();
    this.name = userDetails.getUsername();
    this.instanceList = authorities != null
      ? authorities.stream()
      .map(grantedAuthority -> grantedAuthority.getAuthority())
      .filter(authority -> authority.startsWith("ROLE_"))
      .map(authority -> authority.substring(5))
      .collect(Collectors.toList())
      : new ArrayList<>();
  }

  /**
   * Checks if the current user is authorized to access administration of a specified instance
   * @param instance the CA instance
   * @return true if the current user is authorized to manage the specified instance
   */
  public boolean isAuthorizedFor(String instance) {
    return instanceList.contains(instance);
  }

}
