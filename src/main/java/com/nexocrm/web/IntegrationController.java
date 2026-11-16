package com.nexocrm.web;

import com.nexocrm.integration.csv.CsvImportResult;
import com.nexocrm.integration.csv.CsvService;
import com.nexocrm.integration.excel.ExcelReportService;
import com.nexocrm.integration.exchange.ExchangeRateService;
import com.nexocrm.integration.exchange.ExchangeRatesDto;
import com.nexocrm.integration.webhook.WebhookDtos.DeliveryDto;
import com.nexocrm.integration.webhook.WebhookDtos.SubscriptionDto;
import com.nexocrm.integration.webhook.WebhookDtos.SubscriptionRequest;
import com.nexocrm.integration.webhook.WebhookService;
import com.nexocrm.service.BusinessRuleException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/integrations")
@Tag(name = "Integraciones", description = "CSV, Excel, tasas de cambio y webhooks")
public class IntegrationController {

    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    private static final MediaType CSV = new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8);

    private final CsvService csv;
    private final ExcelReportService excel;
    private final ExchangeRateService exchangeRates;
    private final WebhookService webhooks;

    public IntegrationController(CsvService csv, ExcelReportService excel, ExchangeRateService exchangeRates,
                                 WebhookService webhooks) {
        this.csv = csv;
        this.excel = excel;
        this.exchangeRates = exchangeRates;
        this.webhooks = webhooks;
    }

    // ---- CSV ----

    @GetMapping("/csv/customers")
    @Operation(summary = "Exporta todos los clientes a CSV")
    public ResponseEntity<byte[]> exportCustomers() {
        return download(csv.exportCustomers(), CSV, "clientes-" + LocalDate.now() + ".csv");
    }

    @GetMapping("/csv/deals")
    @Operation(summary = "Exporta todas las oportunidades a CSV")
    public ResponseEntity<byte[]> exportDeals() {
        return download(csv.exportDeals(), CSV, "oportunidades-" + LocalDate.now() + ".csv");
    }

    @PostMapping(value = "/csv/customers", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Importa clientes desde CSV (columnas: empresa, contacto, email, telefono, industria, ciudad, pais, estado)")
    public CsvImportResult importCustomers(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new BusinessRuleException("El archivo está vacío");
        }
        return csv.importCustomers(file.getInputStream());
    }

    // ---- Excel ----

    @GetMapping("/reports/sales.xlsx")
    @Operation(summary = "Descarga el reporte comercial en Excel")
    public ResponseEntity<byte[]> salesReport() {
        return download(excel.salesReport(), XLSX, "reporte-ventas-" + LocalDate.now() + ".xlsx");
    }

    // ---- Tasas de cambio ----

    @GetMapping("/exchange-rates")
    @Operation(summary = "Tasas de cambio desde USD (API externa con caché y respaldo)")
    public ExchangeRatesDto rates() {
        return exchangeRates.getRates();
    }

    // ---- Webhooks ----

    @GetMapping("/webhooks")
    public List<SubscriptionDto> listWebhooks() {
        return webhooks.list();
    }

    @PostMapping("/webhooks")
    @ResponseStatus(HttpStatus.CREATED)
    public SubscriptionDto createWebhook(@Valid @RequestBody SubscriptionRequest request) {
        return webhooks.create(request);
    }

    @PatchMapping("/webhooks/{id}/toggle")
    public SubscriptionDto toggleWebhook(@PathVariable Long id) {
        return webhooks.toggle(id);
    }

    @DeleteMapping("/webhooks/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWebhook(@PathVariable Long id) {
        webhooks.delete(id);
    }

    @PostMapping("/webhooks/{id}/test")
    @Operation(summary = "Envía un evento de prueba firmado a la URL del webhook")
    public DeliveryDto testWebhook(@PathVariable Long id) {
        return webhooks.sendTest(id);
    }

    @GetMapping("/webhooks/deliveries")
    @Operation(summary = "Historial de las últimas entregas")
    public List<DeliveryDto> deliveries() {
        return webhooks.recentDeliveries();
    }

    private static ResponseEntity<byte[]> download(byte[] body, MediaType type, String filename) {
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .body(body);
    }
}
