package com.nexocrm.integration.excel;

import com.nexocrm.domain.Deal;
import com.nexocrm.repository.DealRepository;
import com.nexocrm.service.AnalyticsService;
import com.nexocrm.web.dto.DashboardDto;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

/**
 * Genera un reporte comercial en Excel (.xlsx) con formato listo para compartir:
 * resumen de KPIs, ventas por mes, desempeño por vendedor y el detalle de oportunidades.
 */
@Service
public class ExcelReportService {

    private static final byte[] INK = {(byte) 0x1B, (byte) 0x2A, (byte) 0x2F};
    private static final byte[] EMERALD_SOFT = {(byte) 0xE3, (byte) 0xF1, (byte) 0xEC};

    private final AnalyticsService analytics;
    private final DealRepository deals;

    public ExcelReportService(AnalyticsService analytics, DealRepository deals) {
        this.analytics = analytics;
        this.deals = deals;
    }

    @Transactional(readOnly = true)
    public byte[] salesReport() {
        DashboardDto dashboard = analytics.dashboard();
        List<Deal> allDeals = deals.findAllWithRelations().stream()
                .sorted(Comparator.comparing(Deal::getCreatedAt).reversed())
                .toList();

        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Styles st = new Styles(wb);
            summarySheet(wb, st, dashboard);
            monthlySheet(wb, st, dashboard);
            repsSheet(wb, st, dashboard);
            dealsSheet(wb, st, allDeals);
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void summarySheet(XSSFWorkbook wb, Styles st, DashboardDto d) {
        Sheet sheet = wb.createSheet("Resumen");
        Row title = sheet.createRow(0);
        cell(title, 0, "Reporte comercial NexoCRM", st.title);
        Row sub = sheet.createRow(1);
        cell(sub, 0, "Generado el " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                + " (montos en USD)", st.muted);

        var k = d.kpis();
        Object[][] rows = {
                {"Ingresos ganados (histórico)", k.revenueWon(), st.money},
                {"Ingresos del mes", k.revenueThisMonth(), st.money},
                {"Mes anterior al mismo corte", k.revenueLastMonth(), st.money},
                {"Pipeline abierto", k.openPipeline(), st.money},
                {"Pipeline ponderado (forecast)", k.weightedPipeline(), st.money},
                {"Ticket promedio", k.averageDealSize(), st.money},
                {"Tasa de cierre", k.winRate() / 100, st.percent},
                {"Ciclo de venta promedio (días)", k.averageSalesCycleDays(), st.decimal},
                {"Oportunidades abiertas", k.openDeals(), st.integer},
                {"Clientes activos", k.activeCustomers(), st.integer},
        };
        Row header = sheet.createRow(3);
        cell(header, 0, "Indicador", st.header);
        cell(header, 1, "Valor", st.header);
        int r = 4;
        for (Object[] row : rows) {
            Row x = sheet.createRow(r++);
            cell(x, 0, (String) row[0], st.text);
            number(x, 1, row[1], (CellStyle) row[2]);
        }

        r++;
        Row h2 = sheet.createRow(r++);
        cell(h2, 0, "Pipeline por etapa", st.header);
        cell(h2, 1, "Oportunidades", st.header);
        cell(h2, 2, "Monto", st.header);
        cell(h2, 3, "Ponderado", st.header);
        for (var s : d.pipelineByStage()) {
            Row x = sheet.createRow(r++);
            cell(x, 0, s.label(), st.text);
            number(x, 1, s.count(), st.integer);
            number(x, 2, s.amount(), st.money);
            number(x, 3, s.weighted(), st.money);
        }
        sheet.setColumnWidth(0, 36 * 256);
        for (int c = 1; c <= 3; c++) {
            sheet.setColumnWidth(c, 18 * 256);
        }
    }

    private void monthlySheet(XSSFWorkbook wb, Styles st, DashboardDto d) {
        Sheet sheet = wb.createSheet("Ventas por mes");
        Row h = sheet.createRow(0);
        String[] headers = {"Mes", "Ingresos (USD)", "Negocios ganados"};
        for (int i = 0; i < headers.length; i++) {
            cell(h, i, headers[i], st.header);
        }
        int r = 1;
        for (var m : d.revenueByMonth()) {
            Row x = sheet.createRow(r++);
            cell(x, 0, m.label(), st.text);
            number(x, 1, m.revenue(), st.money);
            number(x, 2, m.dealsWon(), st.integer);
        }
        Row total = sheet.createRow(r);
        cell(total, 0, "Total 12 meses", st.totalLabel);
        Cell sum = total.createCell(1);
        sum.setCellFormula("SUM(B2:B" + r + ")");
        sum.setCellStyle(st.totalMoney);
        Cell count = total.createCell(2);
        count.setCellFormula("SUM(C2:C" + r + ")");
        count.setCellStyle(st.totalInteger);
        sheet.createFreezePane(0, 1);
        sheet.setColumnWidth(0, 18 * 256);
        sheet.setColumnWidth(1, 18 * 256);
        sheet.setColumnWidth(2, 18 * 256);
    }

    private void repsSheet(XSSFWorkbook wb, Styles st, DashboardDto d) {
        Sheet sheet = wb.createSheet("Vendedores");
        String[] headers = {"Vendedor", "Ingresos ganados", "Ingresos del mes", "Meta mensual",
                "Cumplimiento", "Negocios ganados", "Tasa de cierre"};
        Row h = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            cell(h, i, headers[i], st.header);
        }
        int r = 1;
        for (var rep : d.leaderboard()) {
            Row x = sheet.createRow(r++);
            cell(x, 0, rep.name(), st.text);
            number(x, 1, rep.revenueWon(), st.money);
            number(x, 2, rep.revenueThisMonth(), st.money);
            number(x, 3, rep.monthlyQuota(), st.money);
            number(x, 4, rep.quotaAttainment() / 100, st.percent);
            number(x, 5, rep.dealsWon(), st.integer);
            number(x, 6, rep.winRate() / 100, st.percent);
        }
        sheet.createFreezePane(1, 1);
        sheet.setColumnWidth(0, 24 * 256);
        for (int c = 1; c < headers.length; c++) {
            sheet.setColumnWidth(c, 17 * 256);
        }
    }

    private void dealsSheet(XSSFWorkbook wb, Styles st, List<Deal> deals) {
        Sheet sheet = wb.createSheet("Oportunidades");
        String[] headers = {"ID", "Oportunidad", "Cliente", "Industria", "Vendedor", "Etapa", "Monto (USD)",
                "Probabilidad", "Ponderado (USD)", "Cierre esperado", "Creada", "Cerrada"};
        Row h = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            cell(h, i, headers[i], st.header);
        }
        int r = 1;
        for (Deal deal : deals) {
            Row x = sheet.createRow(r++);
            number(x, 0, deal.getId(), st.integer);
            cell(x, 1, deal.getTitle(), st.text);
            cell(x, 2, deal.getCustomer().getCompanyName(), st.text);
            cell(x, 3, deal.getCustomer().getIndustry(), st.text);
            cell(x, 4, deal.getOwner() != null ? deal.getOwner().getName() : "", st.text);
            cell(x, 5, deal.getStage().getLabel(), st.text);
            number(x, 6, deal.getAmount(), st.money);
            number(x, 7, deal.getProbability() / 100.0, st.percent);
            number(x, 8, deal.weightedAmount(), st.money);
            date(x, 9, deal.getExpectedCloseDate() != null ? deal.getExpectedCloseDate().atStartOfDay() : null, st.date);
            date(x, 10, deal.getCreatedAt(), st.date);
            date(x, 11, deal.getClosedAt(), st.date);
        }
        sheet.setAutoFilter(new CellRangeAddress(0, Math.max(0, r - 1), 0, headers.length - 1));
        sheet.createFreezePane(2, 1);
        int[] widths = {7, 34, 28, 16, 20, 14, 15, 13, 16, 15, 13, 13};
        for (int i = 0; i < widths.length; i++) {
            sheet.setColumnWidth(i, widths[i] * 256);
        }
    }

    // ---- utilidades de celdas ----

    private static void cell(Row row, int col, String value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value == null ? "" : value);
        c.setCellStyle(style);
    }

    private static void number(Row row, int col, Object value, CellStyle style) {
        Cell c = row.createCell(col);
        if (value instanceof BigDecimal bd) {
            c.setCellValue(bd.doubleValue());
        } else if (value instanceof Number n) {
            c.setCellValue(n.doubleValue());
        }
        c.setCellStyle(style);
    }

    private static void date(Row row, int col, LocalDateTime value, CellStyle style) {
        Cell c = row.createCell(col);
        if (value != null) {
            c.setCellValue(value);
        }
        c.setCellStyle(style);
    }

    /** Estilos compartidos (POI limita la cantidad de estilos por libro, por eso se crean una sola vez). */
    private static final class Styles {
        final CellStyle title, muted, header, text, money, percent, decimal, integer, date,
                totalLabel, totalMoney, totalInteger;

        Styles(XSSFWorkbook wb) {
            DataFormat fmt = wb.createDataFormat();

            XSSFFont titleFont = wb.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            title = wb.createCellStyle();
            title.setFont(titleFont);

            XSSFFont mutedFont = wb.createFont();
            mutedFont.setItalic(true);
            mutedFont.setColor(new XSSFColor(new byte[]{(byte) 0x6B, (byte) 0x76, (byte) 0x7A}, null));
            muted = wb.createCellStyle();
            muted.setFont(mutedFont);

            XSSFFont headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setColor(new XSSFColor(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF}, null));
            var h = wb.createCellStyle();
            h.setFont(headerFont);
            h.setFillForegroundColor(new XSSFColor(INK, null));
            h.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            h.setVerticalAlignment(VerticalAlignment.CENTER);
            header = h;

            text = wb.createCellStyle();
            money = withFormat(wb, fmt, "\"$\"#,##0.00");
            percent = withFormat(wb, fmt, "0.0%");
            decimal = withFormat(wb, fmt, "0.0");
            integer = withFormat(wb, fmt, "#,##0");
            date = withFormat(wb, fmt, "dd/mm/yyyy");

            XSSFFont bold = wb.createFont();
            bold.setBold(true);
            totalLabel = totalStyle(wb, bold, null, fmt);
            totalMoney = totalStyle(wb, bold, "\"$\"#,##0.00", fmt);
            totalInteger = totalStyle(wb, bold, "#,##0", fmt);
        }

        private static CellStyle withFormat(Workbook wb, DataFormat fmt, String pattern) {
            CellStyle s = wb.createCellStyle();
            s.setDataFormat(fmt.getFormat(pattern));
            return s;
        }

        private static CellStyle totalStyle(XSSFWorkbook wb, XSSFFont font, String pattern, DataFormat fmt) {
            var s = wb.createCellStyle();
            s.setFont(font);
            s.setFillForegroundColor(new XSSFColor(EMERALD_SOFT, null));
            s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            s.setBorderTop(BorderStyle.THIN);
            if (pattern != null) {
                s.setDataFormat(fmt.getFormat(pattern));
            }
            return s;
        }
    }
}
