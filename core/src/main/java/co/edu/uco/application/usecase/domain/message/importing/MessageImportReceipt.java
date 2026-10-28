package co.edu.uco.application.usecase.domain.message.importing;

import java.time.Instant;
import java.util.List;

public record MessageImportReceipt(String importId, int count, List<String> environmentIds, Instant completedAt) {
    public MessageImportReceipt {
        environmentIds = List.copyOf(environmentIds);
    }
}
