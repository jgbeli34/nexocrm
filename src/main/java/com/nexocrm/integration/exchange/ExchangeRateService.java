package com.nexocrm.integration.exchange;

import com.fasterxml.jackson.databind.JsonNode;
import com.nexocrm.config.NexoCrmProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Integración con una API pública de tasas de cambio (open.er-api.com, sin API key).
 * Los montos del CRM se guardan en USD; esta integración permite ver el panel en COP, MXN, EUR, etc.
 * <p>
 * Resiliencia: caché en memoria con TTL y tasas de respaldo si el proveedor no responde.
 */
@Service
public class ExchangeRateService {

    static final List<String> SUPPORTED = List.of("USD", "COP", "MXN", "EUR", "PEN", "CLP", "BRL");
    private static final Logger log = LoggerFactory.getLogger(ExchangeRateService.class);

    private final NexoCrmProperties.ExchangeRates config;
    private final RestClient client;
    private final Clock clock;
    private volatile ExchangeRatesDto cached;

    @Autowired
    public ExchangeRateService(NexoCrmProperties properties, RestClient.Builder builder) {
        this(properties, builder, Clock.systemUTC());
    }

    ExchangeRateService(NexoCrmProperties properties, RestClient.Builder builder, Clock clock) {
        this.config = properties.exchangeRates();
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) config.timeout().toMillis());
        factory.setReadTimeout((int) config.timeout().toMillis());
        this.client = builder.requestFactory(factory).build();
        this.clock = clock;
    }

    public ExchangeRatesDto getRates() {
        ExchangeRatesDto current = cached;
        Instant now = clock.instant();
        if (current != null && current.fetchedAt().plus(config.cacheTtl()).isAfter(now)) {
            return current;
        }
        ExchangeRatesDto fresh = fetchLive(now);
        if (fresh != null) {
            cached = fresh;
            return fresh;
        }
        // Si ya teníamos datos reales, preferimos esos aunque estén vencidos
        if (current != null && "live".equals(current.source())) {
            return current;
        }
        return new ExchangeRatesDto("USD", new LinkedHashMap<>(config.fallback()), "fallback", now);
    }

    private ExchangeRatesDto fetchLive(Instant now) {
        try {
            JsonNode body = client.get().uri(config.url()).retrieve().body(JsonNode.class);
            if (body == null || !"success".equals(body.path("result").asText())) {
                log.warn("La API de tasas respondió sin éxito: {}", body);
                return null;
            }
            JsonNode rates = body.path("rates");
            Map<String, BigDecimal> selected = new LinkedHashMap<>();
            for (String code : SUPPORTED) {
                if (rates.has(code)) {
                    selected.put(code, rates.get(code).decimalValue());
                }
            }
            if (selected.isEmpty()) {
                return null;
            }
            return new ExchangeRatesDto("USD", selected, "live", now);
        } catch (Exception e) {
            log.warn("No se pudo consultar la API de tasas de cambio ({}). Se usan tasas de respaldo.", e.getMessage());
            return null;
        }
    }
}
