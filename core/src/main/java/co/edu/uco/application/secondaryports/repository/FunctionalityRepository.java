package co.edu.uco.application.secondaryports.repository;

import co.edu.uco.application.secondaryports.entity.FunctionalityData;

public interface FunctionalityRepository {

    boolean existsByNameAndApplicationId(String name, String applicationId);

    void create(FunctionalityData functionality, String stateId);
}