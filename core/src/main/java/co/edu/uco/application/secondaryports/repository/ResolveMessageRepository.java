package co.edu.uco.application.secondaryports.repository;

import co.edu.uco.application.secondaryports.entity.MessageData;
import co.edu.uco.application.secondaryports.entity.MessageAvailabilityData;

import java.util.List;
import java.util.Optional;

public interface ResolveMessageRepository {

    /** Finds an active message enabled in the environment and belonging to its application. */
    Optional<MessageData> findAvailableByCodeAndEnvironment(String code, String environmentId);

    /** Lists every message available in the environment, ordered by code. */
    List<MessageData> findAvailableByEnvironment(String environmentId);

    /** Checks availability and message version without loading the message payload. */
    Optional<MessageAvailabilityData> findAvailabilityByCodeAndEnvironment(String code, String environmentId);
}
