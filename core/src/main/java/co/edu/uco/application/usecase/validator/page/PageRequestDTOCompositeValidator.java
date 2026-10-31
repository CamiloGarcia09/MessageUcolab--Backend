package co.edu.uco.application.usecase.validator.page;

import co.edu.uco.application.primaryports.dto.page.PageRequestDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.validator.CompositeValidator;
import co.edu.uco.application.usecase.validator.page.rule.PageRequestAttributeFormatRule;
import org.springframework.stereotype.Component;

import java.util.List;

import static co.edu.uco.application.CrosswordsConstant.COLUMN_SORT_ATTRIBUTE;
import static co.edu.uco.application.CrosswordsConstant.PAGE_ATTRIBUTE;
import static co.edu.uco.application.CrosswordsConstant.SIZE_ATTRIBUTE;
import static co.edu.uco.application.CrosswordsConstant.SORT_ATTRIBUTE;
import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.FUN_033;
import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.FUN_043;
import static co.edu.uco.crosscutting.helpers.UtilText.ONLY_LETTERS;
import static co.edu.uco.crosscutting.helpers.UtilText.ONLY_NUMBERS;

@Component
public final class PageRequestDTOCompositeValidator extends CompositeValidator<PageRequestDTO> {

    public PageRequestDTOCompositeValidator(CatalogPort catalogPort) {
        super(List.of(
                new PageRequestAttributeFormatRule(
                        catalogPort, PageRequestDTO::getPage, ONLY_NUMBERS, PAGE_ATTRIBUTE, FUN_033),
                new PageRequestAttributeFormatRule(
                        catalogPort, PageRequestDTO::getSize, ONLY_NUMBERS, SIZE_ATTRIBUTE, FUN_033),
                new PageRequestAttributeFormatRule(
                        catalogPort, PageRequestDTO::getColumnSort, ONLY_LETTERS, COLUMN_SORT_ATTRIBUTE, FUN_043),
                new PageRequestAttributeFormatRule(
                        catalogPort, PageRequestDTO::getSort, ONLY_LETTERS, SORT_ATTRIBUTE, FUN_043)
        ), catalogPort);
    }
}
