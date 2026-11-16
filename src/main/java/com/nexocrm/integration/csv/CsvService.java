package com.nexocrm.integration.csv;

import com.nexocrm.domain.Customer;
import com.nexocrm.domain.CustomerStatus;
import com.nexocrm.domain.LeadSource;
import com.nexocrm.integration.csv.CsvImportResult.RowError;
import com.nexocrm.repository.CustomerRepository;
import com.nexocrm.repository.DealRepository;
import com.nexocrm.service.BusinessRuleException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Importación y exportación CSV: la integración más común con Excel, Google Sheets,
 * HubSpot o cualquier herramienta que exporte contactos.
 */
@Service
public class CsvService {

    static final String[] CUSTOMER_HEADERS = {
            "empresa", "contacto", "email", "telefono", "industria", "ciudad", "pais", "estado"};
    static final String[] DEAL_HEADERS = {
            "id", "titulo", "cliente", "vendedor", "monto_usd", "etapa", "probabilidad",
            "fecha_cierre_esperada", "creado", "cerrado"};

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int MAX_ROWS = 5_000;

    private final CustomerRepository customers;
    private final DealRepository deals;

    public CsvService(CustomerRepository customers, DealRepository deals) {
        this.customers = customers;
        this.deals = deals;
    }

    @Transactional(readOnly = true)
    public byte[] exportCustomers() {
        return write(CUSTOMER_HEADERS, printer -> {
            for (Customer c : customers.findAllByOrderByCompanyNameAsc()) {
                printer.printRecord(c.getCompanyName(), c.getContactName(), c.getEmail(), c.getPhone(),
                        c.getIndustry(), c.getCity(), c.getCountry(), c.getStatus().name());
            }
        });
    }

    @Transactional(readOnly = true)
    public byte[] exportDeals() {
        return write(DEAL_HEADERS, printer -> {
            for (var d : deals.findAllWithRelations()) {
                printer.printRecord(d.getId(), d.getTitle(), d.getCustomer().getCompanyName(),
                        d.getOwner() != null ? d.getOwner().getName() : "", d.getAmount(), d.getStage().name(),
                        d.getProbability(), d.getExpectedCloseDate(), d.getCreatedAt().toLocalDate(),
                        d.getClosedAt() != null ? d.getClosedAt().toLocalDate() : "");
            }
        });
    }

    /**
     * Importa clientes validando fila por fila. Las filas inválidas o con correo duplicado
     * se omiten y se reportan, sin abortar toda la importación.
     */
    @Transactional
    public CsvImportResult importCustomers(InputStream input) {
        List<RowError> errors = new ArrayList<>();
        Set<String> seenEmails = new HashSet<>();
        List<Customer> toSave = new ArrayList<>();
        int total = 0;

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader().setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true).setTrim(true).setIgnoreEmptyLines(true)
                .get();

        try (Reader reader = new InputStreamReader(stripBom(input), StandardCharsets.UTF_8);
             CSVParser parser = CSVParser.parse(reader, format)) {

            Set<String> headers = new HashSet<>();
            parser.getHeaderNames().forEach(h -> headers.add(h.toLowerCase(Locale.ROOT)));
            for (String required : List.of("empresa", "contacto", "email")) {
                if (!headers.contains(required)) {
                    throw new BusinessRuleException("Falta la columna obligatoria '" + required
                            + "'. Columnas esperadas: " + String.join(", ", CUSTOMER_HEADERS));
                }
            }

            for (CSVRecord record : parser) {
                total++;
                if (total > MAX_ROWS) {
                    throw new BusinessRuleException("El archivo supera el máximo de " + MAX_ROWS + " filas");
                }
                long row = record.getRecordNumber() + 1; // +1 por la fila de encabezados
                String company = value(record, "empresa");
                String contact = value(record, "contacto");
                String email = value(record, "email");

                if (company.isEmpty() || contact.isEmpty() || email.isEmpty()) {
                    errors.add(new RowError(row, "Empresa, contacto y email son obligatorios"));
                    continue;
                }
                if (!EMAIL.matcher(email).matches()) {
                    errors.add(new RowError(row, "Email inválido: " + email));
                    continue;
                }
                String key = email.toLowerCase(Locale.ROOT);
                if (!seenEmails.add(key) || customers.existsByEmailIgnoreCase(email)) {
                    errors.add(new RowError(row, "Ya existe un cliente con el email " + email));
                    continue;
                }

                Customer c = new Customer(company, contact, email);
                c.setPhone(emptyToNull(value(record, "telefono")));
                c.setIndustry(emptyToNull(value(record, "industria")));
                c.setCity(emptyToNull(value(record, "ciudad")));
                c.setCountry(emptyToNull(value(record, "pais")));
                c.setStatus(parseStatus(value(record, "estado")));
                c.setSource(LeadSource.IMPORT);
                toSave.add(c);
            }
        } catch (IOException | UncheckedIOException e) {
            throw new BusinessRuleException("No se pudo leer el archivo CSV: " + e.getMessage());
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new BusinessRuleException("El archivo no tiene un formato CSV válido: " + e.getMessage());
        }

        customers.saveAll(toSave);
        return new CsvImportResult(total, toSave.size(), total - toSave.size(), errors);
    }

    private static String value(CSVRecord record, String column) {
        if (!record.isMapped(column) || !record.isSet(column)) {
            return "";
        }
        String v = record.get(column);
        return v == null ? "" : v.trim();
    }

    private static String emptyToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    private static CustomerStatus parseStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return CustomerStatus.LEAD;
        }
        for (CustomerStatus s : CustomerStatus.values()) {
            if (s.name().equalsIgnoreCase(raw) || s.getLabel().equalsIgnoreCase(raw)) {
                return s;
            }
        }
        return CustomerStatus.LEAD;
    }

    private static InputStream stripBom(InputStream in) throws IOException {
        PushbackInputStream pushback = new PushbackInputStream(new BufferedInputStream(in), 3);
        byte[] bom = new byte[3];
        int read = pushback.read(bom, 0, 3);
        boolean hasBom = read == 3 && (bom[0] & 0xFF) == 0xEF && (bom[1] & 0xFF) == 0xBB && (bom[2] & 0xFF) == 0xBF;
        if (!hasBom && read > 0) {
            pushback.unread(bom, 0, read);
        }
        return pushback;
    }

    @FunctionalInterface
    private interface RowWriter {
        void write(CSVPrinter printer) throws IOException;
    }

    /** Escribe con BOM UTF-8 para que Excel muestre bien las tildes. */
    private static byte[] write(String[] headers, RowWriter rows) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            out.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
            try (CSVPrinter printer = new CSVPrinter(new OutputStreamWriter(out, StandardCharsets.UTF_8),
                    CSVFormat.DEFAULT.builder().setHeader(headers).get())) {
                rows.write(printer);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }
}
