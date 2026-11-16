import { api, state, setCurrency, money, pct, num, esc, avatar, icon, relativeDay, download, cssVar, DEMO } from "../core.js";

let charts = [];

export async function renderDashboard(root) {
  const [d, pending] = await Promise.all([
    api("/api/analytics/dashboard"),
    api("/api/activities?completed=false"),
  ]);
  charts.forEach((c) => c.destroy());
  charts = [];

  const k = d.kpis;
  const currencies = Object.keys(state.rates).filter((c) => ["USD", "COP", "MXN", "EUR"].includes(c));
  const growth = k.monthOverMonthGrowth;
  const monthName = new Date().toLocaleDateString("es-CO", { month: "long" });
  const prevMonthName = new Date(new Date().getFullYear(), new Date().getMonth() - 1, 1).toLocaleDateString("es-CO", { month: "long" });

  root.innerHTML = `
    <header class="page-head">
      <div>
        <h1>Panel comercial</h1>
        <p>Cómo va ${esc(monthName)}: ingresos cerrados, salud del pipeline y lo que viene.</p>
      </div>
      <div class="actions">
        <div class="segmented" role="group" aria-label="Moneda">
          ${currencies.map((c) => `<button type="button" data-cur="${c}" aria-pressed="${c === state.currency}">${c}</button>`).join("")}
        </div>
        <button class="btn" id="dl-excel">${icon("sheet")} Descargar Excel</button>
      </div>
    </header>

    <section class="kpis" aria-label="Indicadores clave">
      <div class="kpi">
        <div class="kpi-label">Ingresos de ${esc(monthName)}</div>
        <div class="kpi-value">${money(k.revenueThisMonth)}</div>
        <div class="kpi-foot">${growth == null ? "Sin datos del mes anterior"
          : `<span class="trend ${growth >= 0 ? "up" : "down"}">${growth >= 0 ? "▲" : "▼"} ${pct(Math.abs(growth))}</span> frente a ${money(k.revenueLastMonth, { compact: true })} al mismo día de ${esc(prevMonthName)}`}</div>
      </div>
      ${kpi("Pipeline abierto", money(k.openPipeline, { compact: true }), `${num(k.openDeals)} oportunidades`)}
      ${kpi("Pronóstico ponderado", money(k.weightedPipeline, { compact: true }), "Monto × probabilidad")}
      ${kpi("Tasa de cierre", pct(k.winRate), "Ganadas sobre cerradas")}
      ${kpi("Ticket promedio", money(k.averageDealSize, { compact: true }), `${num(k.activeCustomers)} clientes activos`)}
      ${kpi("Ciclo de venta", `${num(Math.round(k.averageSalesCycleDays))} días`, "De creada a ganada")}
    </section>

    <section class="panel river" aria-labelledby="river-title">
      <div class="panel-head">
        <h2 id="river-title">Pipeline por etapa</h2>
        <span class="hint">El ancho de cada tramo es proporcional al monto en juego</span>
        <a class="end btn btn-sm btn-ghost" href="#/pipeline">Abrir tablero</a>
      </div>
      ${river(d.pipelineByStage, k)}
    </section>

    <div class="grid-dash">
      <section class="panel span-8">
        <div class="panel-head"><h2>Ingresos ganados por mes</h2><span class="hint">Últimos 12 meses</span></div>
        <div class="chart-box"><canvas id="revenue-chart" aria-label="Gráfico de ingresos ganados por mes"></canvas></div>
      </section>

      <section class="panel span-4">
        <div class="panel-head"><h2>Pronóstico de cierre</h2></div>
        <div class="forecast-list">${forecast(d.forecast)}</div>
      </section>

      <section class="panel span-6">
        <div class="panel-head"><h2>Equipo comercial</h2><span class="hint">Cumplimiento de la meta del mes</span></div>
        <table class="leaders">${leaders(d.leaderboard)}</table>
      </section>

      <section class="panel span-3">
        <div class="panel-head"><h2>Ventas por industria</h2></div>
        <div class="bars">${bars(d.wonByIndustry.slice(0, 7), (b) => b.amount, (b) => money(b.amount, { compact: true }))}</div>
      </section>

      <section class="panel span-3">
        <div class="panel-head"><h2>Conversión por canal</h2></div>
        <div class="bars">${bars(
          [...d.conversionBySource].sort((a, b) => b.conversionRate - a.conversionRate),
          (s) => s.conversionRate, (s) => `${pct(s.conversionRate, 0)} <small style="color:var(--muted)">(${s.won}/${s.total})</small>`, 100)}</div>
      </section>

      <section class="panel span-12">
        <div class="panel-head">
          <h2>Agenda</h2><span class="hint">Lo que toca hacer para mover el pipeline</span>
          <a class="end btn btn-sm btn-ghost" href="#/actividades">Ver todas</a>
        </div>
        ${agenda(pending.slice(0, 6))}
      </section>
    </div>`;

  root.querySelectorAll("[data-cur]").forEach((b) => b.addEventListener("click", () => {
    setCurrency(b.dataset.cur);
    renderDashboard(root);
  }));
  root.querySelector("#dl-excel").addEventListener("click", () => download("/api/integrations/reports/sales.xlsx", "reporte-ventas.xlsx"));
  if (DEMO) root.querySelector("#dl-excel").title = "Disponible al ejecutar el backend Java";

  drawRevenue(root.querySelector("#revenue-chart"), d.revenueByMonth);
}

function kpi(label, value, foot) {
  return `<div class="kpi"><div class="kpi-label">${esc(label)}</div><div class="kpi-value">${value}</div><div class="kpi-foot">${esc(foot)}</div></div>`;
}

function river(stages, k) {
  const total = stages.reduce((s, x) => s + Number(x.amount), 0) || 1;
  const segs = stages.map((s) => {
    const share = Number(s.amount) / total;
    return `<a class="river-seg" data-stage="${s.stage}" href="#/pipeline" style="flex:${Math.max(share, 0.07)}"
              aria-label="${esc(s.label)}: ${s.count} oportunidades por ${money(s.amount)}">
      <span class="name">${esc(s.label)}</span>
      <span class="amount">${money(s.amount, { compact: true })}</span>
      <span class="meta">${s.count} oportunidades, ${Math.round(share * 100)} %</span>
    </a>`;
  }).join("");
  return `<div class="river-band">${segs}</div>
    <div class="river-scale">
      <span>En juego <strong>${money(k.openPipeline)}</strong></span>
      <span>Esperado según probabilidad <strong>${money(k.weightedPipeline)}</strong></span>
    </div>`;
}

function forecast(months) {
  const max = Math.max(...months.map((m) => Number(m.bestCase)), 1);
  return months.map((m, i) => `
    <div class="forecast-row">
      <div class="top"><span class="month">${esc(m.label)}${i === 0 ? " (incluye vencidas)" : ""}</span><span class="value">${money(m.weighted, { compact: true })}</span></div>
      <div class="meter" aria-hidden="true">
        <i class="best" style="width:${(Number(m.bestCase) / max) * 100}%"></i>
        <i style="width:${(Number(m.weighted) / max) * 100}%"></i>
      </div>
      <div class="sub">Si todo se cierra: ${money(m.bestCase, { compact: true })}</div>
    </div>`).join("");
}

function leaders(list) {
  return list.map((r, i) => {
    const att = Number(r.quotaAttainment);
    return `<tr>
      <td class="rank">${i + 1}</td>
      <td><div class="who">${avatar(r.initials, r.id)}<span>${esc(r.name)}</span></div></td>
      <td class="num">${money(r.revenueThisMonth, { compact: true })}<br><small style="color:var(--muted)">${r.dealsWon} ganados, cierre ${pct(r.winRate, 0)}</small></td>
      <td class="quota">
        <div class="meter ${att >= 100 ? "over" : ""}" role="img" aria-label="${pct(att, 0)} de la meta"><i style="width:${Math.min(att, 100)}%"></i></div>
        <small>${pct(att, 0)} de ${money(r.monthlyQuota, { compact: true })}</small>
      </td>
    </tr>`;
  }).join("");
}

function bars(list, valueOf, labelOf, fixedMax) {
  if (!list.length) return `<p class="hint">Aún no hay datos.</p>`;
  const max = fixedMax ?? Math.max(...list.map((x) => Number(valueOf(x))), 1);
  return list.map((x) => `
    <div class="row"><span>${esc(x.label)}</span><b>${labelOf(x)}</b>
      <div class="meter" aria-hidden="true"><i style="width:${(Number(valueOf(x)) / max) * 100}%"></i></div>
    </div>`).join("");
}

function agenda(items) {
  if (!items.length) return `<div class="empty">No hay actividades pendientes. Agenda la siguiente llamada desde Actividades.</div>`;
  return `<ul class="agenda">${items.map((a) => `
    <li>
      <span class="when ${a.overdue ? "late" : ""}">${relativeDay(a.dueAt)}</span>
      <span class="type-icon" data-type="${a.type}" title="${esc(a.typeLabel)}">${icon(a.type)}</span>
      <span class="what"><div>${esc(a.subject)}</div><small>${esc(a.dealTitle || a.customerName || "")}</small></span>
      <span>${a.ownerName ? `<small style="color:var(--muted)">${esc(a.ownerName)}</small>` : ""}</span>
    </li>`).join("")}</ul>`;
}

function drawRevenue(canvas, months) {
  const emerald = cssVar("--emerald");
  const ink = cssVar("--ink");
  const muted = cssVar("--muted");
  const line = cssVar("--line");
  Chart.defaults.font.family = cssVar("--font-ui");
  Chart.defaults.color = muted;

  const last = months.length - 1;
  charts.push(new Chart(canvas, {
    data: {
      labels: months.map((m) => m.label),
      datasets: [
        {
          type: "bar",
          label: "Ingresos",
          data: months.map((m) => Number(m.revenue) * Number(state.rates[state.currency] ?? 1)),
          backgroundColor: months.map((_, i) => (i === last ? cssVar("--marigold") : emerald)),
          borderRadius: 5,
          maxBarThickness: 34,
          yAxisID: "y",
          order: 2,
        },
        {
          type: "line",
          label: "Negocios ganados",
          data: months.map((m) => m.dealsWon),
          borderColor: ink,
          backgroundColor: ink,
          borderWidth: 1.5,
          pointRadius: 2.5,
          tension: 0.35,
          yAxisID: "y2",
          order: 1,
        },
      ],
    },
    options: {
      maintainAspectRatio: false,
      interaction: { mode: "index", intersect: false },
      plugins: {
        legend: { position: "bottom", labels: { boxWidth: 10, boxHeight: 10, useBorderRadius: true, borderRadius: 2 } },
        tooltip: {
          callbacks: {
            label: (ctx) => ctx.dataset.yAxisID === "y"
              ? ` ${money(ctx.raw / Number(state.rates[state.currency] ?? 1))}`
              : ` ${ctx.raw} negocios ganados`,
            afterBody: (items) => (items[0].dataIndex === last ? "Mes en curso" : ""),
          },
        },
      },
      scales: {
        x: { grid: { display: false } },
        y: {
          grid: { color: line },
          border: { display: false },
          ticks: { callback: (v) => money(v / Number(state.rates[state.currency] ?? 1), { compact: true }) },
        },
        y2: { position: "right", grid: { display: false }, border: { display: false }, beginAtZero: true, ticks: { precision: 0 } },
      },
    },
  }));
}
