package se.swedenconnect.ca.cmcclient.configuration.profile;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Description
 *
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
 */
@Configuration
public class CertificateProfileConfiguration {

  @Bean
  Map<String, CertificateProfileProperties.Profile> propertyProfileDataMap(CertificateProfileProperties certificateProfileProperties) {

    final CertificateProfileProperties.Profile defaultValues = certificateProfileProperties.getDefaultValue();
    Map<String, CertificateProfileProperties.Profile> propertyProfileDataMap = new HashMap<>();
    final Map<String, CertificateProfileProperties.Profile> propertyProfiles = certificateProfileProperties.getProfile();
    if (propertyProfiles == null){
      return propertyProfileDataMap;
    }
    for (String key : propertyProfiles.keySet()){
      CertificateProfileProperties.Profile pp = propertyProfiles.get(key);
      CertificateProfileProperties.Profile combined = CertificateProfileProperties.Profile.builder()
        .requestAttributes(pp.getRequestAttributes() != null ? pp.getRequestAttributes() : defaultValues.getRequestAttributes())
        .requestSubjAltNames(pp.getRequestSubjAltNames() != null ? pp.getRequestSubjAltNames() : defaultValues.getRequestSubjAltNames())
        .requestEku(pp.getRequestEku() != null ? pp.getRequestEku() : defaultValues.getRequestEku())
        .requestOther(pp.getRequestOther() != null ? pp.getRequestOther() : defaultValues.getRequestOther())
        .requestFixedValue(pp.getRequestFixedValue() != null ? pp.getRequestFixedValue() : defaultValues.getRequestFixedValue())
        .includeAki(pp.getIncludeAki() != null ? pp.getIncludeAki() : defaultValues.getIncludeAki())
        .inlcudeSki(pp.getInlcudeSki() != null ? pp.getInlcudeSki() : defaultValues.getInlcudeSki())
        .includeCrlDp(pp.getIncludeCrlDp() != null ? pp.getIncludeCrlDp() : defaultValues.getIncludeCrlDp())
        .includeOcspUrl(pp.getIncludeOcspUrl() != null ? pp.getIncludeOcspUrl() : defaultValues.getIncludeOcspUrl())
        .policy(pp.getPolicy() != null ? pp.getPolicy() : defaultValues.getPolicy())
        .anyPolicy(pp.getAnyPolicy() != null ? pp.getAnyPolicy() : defaultValues.getAnyPolicy())
        .policyCritical(pp.getPolicyCritical() != null ? pp.getPolicyCritical() : defaultValues.getPolicyCritical())
        .eku(pp.getEku() != null ? pp.getEku() : defaultValues.getEku())
        .ekuCritical(pp.getEkuCritical() != null ? pp.getEkuCritical() : defaultValues.getEkuCritical())
        .ca(pp.getCa() != null ? pp.getCa() : defaultValues.getCa())
        .bcCritical(pp.getBcCritical() != null ? pp.getBcCritical() : defaultValues.getBcCritical())
        .keyUsages(pp.getKeyUsages() != null ? pp.getKeyUsages() : defaultValues.getKeyUsages())
        .keyUsageCritical(pp.getKeyUsageCritical() != null ? pp.getKeyUsageCritical() : defaultValues.getKeyUsageCritical())
        .subjAltNameCritical(pp.getSubjAltNameCritical() != null ? pp.getSubjAltNameCritical() : defaultValues.getSubjAltNameCritical())
        .build();
      propertyProfileDataMap.put(key, combined);
    }
    return propertyProfileDataMap;
  }
}
