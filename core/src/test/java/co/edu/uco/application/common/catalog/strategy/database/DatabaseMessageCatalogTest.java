package co.edu.uco.application.common.catalog.strategy.database;

import co.edu.uco.application.secondaryports.entity.MessageData;
import co.edu.uco.application.secondaryports.repository.DataBaseMessageRepository;
import co.edu.uco.application.secondaryports.repository.MessageFilter;
import co.edu.uco.application.secondaryports.repository.SimplePage;
import co.edu.uco.application.secondaryports.repository.SimplePageRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatabaseMessageCatalogTest {

    @Mock
    private DataBaseMessageRepository repository;

    @InjectMocks
    private DatabaseMessageCatalog catalog;

    @Test
    void getMessageById_delegatesToRepository() {
        MessageData m = new MessageData();
        when(repository.findById("CODE")).thenReturn(Optional.of(m));

        assertThat(catalog.getMessageById("CODE")).containsSame(m);
    }

    @Test
    void getContent_returnsContentWhenFound() {
        MessageData m = new MessageData();
        m.setContent("hola");
        when(repository.findById("CODE")).thenReturn(Optional.of(m));

        assertThat(catalog.getContent("CODE")).isEqualTo("hola");
    }

    @Test
    void getContent_returnsEmptyWhenNotFound() {
        when(repository.findById("CODE")).thenReturn(Optional.empty());

        assertThat(catalog.getContent("CODE")).isEmpty();
    }

    @Test
    void isExist_returnsTrueWhenMessageExists() {
        when(repository.findById("CODE")).thenReturn(Optional.of(new MessageData()));

        assertThat(catalog.isExist("CODE")).isTrue();
    }

    @Test
    void isExist_returnsFalseWhenMessageAbsent() {
        when(repository.findById("CODE")).thenReturn(Optional.empty());

        assertThat(catalog.isExist("CODE")).isFalse();
    }

    @Test
    void getMessageWithEnvironment_buildsPageRequestAndDelegates() {
        SimplePageRequest request = new SimplePageRequest();
        request.setPage(3);
        request.setSize(30);
        MessageData m = new MessageData();
        SimplePage<MessageData> page = SimplePage.of(List.of(m), 3, 30, 1, 1);
        when(repository.findMessagesByEnvironment(eq("env"), any(org.springframework.data.domain.Pageable.class), any(MessageFilter.class)))
                .thenReturn(page);

        SimplePage<MessageData> result = catalog.getMessageWithEnvironment("env", request);

        assertThat(result).isSameAs(page);
        org.mockito.ArgumentCaptor<org.springframework.data.domain.Pageable> captor =
                org.mockito.ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
        verify(repository).findMessagesByEnvironment(eq("env"), captor.capture(), any(MessageFilter.class));
        assertThat(captor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(captor.getValue().getPageSize()).isEqualTo(30);
    }

    @Test
    void getMessageByCodeAndEnvironment_delegatesToRepository() {
        MessageData m = new MessageData();
        when(repository.findMessageByCodeAndEnvironment("CODE", "env")).thenReturn(Optional.of(m));

        assertThat(catalog.getMessageByCodeAndEnvironment("CODE", "env")).containsSame(m);
    }

    @Test
    void getMessagesWithApplication_passesEnvironmentFilterAndPageable() {
        var request = new co.edu.uco.application.secondaryports.repository.SimplePageRequest();
        request.setFilter(co.edu.uco.application.secondaryports.repository.MessageFilter.builder()
                .environmentIds(java.util.List.of("env-1")).build());
        var page = co.edu.uco.application.secondaryports.repository.SimplePage
                .<co.edu.uco.application.secondaryports.entity.MessageData>of(java.util.List.of(), 1, 10, 0, 0);
        org.mockito.Mockito.when(repository.findMessagesByApplication(
                org.mockito.ArgumentMatchers.eq("app-1"), org.mockito.ArgumentMatchers.eq(java.util.List.of("env-1")),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(request.getFilter())))
                .thenReturn(page);

        org.assertj.core.api.Assertions.assertThat(catalog.getMessagesWithApplication("app-1", request)).isSameAs(page);
    }

    @Test
    void getMessageByCodeAndApplication_delegatesToRepository() {
        var message = java.util.Optional.of(new co.edu.uco.application.secondaryports.entity.MessageData());
        org.mockito.Mockito.when(repository.findByCodeAndApplication("MSG_A", "app-1")).thenReturn(message);

        org.assertj.core.api.Assertions.assertThat(catalog.getMessageByCodeAndApplication("MSG_A", "app-1"))
                .isSameAs(message);
    }
}
