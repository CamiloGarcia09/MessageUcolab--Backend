package co.edu.uco.application.usecase.domain.aggregate.entities.valueobject;

import co.edu.uco.application.crosscutting.exceptions.SizeTitleLessThanTenException;
import co.edu.uco.application.crosscutting.exceptions.SizeTitleMoreThanFiftyException;
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
        validateSizeMoreThanFifty(title);
        validateSizeLessThanTen(title);
        this.title = title;
    }
    private void validateTitle(String title) {
        if (isEmptyOrNull(title)) {
            TitleCanNotBeEmptyException.report();
        }
    }
    private void validateSizeMoreThanFifty(String title) {
        if (UtilNumeric.isGreaterThan(title.length(), 50)) {
            SizeTitleMoreThanFiftyException.report();
        }
    }
    private void validateSizeLessThanTen(String title) {
        if (UtilNumeric.isLessThan(title.length(), 10)) {
            SizeTitleLessThanTenException.report();
        }
    }
}