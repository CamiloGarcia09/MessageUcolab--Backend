package co.edu.uco.infraestructure.secondaryadapters.presenter.serializer;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.crosscutting.exceptions.CrossWordsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SerializerRegistryTest {

    @Mock
    private CatalogPort catalogPort;
    @Mock
    private LoggingPortFactory loggerFactory;
    @Mock
    private LoggingPort log;
    @Mock
    private SerializerType jsonSerializer;
    @Mock
    private SerializerType xmlSerializer;

    private SerializerRegistry registry;

    @BeforeEach
    void setUp() {
        when(loggerFactory.getLogger(SerializerRegistry.class)).thenReturn(log);
        registry = new SerializerRegistry(List.of(jsonSerializer, xmlSerializer), catalogPort, loggerFactory);
    }

    @Test
    void getSerializerForMediaType_returnsFirstMatchingSerializer() {
        when(jsonSerializer.supports("application/json")).thenReturn(true);

        assertThat(registry.getSerializerForMediaType("application/json")).isSameAs(jsonSerializer);
    }

    @Test
    void getSerializerForMediaType_usesDefaultWhenNoMatch() {
        when(jsonSerializer.supports("application/xml")).thenReturn(false);
        when(xmlSerializer.supports("application/xml")).thenReturn(true);

        assertThat(registry.getSerializerForMediaType("application/xml")).isSameAs(xmlSerializer);
    }

    @Test
    void getSerializerForMediaType_returnsDefaultWhenWildcardAndDefaultExists() {
        when(jsonSerializer.supports("*/*")).thenReturn(false);
        when(jsonSerializer.isDefault()).thenReturn(true);
        when(xmlSerializer.supports("*/*")).thenReturn(false);

        assertThat(registry.getSerializerForMediaType("*/*")).isSameAs(jsonSerializer);
    }

    @Test
    void getSerializerForMediaType_throws_whenNoMatchAndNoDefault() {
        when(jsonSerializer.supports("application/pdf")).thenReturn(false);
        when(jsonSerializer.isDefault()).thenReturn(false);
        when(xmlSerializer.supports("application/pdf")).thenReturn(false);
        when(xmlSerializer.isDefault()).thenReturn(false);
        when(catalogPort.getMessage("TCH_017")).thenReturn("no serializer");

        assertThatThrownBy(() -> registry.getSerializerForMediaType("application/pdf"))
                .isInstanceOf(CrossWordsException.class);
        verify(log).error("no serializer", "application/pdf");
    }
}