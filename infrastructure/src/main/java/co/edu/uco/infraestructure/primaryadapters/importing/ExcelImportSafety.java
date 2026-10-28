package co.edu.uco.infraestructure.primaryadapters.importing;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import org.apache.poi.openxml4j.opc.PackageRelationshipCollection;
import org.apache.poi.openxml4j.opc.TargetMode;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbookType;
import org.apache.poi.ss.usermodel.CellType;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.zip.ZipInputStream;
import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.IMP_001;

final class ExcelImportSafety {
    static final int MAX_BYTES = 5 * 1024 * 1024;
    private static final int MAX_EXPANDED_BYTES = 20 * 1024 * 1024;
    private final CatalogPort catalog;

    ExcelImportSafety(CatalogPort catalog) {
        this.catalog = catalog;
    }

    void checkArchive(byte[] bytes, String filename) throws IOException {
        if (bytes.length == 0 || bytes.length > MAX_BYTES || filename == null
                || !filename.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw invalid();
        }
        int expanded = 0;
        int entries = 0;
        boolean workbook = false;
        boolean contentTypes = false;
        var buffer = new byte[8192];
        try (var zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                var name = entry.getName().toLowerCase(Locale.ROOT);
                if (++entries > 2000 || forbidden(name)) {
                    throw invalid();
                }
                workbook |= "xl/workbook.xml".equals(name);
                contentTypes |= "[content_types].xml".equals(name);
                for (int count = zip.read(buffer); count != -1; count = zip.read(buffer)) {
                    expanded += count;
                    if (expanded > MAX_EXPANDED_BYTES) {
                        throw invalid();
                    }
                }
            }
        }
        if (!workbook || !contentTypes) {
            throw invalid();
        }
    }

    private boolean forbidden(String name) {
        return name.contains("vbaproject") || name.contains("externallinks/") || name.contains("embeddings/")
                || name.contains("connections.xml") || name.contains("querytables/") || name.contains("../");
    }

    void checkWorkbook(XSSFWorkbook workbook) throws org.apache.poi.openxml4j.exceptions.InvalidFormatException {
        if (workbook.getWorkbookType() != XSSFWorkbookType.XLSX || !workbook.getExternalLinksTable().isEmpty()
                || workbook.getNumberOfSheets() > 4) {
            throw invalid();
        }
        checkRelationships(workbook.getPackage().getRelationships());
        for (var part : workbook.getPackage().getParts()) {
            if (!part.isRelationshipPart()) {
                checkRelationships(part.getRelationships());
            }
        }
        for (var sheet : workbook) {
            for (var row : sheet) {
                for (var cell : row) {
                    if (cell.getCellType() == CellType.FORMULA) {
                        throw invalid();
                    }
                }
            }
        }
    }

    private void checkRelationships(PackageRelationshipCollection relationships) {
        for (var relationship : relationships) {
            if (relationship.getTargetMode() == TargetMode.EXTERNAL) {
                throw invalid();
            }
        }
    }

    BusinessRuleException invalid() {
        return BusinessRuleException.buildUserException(catalog.getMessage(IMP_001.getCode()));
    }
}
