package kosukeroku.ms_account_reservation;

import kosukeroku.currencyclient.config.CurrencyClientAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@ConfigurationPropertiesScan("kosukeroku.currencyclient.properties")
@Import(CurrencyClientAutoConfiguration.class)
@EnableCaching
public class MsAccountReservationApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsAccountReservationApplication.class, args);
	}

}
