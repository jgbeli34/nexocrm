package com.nexocrm.integration.exchange;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * @param source  "live" si viene de la API externa, "fallback" si se usaron tasas de respaldo
 */
public record ExchangeRatesDto(String base, Map<String, BigDecimal> rates, String source, Instant fetchedAt) {
}
