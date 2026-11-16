import { api, state, esc, icon, dateTime, formModal, field, options, toast, confirmDialog, download, DEMO } from "../core.js";

export async function renderIntegrations(root) {
  const [rates, hooks, deliveries] = await Promise.all([
    api("/api/integrations/exchange-rates"),
    api("/api/integrations/webhooks"),
    api("/api/integrations/webhooks/deliveries"),
  ]);
  state.rates = rates.rates;

  root.innerHTML = `
    <header class="page-head">
      <div>
        <h1>Integraciones</h1>
        <p>Conecta el CRM con otras herramientas: hojas de cálculo, sistemas de mensajería y tu propio software.</p>
      </div>
    </header>

    <div class="int-grid">
      <section class="panel int-card">
        <span class="int-icon">${icon("api")}</span>
        <h2>API REST</h2>
        <p>Todos los datos del CRM están disponibles por HTTP con documentación OpenAPI interactiva para probar cada endpoint.</p>
        <div class="end">
          ${DEMO ? `<span class="pill warn">Disponible al ejecutar el backend</span>`
            : `<a class="btn btn-sm" href="swagger-ui.html" target="_blank" rel="noopener">Abrir Swagger UI</a>
               <a class="btn btn-sm btn-ghost" href="v3/api-docs" target="_blank" rel="noopener">OpenAPI JSON</a>`}
        </div>
      </section>

      <section class="panel int-card">
        <span class="int-icon">${icon("file")}</span>
        <h2>Archivos CSV</h2>
        <p>Importa contactos desde Excel, Google Sheets u otro CRM, y exporta clientes u oportunidades para analizarlos donde quieras.</p>
        <div class="end">
          <button class="btn btn-sm" data-dl="/api/integrations/csv/customers">${icon("download")} Clientes</button>
          <button class="btn btn-sm" data-dl="/api/integrations/csv/deals">${icon("download")} Oportunidades</button>
          <a class="btn btn-sm btn-ghost" href="#/clientes">Importar</a>
        </div>
      </section>

      <section class="panel int-card">
        <span class="int-icon">${icon("sheet")}</span>
        <h2>Reporte en Excel</h2>
        <p>Libro con formato listo para gerencia: resumen de KPIs, ventas por mes, desempeño por vendedor y detalle con filtros.</p>
        <div class="end">
          <button class="btn btn-sm" data-dl="/api/integrations/reports/sales.xlsx">${icon("download")} Descargar reporte</button>
        </div>
      </section>
    </div>

    <div class="grid-dash">
      <section class="panel span-4">
        <div class="panel-head">
          <h2>Tasas de cambio</h2>
          <span class="end pill ${rates.source === "live" ? "ok" : "warn"}">${{ live: "En vivo", demo: "Valores de referencia" }[rates.source] || "Tasas de respaldo"}</span>
        </div>
        <p class="hint" style="margin:0 0 12px;font-size:var(--t-sm);color:var(--muted)">
          Cuánto vale un dólar en cada moneda. Fuente: open.er-api.com, con caché de 6 horas.
          ${{ live: "", demo: "En la demo se usan valores de referencia; con el backend Java se consultan en vivo." }[rates.source]
            ?? "La API externa no respondió, así que se usan valores de referencia."}
        </p>
        <div class="rates">${Object.entries(rates.rates).filter(([c]) => c !== "USD")
          .map(([c, v]) => `<div><span>${c}</span><b>${Number(v).toLocaleString("es-CO", { maximumFractionDigits: 2 })}</b></div>`).join("")}</div>
        <p style="margin:12px 0 0;font-size:var(--t-xs);color:var(--muted)">Actualizado ${esc(dateTime(rates.fetchedAt))}. El panel usa estas tasas para mostrar montos en tu moneda.</p>
      </section>

      <section class="panel span-8">
        <div class="panel-head">
          <h2>Webhooks</h2>
          <span class="hint">Avisa a otros sistemas cuando algo pasa en el CRM</span>
          <button class="end btn btn-sm btn-primary" id="new-hook">${icon("plus")} Nuevo webhook</button>
        </div>
        <ul class="hooks">${hooks.map(hook).join("") || `<li><span class="hint">Sin webhooks configurados.</span></li>`}</ul>
        <p style="margin:12px 0 0;font-size:var(--t-sm);color:var(--muted)">
          Cada envío es un POST JSON firmado con HMAC-SHA256 en la cabecera <code>X-NexoCRM-Signature</code>.
          Se reintenta hasta 3 veces si el destino falla.
        </p>
      </section>

      <section class="panel span-12">
        <div class="panel-head">
          <h2>Historial de envíos</h2>
          <span class="hint">Mueve una oportunidad a Ganado en el pipeline para ver un envío nuevo</span>
          <button class="end btn btn-sm btn-ghost" id="reload-log">${icon("refresh")} Actualizar</button>
        </div>
        <div id="delivery-log">${log(deliveries)}</div>
      </section>
    </div>`;

  root.querySelectorAll("[data-dl]").forEach((b) => b.addEventListener("click", () => download(b.dataset.dl, "archivo")));
  root.querySelector("#new-hook").addEventListener("click", () => hookForm(() => renderIntegrations(root)));
  root.querySelector("#reload-log").addEventListener("click", async () => {
    root.querySelector("#delivery-log").innerHTML = log(await api("/api/integrations/webhooks/deliveries"));
    wireLog(root);
  });
  wireLog(root);

  root.querySelectorAll(".hooks li[data-id]").forEach((li) => {
    const id = li.dataset.id;
    li.querySelector("[data-test]").addEventListener("click", async (e) => {
      const btn = e.currentTarget;
      btn.disabled = true;
      try {
        const d = await api(`/api/integrations/webhooks/${id}/test`, { method: "POST" });
        toast(d.success ? `Entregado (HTTP ${d.statusCode}) en ${d.durationMs} ms` : `Falló: ${d.errorMessage}`, d.success ? "ok" : "error");
        root.querySelector("#delivery-log").innerHTML = log(await api("/api/integrations/webhooks/deliveries"));
        wireLog(root);
      } finally {
        btn.disabled = false;
      }
    });
    li.querySelector("[data-toggle]").addEventListener("click", async () => {
      await api(`/api/integrations/webhooks/${id}/toggle`, { method: "PATCH" });
      renderIntegrations(root);
    });
    li.querySelector("[data-delete]").addEventListener("click", async () => {
      if (!(await confirmDialog("Se eliminará el webhook y su historial de envíos."))) return;
      await api(`/api/integrations/webhooks/${id}`, { method: "DELETE" });
      toast("Webhook eliminado");
      renderIntegrations(root);
    });
  });
}

function hook(h) {
  return `<li data-id="${h.id}">
    <div class="name">${esc(h.name)}
      <span class="pill">${esc(h.eventLabel)}</span>
      <span class="pill ${h.active ? "ok" : "off"}">${h.active ? "Activo" : "Pausado"}</span>
    </div>
    <div class="url" title="${esc(h.targetUrl)}">${esc(h.targetUrl)}</div>
    <div class="ops">
      <button class="btn btn-sm" data-test>${icon("send")} Probar</button>
      <button class="icon-btn" data-toggle aria-label="${h.active ? "Pausar" : "Activar"}" title="${h.active ? "Pausar" : "Activar"}">${icon("power")}</button>
      <button class="icon-btn" data-delete aria-label="Eliminar webhook">${icon("trash")}</button>
    </div>
  </li>`;
}

function log(list) {
  if (!list.length) return `<div class="empty">Aún no hay envíos. Usa el botón Probar de un webhook.</div>`;
  return `<div style="overflow-x:auto"><table class="log">
    <thead><tr><th>Cuándo</th><th>Webhook</th><th>Evento</th><th>Resultado</th><th>Intentos</th><th>Duración</th><th></th></tr></thead>
    <tbody>${list.slice(0, 12).map((d) => `<tr>
      <td>${esc(dateTime(d.deliveredAt))}</td>
      <td>${esc(d.subscriptionName)}</td>
      <td>${esc(d.eventLabel)}</td>
      <td><span class="pill ${d.success ? "ok" : "bad"}">${d.success ? `HTTP ${d.statusCode}` : esc(d.statusCode ? `HTTP ${d.statusCode}` : "Sin conexión")}</span></td>
      <td>${d.attempts}</td>
      <td>${d.durationMs} ms</td>
      <td><button class="btn btn-sm btn-ghost" data-payload="${d.id}">Ver JSON</button></td>
    </tr>`).join("")}</tbody></table></div>`;
}

function wireLog(root) {
  root.querySelectorAll("[data-payload]").forEach((b) => b.addEventListener("click", async () => {
    const list = await api("/api/integrations/webhooks/deliveries");
    const d = list.find((x) => String(x.id) === b.dataset.payload);
    let pretty = d.payload;
    try { pretty = JSON.stringify(JSON.parse(d.payload), null, 2); } catch { /* se muestra tal cual */ }
    formModal({
      title: `Envío de ${d.eventLabel.toLowerCase()}`,
      submitLabel: "Cerrar",
      body: `<p style="margin:0;color:var(--muted);font-size:var(--t-sm)">${esc(d.subscriptionName)}${d.errorMessage ? `. Error: ${esc(d.errorMessage)}` : ""}</p>
             <pre class="payload">${esc(pretty)}</pre>`,
      onSubmit: async () => {},
    });
  }));
}

function hookForm(done) {
  formModal({
    title: "Nuevo webhook",
    submitLabel: "Crear webhook",
    body: `
      ${field("Nombre", "name", `<input class="input" name="name" placeholder="Ej. Canal #ventas en Slack" required>`)}
      ${field("URL de destino", "targetUrl", `<input class="input" name="targetUrl" type="url" placeholder="https://hooks.zapier.com/..." required>`)}
      ${field("Cuándo enviarlo", "event", `<select class="select" name="event">${options(state.meta.webhookEvents, "DEAL_WON")}</select>`)}
      <p style="margin:0;font-size:var(--t-sm);color:var(--muted)">Para probar sin servicios externos usa <code>${esc(location.origin)}/api/webhooks/receiver</code> o una URL de webhook.site.</p>`,
    onSubmit: async (data) => {
      const created = await api("/api/integrations/webhooks", { method: "POST", body: data });
      done();
      formModal({
        title: "Webhook creado",
        submitLabel: "Entendido",
        body: `<p style="margin:0">Guarda este secreto en el sistema que recibe los avisos para verificar la firma. No se volverá a mostrar completo.</p>
               <pre class="payload">${esc(created.secret)}</pre>`,
        onSubmit: async () => {},
      });
    },
  });
}
