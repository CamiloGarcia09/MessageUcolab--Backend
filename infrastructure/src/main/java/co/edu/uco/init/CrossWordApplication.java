package co.edu.uco.init;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

import static co.edu.uco.infraestructure.config.InfrastructureConstant.PACKAGE_BASE;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class })
@ComponentScan(basePackages = {PACKAGE_BASE})
@EnableScheduling
public class CrossWordApplication {
    public static void main(String[] args) {
        configureUtcTimeZone();
        SpringApplication.run(CrossWordApplication.class, args);
    }

    static void configureUtcTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        System.setProperty("user.timezone", "UTC");
    }
}
