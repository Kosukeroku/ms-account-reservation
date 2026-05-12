package kosukeroku.ms_account_reservation.config;

import net.ttddyy.dsproxy.asserts.ProxyTestDataSource;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@TestConfiguration
public class TestDataSourceConfig {

    @Bean
    @Primary
    public ProxyTestDataSource dataSource(DataSource originalDataSource) {
        return new ProxyTestDataSource(originalDataSource);
    }
}