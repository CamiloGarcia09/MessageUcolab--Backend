package co.edu.uco.crosscutting.helpers;

import org.junit.jupiter.api.Test;
import co.edu.uco.crosscutting.exceptions.BusinessException;

import static co.edu.uco.crosscutting.catalog.BootstrapMessageCatalogEnum.TCH_082;
import static co.edu.uco.crosscutting.catalog.BootstrapMessageCatalogEnum.TCH_083;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PropertiesHelperTest {

    private static final String PROPERTIES_FILE = "test-catalog.properties";

    @Test
    void getValue_returnsValue_whenKeyExists() {
        String value = PropertiesHelper.getValue(PROPERTIES_FILE, "catalog.param.test.value");
        assertThat(value).isEqualTo("test-value");
    }

    @Test
    void getValue_throwsIllegalArgumentException_whenFileNameIsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> PropertiesHelper.getValue(null, "key"));
    }

    @Test
    void getValue_throwsIllegalArgumentException_whenFileNameIsEmpty() {
        assertThrows(IllegalArgumentException.class,
                () -> PropertiesHelper.getValue("  ", "key"));
    }

    @Test
    void getValue_throwsIllegalArgumentException_whenKeyIsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> PropertiesHelper.getValue(PROPERTIES_FILE, null));
    }

    @Test
    void getValue_throwsIllegalArgumentException_whenKeyIsEmpty() {
        assertThrows(IllegalArgumentException.class,
                () -> PropertiesHelper.getValue(PROPERTIES_FILE, "  "));
    }

    @Test
    void getValue_throwsBusinessException_whenFileDoesNotExist() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> PropertiesHelper.getValue("nonexistent-file.properties", "key"));
        assertThat(exception.getTechnicalMessage()).isEqualTo(TCH_082.getContent().formatted("nonexistent-file.properties"));
    }

    @Test
    void getValue_throwsBusinessException_whenKeyDoesNotExist() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> PropertiesHelper.getValue(PROPERTIES_FILE, "catalog.param.does.not.exist"));
        assertThat(exception.getTechnicalMessage()).isEqualTo(
                TCH_083.getContent().formatted("catalog.param.does.not.exist", PROPERTIES_FILE));
    }

    @Test
    void getValue_throwsBusinessException_whenValueIsBlank() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> PropertiesHelper.getValue(PROPERTIES_FILE, "catalog.param.blank"));
        assertThat(exception.getTechnicalMessage()).isEqualTo(
                TCH_083.getContent().formatted("catalog.param.blank", PROPERTIES_FILE));
    }
}
