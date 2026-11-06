package co.edu.uco.application.usecase.domain.aggregate.entities.valueobject;

import co.edu.uco.application.crosscutting.exceptions.TitleBelowMinimumLengthException;
import co.edu.uco.application.crosscutting.exceptions.TitleExceedsMaximumLengthException;
import co.edu.uco.application.crosscutting.exceptions.TitleCanNotBeEmptyException;
import co.edu.uco.crosscutting.helpers.UtilNumeric;
import lombok.Getter;

import static co.edu.uco.crosscutting.helpers.UtilText.isEmptyOrNull;

@Getter
public final class TitleVO {
    private String title;
    public TitleVO(String title) {
        setTitle(title);
    }
    public void setTitle(String title) {
        validateTitle(title);
        validateMaximumLength(title);
        validateMinimumLength(title);
        this.title = title;
    }
    private void validateTitle(String title) {
        if (isEmptyOrNull(title)) {
            TitleCanNotBeEmptyException.report();
        }
    }
    private void validateMaximumLength(String title) {
        if (UtilNumeric.isGreaterThan(title.length(), 1000)) {
            TitleExceedsMaximumLengthException.report();
        }
    }
    private void validateMinimumLength(String title) {
        if (UtilNumeric.isLessThan(title.length(), 5)) {
            TitleBelowMinimumLengthException.report();
        }
    }
}
