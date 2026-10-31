package co.edu.uco.application.usecase.domain;

import co.edu.uco.crosscutting.helpers.UtilUUID;
import lombok.Getter;

import java.util.UUID;

import static co.edu.uco.crosscutting.helpers.UtilObject.getDefaultIsNullObject;
import static co.edu.uco.crosscutting.helpers.UtilText.trim;

@Getter
public final class MessageDomain {
    private UUID id;
    private String code;
    private String title;
    private String content;
    private MessageTypeDomain type;
    private MessageCategoryDomain category;
    private MessageStatusDomain status;
    private String application;
    private FunctionalityDomain functionality;

    public void setId(UUID id) {
        this.id = UtilUUID.getDefaultUUID(id);
    }

    public void setCode(String code) {
        this.code = trim(code);
    }

    public void setTitle(String title) {
        this.title = trim(title);
    }

    public void setContent(String content) {
        this.content = trim(content);
    }

    public void setType(MessageTypeDomain type) {
        this.type = getDefaultIsNullObject(type, MessageTypeDomain.create(null, null));
    }

    public void setCategory(MessageCategoryDomain category) {
        this.category = getDefaultIsNullObject(category, MessageCategoryDomain.create(null, null));
    }

    public void setStatus(MessageStatusDomain status) {
        this.status = getDefaultIsNullObject(status, MessageStatusDomain.create(null, null));
    }

    public void setApplication(String application) {
        this.application = trim(application);
    }

    public void setFunctionality(FunctionalityDomain functionality) {
        this.functionality = getDefaultIsNullObject(functionality, FunctionalityDomain.create(null, null));
    }
}
