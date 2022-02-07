package se.swedenconnect.ca.cmcclient.ca;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.cms.CMSObjectIdentifiers;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.asn1.cms.SignedData;
import org.bouncycastle.asn1.x509.AccessDescription;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.cert.X509CertificateHolder;
import se.idsec.x509cert.extensions.SubjectInformationAccess;
import se.swedenconnect.ca.cmcclient.http.GenericHttpConnector;
import se.swedenconnect.ca.cmcclient.http.HttpResponse;
import se.swedenconnect.ca.cmcclient.utils.CertificateUtils;

import java.io.IOException;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Collector of certificates from the CA repository exported as a PKCS#7 certs only file using the URL obtained in the issuer certificate
 *
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
 */
@Slf4j
public class CaRepositoryCollector {

  /** HTTP functions to download data from a URL using HTTP GET */
  public final GenericHttpConnector httpConnector;

  /**
   * Constructor
   * @param httpConnector HTTP Get processor
   */
  public CaRepositoryCollector(GenericHttpConnector httpConnector) {
    this.httpConnector = httpConnector;
  }

  /**
   * Collects all certificates published by the sigval trust CA representing the latest set of X certificates of trusted services
   * Equivalent duplicates are not present in this certificate list. Only the latest valid certificate per entity is returned.
   *
   * @param issuerCert the certificate of the issuer that issued the certificates to download
   * @return List of current X certificates for each certified entity
   * @throws Exception thrown if a certificate list could not be obtained
   */
  public List<X509Certificate> collectCaRepositoryCertificates(X509Certificate issuerCert) throws Exception{
    final URL p7bUrl = getP7bUrl(issuerCert);
    final HttpResponse response = httpConnector.getResource(p7bUrl);
    if (response.getResponseCode() == 200){
      return getP7bCertList(response.getData(), issuerCert);
    }
    throw new IOException("Unable to download p7b data");
  }


  /**
   * Get list of valid certificates included in the
   *
   * @param p7bBytes the bytes of the pkcs#7 certs only data
   * @param issuer the issuer of the certificates in the certs only data
   * @return list of certificates
   * @throws IOException error processing pkcs#7 data
   * @throws CertificateException error parsing certificates
   */
  private List<X509Certificate> getP7bCertList(byte[] p7bBytes, X509Certificate issuer) throws IOException, CertificateException {

    List<X509Certificate> certificateList = new ArrayList<>();
    ASN1InputStream ain = new ASN1InputStream(p7bBytes);
    ContentInfo cmsContentInfo = ContentInfo.getInstance(ain.readObject());
    if (!cmsContentInfo.getContentType().equals(CMSObjectIdentifiers.signedData)) {
      throw new IOException("Illegal content type");
    }
    SignedData signedData = SignedData.getInstance(cmsContentInfo.getContent());
    ASN1Set certificates = signedData.getCertificates();
    if (certificates == null){
      log.debug("CSCA Cert List is empty. No national DSC certificates are available");
      return certificateList;
    }
    for (ASN1Encodable asn1Encodable : certificates) {
      byte[] certByte = asn1Encodable.toASN1Primitive().getEncoded("DER");
      X509Certificate certificate = CertificateUtils.decodeCertificate(certByte);
      try {
        certificate.verify(issuer.getPublicKey());
        certificateList.add(certificate);
        log.trace("Added certificate from P7b {}", certificate.getSubjectX500Principal().toString());
      }
      catch (Exception ex) {
        log.error("A certificate from the sigval trust CA P7B source failed signature validation {}", ex.getMessage(), ex);
      }
    }
    return certificateList;
  }

  /**
   * Obtain the URL to the PKCS#7 certs only file from the subject info access extension
   * @param certificate CA certificates holding the URL to its issued certificates
   * @return URL to issued certificates
   */
  public static URL getP7bUrl(X509Certificate certificate) {
    try {
      X509CertificateHolder certHolder = new X509CertificateHolder(certificate.getEncoded());
      Extension siaExt = certHolder.getExtension(Extension.subjectInfoAccess);
      SubjectInformationAccess subjInfoAccess = SubjectInformationAccess.getInstance(siaExt.getParsedValue());
      String p7bUrlStr = Arrays.stream(subjInfoAccess.getAccessDescriptions())
        .filter(accessDescription -> accessDescription.getAccessMethod().equals(SubjectInformationAccess.caRepository))
        .map(AccessDescription::getAccessLocation)
        .filter(generalName -> generalName.getTagNo() == GeneralName.uniformResourceIdentifier)
        .map(generalName -> getASN1StringValue(generalName.getName()))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("No P7b URL available"));
      return new URL(p7bUrlStr);
    }
    catch (Exception ex) {
      log.error("Failed to extract CA Repository URL from ca cert", ex);
      return null;
    }
  }

  public static String getASN1StringValue(ASN1Encodable value) {
    try {
      if (value instanceof DERPrintableString) {
        return ((DERPrintableString) value).getString();
      }
      if (value instanceof DERUTF8String) {
        return ((DERUTF8String) value).getString();
      }
      if (value instanceof DERIA5String) {
        return ((DERIA5String) value).getString();
      }
      if (value instanceof ASN1GeneralizedTime) {
        return ((ASN1GeneralizedTime) value).getTimeString().substring(0, 8);
      }
      return value.toString();
    } catch (Exception ex) {
      //The string fails validation
      throw new IllegalArgumentException("Unsafe or illegal content in subject name attribute - " + ex.getMessage());
    }
  }



}
