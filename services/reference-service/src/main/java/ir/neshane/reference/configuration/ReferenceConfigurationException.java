package ir.neshane.reference.configuration;

final class ReferenceConfigurationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    ReferenceConfigurationException(String category, String key) {
        super("Configuration validation failed [" + category + "] for " + key);
    }
}
