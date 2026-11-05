package co.edu.uco.application.usecase.domain.aggregate.entities.valueobject;

import co.edu.uco.application.crosscutting.exceptions.ContentCanNotBeEmptyException;
import co.edu.uco.application.crosscutting.exceptions.SizeContentLessThanTenException;
import co.edu.uco.application.crosscutting.exceptions.ContentExceedsMaximumLengthException;
import co.edu.uco.crosscutting.helpers.UtilNumeric;
import lombok.Getter;

import static co.edu.uco.crosscutting.helpers.UtilText.isEmptyOrNull;

@Getter
public final class ContentVO {
    private String content;
    public ContentVO(String content) {
        setContent(content);
    }
    public void setContent(String content) {
        validateContent(content);
        validateSizeLessThanTen(content);
        validateMaximumLength(content);
        this.content = content;
    }
    private void validateContent(String content) {
        if (isEmptyOrNull(content)) {
            ContentCanNotBeEmptyException.report();
        }
    }
    private void validateMaximumLength(String content) {
        if (UtilNumeric.isGreaterThan(content.length(), 500)) {
            ContentExceedsMaximumLengthException.report();
        }
    }
    private void validateSizeLessThanTen(String content) {
        if (UtilNumeric.isLessThan(content.length(), 10)) {
            SizeContentLessThanTenException.report();
        }
    }
}
