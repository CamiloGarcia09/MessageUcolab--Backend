package co.edu.uco.application.secondaryports.repository;

import co.edu.uco.application.usecase.domain.aggregate.entities.OrganizationEntity;

import java.util.Optional;
import java.util.UUID;

public interface OrganizationRepository {

    Optional<OrganizationEntity> findById(UUID id);

    Optional<OrganizationEntity> findByName(String name);

    void create(OrganizationEntity organization);
}
