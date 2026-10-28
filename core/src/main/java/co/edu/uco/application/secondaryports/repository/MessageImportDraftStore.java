package co.edu.uco.application.secondaryports.repository;

import co.edu.uco.application.usecase.domain.message.importing.MessageImportPreview;
import java.util.Optional;

public interface MessageImportDraftStore {
    void save(MessageImportPreview preview);

    Optional<MessageImportPreview> find(String importId, String applicationId);
}
