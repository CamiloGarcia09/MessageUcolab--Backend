package co.edu.uco.application.usecase.domain.message.importing;

import co.edu.uco.application.primaryports.dto.message.CreateMessageDTO;
import java.util.List;

public record ImportRowResult(ImportRow row, String normalizedCode, CreateMessageDTO message,
                              List<ImportIssue> issues) {
    public ImportRowResult {
        issues = List.copyOf(issues);
    }
}
