package co.edu.uco.application.secondaryports.repository;

import java.util.List;
import java.util.Optional;

public interface UpdateFunctionalityStateRepository {
    boolean existsInApplication(String functionalityId, String applicationId);

    Optional<String> findStateNameById(String stateId);

    /** Updates the functionality and its messages atomically; null preserves message states. */
    List<String> updateState(String functionalityId, String applicationId, String stateId,
                             String inactiveMessageStateId);
}
