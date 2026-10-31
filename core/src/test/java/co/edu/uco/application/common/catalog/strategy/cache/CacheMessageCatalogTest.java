package co.edu.uco.application.common.catalog.strategy.cache;

import co.edu.uco.application.secondaryports.entity.MessageData;
import co.edu.uco.application.secondaryports.repository.CacheMessageRepository;
import co.edu.uco.application.secondaryports.repository.SimplePage;
import co.edu.uco.application.secondaryports.repository.SimplePageRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CacheMessageCatalogTest {

    @Mock
    private CacheMessageRepository repository;

    @InjectMocks
    private CacheMessageCatalog catalog;

    @Test
    void getMessageById_delegatesToRepository() {
        UUID id = UUID.randomUUID();
        MessageData m = new MessageData();
        when(repository.findById(id)).thenReturn(Optional.of(m));

        assertThat(catalog.getMessageById(id.toString())).containsSame(m);
    }

    @Test
    void getContent_returnsContentWhenFound() {
        UUID id = UUID.randomUUID();
        MessageData m = new MessageData();
        m.setContent("hola");
        when(repository.findById(id)).thenReturn(Optional.of(m));

        assertThat(catalog.getContent(id.toString())).isEqualTo("hola");
    }

    @Test
    void getContent_returnsEmptyWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThat(catalog.getContent(id.toString())).isEmpty();
    }

    @Test
    void addMessage_savesToRepository() {
        MessageData m = new MessageData();
        catalog.addMessage(m);

        verify(repository).save(m);
    }

    @Test
    void addMessageWithEnvironment_savesWithEnvironment() {
        MessageData m = new MessageData();
        catalog.addMessageWithEnvironment(m, "env");

        verify(repository).saveWithEnvironment(m, "env");
    }

    @Test
    void isExist_returnsTrueWhenMessageExists() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(new MessageData()));

        assertThat(catalog.isExist(id.toString())).isTrue();
    }

    @Test
    void isExist_returnsFalseWhenMessageAbsent() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThat(catalog.isExist(id.toString())).isFalse();
    }

    @Test
    void getMessageWithEnvironment_buildsPageRequestAndDelegates() {
        SimplePageRequest request = new SimplePageRequest();
        request.setPage(2);
        request.setSize(20);
        MessageData m = new MessageData();
        SimplePage<MessageData> page = SimplePage.of(List.of(m), 2, 20, 1, 1);
        when(repository.findMessagesByEnvironment(eq("env"), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(page);

        SimplePage<MessageData> result = catalog.getMessageWithEnvironment("env", request);

        assertThat(result).isSameAs(page);
        org.mockito.ArgumentCaptor<org.springframework.data.domain.Pageable> captor =
                org.mockito.ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
        verify(repository).findMessagesByEnvironment(eq("env"), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    void getMessageByCodeAndEnvironment_delegatesToRepository() {
        MessageData m = new MessageData();
        when(repository.findMessageByCodeAndEnvironment("CODE", "env")).thenReturn(Optional.of(m));

        assertThat(catalog.getMessageByCodeAndEnvironment("CODE", "env")).containsSame(m);
    }
}