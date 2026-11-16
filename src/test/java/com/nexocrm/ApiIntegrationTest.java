package com.nexocrm;

import com.nexocrm.domain.DealStage;
import com.nexocrm.repository.DealRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Pruebas de extremo a extremo de la API sobre H2 con los datos de demostración. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    DealRepository deals;

    @Test
    void panelDevuelveIndicadoresCalculados() throws Exception {
        mvc.perform(get("/api/analytics/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpis.revenueWon", greaterThan(0.0)))
                .andExpect(jsonPath("$.kpis.winRate", both(greaterThan(0.0)).and(lessThanOrEqualTo(100.0))))
                .andExpect(jsonPath("$.revenueByMonth", hasSize(12)))
                .andExpect(jsonPath("$.pipelineByStage", hasSize(4)))
                .andExpect(jsonPath("$.leaderboard", hasSize(5)));
    }

    @Test
    void creaClienteYValidaCampos() throws Exception {
        mvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content("""
                        {"companyName":"Empresa Test","contactName":"Ana","email":"ana@empresa.test","status":"LEAD"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.statusLabel").value("Lead"));

        mvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content("""
                        {"companyName":"","contactName":"Ana","email":"no-es-email","status":"LEAD"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.email").value("El correo no es válido"))
                .andExpect(jsonPath("$.fields.companyName").exists());
    }

    @Test
    void moverOportunidadAplicaReglasDeNegocio() throws Exception {
        Long id = deals.findAll().stream().filter(d -> d.getStage().isOpen()).findFirst().orElseThrow().getId();

        mvc.perform(patch("/api/deals/{id}/stage", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stage\":\"LOST\"}"))
                .andExpect(status().isUnprocessableEntity());

        mvc.perform(patch("/api/deals/{id}/stage", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stage\":\"WON\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stage").value(DealStage.WON.name()))
                .andExpect(jsonPath("$.probability").value(100))
                .andExpect(jsonPath("$.closedAt").isNotEmpty());
    }

    @Test
    void oportunidadInexistenteDevuelve404() throws Exception {
        mvc.perform(get("/api/deals/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"));
    }

    @Test
    void importaClientesDesdeCsvReportandoErrores() throws Exception {
        String csv = """
                empresa,contacto,email,telefono,industria,ciudad,pais,estado
                Nueva Uno,Pedro Paz,pedro@nuevauno.test,,Retail,Bogotá,Colombia,LEAD
                Nueva Dos,Rosa Sol,rosa@nuevados.test,,Salud,Cali,Colombia,Activo
                Sin Email,Luis,,,,,,
                Duplicada,Pedro Paz,PEDRO@nuevauno.test,,,,,
                """;
        var file = new MockMultipartFile("file", "clientes.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        mvc.perform(multipart("/api/integrations/csv/customers").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRows").value(4))
                .andExpect(jsonPath("$.imported").value(2))
                .andExpect(jsonPath("$.errors", hasSize(2)))
                .andExpect(jsonPath("$.errors[0].row").value(4));
    }

    @Test
    void exportaCsvYExcel() throws Exception {
        byte[] csv = mvc.perform(get("/api/integrations/csv/customers"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("clientes-")))
                .andReturn().getResponse().getContentAsByteArray();
        String text = new String(csv, StandardCharsets.UTF_8);
        assertThat(text).startsWith("﻿empresa,contacto,email");

        byte[] xlsx = mvc.perform(get("/api/integrations/reports/sales.xlsx"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(xlsx).startsWith((byte) 'P', (byte) 'K'); // un .xlsx es un ZIP
    }

    @Test
    void tasasDeCambioUsanRespaldoSiLaApiNoResponde() throws Exception {
        mvc.perform(get("/api/integrations/exchange-rates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.base").value("USD"))
                .andExpect(jsonPath("$.source").value("fallback"))
                .andExpect(jsonPath("$.rates.COP").value(4050));
    }

    @Test
    void webhookConUrlInvalidaSeRechaza() throws Exception {
        mvc.perform(post("/api/integrations/webhooks").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"X","targetUrl":"ftp://algo","event":"DEAL_WON"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.targetUrl").exists());
    }
}
