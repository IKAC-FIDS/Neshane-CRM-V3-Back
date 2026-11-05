package ir.neshane.referencepeer.configuration;

import java.util.Set;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;

final class PeerStartupValidator implements InitializingBean {
    private static final Set<String> SUPPORTED_PROFILES = Set.of("development", "test", "production");
    private final Environment environment;

    PeerStartupValidator(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void afterPropertiesSet() {
        String[] active = environment.getActiveProfiles();
        if (active.length != 1 || !SUPPORTED_PROFILES.contains(active[0])) {
            throw new IllegalStateException(
                    "reference-peer-service requires exactly one supported deployment profile");
        }
        String requested = environment.getProperty("NESHANE_DEPLOYMENT_PROFILE");
        if (requested != null && !requested.isBlank() && !active[0].equals(requested.trim())) {
            throw new IllegalStateException("reference-peer-service deployment profile sources conflict");
        }
    }
}
