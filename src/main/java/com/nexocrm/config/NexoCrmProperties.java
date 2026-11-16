package com.nexocrm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;

/** Configuración propia de la aplicación (prefijo {@code nexocrm.*} en application.yml). */
@ConfigurationProperties(prefix = "nexocrm")
public record NexoCrmProperties(Seed seed, ExchangeRates exchangeRates, Webhooks webhooks) {

    public record Seed(boolean enabled) {
    }

    /**
     * @param url       endpoint de la API pública de tasas (base USD)
     * @param cacheTtl  tiempo que se reutiliza la última respuesta
     * @param fallback  tasas de respaldo si la API externa no responde
     */
    public record ExchangeRates(String url, Duration cacheTtl, Duration timeout, Map<String, BigDecimal> fallback) {
    }

    public record Webhooks(Duration timeout, int maxAttempts) {
    }
}
