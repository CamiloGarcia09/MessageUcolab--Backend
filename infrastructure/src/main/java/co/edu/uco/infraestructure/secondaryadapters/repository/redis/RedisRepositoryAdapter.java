package co.edu.uco.infraestructure.secondaryadapters.repository.redis;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RedisRepositoryAdapter extends CrudRepository<MessageRedis, UUID> {
    Page<MessageRedis> findByEnvironmentId(String environmentId, Pageable pageable);
    long countByEnvironmentId(String environmentId);
    Optional<MessageRedis> findByCodeAndEnvironmentId(String code, String environmentId);
    List<MessageRedis> findAllByCode(String code);
}