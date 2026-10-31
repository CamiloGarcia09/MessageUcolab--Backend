package co.edu.uco.application.secondaryports.repository;

import java.util.Optional;

public interface ApplicationLanguageRepository {
    Optional<String> findBaseLanguageCode(String applicationId);
}
