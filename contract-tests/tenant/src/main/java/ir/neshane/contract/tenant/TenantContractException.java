package ir.neshane.contract.tenant;

final class TenantContractException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    TenantContractException(String message) {
        super(message);
    }

    TenantContractException(String message, java.util.List<String> violations) {
        super(message + System.lineSeparator() + String.join(System.lineSeparator(), violations));
    }
}
