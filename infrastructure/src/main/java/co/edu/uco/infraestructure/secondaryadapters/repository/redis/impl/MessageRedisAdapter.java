package co.edu.uco.infraestructure.secondaryadapters.repository.redis.impl;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.entity.MessageData;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.repository.CacheMessageRepository;
import co.edu.uco.application.secondaryports.repository.SimplePage;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.infraestructure.secondaryadapters.repository.data.DataMapper;
import co.edu.uco.infraestructure.secondaryadapters.repository.redis.MessageRedis;
import co.edu.uco.infraestructure.secondaryadapters.repository.redis.RedisRepositoryAdapter;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static co.edu.uco.crosscutting.helpers.UtilObject.isNullObject;
import static co.edu.uco.infraestructure.config.InfrastructureConstant.CACHE_REDIS_ADAPTER;

@Component(CACHE_REDIS_ADAPTER)
public final class MessageRedisAdapter implements CacheMessageRepository {
    private final LoggingPort log;
    private final CatalogPort catalogPort;
    private final RedisRepositoryAdapter repository;
    private final DataMapper<MessageData, MessageRedis> mapper;

    public MessageRedisAdapter(
            RedisRepositoryAdapter repository,
            DataMapper<MessageData, MessageRedis> mapper,
            CatalogPort catalogPort,
            LoggingPortFactory loggerFactory
    ) {
        this.log = loggerFactory.getLogger(MessageRedisAdapter.class);
        this.catalogPort = catalogPort;
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public void save(MessageData data) {
        if (isNullObject(data)) {
            return;
        }
        try {
            repository.save(mapper.mapperModel(data));
        } catch (DataAccessException ex) {
            log.error(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_014.getCode()), ex);
        } catch (Exception ex) {
            log.error(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_015.getCode()), ex);
        }
    }

    @Override
    public void saveWithEnvironment(MessageData data, String environmentId) {
        if (isNullObject(data)) {
            return;
        }
        try {
            var messageRedis = mapper.mapperModel(data);
            messageRedis.setEnvironmentId(environmentId);
            repository.save(messageRedis);
        } catch (DataAccessException ex) {
            log.error(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_014.getCode()), ex);
        } catch (Exception ex) {
            log.error(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_015.getCode()), ex);
        }
    }

    @Override
    public Optional<MessageData> findById(UUID id) {
        if (isNullObject(id)) {
            return Optional.empty();
        }
        try {
            return repository.findById(id).map(mapper::mapperData);
        } catch (DataAccessException ex) {
            log.error(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_014.getCode()), ex);
            return Optional.empty();
        } catch (Exception ex) {
            log.error(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_015.getCode()), ex);
            return Optional.empty();
        }
    }

    @Override
    public SimplePage<MessageData> findMessagesByEnvironment(String environment, Pageable pageable) {
        if (isNullObject(environment) || isNullObject(pageable)) {
            return SimplePage.of(Collections.emptyList(), 1, 10, 0L, 0);
        }
        try {
            long total = repository.countByEnvironmentId(environment);
            int size = pageable.getPageSize();
            int totalPages = size > 0 ? Math.max(1, (int) Math.ceil((double) total / size)) : 1;
            List<MessageData> content = repository.findByEnvironmentId(environment, pageable)
                    .map(mapper::mapperData)
                    .getContent();
            return SimplePage.of(content, pageable.getPageNumber() + 1, size, total, totalPages);
        } catch (DataAccessException ex) {
            log.error(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_014.getCode()), ex);
            return SimplePage.of(Collections.emptyList(), pageable.getPageNumber() + 1, pageable.getPageSize(), 0L, 0);
        } catch (Exception ex) {
            log.error(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_015.getCode()), ex);
            return SimplePage.of(Collections.emptyList(), pageable.getPageNumber() + 1, pageable.getPageSize(), 0L, 0);
        }
    }

    @Override
    public Optional<MessageData> findMessageByCodeAndEnvironment(String code, String environmentId) {
        if (isNullObject(code) || isNullObject(environmentId)) {
            return Optional.empty();
        }
        try {
            return repository.findByCodeAndEnvironmentId(code, environmentId)
                    .map(mapper::mapperData);
        } catch (DataAccessException ex) {
            log.error(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_014.getCode()), ex);
            return Optional.empty();
        } catch (Exception ex) {
            log.error(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_015.getCode()), ex);
            return Optional.empty();
        }
    }

    @Override
    public void evictByCode(String code) {
        if (isNullObject(code)) {
            return;
        }
        try {
            repository.deleteAll(repository.findAllByCode(code));
        } catch (DataAccessException ex) {
            log.error(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_014.getCode()), ex);
        } catch (Exception ex) {
            log.error(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_015.getCode()), ex);
        }
    }
}