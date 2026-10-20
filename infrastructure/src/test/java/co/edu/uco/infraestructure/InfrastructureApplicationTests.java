package co.edu.uco.infraestructure;

import co.edu.uco.init.CrossWordApplication;
import com.surrealdb.Surreal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = CrossWordApplication.class)
@ActiveProfiles("test")
class InfrastructureApplicationTests {

    @MockitoBean
    private Surreal surreal;

    @Autowired
    private Clock clock;

    @Autowired
    private ObjectMapper adapterObjectMapper;

    @Autowired
    private tools.jackson.databind.ObjectMapper httpObjectMapper;

    @Test
    void contextLoads() {
        assertThat(clock.getZone()).isEqualTo(ZoneOffset.UTC);
    }

    @Test
    void jackson2AdapterMapper_andJackson3HttpMapper_coexistInBootContext() {
        assertThat(adapterObjectMapper).isNotNull();
        assertThat(httpObjectMapper).isNotNull();
        assertThat(adapterObjectMapper.getClass().getName()).startsWith("com.fasterxml.jackson.");
        assertThat(httpObjectMapper.getClass().getName()).startsWith("tools.jackson.");
    }
}
