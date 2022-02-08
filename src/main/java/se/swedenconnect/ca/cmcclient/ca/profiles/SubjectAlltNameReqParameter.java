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

package se.swedenconnect.ca.cmcclient.ca.profiles;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bouncycastle.asn1.x509.GeneralName;

/**
 * Enumeration of Subject alt name request parameters supported at the request input page
 *
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
 */
@AllArgsConstructor
@Getter
public enum SubjectAlltNameReqParameter {

  altNameDnsName(GeneralName.dNSName, "DNS name"),
  altNameEmail(GeneralName.rfc822Name, "E-mail adress");

  private final int generalNameIndex;
  private final String inputLabel;

}
