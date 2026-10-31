package co.edu.uco.application.usecase.domain.aggregate.entities;

import co.edu.uco.application.usecase.domain.aggregate.Entity;
import co.edu.uco.application.usecase.domain.aggregate.entities.valueobject.ContentVO;
import co.edu.uco.application.usecase.domain.aggregate.entities.valueobject.TitleVO;
import lombok.Getter;

import java.util.UUID;

import static co.edu.uco.crosscutting.helpers.UtilText.trim;

@Getter
public final class MessageEntity extends Entity<UUID> {
    private String code;
    private TitleVO title;
    private ContentVO content;
    private MessageTypeEntity type;
    private MessageCategoryEntity category;
    private MessageStatusEntity status;
    private String application;
    private FunctionalityEntity functionality;
    public void setCode(String code) {
        this.code = trim(code);
    }
    public void setTitle(String title) {
        this.title = new TitleVO(title);
    }
    public void setContent(String content) {
        this.content = new ContentVO(content);
    }
    public void setApplication(String application) {
        this.application = trim(application);
    }
}