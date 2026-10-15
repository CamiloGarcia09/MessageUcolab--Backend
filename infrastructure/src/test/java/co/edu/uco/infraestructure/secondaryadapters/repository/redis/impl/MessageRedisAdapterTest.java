package co.edu.uco.infraestructure.secondaryadapters.repository.redis.impl;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.entity.MessageData;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.repository.CacheMessageRepository;
import co.edu.uco.application.secondaryports.repository.SimplePage;
import co.edu.uco.infraestructure.secondaryadapters.repository.data.DataMapper;
import co.edu.uco.infraestructure.secondaryadapters.repository.redis.MessageRedis;
import co.edu.uco.infraestructure.secondaryadapters.repository.redis.RedisRepositoryAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageRedisAdapterTest {

    @Mock
    private RedisRepositoryAdapter repository;
    @Mock
    private DataMapper<MessageData, MessageRedis> mapper;
    @Mock
    private CatalogPort catalogPort;
    @Mock
    private LoggingPortFactory loggerFactory;
    @Mock
    private LoggingPort log;
    @Mock
    private MessageRedis messageRedis;

    private MessageRedisAdapter adapter;

    @BeforeEach
    void setUp() {
        when(loggerFactory.getLogger(MessageRedisAdapter.class)).thenReturn(log);
        adapter = new MessageRedisAdapter(repository, mapper, catalogPort, loggerFactory);
    }

    private MessageData sampleData() {
        return MessageData.build();
    }

    @Test
    void save_doesNothing_whenDataIsNull() {
        adapter.save(null);
        verify(repository, never()).save(any());
    }

    @Test
    void save_storesMappedModel() {
        MessageData data = sampleData();
        when(mapper.mapperModel(data)).thenReturn(messageRedis);

        adapter.save(data);

        verify(repository).save(messageRedis);
    }

    @Test
    void save_logsDataAccessError_whenRepositoryFails() {
        MessageData data = sampleData();
        when(mapper.mapperModel(data)).thenReturn(messageRedis);
        doThrow(new DataAccessException("db") {}).when(repository).save(messageRedis);
        when(catalogPort.getMessage("FUN_014")).thenReturn("data access");

        adapter.save(data);

        verify(log).error(eq("data access"), any(DataAccessException.class));
    }

    @Test
    void save_logsGenericError_whenUnexpectedException() {
        MessageData data = sampleData();
        when(mapper.mapperModel(data)).thenReturn(messageRedis);
        doThrow(new RuntimeException("boom")).when(repository).save(messageRedis);
        when(catalogPort.getMessage("FUN_015")).thenReturn("generic");

        adapter.save(data);

        verify(log).error(eq("generic"), any(RuntimeException.class));
    }

    @Test
    void saveWithEnvironment_setsEnvironmentAndStores() {
        MessageData data = sampleData();
        when(mapper.mapperModel(data)).thenReturn(messageRedis);

        adapter.saveWithEnvironment(data, "env-1");

        verify(messageRedis).setEnvironmentId("env-1");
        verify(repository).save(messageRedis);
    }

    @Test
    void saveWithEnvironment_doesNothing_whenDataIsNull() {
        adapter.saveWithEnvironment(null, "env-1");
        verify(repository, never()).save(any());
    }

    @Test
    void findById_returnsEmpty_whenIdIsNull() {
        assertThat(adapter.findById(null)).isEmpty();
        verify(repository, never()).findById(any());
    }

    @Test
    void findById_returnsMappedData_whenFound() {
        UUID id = UUID.randomUUID();
        MessageData data = sampleData();
        when(repository.findById(id)).thenReturn(Optional.of(messageRedis));
        when(mapper.mapperData(messageRedis)).thenReturn(data);

        Optional<MessageData> result = adapter.findById(id);

        assertThat(result).contains(data);
    }

    @Test
    void findById_returnsEmpty_whenRepositoryThrows() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenThrow(new DataAccessException("db") {});
        when(catalogPort.getMessage("FUN_014")).thenReturn("data access");

        assertThat(adapter.findById(id)).isEmpty();
        verify(log).error(eq("data access"), any(DataAccessException.class));
    }

    @Test
    void findMessagesByEnvironment_returnsEmptyPage_whenArgumentsNull() {
        SimplePage<MessageData> page = adapter.findMessagesByEnvironment(null, PageRequest.of(0, 10));

        assertThat(page.getData()).isEmpty();
        assertThat(page.getTotalItems()).isZero();
    }

    @Test
    void findMessagesByEnvironment_returnsMappedPage_whenFound() {
        MessageData data = sampleData();
        when(repository.countByEnvironmentId("env-1")).thenReturn(1L);
        when(repository.findByEnvironmentId("env-1", PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(messageRedis)));
        when(mapper.mapperData(messageRedis)).thenReturn(data);

        SimplePage<MessageData> page = adapter.findMessagesByEnvironment("env-1", PageRequest.of(0, 10));

        assertThat(page.getData()).containsExactly(data);
        assertThat(page.getTotalItems()).isEqualTo(1);
    }

    @Test
    void findMessagesByEnvironment_returnsEmptyPage_whenRepositoryThrows() {
        Pageable pageable = PageRequest.of(2, 10);
        when(repository.findByEnvironmentId("env-1", pageable)).thenThrow(new RuntimeException("boom"));
        when(catalogPort.getMessage("FUN_015")).thenReturn("generic");

        SimplePage<MessageData> page = adapter.findMessagesByEnvironment("env-1", pageable);

        assertThat(page.getData()).isEmpty();
        assertThat(page.getPage()).isEqualTo(3);
        assertThat(page.getSize()).isEqualTo(10);
        verify(log).error(eq("generic"), any(RuntimeException.class));
    }

    @Test
    void findMessageByCodeAndEnvironment_returnsEmpty_whenArgumentsNull() {
        assertThat(adapter.findMessageByCodeAndEnvironment(null, "env-1")).isEmpty();
        assertThat(adapter.findMessageByCodeAndEnvironment("code", null)).isEmpty();
    }

    @Test
    void findMessageByCodeAndEnvironment_returnsMappedData_whenFound() {
        MessageData data = sampleData();
        when(repository.findByCodeAndEnvironmentId("code", "env-1")).thenReturn(Optional.of(messageRedis));
        when(mapper.mapperData(messageRedis)).thenReturn(data);

        Optional<MessageData> result = adapter.findMessageByCodeAndEnvironment("code", "env-1");

        assertThat(result).contains(data);
    }

    @Test
    void findMessageByCodeAndEnvironment_returnsEmpty_whenRepositoryThrows() {
        when(repository.findByCodeAndEnvironmentId("code", "env-1")).thenThrow(new RuntimeException("boom"));
        when(catalogPort.getMessage("FUN_015")).thenReturn("generic");

        assertThat(adapter.findMessageByCodeAndEnvironment("code", "env-1")).isEmpty();
        verify(log).error(eq("generic"), any(RuntimeException.class));
    }
}