package br.com.acdev.revenium;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication(scanBasePackages = "br.com.acdev.revenium")
@ConfigurationPropertiesScan
@ComponentScan(excludeFilters = {
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "br\\.com\\.acdev\\.revenium\\.controller.*"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "br\\.com\\.acdev\\.revenium\\.scheduler.*"),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ReveniumApplication.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ReveniumSchedulerApplication.class)
})
@EnableKafka
public class ReveniumWorkerApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(ReveniumWorkerApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);
    }

}
