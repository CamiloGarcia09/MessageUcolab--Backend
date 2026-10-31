package co.edu.uco.application.usecase.domain.aggregate.entities;

import co.edu.uco.application.usecase.domain.aggregate.entities.valueobject.ContentVO;
import co.edu.uco.application.usecase.domain.aggregate.entities.valueobject.TitleVO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MessageEntityTest {

    @Test
    void setters_storeAndNormalizeValues() {
        MessageEntity entity = new MessageEntity();

        entity.setCode("  CODE  ");
        entity.setTitle("0123456789");
        entity.setContent("0123456789");
        entity.setApplication("  app  ");

        assertThat(entity.getCode()).isEqualTo("CODE");
        assertThat(entity.getApplication()).isEqualTo("app");
        assertThat(entity.getTitle()).isInstanceOf(TitleVO.class);
        assertThat(entity.getTitle().getTitle()).isEqualTo("0123456789");
        assertThat(entity.getContent()).isInstanceOf(ContentVO.class);
        assertThat(entity.getContent().getContent()).isEqualTo("0123456789");
    }

    @Test
    void setTitle_createsTitleVO() {
        MessageEntity entity = new MessageEntity();

        entity.setTitle("0123456789");

        assertThat(entity.getTitle()).isInstanceOf(TitleVO.class);
        assertThat(entity.getTitle().getTitle()).isEqualTo("0123456789");
    }

    @Test
    void setContent_createsContentVO() {
        MessageEntity entity = new MessageEntity();

        entity.setContent("0123456789");

        assertThat(entity.getContent()).isInstanceOf(ContentVO.class);
        assertThat(entity.getContent().getContent()).isEqualTo("0123456789");
    }
}
