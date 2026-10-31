package co.edu.uco.crosscutting.helpers;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UtilObjectTest {

    @Test
    void isNullObject_returnsTrue_whenObjectIsNull() {
        assertThat(UtilObject.isNullObject(null)).isTrue();
    }

    @Test
    void isNullObject_returnsFalse_whenObjectIsNotNull() {
        assertThat(UtilObject.isNullObject("value")).isFalse();
    }

    @Test
    void getDefaultIsNullObject_returnsDefault_whenObjectIsNull() {
        String result = UtilObject.getDefaultIsNullObject(null, "default");
        assertThat(result).isEqualTo("default");
    }

    @Test
    void getDefaultIsNullObject_returnsObject_whenObjectIsNotNull() {
        String result = UtilObject.getDefaultIsNullObject("value", "default");
        assertThat(result).isEqualTo("value");
    }
}
