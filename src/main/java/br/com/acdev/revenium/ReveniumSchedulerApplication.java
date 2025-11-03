package br.com.acdev.revenium;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "br.com.acdev.revenium")
@ConfigurationPropertiesScan
@ComponentScan(excludeFilters = {
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "br\\.com\\.acdev\\.revenium\\.controller.*"),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = br.com.acdev.revenium.ReveniumApplication.class)
})
@EnableScheduling
public class ReveniumSchedulerApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(ReveniumSchedulerApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);
    }

}
