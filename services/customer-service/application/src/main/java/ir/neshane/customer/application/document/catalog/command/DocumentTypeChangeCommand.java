package ir.neshane.customer.application.document.catalog.command;

import ir.neshane.customer.domain.document.DocumentRequirementPolicy;
import ir.neshane.customer.domain.document.DocumentTypeCatalog;
import ir.neshane.customer.domain.document.DocumentTypeDefinition;

import java.util.Objects;

/**
 * Internal synthetic document type management assessment.
 */
public record DocumentTypeChangeCommand(
        DocumentRequirementPolicy policy,
        String expectedRevision,
        String nextRevision,
        DocumentTypeDefinition definition,
        DocumentTypeCatalog.Authority authority,
        Operation operation) {

    public enum Operation {
        ADD,
        REVISE
    }

    public DocumentTypeChangeCommand {
        Objects.requireNonNull(policy);
        Objects.requireNonNull(expectedRevision);
        Objects.requireNonNull(nextRevision);
        Objects.requireNonNull(definition);
        Objects.requireNonNull(authority);
        Objects.requireNonNull(operation);
    }

    @Override
    public String toString() {
        return "DocumentTypeChangeCommand[redacted]";
    }
}