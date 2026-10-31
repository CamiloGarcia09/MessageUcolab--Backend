package co.edu.uco.infraestructure.primaryadapters.importing;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.domain.message.importing.*;
import co.edu.uco.application.primaryports.dto.message.CreateMessageDTO;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.SheetVisibility;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;
import java.io.IOException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageImportWorkbookAdapterTest {
    private static final String APP_ID = "123e4567-e89b-12d3-a456-426614175106";
    private static final String FUNCTION_ID = "123e4567-e89b-12d3-a456-426614175107";
    private static final String TYPE_ID = "123e4567-e89b-12d3-a456-426614175108";
    private static final String CATEGORY_ID = "123e4567-e89b-12d3-a456-426614175109";
    private static final String STATE_ID = "123e4567-e89b-12d3-a456-426614175110";
    @Mock private CatalogPort catalog;

    @Test
    void template_containsImportMetadataAndDropdownReferences() throws IOException {
        configureCatalog();
        var adapter = new MessageImportWorkbookAdapter(catalog);
        var workbookBytes = adapter.template(references(), APP_ID);

        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(workbookBytes))) {
            assertThat(workbook.getSheetName(0)).isEqualTo("Mensajes");
            assertThat(workbook.getSheetVisibility(workbook.getSheetIndex("Referencias")))
                    .isEqualTo(SheetVisibility.HIDDEN);
            assertThat(workbook.getSheetVisibility(workbook.getSheetIndex("Mensajes")))
                    .isEqualTo(SheetVisibility.VISIBLE);
            assertThat(workbook.getSheetVisibility(workbook.getSheetIndex("Instrucciones")))
                    .isEqualTo(SheetVisibility.VISIBLE);
            var messageHeaders = workbook.getSheetAt(0).getRow(0);
            var referenceHeaders = workbook.getSheet("Referencias").getRow(0);
            assertAll(
                    () -> assertThat(messageHeaders.getCell(0).getStringCellValue()).isEqualTo("Código"),
                    () -> assertThat(messageHeaders.getCell(1).getStringCellValue()).isEqualTo("Funcionalidad"),
                    () -> assertThat(messageHeaders.getCell(3).getStringCellValue()).isEqualTo("Categoría"),
                    () -> assertThat(messageHeaders.getCell(5).getStringCellValue()).isEqualTo("Título"),
                    () -> assertThat(referenceHeaders.getCell(0).getStringCellValue())
                            .isEqualTo("Funcionalidades activas"));
            assertThat(workbook.getSheetAt(1).getRow(0).getCell(1).getStringCellValue()).isEqualTo(APP_ID);
            assertThat(workbook.getSheetAt(1).getRow(0).getCell(0).getStringCellValue())
                    .isEqualTo("MESSAGEUCOLAB_IMPORT_V2");
            assertThat(workbook.getSheetAt(1).getRow(0).getZeroHeight()).isTrue();
            assertThat(workbook.getSheetAt(0).getDataValidations()).hasSize(4);
            assertThat(workbook.getName("ImportReference0").getRefersToFormula())
                    .isEqualTo("'Referencias'!$A$2:$A$2");
            assertThat(workbook.getName("ImportReference3").getRefersToFormula())
                    .isEqualTo("'Referencias'!$D$2:$D$2");
            assertThat(workbook.getSheetAt(2).getRow(1).getCell(0).getStringCellValue())
                    .isEqualTo("Function");
        }
    }

    @Test
    void template_excludesInactiveFunctionalityFromSelectableReferences() throws IOException {
        configureCatalog();
        var selectable = List.of(new ImportReference(FUNCTION_ID, "Function", true),
                new ImportReference("123e4567-e89b-12d3-a456-426614175113", "Inactive", false));
        var references = new ImportReferences(selectable, references().types(), references().categories(), references().states());

        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(
                new MessageImportWorkbookAdapter(catalog).template(references, APP_ID)))) {
            var referenceSheet = workbook.getSheet("Referencias");
            assertThat(workbook.getSheetVisibility(workbook.getSheetIndex("Referencias")))
                    .isEqualTo(SheetVisibility.HIDDEN);
            assertThat(referenceSheet.getRow(1).getCell(0).getStringCellValue())
                    .isEqualTo("Function");
            assertThat((Object) referenceSheet.getRow(2)).isNull();
            assertThat(workbook.getSheet("Mensajes").getDataValidations()).hasSize(4);
        }
    }

    @Test
    void template_withEmptyReferences_keepsHiddenReferenceSheetAndReadableStructure() throws IOException {
        configureCatalog();
        var emptyReferences = new ImportReferences(List.of(), List.of(), List.of(), List.of());
        var adapter = new MessageImportWorkbookAdapter(catalog);
        var bytes = adapter.template(emptyReferences, APP_ID);

        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertThat(workbook.getSheetVisibility(workbook.getSheetIndex("Referencias")))
                    .isEqualTo(SheetVisibility.HIDDEN);
            assertThat(workbook.getSheet("Mensajes").getDataValidations()).isEmpty();
            assertThat(workbook.getSheet("Referencias").getRow(0).getCell(0).getStringCellValue())
                    .isEqualTo("Funcionalidades activas");
        }
        assertThat(adapter.read(bytes, "empty-references.xlsx", APP_ID)).isEmpty();
    }

    @Test
    void read_acceptsTemplateWithValidDataAndPreservesOriginalExcelRow() throws IOException {
        configureCatalog();
        var adapter = new MessageImportWorkbookAdapter(catalog);
        var bytes = adapter.template(references(), APP_ID);
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            var row = workbook.getSheet("Mensajes").createRow(1);
            row.createCell(0).setCellValue("GREETING");
            row.createCell(1).setCellValue("Function");
            row.createCell(2).setCellValue("Type");
            row.createCell(3).setCellValue("Category");
            row.createCell(4).setCellValue("Active");
            row.createCell(5).setCellValue("A greeting");
            row.createCell(6).setCellValue("A greeting content");
            var input = workbookBytes(workbook);

            assertThat(adapter.read(input, "import.xlsx", APP_ID)).containsExactly(new ImportRow(2, "GREETING",
                    "Function", "Type", "Category", "Active",
                    "A greeting", "A greeting content"));
        }
    }

    @Test
    void read_rejectsFormulaCellsAsDomainValidationFailure() throws IOException {
        configureCatalog();
        var adapter = new MessageImportWorkbookAdapter(catalog);
        var bytes = adapter.template(references(), APP_ID);
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            workbook.getSheet("Mensajes").createRow(1).createCell(0).setCellFormula("1+1");
            var input = workbookBytes(workbook);

            assertThatThrownBy(() -> adapter.read(input, "import.xlsx", APP_ID))
                    .isInstanceOf(BusinessRuleException.class);
        }
    }

    @Test
    void read_rejectsWorkbookIssuedForDifferentApplication() throws IOException {
        configureCatalog();
        var adapter = new MessageImportWorkbookAdapter(catalog);
        var bytes = adapter.template(references(), APP_ID);

        assertThatThrownBy(() -> adapter.read(bytes, "import.xlsx", "another-app"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void read_rejectsLegacyV1Workbook() throws IOException {
        configureCatalog();
        var bytes = new MessageImportWorkbookAdapter(catalog).template(references(), APP_ID);
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            workbook.getSheet("Instrucciones").getRow(0).getCell(0).setCellValue("MESSAGEUCOLAB_IMPORT_V1");

            assertThatThrownBy(() -> new MessageImportWorkbookAdapter(catalog)
                    .read(workbookBytes(workbook), "legacy.xlsx", APP_ID))
                    .isInstanceOf(BusinessRuleException.class);
        }
    }

    @Test
    void read_rejectsDataInUnexpectedColumn() throws IOException {
        configureCatalog();
        var adapter = new MessageImportWorkbookAdapter(catalog);
        var bytes = adapter.template(references(), APP_ID);
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            workbook.getSheet("Mensajes").createRow(1).createCell(7).setCellValue("unexpected");
            var input = workbookBytes(workbook);

            assertThatThrownBy(() -> adapter.read(input, "import.xlsx", APP_ID))
                    .isInstanceOf(BusinessRuleException.class);
        }
    }

    @Test
    void report_writesFormulaLikeUserContentAsStringCells() throws IOException {
        configureCatalog();
        var row = new ImportRow(7, "SAFE", "Function", "Type", "Category", "Active", "Title", "=HYPERLINK(\"x\")");
        var message = CreateMessageDTO.builder().build();
        var preview = new MessageImportPreview("import", APP_ID, List.of(),
                List.of(new ImportRowResult(row, "MSG_SAFE", message,
                        List.of(new ImportIssue("content", "Correct the content")))), Instant.now().plusSeconds(60));
        var bytes = new MessageImportWorkbookAdapter(catalog).report(preview);

        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertThat(workbook.getSheetVisibility(workbook.getSheetIndex("Referencias")))
                    .isEqualTo(SheetVisibility.HIDDEN);
            assertThat(workbook.getSheetVisibility(workbook.getSheetIndex("Mensajes")))
                    .isEqualTo(SheetVisibility.VISIBLE);
            assertThat(workbook.getSheetVisibility(workbook.getSheetIndex("Errores")))
                    .isEqualTo(SheetVisibility.VISIBLE);
            assertThat(workbook.getSheet("Errores").getRow(1).getCell(2).getStringCellValue())
                    .isEqualTo("Correct the content");
            assertThat(workbook.getSheet("Instrucciones").getRow(0).getZeroHeight()).isTrue();
            var contentCell = workbook.getSheet("Mensajes").getRow(6).getCell(6);
            var originalRow = workbook.getSheet("Errores").getRow(1).getCell(0);
            var issueField = workbook.getSheet("Errores").getRow(1).getCell(1);
            assertThat(contentCell.getCellType()).isEqualTo(CellType.STRING);
            assertThat(contentCell.getStringCellValue()).startsWith("=HYPERLINK");
            assertThat(originalRow.getStringCellValue()).isEqualTo("7");
            assertThat(issueField.getStringCellValue()).isEqualTo("Contenido");
        }
        assertThat(new MessageImportWorkbookAdapter(catalog).read(bytes, "errors.xlsx", APP_ID))
                .containsExactly(row);
    }

    private void configureCatalog() {
        when(catalog.getMessage(anyString())).thenAnswer(invocation -> switch ((String) invocation.getArgument(0)) {
            case "IMP_001" -> "Invalid workbook";
            case "IMP_005" -> "Invalid reference";
            case "IMP_011" -> "Código|Funcionalidad|Tipo|Categoría|Estado|Título|Contenido";
            case "IMP_012" -> "Import instructions";
            case "IMP_013" -> "Row|Field|Issue";
            case "IMP_014" -> "Funcionalidades activas|Tipos|Categorías|Estados";
            case "IMP_015" -> "How to use|Keep data textual";
            case "IMP_017" -> "Código|Funcionalidad|Tipo|Categoría|Estado|Título|Contenido|Ambientes";
            default -> "Catalog message";
        });
    }

    private static ImportReferences references() {
        return new ImportReferences(List.of(new ImportReference(FUNCTION_ID, "Function", true)),
                List.of(new ImportReference(TYPE_ID, "Type", true)),
                List.of(new ImportReference(CATEGORY_ID, "Category", true)),
                List.of(new ImportReference(STATE_ID, "Active", true)));
    }

    private static byte[] workbookBytes(XSSFWorkbook workbook) throws java.io.IOException {
        try (var output = new java.io.ByteArrayOutputStream()) {
            workbook.write(output);
            return output.toByteArray();
        }
    }
}
