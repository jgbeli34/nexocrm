package com.nexocrm.config;

import com.nexocrm.domain.*;
import com.nexocrm.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.*;

/**
 * Genera datos de demostración realistas (fechas relativas a hoy) para que el panel
 * siempre tenga 12 meses de historia. Es determinista: usa una semilla fija.
 * Se desactiva con {@code nexocrm.seed.enabled=false}.
 */
@Component
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private static final String[][] COMPANIES = {
            // empresa, industria, ciudad, país
            {"Café Andino S.A.S.", "Agroindustria", "Manizales", "Colombia"},
            {"Logística del Pacífico", "Logística", "Buenaventura", "Colombia"},
            {"Textiles La Montaña", "Manufactura", "Medellín", "Colombia"},
            {"Clínica Santa Lucía", "Salud", "Bogotá", "Colombia"},
            {"Grupo Educativo Horizonte", "Educación", "Cali", "Colombia"},
            {"Fintech Cóndor", "Finanzas", "Bogotá", "Colombia"},
            {"Hoteles Caribe Azul", "Turismo", "Cartagena", "Colombia"},
            {"Distribuidora El Puerto", "Retail", "Barranquilla", "Colombia"},
            {"Constructora Vértice", "Construcción", "Bucaramanga", "Colombia"},
            {"AgroValle Orgánicos", "Agroindustria", "Palmira", "Colombia"},
            {"Laboratorios Bioandes", "Salud", "Medellín", "Colombia"},
            {"Moda Urbana Co.", "Retail", "Bogotá", "Colombia"},
            {"Transportes Ruta 40", "Logística", "Pereira", "Colombia"},
            {"Seguros Altamira", "Finanzas", "Bogotá", "Colombia"},
            {"Panadería La Espiga", "Retail", "Cali", "Colombia"},
            {"Instituto Técnico Nova", "Educación", "Medellín", "Colombia"},
            {"Energía Solar Llanos", "Energía", "Villavicencio", "Colombia"},
            {"Metalmecánica Andes", "Manufactura", "Cali", "Colombia"},
            {"Inmobiliaria Prisma", "Construcción", "Bogotá", "Colombia"},
            {"Ópticas Visión Clara", "Salud", "Pereira", "Colombia"},
            {"Mercados Frescos", "Retail", "Ciudad de México", "México"},
            {"Grupo Industrial Maya", "Manufactura", "Monterrey", "México"},
            {"Viajes Sol y Mar", "Turismo", "Cancún", "México"},
            {"Banca Comunal Azteca", "Finanzas", "Guadalajara", "México"},
            {"Pesquera del Sur", "Agroindustria", "Lima", "Perú"},
            {"Universidad Inca Digital", "Educación", "Lima", "Perú"},
            {"Minera Altiplano", "Energía", "Arequipa", "Perú"},
            {"Vinos del Valle Central", "Agroindustria", "Santiago", "Chile"},
            {"Retail Patagonia", "Retail", "Santiago", "Chile"},
            {"Salud Integral Quito", "Salud", "Quito", "Ecuador"},
            {"Cacao Esmeralda", "Agroindustria", "Guayaquil", "Ecuador"},
            {"Software Ceiba", "Tecnología", "Medellín", "Colombia"},
            {"Datos & Nube Ltda.", "Tecnología", "Bogotá", "Colombia"},
            {"Ferretería Industrial Norte", "Retail", "Cúcuta", "Colombia"},
            {"Agencia Creativa Pulpo", "Tecnología", "Bogotá", "Colombia"},
            {"Lácteos San Pedro", "Agroindustria", "Ubaté", "Colombia"},
            {"Farmacias Vida", "Salud", "Barranquilla", "Colombia"},
            {"Coworking Colmena", "Construcción", "Medellín", "Colombia"},
            {"Envíos Express Ya", "Logística", "Bogotá", "Colombia"},
            {"Colegio Bilingüe Arcoíris", "Educación", "Chía", "Colombia"},
            {"Aerolínea Regional Cóndor", "Turismo", "Rionegro", "Colombia"},
            {"Cooperativa Financiera Unión", "Finanzas", "Tunja", "Colombia"},
    };

    private static final String[] FIRST = {"María", "Juan", "Daniela", "Carlos", "Paula", "Felipe", "Sofía",
            "Alejandro", "Natalia", "Diego", "Isabela", "Mateo", "Juliana", "Sebastián", "Mariana", "Tomás",
            "Catalina", "Nicolás", "Gabriela", "Andrés", "Lucía", "Ricardo", "Elena", "Jorge"};
    private static final String[] LAST = {"García", "Rodríguez", "Martínez", "López", "Hernández", "Gómez",
            "Díaz", "Restrepo", "Ospina", "Castro", "Vargas", "Rojas", "Mejía", "Torres", "Salazar", "Cárdenas",
            "Pineda", "Navarro", "Quintero", "Duarte"};
    private static final String[] PRODUCTS = {"Licencias CRM (25 usuarios)", "Implementación ERP", "Soporte anual premium",
            "Migración a la nube", "Tablero de BI comercial", "Integración e-commerce", "Plan Enterprise",
            "Renovación anual", "Capacitación equipo comercial", "Automatización de facturación",
            "App móvil de pedidos", "Licencias CRM (50 usuarios)", "Consultoría de datos", "Portal de clientes"};
    private static final String[] LOST_REASONS = {"Eligió a la competencia", "Precio fuera de presupuesto",
            "Proyecto pospuesto", "Sin respuesta del cliente", "No cumplía requisitos técnicos"};
    private static final LeadSource[] SOURCES = {LeadSource.WEBSITE, LeadSource.REFERRAL, LeadSource.LINKEDIN,
            LeadSource.LINKEDIN, LeadSource.EVENT, LeadSource.COLD_OUTREACH, LeadSource.REFERRAL, LeadSource.WEBSITE};

    private final SalesRepRepository reps;
    private final CustomerRepository customers;
    private final DealRepository deals;
    private final ActivityRepository activities;
    private final WebhookSubscriptionRepository webhooks;
    private final NexoCrmProperties properties;
    private final int serverPort;
    private final Random random = new Random(20261116L);

    public DemoDataSeeder(SalesRepRepository reps, CustomerRepository customers, DealRepository deals,
                          ActivityRepository activities, WebhookSubscriptionRepository webhooks,
                          NexoCrmProperties properties, @Value("${server.port:8080}") int serverPort) {
        this.reps = reps;
        this.customers = customers;
        this.deals = deals;
        this.activities = activities;
        this.webhooks = webhooks;
        this.properties = properties;
        this.serverPort = serverPort;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (properties.seed() == null || !properties.seed().enabled() || reps.count() > 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();

        List<SalesRep> team = reps.saveAll(List.of(
                new SalesRep("Valentina Ríos", "valentina.rios@nexocrm.dev", new BigDecimal("60000")),
                new SalesRep("Andrés Morales", "andres.morales@nexocrm.dev", new BigDecimal("55000")),
                new SalesRep("Camila Herrera", "camila.herrera@nexocrm.dev", new BigDecimal("50000")),
                new SalesRep("Santiago Vélez", "santiago.velez@nexocrm.dev", new BigDecimal("45000")),
                new SalesRep("Laura Gómez", "laura.gomez@nexocrm.dev", new BigDecimal("40000"))));

        List<Customer> clientList = new ArrayList<>();
        for (int i = 0; i < COMPANIES.length; i++) {
            String[] row = COMPANIES[i];
            String first = FIRST[random.nextInt(FIRST.length)];
            String last = LAST[random.nextInt(LAST.length)];
            Customer c = new Customer(row[0], first + " " + last, email(first, last, row[0]));
            c.setIndustry(row[1]);
            c.setCity(row[2]);
            c.setCountry(row[3]);
            c.setPhone(phone(row[3]));
            c.setSource(SOURCES[random.nextInt(SOURCES.length)]);
            c.setOwner(team.get(i % team.size()));
            c.setStatus(CustomerStatus.LEAD);
            c.setCreatedAt(now.minusDays(30 + random.nextInt(400)));
            clientList.add(c);
        }
        customers.saveAll(clientList);

        List<Deal> generated = new ArrayList<>();
        YearMonth current = YearMonth.from(now);

        // Historia cerrada: 12 meses con tendencia de crecimiento
        for (int i = 11; i >= 0; i--) {
            YearMonth ym = current.minusMonths(i);
            int lastDay = i == 0 ? now.getDayOfMonth() : ym.lengthOfMonth();
            int wonCount = 4 + (11 - i) / 2 + random.nextInt(3);
            if (i == 0) {
                wonCount = Math.max(4, Math.round((float) wonCount * lastDay / ym.lengthOfMonth()) + 3);
            }
            int lostCount = 2 + random.nextInt(3);
            for (int k = 0; k < wonCount + lostCount; k++) {
                boolean won = k < wonCount;
                LocalDateTime closedAt = ym.atDay(1 + random.nextInt(lastDay)).atTime(randomTime());
                if (closedAt.isAfter(now)) {
                    closedAt = now.minusHours(2);
                }
                Deal d = newDeal(clientList, team, closedAt.minusDays(12 + random.nextInt(60)), 1.0 + (11 - i) * 0.04);
                d.setExpectedCloseDate(closedAt.toLocalDate().plusDays(random.nextInt(10) - 5));
                d.moveTo(won ? DealStage.WON : DealStage.LOST,
                        won ? null : LOST_REASONS[random.nextInt(LOST_REASONS.length)], closedAt);
                if (won) {
                    d.getCustomer().setStatus(CustomerStatus.ACTIVE);
                }
                generated.add(d);
            }
        }

        // Pipeline abierto actual
        DealStage[] openStages = {DealStage.LEAD, DealStage.QUALIFIED, DealStage.PROPOSAL, DealStage.NEGOTIATION};
        int[] perStage = {11, 9, 8, 6};
        for (int s = 0; s < openStages.length; s++) {
            for (int k = 0; k < perStage[s]; k++) {
                LocalDateTime created = now.minusDays(3 + random.nextInt(80)).with(randomTime());
                Deal d = newDeal(clientList, team, created, 1.15);
                if (openStages[s] != DealStage.LEAD) {
                    long sinceCreated = java.time.Duration.between(created, now).toDays();
                    d.moveTo(openStages[s], null, created.plusDays(random.nextLong(Math.max(1, sinceCreated))));
                }
                d.setExpectedCloseDate(LocalDate.now().plusDays(-6 + random.nextInt(90) - s * 6L));
                generated.add(d);
            }
        }
        deals.saveAll(generated);

        // Un cliente existe antes que su primera oportunidad
        for (Deal d : generated) {
            Customer c = d.getCustomer();
            if (d.getCreatedAt().minusDays(7).isBefore(c.getCreatedAt())) {
                c.setCreatedAt(d.getCreatedAt().minusDays(7 + random.nextInt(20)));
            }
        }

        // Clientes sin compras recientes pasan a inactivos para dar variedad
        clientList.stream().filter(c -> c.getStatus() == CustomerStatus.LEAD).limit(4)
                .forEach(c -> c.setStatus(CustomerStatus.INACTIVE));

        seedActivities(generated, now);
        seedWebhooks();
        log.info("Datos de demostración creados: {} vendedores, {} clientes, {} oportunidades",
                team.size(), clientList.size(), generated.size());
    }

    private Deal newDeal(List<Customer> clientList, List<SalesRep> team, LocalDateTime createdAt, double growth) {
        Customer customer = clientList.get(random.nextInt(clientList.size()));
        SalesRep owner = random.nextInt(10) < 8 && customer.getOwner() != null
                ? customer.getOwner() : team.get(random.nextInt(team.size()));
        String product = PRODUCTS[random.nextInt(PRODUCTS.length)];
        double base = 2_500 + Math.pow(random.nextDouble(), 1.8) * 42_000;
        long amount = Math.round(base * growth / 500.0) * 500;
        Deal d = new Deal(product, customer, owner,
                BigDecimal.valueOf(amount));
        d.setCreatedAt(createdAt);
        return d;
    }

    private void seedActivities(List<Deal> all, LocalDateTime now) {
        List<Deal> open = all.stream().filter(d -> d.getStage().isOpen()).toList();
        String[][] templates = {
                {"CALL", "Llamada de seguimiento"},
                {"MEETING", "Demo del producto"},
                {"EMAIL", "Enviar propuesta actualizada"},
                {"TASK", "Preparar cotización"},
                {"MEETING", "Reunión de cierre"},
                {"CALL", "Validar presupuesto con finanzas"},
                {"EMAIL", "Compartir caso de éxito"},
                {"TASK", "Revisar contrato con jurídica"},
        };
        List<Activity> list = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            Deal deal = open.get(random.nextInt(open.size()));
            String[] t = templates[random.nextInt(templates.length)];
            LocalDateTime due;
            boolean done;
            if (i < 14) {          // próximas
                due = now.plusDays(random.nextInt(9)).with(randomTime());
                if (due.isBefore(now)) {
                    due = due.plusDays(1);
                }
                done = false;
            } else if (i < 18) {   // vencidas
                due = now.minusDays(1 + random.nextInt(4)).with(randomTime());
                done = false;
            } else {               // completadas
                due = now.minusDays(1 + random.nextInt(40)).with(randomTime());
                done = true;
            }
            Activity a = new Activity(ActivityType.valueOf(t[0]), t[1] + " con " + shortName(deal.getCustomer().getCompanyName()), due);
            a.setDeal(deal);
            a.setCustomer(deal.getCustomer());
            a.setOwner(deal.getOwner());
            a.setDescription("Relacionada con: " + deal.getTitle());
            if (done) {
                a.complete(due.plusHours(1));
            }
            list.add(a);
        }
        activities.saveAll(list);
    }

    private void seedWebhooks() {
        webhooks.save(new WebhookSubscription("Sandbox interno: negocios ganados",
                "http://localhost:" + serverPort + "/api/webhooks/receiver", WebhookEvent.DEAL_WON,
                com.nexocrm.integration.webhook.WebhookSigner.newSecret()));
        webhooks.save(new WebhookSubscription("Sandbox interno: cambios de etapa",
                "http://localhost:" + serverPort + "/api/webhooks/receiver", WebhookEvent.DEAL_STAGE_CHANGED,
                com.nexocrm.integration.webhook.WebhookSigner.newSecret()));
        WebhookSubscription slack = new WebhookSubscription("Canal #ventas en Slack (ejemplo)",
                "https://hooks.slack.com/services/T000/B000/REEMPLAZAR", WebhookEvent.DEAL_WON,
                com.nexocrm.integration.webhook.WebhookSigner.newSecret());
        slack.setActive(false);
        webhooks.save(slack);
    }

    private LocalTime randomTime() {
        return LocalTime.of(8 + random.nextInt(10), random.nextBoolean() ? 0 : 30);
    }

    private static String shortName(String company) {
        return company.replace(" S.A.S.", "").replace(" Ltda.", "").replace(" Co.", "");
    }

    private static String email(String first, String last, String company) {
        String domain = normalize(shortName(company)).replaceAll("[^a-z0-9]", "");
        return normalize(first) + "." + normalize(last) + "@" + domain + ".example";
    }

    private static String normalize(String s) {
        return java.text.Normalizer.normalize(s.toLowerCase(Locale.ROOT), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replace(" ", "");
    }

    private String phone(String country) {
        String prefix = switch (country) {
            case "México" -> "+52 55 ";
            case "Perú" -> "+51 9";
            case "Chile" -> "+56 9 ";
            case "Ecuador" -> "+593 9";
            default -> "+57 3";
        };
        return prefix + (10 + random.nextInt(89)) + " " + (100 + random.nextInt(899)) + " " + (1000 + random.nextInt(8999));
    }
}
