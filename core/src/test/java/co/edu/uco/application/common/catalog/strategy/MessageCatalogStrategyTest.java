package co.edu.uco.application.common.catalog.strategy;

import co.edu.uco.application.common.catalog.strategy.cache.CacheCatalog;
import co.edu.uco.application.common.catalog.strategy.database.DatabaseCatalog;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.entity.MessageData;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.repository.MessageFilter;
import co.edu.uco.application.secondaryports.repository.SimplePage;
import co.edu.uco.application.secondaryports.repository.SimplePageRequest;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageCatalogStrategyTest {

    @Mock
    private CacheCatalog cacheCatalog;
    @Mock
    private DatabaseCatalog databaseCatalog;
    @Mock
    private CatalogPort catalogPort;
    @Mock
    private LoggingPortFactory loggerFactory;
    @Mock
    private LoggingPort log;

    private MessageCatalogStrategy strategy;

    @BeforeEach
    void setUp() {
        when(loggerFactory.getLogger(MessageCatalogStrategy.class)).thenReturn(log);
        strategy = new MessageCatalogStrategy(cacheCatalog, databaseCatalog, catalogPort, loggerFactory);
    }

    private MessageData message() {
        MessageData m = new MessageData();
        m.setCode("CODE");
        return m;
    }

    private SimplePageRequest request(int page, int size) {
        SimplePageRequest r = new SimplePageRequest();
        r.setPage(page);
        r.setSize(size);
        return r;
    }

    @Test
    void getMessagesWithEnvironment_returnsDbMessagesAndFillsCache_whenCacheEmpty() {
        SimplePageRequest request = request(1, 10);
        MessageData m = message();
        SimplePage<MessageData> dbPage = SimplePage.of(List.of(m), 1, 10, 1, 1);
        when(cacheCatalog.getMessageWithEnvironment("env", request))
                .thenReturn(SimplePage.of(List.of(), 1, 10, 0, 0));
        when(databaseCatalog.getMessageWithEnvironment("env", request)).thenReturn(dbPage);
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_006.getCode())).thenReturn("cache empty");
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_007.getCode())).thenReturn("filling cache");

        SimplePage<MessageData> result = strategy.getMessagesWithEnvironment("env", request);

        assertThat(result).isSameAs(dbPage);
        verify(cacheCatalog).addMessageWithEnvironment(m, "env");
        verify(log).info("cache empty");
        verify(log).info("filling cache");
    }

    @Test
    void getMessagesWithEnvironment_throwsWhenCacheAndDbEmpty() {
        SimplePageRequest request = request(1, 10);
        when(cacheCatalog.getMessageWithEnvironment("env", request))
                .thenReturn(SimplePage.of(List.of(), 1, 10, 0, 0));
        when(databaseCatalog.getMessageWithEnvironment("env", request))
                .thenReturn(SimplePage.of(List.of(), 1, 10, 0, 0));
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_006.getCode())).thenReturn("cache empty");
        when(catalogPort.getMessage(MessageCatalogCodeEnum.TCH_009.getCode())).thenReturn("no messages");

        assertThatThrownBy(() -> strategy.getMessagesWithEnvironment("env", request))
                .isInstanceOf(NotFoundException.class)
                .satisfies(ex -> assertThat(((NotFoundException) ex).getUserMessage()).isEqualTo("no messages"));
    }

    @Test
    void getMessagesWithEnvironment_refreshesCache_whenSizesDiffer() {
        SimplePageRequest request = request(1, 10);
        MessageData m1 = message();
        MessageData m2 = message();
        SimplePage<MessageData> cached = SimplePage.of(List.of(m1), 1, 10, 1, 1);
        SimplePage<MessageData> dbPage = SimplePage.of(List.of(m1, m2), 1, 10, 2, 2);
        when(cacheCatalog.getMessageWithEnvironment("env", request)).thenReturn(cached);
        when(databaseCatalog.getMessageWithEnvironment("env", request)).thenReturn(dbPage);
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_008.getCode())).thenReturn("refreshing");

        SimplePage<MessageData> result = strategy.getMessagesWithEnvironment("env", request);

        assertThat(result).isSameAs(dbPage);
        verify(cacheCatalog).addMessageWithEnvironment(m1, "env");
        verify(cacheCatalog).addMessageWithEnvironment(m2, "env");
        verify(log).info("refreshing");
    }

    @Test
    void getMessagesWithEnvironment_returnsCache_whenSizesMatch() {
        SimplePageRequest request = request(1, 10);
        MessageData m = message();
        SimplePage<MessageData> cached = SimplePage.of(List.of(m), 1, 10, 1, 1);
        SimplePage<MessageData> dbPage = SimplePage.of(List.of(m), 1, 10, 1, 1);
        when(cacheCatalog.getMessageWithEnvironment("env", request)).thenReturn(cached);
        when(databaseCatalog.getMessageWithEnvironment("env", request)).thenReturn(dbPage);
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_009.getCode())).thenReturn("using cache");

        SimplePage<MessageData> result = strategy.getMessagesWithEnvironment("env", request);

        assertThat(result).isNotSameAs(cached);
        assertThat(result.getData()).isSameAs(cached.getData());
        assertThat(result.getTotalItems()).isEqualTo(dbPage.getTotalItems());
        assertThat(result.getTotalPages()).isEqualTo(dbPage.getTotalPages());
        verify(log).info("using cache");
        verify(cacheCatalog, never()).addMessageWithEnvironment(any(MessageData.class), anyString());
    }

    @Test
    void getMessagesWithEnvironment_throwsWhenDbEmptyButCacheNotEmpty() {
        SimplePageRequest request = request(1, 10);
        MessageData m = message();
        when(cacheCatalog.getMessageWithEnvironment("env", request))
                .thenReturn(SimplePage.of(List.of(m), 1, 10, 1, 1));
        when(databaseCatalog.getMessageWithEnvironment("env", request))
                .thenReturn(SimplePage.of(List.of(), 1, 10, 0, 0));
        when(catalogPort.getMessage(MessageCatalogCodeEnum.TCH_009.getCode())).thenReturn("no messages");

        assertThatThrownBy(() -> strategy.getMessagesWithEnvironment("env", request))
                .isInstanceOf(NotFoundException.class)
                .satisfies(ex -> assertThat(((NotFoundException) ex).getUserMessage()).isEqualTo("no messages"));
    }

    @Test
    void getMessagesWithEnvironment_returnsEmptyPage_whenCacheThrows() {
        SimplePageRequest request = request(1, 10);
        when(cacheCatalog.getMessageWithEnvironment("env", request))
                .thenThrow(new IllegalStateException("cache down"));
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_013.getCode())).thenReturn("cache error");
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_006.getCode())).thenReturn("cache empty");
        when(databaseCatalog.getMessageWithEnvironment("env", request))
                .thenReturn(SimplePage.of(List.of(), 1, 10, 0, 0));
        when(catalogPort.getMessage(MessageCatalogCodeEnum.TCH_009.getCode())).thenReturn("no messages");

        assertThatThrownBy(() -> strategy.getMessagesWithEnvironment("env", request))
                .isInstanceOf(NotFoundException.class)
                .satisfies(ex -> assertThat(((NotFoundException) ex).getUserMessage()).isEqualTo("no messages"));

        ArgumentCaptor<Exception> exceptionCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(log).error(org.mockito.ArgumentMatchers.eq("cache error"), exceptionCaptor.capture());
        assertThat(exceptionCaptor.getValue()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getMessageByCodeAndEnvironment_returnsCacheHit() {
        MessageData m = message();
        when(cacheCatalog.getMessageByCodeAndEnvironment("CODE", "env"))
                .thenReturn(Optional.of(m));
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_009.getCode())).thenReturn("using cache");

        Optional<MessageData> result = strategy.getMessageByCodeAndEnvironment("CODE", "env");

        assertThat(result).containsSame(m);
        verifyNoInteractions(databaseCatalog);
    }

    @Test
    void getMessageByCodeAndEnvironment_queriesDbOnCacheMiss() {
        MessageData m = message();
        when(cacheCatalog.getMessageByCodeAndEnvironment("CODE", "env"))
                .thenReturn(Optional.empty());
        when(databaseCatalog.getMessageByCodeAndEnvironment("CODE", "env"))
                .thenReturn(Optional.of(m));
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_006.getCode())).thenReturn("cache empty");

        Optional<MessageData> result = strategy.getMessageByCodeAndEnvironment("CODE", "env");

        assertThat(result).containsSame(m);
        verify(cacheCatalog, never()).addMessageWithEnvironment(any(MessageData.class), anyString());
    }

    @Test
    void getMessageByCodeAndEnvironment_returnsEmpty_whenDbMiss() {
        when(cacheCatalog.getMessageByCodeAndEnvironment("CODE", "env"))
                .thenReturn(Optional.empty());
        when(databaseCatalog.getMessageByCodeAndEnvironment("CODE", "env"))
                .thenReturn(Optional.empty());
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_006.getCode())).thenReturn("cache empty");

        Optional<MessageData> result = strategy.getMessageByCodeAndEnvironment("CODE", "env");

        assertThat(result).isEmpty();
        verify(cacheCatalog, never()).addMessageWithEnvironment(any(MessageData.class), anyString());
    }

    @Test
    void getMessagesWithEnvironment_skipsCacheAndReturnsDbResult_whenFilterHasFilters() {
        SimplePageRequest request = request(1, 10);
        MessageFilter filter = MessageFilter.builder().code("MSG").build();
        request.setFilter(filter);
        MessageData m = message();
        SimplePage<MessageData> dbPage = SimplePage.of(List.of(m), 1, 10, 1, 1);
        when(databaseCatalog.getMessageWithEnvironment("env", request)).thenReturn(dbPage);
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_006.getCode())).thenReturn("filter active");

        SimplePage<MessageData> result = strategy.getMessagesWithEnvironment("env", request);

        assertThat(result).isSameAs(dbPage);
        verifyNoInteractions(cacheCatalog);
    }

    @Test
    void getMessagesWithEnvironment_returnsEmptyPage_whenFilterActiveAndDbEmpty() {
        SimplePageRequest request = request(1, 10);
        MessageFilter filter = MessageFilter.builder().title("notfound").build();
        request.setFilter(filter);
        when(databaseCatalog.getMessageWithEnvironment("env", request))
                .thenReturn(SimplePage.of(List.of(), 1, 10, 0, 0));
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_006.getCode())).thenReturn("filter active");

        SimplePage<MessageData> result = strategy.getMessagesWithEnvironment("env", request);

        assertThat(result.getData()).isEmpty();
        assertThat(result.getTotalPages()).isEqualTo(1);
        verifyNoInteractions(cacheCatalog);
    }

    @Test
    void getSystemMessageContent_delegatesToCatalogPort() {
        when(catalogPort.getMessage("FUN_001")).thenReturn("hola");

        assertThat(strategy.getSystemMessageContent("FUN_001")).isEqualTo("hola");
    }

    @Test
    void getMessagesWithEnvironment_refreshesCache_whenTotalItemsDifferAcrossPages() {
        SimplePageRequest request = request(2, 5);
        MessageData m = message();
        SimplePage<MessageData> cached = SimplePage.of(List.of(m), 2, 5, 5, 1);
        SimplePage<MessageData> dbPage = SimplePage.of(List.of(m), 2, 5, 6, 2);
        when(cacheCatalog.getMessageWithEnvironment("env", request)).thenReturn(cached);
        when(databaseCatalog.getMessageWithEnvironment("env", request)).thenReturn(dbPage);
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_008.getCode())).thenReturn("refreshing");

        SimplePage<MessageData> result = strategy.getMessagesWithEnvironment("env", request);

        assertThat(result).isSameAs(dbPage);
        assertThat(result.getTotalItems()).isEqualTo(6L);
        assertThat(result.getTotalPages()).isEqualTo(2);
        verify(cacheCatalog).addMessageWithEnvironment(m, "env");
        verify(log).info("refreshing");
    }

    @Test
    void getMessagesWithEnvironment_usesDbTotalMetadata_whenTotalItemsMatch() {
        SimplePageRequest request = request(1, 5);
        MessageData m = message();
        SimplePage<MessageData> cached = SimplePage.of(List.of(m), 1, 5, 10, 2);
        SimplePage<MessageData> dbPage = SimplePage.of(List.of(m), 1, 5, 10, 3);
        when(cacheCatalog.getMessageWithEnvironment("env", request)).thenReturn(cached);
        when(databaseCatalog.getMessageWithEnvironment("env", request)).thenReturn(dbPage);
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_009.getCode())).thenReturn("using cache");

        SimplePage<MessageData> result = strategy.getMessagesWithEnvironment("env", request);

        assertThat(result).isNotSameAs(cached);
        assertThat(result.getData()).isSameAs(cached.getData());
        assertThat(result.getTotalItems()).isEqualTo(dbPage.getTotalItems());
        assertThat(result.getTotalPages()).isEqualTo(dbPage.getTotalPages());
        assertThat(result.getTotalPages()).isNotEqualTo(cached.getTotalPages());
        verify(log).info("using cache");
        verify(cacheCatalog, never()).addMessageWithEnvironment(any(MessageData.class), anyString());
    }

    @Test
    void getMessagesWithApplication_alwaysQueriesDatabaseAndSkipsCache() {
        SimplePageRequest request = request(1, 10);
        var page = co.edu.uco.application.secondaryports.repository.SimplePage
                .<MessageData>of(java.util.List.of(message()), 1, 10, 1, 1);
        when(databaseCatalog.getMessagesWithApplication("app-1", request)).thenReturn(page);

        org.assertj.core.api.Assertions.assertThat(strategy.getMessagesWithApplication("app-1", request)).isSameAs(page);
        org.mockito.Mockito.verifyNoInteractions(cacheCatalog);
    }

    @Test
    void getMessageByCodeAndApplication_queriesDatabase() {
        var found = java.util.Optional.of(message());
        when(databaseCatalog.getMessageByCodeAndApplication("CODE", "app-1")).thenReturn(found);

        org.assertj.core.api.Assertions.assertThat(strategy.getMessageByCodeAndApplication("CODE", "app-1"))
                .isSameAs(found);
        org.mockito.Mockito.verifyNoInteractions(cacheCatalog);
    }
}
