package co.edu.uco.application.primaryports.dto.message;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConsumerTranslationRequestDTOTest {
    @Test
    void setTargetLanguage_trimsWhitespace() {
        var request = new ConsumerTranslationRequestDTO();

        request.setTargetLanguage("  zh-Hans  ");

        assertThat(request.getTargetLanguage()).isEqualTo("zh-Hans");
    }
}
