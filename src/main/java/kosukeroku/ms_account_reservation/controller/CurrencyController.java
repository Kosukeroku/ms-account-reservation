package kosukeroku.ms_account_reservation.controller;

import kosukeroku.ms_account_reservation.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CurrencyController {

    private final ExchangeRateService exchangeRateService;

    @GetMapping("/rate")
    public Map<String, Object> getRate(@RequestParam String from, @RequestParam String to) {
        BigDecimal rate = exchangeRateService.getExchangeRate(from, to);
        return Map.of(
                "to", to.toUpperCase(),
                "from", from.toUpperCase(),
                "rate", rate
        );
    }
}