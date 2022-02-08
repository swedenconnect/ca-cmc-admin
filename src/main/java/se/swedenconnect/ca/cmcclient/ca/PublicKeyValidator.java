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

package se.swedenconnect.ca.cmcclient.ca;

import java.security.PublicKey;

/**
 * An interface for validating a public key to be used in a certificate request
 *
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
 */
public interface PublicKeyValidator {

  /**
   * Evaluate a public key to determine that it meets the defined security policy
   * @param publicKey public key to validate
   */
  void validatePublicKey(PublicKey publicKey) throws PublicKeyPolicyException;

}
