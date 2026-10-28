package co.edu.uco.application.secondaryports.repository;

import co.edu.uco.application.usecase.domain.message.importing.MessageImportPreview;
import co.edu.uco.application.usecase.domain.message.importing.MessageImportReceipt;
import java.util.Optional;

public interface MessageImportRepository {
    Optional<MessageImportReceipt> findReceipt(String importId, String applicationId);

    MessageImportReceipt createAtomically(MessageImportPreview preview, String environmentStateId);
}
