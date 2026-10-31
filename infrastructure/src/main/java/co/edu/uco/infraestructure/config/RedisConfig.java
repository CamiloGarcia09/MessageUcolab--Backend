package co.edu.uco.infraestructure.config;

import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.infraestructure.secondaryadapters.repository.redis.MessageRedis;
import co.edu.uco.crosscutting.catalog.BootstrapMessageCatalogEnum;
import co.edu.uco.crosscutting.exceptions.BusinessException;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import static co.edu.uco.infraestructure.config.InfrastructureConstant.PACKAGE_REPOSITORY_ADAPTER;

@Configuration
@EnableRedisRepositories(basePackages = {PACKAGE_REPOSITORY_ADAPTER})
public class RedisConfig {
    private final LoggingPort log;

    public RedisConfig(LoggingPortFactory loggerFactory) {
        this.log = loggerFactory.getLogger(RedisConfig.class);
    }

    @Bean
    public RedisTemplate<String, MessageRedis> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, MessageRedis> template = new RedisTemplate<>();
        try {
            template.setConnectionFactory(connectionFactory);
            var objectMapper = new ObjectMapper();
            objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
            PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                    .allowIfSubType(MessageRedis.class)
                    .build();
            objectMapper.activateDefaultTyping(
                    ptv,
                    ObjectMapper.DefaultTyping.NON_FINAL,
                    JsonTypeInfo.As.PROPERTY
            );
            Jackson2JsonRedisSerializer<MessageRedis> serializer = new Jackson2JsonRedisSerializer<>(objectMapper, MessageRedis.class);
            template.setKeySerializer(new StringRedisSerializer());
            template.setValueSerializer(serializer);
            template.afterPropertiesSet();
        } catch (RedisConnectionFailureException ex) {
            log.error(BootstrapMessageCatalogEnum.FUN_013.getContent(), ex);
            throw BusinessException.buildTechnicalException(BootstrapMessageCatalogEnum.FUN_013.getContent());
        } catch (DataAccessException ex) {
            log.error(BootstrapMessageCatalogEnum.FUN_014.getContent(), ex);
            throw BusinessException.buildTechnicalException(BootstrapMessageCatalogEnum.FUN_014.getContent());
        } catch (Exception ex) {
            log.error(BootstrapMessageCatalogEnum.FUN_015.getContent(), ex);
            throw BusinessException.buildTechnicalException(BootstrapMessageCatalogEnum.FUN_015.getContent());
        }
        return template;
    }
}
