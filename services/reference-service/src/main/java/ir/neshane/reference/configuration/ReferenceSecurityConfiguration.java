package ir.neshane.reference.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods = false)
class ReferenceSecurityConfiguration {

    @Bean
    SecretMaterialSource secretMaterialSource() {
        return new SystemSecretMaterialSource();
    }

    @Bean
    ReferenceStartupValidator referenceStartupValidator(
            Environment environment,
            ReferenceServiceProperties serviceProperties,
            ReferenceSecurityProperties securityProperties,
            SecretMaterialSource materialSource) {
        return new ReferenceStartupValidator(
                environment,
                serviceProperties,
                securityProperties,
                new ExternalSecretContract(materialSource));
    }
}
