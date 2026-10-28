package co.edu.uco.application.usecase.domain.message.importing;

import java.time.Instant;
import java.util.List;

public record MessageImportPreview(String importId, String applicationId, List<String> environmentIds,
                                    List<ImportRowResult> rows, Instant expiresAt) {
    public MessageImportPreview {
        environmentIds = List.copyOf(environmentIds);
        rows = List.copyOf(rows);
    }
}
