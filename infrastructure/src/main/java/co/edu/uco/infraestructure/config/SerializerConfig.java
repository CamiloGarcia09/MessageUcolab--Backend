package co.edu.uco.infraestructure.config;

import co.edu.uco.infraestructure.secondaryadapters.presenter.serializer.SerializerType;
import co.edu.uco.infraestructure.secondaryadapters.presenter.serializer.impl.html.HTMLSerializer;
import co.edu.uco.infraestructure.secondaryadapters.presenter.serializer.impl.json.JsonSerializer;
import co.edu.uco.infraestructure.secondaryadapters.presenter.serializer.impl.text.PlainTextSerializer;
import co.edu.uco.infraestructure.secondaryadapters.presenter.serializer.impl.xml.XMLSerializer;
import co.edu.uco.infraestructure.secondaryadapters.presenter.serializer.impl.yaml.YamlSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SerializerConfig {
    @Bean
    public SerializerType jsonSerializer() {
        return new JsonSerializer();
    }
    @Bean
    public SerializerType yamlSerializer() {
        return new YamlSerializer();
    }
    @Bean
    public SerializerType textSerializer() { return new PlainTextSerializer(); }
    @Bean
    public SerializerType htmlSerializer() { return new HTMLSerializer(); }
    @Bean SerializerType xmlSerializer() { return new XMLSerializer(); }
}