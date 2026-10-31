package co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl;

import co.edu.uco.application.secondaryports.entity.ApplicationData;
import co.edu.uco.application.secondaryports.entity.FunctionalityData;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.crosscutting.exceptions.BusinessException;
import com.surrealdb.Array;
import com.surrealdb.Object;
import com.surrealdb.Response;
import com.surrealdb.Surreal;
import com.surrealdb.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FunctionalitySurrealRepositoryAdapterImplTest {

    @Mock
    private Surreal surreal;
    @Mock
    private LoggingPortFactory loggerFactory;
    @Mock
    private LoggingPort log;

    private FunctionalitySurrealRepositoryAdapterImpl adapter;

    @BeforeEach
    void setUp() {
        when(loggerFactory.getLogger(FunctionalitySurrealRepositoryAdapterImpl.class)).thenReturn(log);
        adapter = new FunctionalitySurrealRepositoryAdapterImpl(surreal, loggerFactory);
    }

    private Response responseWithOne(Object document) {
        Response response = mock(Response.class);
        when(response.size()).thenReturn(1);
        Value statement = mock(Value.class);
        when(statement.isArray()).thenReturn(true);
        Array array = mock(Array.class);
        when(array.len()).thenReturn(1);
        Value item = mock(Value.class);
        when(item.isObject()).thenReturn(true);
        when(item.getObject()).thenReturn(document);
        when(array.get(0)).thenReturn(item);
        when(statement.getArray()).thenReturn(array);
        when(response.take(0)).thenReturn(statement);
        return response;
    }

    @Test
    void existsByNameAndApplicationId_returnsTrue_whenDocumentFound() {
        doReturn(responseWithOne(mock(Object.class))).when(surreal).query(anyString());

        assertThat(adapter.existsByNameAndApplicationId("Search", "app-1")).isTrue();
    }

    @Test
    void existsByNameAndApplicationId_returnsFalse_whenResponseEmpty() {
        Response response = mock(Response.class);
        when(response.size()).thenReturn(0);
        when(surreal.query(anyString())).thenReturn(response);

        assertThat(adapter.existsByNameAndApplicationId("Search", "app-1")).isFalse();
    }

    @Test
    void create_persistsFunctionality() {
        FunctionalityData functionality = FunctionalityData.build();
        functionality.setName("Search");
        ApplicationData application = ApplicationData.build();
        application.setId(UUID.randomUUID());
        functionality.setApplication(application);

        adapter.create(functionality, "state-1");

        verify(surreal).query(anyString());
        verify(log).info(anyString(), anyString());
    }

    @Test
    void create_throwsBusinessException_whenQueryFails() {
        FunctionalityData functionality = FunctionalityData.build();
        functionality.setApplication(ApplicationData.build());
        doThrow(new RuntimeException("db down")).when(surreal).query(anyString());

        assertThatThrownBy(() -> adapter.create(functionality, "state-1"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getTechnicalMessage())
                        .isEqualTo("Error al persistir la funcionalidad en la base de datos SurrealDB"));
        verify(log).error(anyString(), any(RuntimeException.class));
    }
}