package ir.neshane.customer.application.document.catalog.usecase;

import ir.neshane.customer.application.port.in.DocumentUploadMetadataUseCase;
import ir.neshane.customer.application.document.catalog.query.DocumentUploadMetadataQuery;
import ir.neshane.customer.application.document.catalog.query.DocumentUploadMetadataResult;
import ir.neshane.customer.domain.document.DocumentTypeCatalog;

import java.util.Objects;

/**
 * Evaluates synthetic document metadata.
 * No file access, malware inspection or storage integration.
 */
public final class AssessDocumentUploadMetadata
        implements DocumentUploadMetadataUseCase {

    @Override
    public DocumentUploadMetadataResult execute(
            DocumentUploadMetadataQuery query) {

        Objects.requireNonNull(query, "QUERY_REQUIRED");

        var assessment = query.catalog().assessUpload(
                query.document());

        if (assessment.outcome()
                == DocumentTypeCatalog.Outcome.ELIGIBLE_CANDIDATE) {

            return new DocumentUploadMetadataResult(
                    DocumentUploadMetadataResult.Status.ELIGIBLE_CANDIDATE,
                    "DOCUMENT_UPLOAD_METADATA_CANDIDATE",
                    assessment.auditRequired());
        }

        return new DocumentUploadMetadataResult(
                DocumentUploadMetadataResult.Status.BLOCKED,
                safeCode(assessment.code()),
                assessment.auditRequired());
    }

    private static String safeCode(String code) {

        if (code == null) {
            return "DOCUMENT_METADATA_ASSESSMENT_BLOCKED";
        }

        return switch (code) {

            case "DOCUMENT_TYPE_SCOPE_MISMATCH",
                 "DOCUMENT_TYPE_UNKNOWN",
                 "DOCUMENT_TYPE_NOT_SELECTABLE",
                 "DOCUMENT_TYPE_NOT_APPLICABLE",
                 "DOCUMENT_TYPE_SINGLE_FILE_REQUIRED",
                 "DOCUMENT_TYPE_EXPIRY_NOT_APPLICABLE",
                 "DOCUMENT_TYPE_FILE_FORMAT_NOT_ALLOWED",
                 "DOCUMENT_TYPE_FILE_SIZE_EXCEEDED" ->
                    code;

            default ->
                "DOCUMENT_METADATA_ASSESSMENT_BLOCKED";
        };
    }
}