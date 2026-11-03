package ir.neshane.reference.configuration;

import java.util.Arrays;
import java.util.Set;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;

final class ReferenceStartupValidator implements InitializingBean {
    private static final Set<String> SUPPORTED_PROFILES = Set.of("development", "test", "production");

    private final Environment environment;
    private final ReferenceServiceProperties serviceProperties;
    private final ReferenceSecurityProperties securityProperties;
    private final ExternalSecretContract secretContract;

    ReferenceStartupValidator(
            Environment environment,
            ReferenceServiceProperties serviceProperties,
            ReferenceSecurityProperties securityProperties,
            ExternalSecretContract secretContract) {
        this.environment = environment;
        this.serviceProperties = serviceProperties;
        this.securityProperties = securityProperties;
        this.secretContract = secretContract;
    }

    @Override
    public void afterPropertiesSet() {
        String profile = deploymentProfile();
        if ("production".equals(profile) && serviceProperties.diagnostics().enabled()) {
            throw new ReferenceConfigurationException(
                    "incompatible-production-diagnostics", "neshane.reference.diagnostics.enabled");
        }
        secretContract.validate(securityProperties.requiredSecret(), "production".equals(profile));
    }

    private String deploymentProfile() {
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length != 1 || !SUPPORTED_PROFILES.contains(activeProfiles[0])) {
            throw new ReferenceConfigurationException(
                    "unsupported-deployment-profile",
                    "spring.profiles.active (expected exactly one of "
                            + String.join(",", SUPPORTED_PROFILES)
                            + "; active-count="
                            + activeProfiles.length
                            + ")");
        }
        String requestedProfile = environment.getProperty("NESHANE_DEPLOYMENT_PROFILE");
        if (requestedProfile != null
                && !requestedProfile.isBlank()
                && !activeProfiles[0].equals(requestedProfile.trim())) {
            throw new ReferenceConfigurationException(
                    "conflicting-deployment-profile", "NESHANE_DEPLOYMENT_PROFILE/spring.profiles.active");
        }
        return Arrays.stream(activeProfiles).findFirst().orElseThrow();
    }
}
