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

package se.swedenconnect.ca.cmcclient.ca.request;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.cmc.ExtensionReq;
import org.bouncycastle.asn1.pkcs.Attribute;
import org.bouncycastle.asn1.x500.AttributeTypeAndValue;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.CertException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCSException;
import org.bouncycastle.util.encoders.Base64;
import se.swedenconnect.ca.cmcclient.ca.profiles.AttrReqParameter;
import se.swedenconnect.ca.cmcclient.ca.profiles.SubjectAlltNameReqParameter;
import se.swedenconnect.ca.engine.ca.attribute.CertAttributes;
import se.swedenconnect.ca.cmcclient.ca.PublicKeyPolicyException;
import se.swedenconnect.ca.cmcclient.ca.PublicKeyValidator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.security.PublicKey;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Extract request data from a string representing a certificate or a PKCS#10 request.
 * <p>
 * The data can be either in PEM format or as raw Base64 encoded binary data with whitespace
 *
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
 */
@Slf4j
public class RequestData {

  /** The string represented the certificate or pkcs#10 request */
  protected final String requestObject;
  /** The resulting request data */
  @Getter protected RequestDataResult requestDataResult;
  /** The public key extracted from the request data */
  @Getter protected PublicKey publicKey;
  protected X509CertificateHolder requestCertificate;
  protected PKCS10CertificationRequest pkcs10Request;
  protected Map<String, String> fixedValueMap;

  public RequestData(String requestObject, PublicKeyValidator publicKeyValidator) {
    this(requestObject, publicKeyValidator, null);
  }

  public RequestData(String requestObject, PublicKeyValidator publicKeyValidator, Map<String, String> fixedValueMap) {
    this.fixedValueMap = fixedValueMap == null ? new HashMap<>() : fixedValueMap;
    this.requestObject = requestObject;
    this.requestDataResult = new RequestDataResult();
    // Extract certificate or PKCS#10 request
    parseRequestData();
    // Validate signatures and proof of possession (self signed property)
    validateSignatures();
    if (requestDataResult.getErrorMessage() != null) {
      //We have an error end here:
      return;
    }

    try {
      publicKeyValidator.validatePublicKey(publicKey);
    }
    catch (PublicKeyPolicyException e) {
      requestDataResult = new RequestDataResult("Illegal public key in request - " + e.getMessage());
      return;
    }

    if (requestCertificate != null) {
      // Extract request data from cert
      getDataFromCert();
    }
    if (pkcs10Request != null) {
      // Extract request data from pkcs#10 request
      getDataFromPkcs10();
    }
  }

  /**
   * Validate that the certificate or PKCS#10 request is self signed and therefore satisfies the
   * requirements for proof of possession of the private signing key.
   */
  protected void validateSignatures() {
    if (requestCertificate != null) {
      try {
        JcaContentVerifierProviderBuilder builder = new JcaContentVerifierProviderBuilder().setProvider("BC");
        boolean signatureValid = requestCertificate.isSignatureValid(builder.build(requestCertificate.getSubjectPublicKeyInfo()));
        if (signatureValid) {
          this.publicKey = BouncyCastleProvider.getPublicKey(requestCertificate.getSubjectPublicKeyInfo());
        }
        else {
          requestDataResult = new RequestDataResult("Invalid certificate input - The provided certificate is not self signed");
        }
      }
      catch (OperatorCreationException | IOException | CertException e) {
        // certificate signature validation failed
        log.debug("Invalid certificate input - Error processing certificate signature - {}", e.getMessage());
        requestDataResult = new RequestDataResult("Invalid certificate input - The provided certificate is not self signed");
        return;
      }
    }
    if (pkcs10Request != null) {
      try {
        JcaContentVerifierProviderBuilder builder = new JcaContentVerifierProviderBuilder().setProvider("BC");
        boolean signatureValid = pkcs10Request.isSignatureValid(builder.build(pkcs10Request.getSubjectPublicKeyInfo()));
        if (signatureValid) {
          this.publicKey = BouncyCastleProvider.getPublicKey(pkcs10Request.getSubjectPublicKeyInfo());
        }
        else {
          requestDataResult = new RequestDataResult("Invalid PKCS#10 request input - The provided request is not self signed");
        }
      }
      catch (OperatorCreationException | PKCSException | IOException e) {
        log.debug("Invalid PKCS#10 request input - Error parsing request signature - {}", e.getMessage());
        requestDataResult = new RequestDataResult("Invalid PKCS#10 request input - The provided request is not self signed");
      }
    }
  }

  private void getDataFromPkcs10() {
    GeneralNames subjAltName = null;
    try {
      Attribute[] attributes = pkcs10Request.getAttributes(new ASN1ObjectIdentifier("1.2.840.113549.1.9.14"));
      for (int i = 0; i < attributes.length; i++) {
        ASN1Sequence exReqSeq = ASN1Sequence.getInstance(attributes[i].getAttrValues().getObjectAt(0));
        ExtensionReq extensionReq = ExtensionReq.getInstance(exReqSeq);
        Extension[] extensions = extensionReq.getExtensions();
        for (Extension extension : extensions) {
          if (extension.getExtnId().equals(Extension.subjectAlternativeName)) {
            subjAltName = GeneralNames.getInstance(extension.getParsedValue());
          }
        }
      }
    }
    catch (Exception ignored) {
      // The PKCS#10 request lacks a SubjectAltName extension
    }
    addNameDataToResult(pkcs10Request.getSubject(), subjAltName);
  }

  private void getDataFromCert() {
    GeneralNames subjAltName = null;
    try {
      subjAltName = GeneralNames.getInstance(requestCertificate.getExtension(Extension.subjectAlternativeName).getParsedValue());
    }
    catch (Exception ex) {
      // The certificate did not contain a SubjectAltNameExtension
    }
    addNameDataToResult(requestCertificate.getSubject(), subjAltName);
  }

  protected void addNameDataToResult(X500Name name, GeneralNames subjAltName) {
    final Map<String, String> attributeValueMap = requestDataResult.getAttributeValueMap();
    try {
      RDN[] rdNs = name.getRDNs();
      for (RDN rdn : rdNs) {
        AttributeTypeAndValue[] typesAndValues = rdn.getTypesAndValues();
        for (AttrReqParameter attr : AttrReqParameter.values()) {
          for (AttributeTypeAndValue atav : typesAndValues) {
            ASN1ObjectIdentifier attrOid = atav.getType();
            if (attr.getAttributeOid().equals(attrOid)) {
              if (fixedValueMap.containsKey(attr.name())) {
                attributeValueMap.put(attr.name(), fixedValueMap.get(attr.name()));
              }
              else {
                attributeValueMap.put(attr.name(), getSafeStringValue(atav.getValue()));
              }
            }
          }
        }
      }
    }
    catch (Exception ex) {
      this.requestDataResult = new RequestDataResult("Error parsing subject name - " + ex.getMessage());
    }
    try {
      if (subjAltName != null) {
        Map<String, List<String>> subjAltNameMap = new HashMap<>();
        GeneralName[] names = subjAltName.getNames();
        for (GeneralName genName : names) {
          for (SubjectAlltNameReqParameter subjAltNameType : SubjectAlltNameReqParameter.values()) {
            if (genName.getTagNo() == subjAltNameType.getGeneralNameIndex()) {
              List<String> valueList = subjAltNameMap.containsKey(subjAltNameType.name())
                ? subjAltNameMap.get(subjAltNameType.name())
                : new ArrayList<>();
              valueList.add(getSafeStringValue(genName.getName(), true));
              subjAltNameMap.put(subjAltNameType.name(), valueList);
            }
          }
        }
        final Set<String> availableSubjAltNames = subjAltNameMap.keySet();
        for (String key : availableSubjAltNames) {
          // Override if a fixed value is set by profile
          if (fixedValueMap.containsKey(key)) {
            attributeValueMap.put(key, fixedValueMap.get(key));
          }
          else {
            attributeValueMap.put(key, String.join(", ", subjAltNameMap.get(key)));
          }
        }
      }
    }
    catch (Exception ex) {
      this.requestDataResult = new RequestDataResult("Error parsing subject alternative name - " + ex.getMessage());
    }
  }

  protected String getSafeStringValue(ASN1Encodable value) {
    return getSafeStringValue(value, true);
  }

  protected String getSafeStringValue(ASN1Encodable value, boolean email) {
    try {
      if (value instanceof DERPrintableString) {
        return validateString(((DERPrintableString) value).getString(), email);
      }
      if (value instanceof DERUTF8String) {
        return validateString(((DERUTF8String) value).getString(), email);
      }
      if (value instanceof DERIA5String) {
        return validateString(((DERIA5String) value).getString(), email);
      }
      if (value instanceof ASN1GeneralizedTime) {
        return validateString(((ASN1GeneralizedTime) value).getTimeString().substring(0, 8), email);
      }
      return validateString(value.toString(), email);
    }
    catch (Exception ex) {
      //The string fails validation
      throw new IllegalArgumentException("Unsafe or illegal content in subject name attribute - " + ex.getMessage());
    }

  }

  /**
   * Characters / patterns that must never appear in a certificate attribute value because
   * they are the building blocks of HTML/JavaScript injection:
   * <ul>
   *   <li>{@code '<'} and {@code '>'} — would allow HTML/XML tag construction.</li>
   *   <li>Any {@code '&'} followed somewhere later by {@code ';'} with no whitespace
   *       between them — i.e. {@code '&'} and {@code ';'} occurring in the same
   *       whitespace-delimited word, in that order. That is the shape of an HTML entity
   *       ({@code &name;} or {@code &#nnn;}), so blocking it prevents any entity from
   *       being expressed (e.g. {@code &amp;}, {@code &#x3C;}, {@code &lt;}). A bare
   *       {@code '&'} whose word does not also contain a later {@code ';'} stays allowed,
   *       so names such as "AT&amp;T", "R&amp;D Group AB", or "Smith &amp; Jones AB"
   *       pass through.</li>
   *   <li>The literal sequence <code>&#92;u</code> or <code>&#92;U</code> — the
   *       JavaScript/JSON Unicode escape prefix (e.g. <code>&#92;u003C</code> or
   *       <code>&#92;u{3C}</code>). Although HTML parsers do not decode these,
   *       certificate values may be consumed by external tools that feed them through a
   *       JavaScript or JSON parser; blocking the prefix closes that avenue regardless
   *       of where the value ends up.</li>
   *   <li>U+0000 .. U+001F and U+007F — ASCII control characters, used for parser-confusion
   *       attacks and embedded newlines in DN values.</li>
   * </ul>
   * Together these guarantee that no HTML tag, HTML entity, or JavaScript Unicode escape
   * can be expressed inside the value, regardless of how a downstream tool processes it.
   */
  private static final Pattern DN_DISALLOWED =
      Pattern.compile("[<>\\u0000-\\u001F\\u007F]|&[^\\s]*;|\\\\[uU]");

  /**
   * Validate a string value intended for inclusion in an X.509 certificate attribute
   * (subject DN or SAN component).
   * <p>
   * The validator rejects characters that could form HTML or JavaScript inside the value,
   * plus ASCII control characters. All other characters — including {@code +}, apostrophe,
   * parentheses, {@code @}, accented letters and other Unicode — are accepted; structural
   * validation appropriate to each ASN.1 string type is performed downstream by the
   * attribute encoder.
   *
   * @param string string value to validate
   * @return the input string unchanged, if valid
   * @throws IllegalArgumentException if the string is null, too long, or contains
   *                                  disallowed content
   */
  public static String validateString(String string) throws IllegalArgumentException {
    return validateString(string, true);
  }

  /**
   * Validate a string value intended for inclusion in an X.509 certificate attribute.
   *
   * @param string string value to validate
   * @param email  retained for binary compatibility with prior versions; the previous
   *               implementation used an OWASP HTML sanitizer that misinterpreted '@' in
   *               email-shaped inputs, so callers passed this flag to suppress that. The
   *               current validator does not need it and the parameter is ignored.
   * @return the input string unchanged, if valid
   * @throws IllegalArgumentException if the string is null, too long, or contains
   *                                  disallowed content
   */
  public static String validateString(String string, boolean email) throws IllegalArgumentException {
    if (string == null) {
      throw new IllegalArgumentException("Null string value");
    }
    if (string.length() > 250) {
      throw new IllegalArgumentException(
          "String too long (" + string.length() + ") characters exceeds maximum of 250 characters");
    }
    if (DN_DISALLOWED.matcher(string).find()) {
      throw new IllegalArgumentException("String contained illegal content");
    }
    return string;
  }

  protected void parseRequestData() {
    if (StringUtils.isBlank(requestObject)) {
      requestDataResult = new RequestDataResult("empty");
      return;
    }

    // Attempt to read data as a PEM object
    try {
      getPemObject();
      // We found PEM object we are done
      return;
    }
    catch (Exception ignored) {
      // No PEM data was found
    }

    // Attempt to base 64 decode the input data
    byte[] rawData;
    try {
      rawData = Base64.decode(requestObject);
      if (rawData == null) {
        requestDataResult = new RequestDataResult("Invalid request data");
        return;
      }
    }
    catch (Exception ex) {
      // This is not legal base 64 data. Exit
      requestDataResult = new RequestDataResult("Invalid request data");
      return;
    }

    // Attempt to decode certificate
    try {
      requestCertificate = new X509CertificateHolder(rawData);
      return;
    }
    catch (IOException ignored) {
      // This was not a certificate. This is perfectly fine
    }

    try {
      pkcs10Request = new PKCS10CertificationRequest(rawData);
      return;
    }
    catch (IOException ignored) {
      // This was not a PKCS10 request
    }
    // Reaching this point means that we didn't find any valid request data
    requestDataResult = new RequestDataResult("Invalid request data");
  }

  /**
   * Retrieve the first encountered PEM object found in the provided input string that are of the types Certificate or PKCS10Request
   *
   * @throws IOException              on error parsing data
   * @throws IllegalArgumentException if no PEM data is found
   */
  protected void getPemObject() throws IOException {
    Reader rdr = new BufferedReader(new StringReader(requestObject));
    PEMParser parser = new PEMParser(rdr);
    Object o;
    while ((o = parser.readObject()) != null) {
      if (o instanceof PKCS10CertificationRequest) {
        pkcs10Request = (PKCS10CertificationRequest) o;
        return;
      }
      if (o instanceof X509CertificateHolder) {
        requestCertificate = (X509CertificateHolder) o;
        return;
      }
    }
    throw new IllegalArgumentException("No PEM object found");
  }
}
