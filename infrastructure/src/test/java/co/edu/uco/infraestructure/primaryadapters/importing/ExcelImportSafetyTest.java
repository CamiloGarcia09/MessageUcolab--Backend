package co.edu.uco.infraestructure.primaryadapters.importing;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExcelImportSafetyTest {
    @Mock private CatalogPort catalog;

    @Test
    void checkArchive_acceptsValidXlsxAndCaseInsensitiveExtension() throws IOException {
        var bytes = validWorkbook();

        assertDoesNotThrow(() -> new ExcelImportSafety(catalog).checkArchive(bytes, "messages.XLSX"));
    }

    @Test
    void checkArchive_rejectsEmptyFileWithDomainError() {
        when(catalog.getMessage("IMP_001")).thenReturn("Archivo Excel inválido.");

        assertThatThrownBy(() -> new ExcelImportSafety(catalog).checkArchive(new byte[0], "messages.xlsx"))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        exception -> assertThat(exception.getUserMessage()).contains("Archivo Excel inválido"));
    }

    @Test
    void checkArchive_rejectsMisleadingExtension() {
        when(catalog.getMessage("IMP_001")).thenReturn("Archivo Excel inválido.");

        assertThatThrownBy(() -> new ExcelImportSafety(catalog).checkArchive(new byte[]{1}, "messages.xls"))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        exception -> assertThat(exception.getUserMessage()).contains("Archivo Excel inválido"));
    }

    @Test
    void checkArchive_rejectsWorkbookWithExternalLinkArchiveEntry() throws IOException {
        when(catalog.getMessage("IMP_001")).thenReturn("Archivo Excel inválido.");
        var bytes = archive("[Content_Types].xml", "xl/workbook.xml", "xl/externalLinks/link.xml");

        assertThatThrownBy(() -> new ExcelImportSafety(catalog).checkArchive(bytes, "messages.xlsx"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void checkWorkbook_rejectsMoreThanFourSheets() throws IOException {
        when(catalog.getMessage("IMP_001")).thenReturn("Archivo Excel inválido.");
        byte[] bytes;
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            for (int index = 0; index < 5; index++) {
                workbook.createSheet("sheet" + index);
            }
            workbook.write(output);
            bytes = output.toByteArray();
        }

        assertThatThrownBy(() -> new MessageImportWorkbookAdapter(catalog).read(bytes, "messages.xlsx", "app"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void checkArchive_rejectsMacroEntry() throws IOException {
        when(catalog.getMessage("IMP_001")).thenReturn("Archivo Excel inválido.");
        var bytes = archive("[Content_Types].xml", "xl/workbook.xml", "xl/vbaProject.bin");

        assertThatThrownBy(() -> new ExcelImportSafety(catalog).checkArchive(bytes, "messages.xlsx"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void checkArchive_rejectsFileAboveCompressedByteLimit() {
        when(catalog.getMessage("IMP_001")).thenReturn("Archivo Excel inválido.");
        var oversized = new byte[ExcelImportSafety.MAX_BYTES + 1];

        assertThatThrownBy(() -> new ExcelImportSafety(catalog).checkArchive(oversized, "messages.xlsx"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void checkArchive_rejectsMoreThanTwoThousandZipEntries() throws IOException {
        when(catalog.getMessage("IMP_001")).thenReturn("Archivo Excel inválido.");
        var names = new String[2_001];
        for (int index = 0; index < names.length; index++) {
            names[index] = "empty/entry" + index;
        }
        names[0] = "[Content_Types].xml";
        names[1] = "xl/workbook.xml";

        assertThatThrownBy(() -> new ExcelImportSafety(catalog).checkArchive(archive(names), "messages.xlsx"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void checkArchive_rejectsExpandedArchiveAboveTwentyMiB() throws IOException {
        when(catalog.getMessage("IMP_001")).thenReturn("Archivo Excel inválido.");
        var expanded = new byte[21 * 1024 * 1024];
        var bytes = archiveWithPayload("[Content_Types].xml", "xl/workbook.xml", expanded);

        assertThatThrownBy(() -> new ExcelImportSafety(catalog).checkArchive(bytes, "messages.xlsx"))
                .isInstanceOf(BusinessRuleException.class);
    }

    private static byte[] validWorkbook() throws IOException {
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            workbook.createSheet("data");
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private static byte[] archive(String... names) throws IOException {
        try (var output = new ByteArrayOutputStream(); var zip = new ZipOutputStream(output)) {
            for (var name : names) {
                zip.putNextEntry(new ZipEntry(name));
                zip.write(1);
                zip.closeEntry();
            }
            zip.finish();
            return output.toByteArray();
        }
    }

    private static byte[] archiveWithPayload(String first, String second, byte[] payload) throws IOException {
        try (var output = new ByteArrayOutputStream(); var zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry(first));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry(second));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("xl/sharedStrings.xml"));
            zip.write(payload);
            zip.closeEntry();
            zip.finish();
            return output.toByteArray();
        }
    }
}
