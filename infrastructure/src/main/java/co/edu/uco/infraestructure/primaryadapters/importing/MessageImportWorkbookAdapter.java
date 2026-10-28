package co.edu.uco.infraestructure.primaryadapters.importing;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.domain.message.importing.*;
import co.edu.uco.application.usecase.validator.message.importing.ImportRowsRule;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.BusinessException;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import co.edu.uco.crosscutting.exceptions.enumeration.ExceptionLocation;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.*;

@Component
public final class MessageImportWorkbookAdapter {
    public static final int MAX_BYTES = ExcelImportSafety.MAX_BYTES;
    private static final String MESSAGES = "Mensajes";
    private static final String INSTRUCTIONS = "Instrucciones";
    private static final String REFERENCES = "Referencias";
    private static final String VERSION = "MESSAGEUCOLAB_IMPORT_V2";
    private final CatalogPort catalog;
    private final ExcelImportSafety safety;

    public MessageImportWorkbookAdapter(CatalogPort catalog) {
        this.catalog = catalog;
        this.safety = new ExcelImportSafety(catalog);
    }

    public List<ImportRow> read(byte[] bytes, String filename, String applicationId) {
        try {
            safety.checkArchive(bytes, filename);
            try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
                safety.checkWorkbook(workbook);
                checkStructure(workbook, applicationId);
                var rows = new ArrayList<ImportRow>();
                for (var row : workbook.getSheet(MESSAGES)) {
                    if (row.getRowNum() == 0 || empty(row)) {
                        continue;
                    }
                    if (row.getRowNum() > ImportRowsRule.MAX_ROWS) {
                        throw invalid(IMP_002);
                    }
                    if (row.getLastCellNum() > 7 && extraData(row)) {
                        throw invalid(IMP_016);
                    }
                    rows.add(new ImportRow(row.getRowNum() + 1, value(row.getCell(0)), value(row.getCell(1)),
                            value(row.getCell(2)), value(row.getCell(3)), value(row.getCell(4)),
                            value(row.getCell(5)), value(row.getCell(6))));
                }
                return List.copyOf(rows);
            }
        } catch (IOException | org.apache.poi.openxml4j.exceptions.InvalidFormatException ex) {
            throw safety.invalid();
        } catch (org.apache.poi.ooxml.POIXMLException | org.apache.poi.EncryptedDocumentException ex) {
            throw safety.invalid();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            throw safety.invalid();
        }
    }

    public byte[] template(ImportReferences references, String applicationId) {
        try (var workbook = new XSSFWorkbook()) {
            var sheet = messagesSheet(workbook);
            instructions(workbook, applicationId);
            referenceLists(workbook, references, sheet);
            return bytes(workbook);
        } catch (IOException ex) {
            throw technical(ex);
        }
    }

    public byte[] report(MessageImportPreview preview) {
        try (var workbook = new XSSFWorkbook()) {
            var sheet = messagesSheet(workbook);
            instructions(workbook, preview.applicationId());
            var errors = workbook.createSheet("Errores");
            writeRow(errors, 0, catalog.getMessage(IMP_013.getCode()).split("\\|"));
            int errorRow = 1;
            for (var result : preview.rows()) {
                var row = result.row();
                writeRow(sheet, row.rowNumber() - 1, row.code(), row.functionality(), row.type(), row.category(),
                        row.status(), row.title(), row.content());
                for (var issue : result.issues()) {
                    writeRow(errors, errorRow++, Integer.toString(row.rowNumber()), issueFieldLabel(issue.field()), issue.message());
                }
            }
            // Report remains re-importable after correction; the references are downloaded with a fresh template.
            workbook.createSheet(REFERENCES);
            errors.setColumnWidth(0, 18 * 256);
            errors.setColumnWidth(1, 20 * 256);
            errors.setColumnWidth(2, 100 * 256);
            return bytes(workbook);
        } catch (IOException ex) {
            throw technical(ex);
        }
    }

    private void checkStructure(XSSFWorkbook workbook, String applicationId) {
        var sheet = workbook.getSheet(MESSAGES);
        var instructions = workbook.getSheet(INSTRUCTIONS);
        if (sheet == null || instructions == null || workbook.getSheet(REFERENCES) == null
                || !VERSION.equals(value(cell(instructions, 0, 0)))) {
            throw invalid(IMP_003);
        }
        if (!applicationId.equals(value(cell(instructions, 0, 1)))) {
            throw invalid(IMP_004);
        }
        var headers = headers();
        for (int column = 0; column < headers.length; column++) {
            if (!headers[column].equals(value(cell(sheet, 0, column)))) {
                throw invalid(IMP_003);
            }
        }
        if (sheet.getRow(0) != null && extraData(sheet.getRow(0))) {
            throw invalid(IMP_003);
        }
    }

    private Sheet messagesSheet(XSSFWorkbook workbook) {
        var sheet = workbook.createSheet(MESSAGES);
        writeRow(sheet, 0, headers());
        sheet.createFreezePane(0, 1);
        sheet.setAutoFilter(new CellRangeAddress(0, ImportRowsRule.MAX_ROWS, 0, 6));
        var header = workbook.createCellStyle();
        header.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        var font = workbook.createFont();
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setBold(true);
        header.setFont(font);
        var text = workbook.createCellStyle();
        text.setDataFormat(workbook.createDataFormat().getFormat("@"));
        for (int column = 0; column < 7; column++) {
            sheet.getRow(0).getCell(column).setCellStyle(header);
            sheet.setDefaultColumnStyle(column, text);
            sheet.setColumnWidth(column, (column >= 5 ? 55 : column == 0 ? 18 : 48) * 256);
        }
        return sheet;
    }

    private void instructions(XSSFWorkbook workbook, String applicationId) {
        var sheet = workbook.createSheet(INSTRUCTIONS);
        writeRow(sheet, 0, VERSION, applicationId);
        sheet.getRow(0).setZeroHeight(true);
        writeRow(sheet, 2, catalog.getMessage(IMP_012.getCode()));
        var style = workbook.createCellStyle();
        style.setWrapText(true);
        style.setVerticalAlignment(VerticalAlignment.TOP);
        sheet.getRow(2).getCell(0).setCellStyle(style);
        sheet.addMergedRegion(new CellRangeAddress(2, 6, 0, 6));
        writeRow(sheet, 8, headers()[0], headers()[5], headers()[6]);
        writeRow(sheet, 9, catalog.getMessage(IMP_015.getCode()).split("\\|"));
        for (int column = 0; column < 7; column++) {
            sheet.setColumnWidth(column, 25 * 256);
        }
    }

    private void referenceLists(XSSFWorkbook workbook, ImportReferences references, Sheet messages) {
        var sheet = workbook.createSheet(REFERENCES);
        writeRow(sheet, 0, catalog.getMessage(IMP_014.getCode()).split("\\|"));
        var groups = List.of(references.functionalities(), references.types(), references.categories(), references.states());
        for (int column = 0; column < groups.size(); column++) {
            var options = groups.get(column).stream().filter(ImportReference::available)
                    .map(ImportReference::name).distinct().toList();
            for (int index = 0; index < options.size(); index++) {
                var row = sheet.getRow(index + 1);
                if (row == null) {
                    row = sheet.createRow(index + 1);
                }
                row.createCell(column, CellType.STRING).setCellValue(options.get(index));
            }
            sheet.setColumnWidth(column, 70 * 256);
            if (!options.isEmpty()) {
                addDropdown(workbook, messages, column, options.size());
            }
        }
    }

    private void addDropdown(XSSFWorkbook workbook, Sheet messages, int referenceColumn, int count) {
        var name = workbook.createName();
        name.setNameName("ImportReference" + referenceColumn);
        var letter = org.apache.poi.ss.util.CellReference.convertNumToColString(referenceColumn);
        name.setRefersToFormula("'" + REFERENCES + "'!$" + letter + "$2:$" + letter + "$" + (count + 1));
        var helper = messages.getDataValidationHelper();
        var validation = helper.createValidation(helper.createFormulaListConstraint(name.getNameName()),
                new CellRangeAddressList(1, ImportRowsRule.MAX_ROWS, referenceColumn + 1, referenceColumn + 1));
        validation.setShowErrorBox(true);
        validation.setErrorStyle(DataValidation.ErrorStyle.STOP);
        validation.createErrorBox(headers()[referenceColumn + 1], catalog.getMessage(IMP_005.getCode()));
        messages.addValidationData(validation);
    }

    private boolean extraData(Row row) {
        for (var cell : row) {
            if (cell.getColumnIndex() >= 7 && !value(cell).isBlank()) {
                return true;
            }
        }
        return false;
    }

    private boolean empty(Row row) {
        for (var cell : row) {
            if (!value(cell).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private Cell cell(Sheet sheet, int row, int column) {
        return sheet.getRow(row) == null ? null : sheet.getRow(row).getCell(column);
    }

    private String value(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return "";
        }
        var value = switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    throw invalid(IMP_016);
                }
                yield org.apache.poi.ss.util.NumberToTextConverter.toText(cell.getNumericCellValue());
            }
            default -> throw invalid(IMP_016);
        };
        if (value.length() > 4000) {
            throw invalid(IMP_016);
        }
        return value;
    }

    private String[] headers() {
        return catalog.getMessage(IMP_011.getCode()).split("\\|");
    }

    private String issueFieldLabel(String field) {
        var fields = List.of("code", "functionality", "type", "category", "status", "title", "content", "environments");
        var index = fields.indexOf(field);
        return index < 0 ? field : catalog.getMessage(IMP_017.getCode()).split("\\|")[index];
    }

    private void writeRow(Sheet sheet, int index, String... values) {
        var row = sheet.createRow(index);
        for (int column = 0; column < values.length; column++) {
            // Explicit STRING prevents formula injection in downloadable error reports.
            row.createCell(column, CellType.STRING).setCellValue(values[column] == null ? "" : values[column]);
        }
    }

    private byte[] bytes(XSSFWorkbook workbook) throws IOException {
        workbook.setSheetHidden(workbook.getSheetIndex(REFERENCES), true);
        var output = new ByteArrayOutputStream();
        workbook.write(output);
        return output.toByteArray();
    }

    private BusinessRuleException invalid(MessageCatalogCodeEnum code) {
        return BusinessRuleException.buildUserException(catalog.getMessage(code.getCode()));
    }

    private BusinessException technical(Exception ex) {
        return BusinessException.buildTechnicalException(catalog.getMessage(TCH_079.getCode()), ex, ExceptionLocation.INFRASTRUCTURE);
    }
}
