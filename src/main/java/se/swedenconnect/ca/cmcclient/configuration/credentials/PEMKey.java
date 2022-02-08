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

package se.swedenconnect.ca.cmcclient.configuration.credentials;

import lombok.Getter;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.pkcs.PKCSException;
import org.springframework.core.io.Resource;

import java.io.*;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.List;

/**
 * Operations related to a key imported in PEM format where the key may be encrypted under a password.
 *
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
 */
public class PEMKey {

  @Getter PrivateKey privateKey;

  /**
   * Constructor, extracting any existing private key from a resource
   * @param location
   * @param password
   * @throws IOException
   * @throws OperatorCreationException
   * @throws PKCSException
   */
  public PEMKey(Resource location, String password) throws IOException, OperatorCreationException, PKCSException {
    final List<Object> pemObjects = getPemObjects(location.getInputStream(), password);
    privateKey = pemObjects.stream()
      .filter(o -> o instanceof KeyPair || o instanceof PrivateKey)
      .map(o -> {
        if (o instanceof KeyPair) {
          return ((KeyPair) o).getPrivate();
        }
        return (PrivateKey) o;
      }).findFirst().orElse(null);
  }

  /**
   * Retrieve a list of PEM objects found in the provided input stream that are of the types PrivateKey (Encrypted or Plaintext), KeyPair or certificate
   * @param is Inputstream with the PEM resources
   * @param password Optional Password for decrypting PKCS8 private key
   * @return A list of objects (PrivateKey, KeyPair or X509CertificateHolder)
   * @throws IOException
   * @throws OperatorCreationException
   * @throws PKCSException
   */
  public static List<Object> getPemObjects(InputStream is, String password) throws IOException, OperatorCreationException, PKCSException {
    List<Object> pemObjList = new ArrayList<>();
    JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
    Reader rdr = new BufferedReader(new InputStreamReader(is));
    PEMParser parser = new PEMParser(rdr);
    Object o;
    while ((o = parser.readObject()) != null) {
      if (o instanceof KeyPair) {
        pemObjList.add(o);
      }
      if (o instanceof PrivateKeyInfo) {
        PrivateKey privateKey = converter.getPrivateKey(PrivateKeyInfo.getInstance(o));
        pemObjList.add(privateKey);
      }
      if (o instanceof PKCS8EncryptedPrivateKeyInfo && password !=null){
        InputDecryptorProvider pkcs8Prov = new JceOpenSSLPKCS8DecryptorProviderBuilder().build(password.toCharArray());
        PrivateKey privateKey = converter.getPrivateKey(((PKCS8EncryptedPrivateKeyInfo) o).decryptPrivateKeyInfo(pkcs8Prov));
        pemObjList.add(privateKey);
      }
      if (o instanceof X509CertificateHolder){
        pemObjList.add(o);
      }
    }
    return pemObjList;
  }

}
