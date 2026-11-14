package co.edu.uco.application.secondaryports.repository;

import java.util.List;
import java.util.UUID;

public interface DeleteApplicationRepository {
    boolean ownsApplication(UUID userId, UUID applicationId);

    List<UUID> deleteAtomically(UUID userId, UUID applicationId);
}
