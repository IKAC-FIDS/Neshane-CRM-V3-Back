package ir.neshane.customer.application.port.in;

import ir.neshane.customer.application.document.catalog.query.DocumentUploadMetadataQuery;
import ir.neshane.customer.application.document.catalog.query.DocumentUploadMetadataResult;

public interface DocumentUploadMetadataUseCase {

    DocumentUploadMetadataResult execute(
            DocumentUploadMetadataQuery query);
}