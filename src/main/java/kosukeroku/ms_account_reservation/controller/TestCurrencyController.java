package kosukeroku.ms_account_reservation.controller;

import kosukeroku.ms_account_reservation.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestCurrencyController {

    private final ExchangeRateService exchangeRateService;

    @GetMapping("/rate")
    public BigDecimal getRate(@RequestParam String from, @RequestParam String to) {
        return exchangeRateService.getExchangeRate(from, to);
    }
}