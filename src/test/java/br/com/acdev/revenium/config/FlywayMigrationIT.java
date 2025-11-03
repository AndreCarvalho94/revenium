package br.com.acdev.revenium.config;

import br.com.acdev.revenium.ReveniumApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@Import(br.com.acdev.revenium.config.TestcontainersConfiguration.class)
@SpringBootTest(classes = ReveniumApplication.class)
class FlywayMigrationIT {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void flywaySchemaHistoryExistsAndHasAtLeastOneRow() {
        Integer count = jdbcTemplate.queryForObject("select count(*) from flyway_schema_history", Integer.class);
        assertThat(count).isNotNull();
        assertThat(count).isGreaterThanOrEqualTo(1);
    }
}
