package kosukeroku.ms_account_reservation.aspect;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class CurrencyMetricsAspect {

    private final MeterRegistry meterRegistry;

    @Before(value = "execution(* kosukeroku.currencyclient.service.CurrencyService.getExchangeRate(..)) && args(from, to)", argNames = "from,to")
    public void countRequest(String from, String to) {
        meterRegistry.counter("currency.exchange.rate.requests",
                "from", from.toUpperCase(),
                "to", to.toUpperCase()
        ).increment();
    }
}