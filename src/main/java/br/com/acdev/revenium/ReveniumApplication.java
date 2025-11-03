package br.com.acdev.revenium;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication
@ConfigurationPropertiesScan
@ComponentScan(excludeFilters = {
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "br\\.com\\.acdev\\.revenium\\.scheduler.*"),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = br.com.acdev.revenium.ReveniumSchedulerApplication.class)
})
public class ReveniumApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReveniumApplication.class, args);
    }

}
