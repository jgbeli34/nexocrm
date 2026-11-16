import { api, state, money, esc, icon, date, formModal, field, options, toast, confirmDialog, drawer, download, relativeDay, DEMO } from "../core.js";
import { dealForm } from "./pipeline.js";

const query = { q: "", status: "", page: 0, size: 12, sort: "name" };

export async function renderCustomers(root) {
  root.innerHTML = `
    <header class="page-head">
      <div>
        <h1>Clientes</h1>
        <p>Empresas con las que hablamos, su contacto principal y quién las atiende.</p>
      </div>
      <div class="actions">
        <label class="btn" tabindex="0">${icon("upload")} Importar CSV
          <input type="file" id="csv-file" accept=".csv,text/csv" hidden></label>
        <button class="btn" id="csv-export">${icon("download")} Exportar CSV</button>
        <button class="btn btn-primary" id="new-customer">${icon("plus")} Nuevo cliente</button>
      </div>
    </header>
    <div class="toolbar">
      <label class="search"><span class="sr-only">Buscar clientes</span>${icon("search")}
        <input class="input" id="cust-q" type="search" placeholder="Buscar por empresa, contacto, correo o ciudad" value="${esc(query.q)}"></label>
      <div class="segmented" role="group" aria-label="Filtrar por estado">
        ${[{ value: "", label: "Todos" }, ...state.meta.customerStatuses]
          .map((s) => `<button type="button" data-status="${s.value}" aria-pressed="${s.value === query.status}">${esc(s.label)}</button>`).join("")}
      </div>
      <select class="select" id="cust-sort" style="width:auto;margin-left:auto" aria-label="Ordenar">
        ${options([{ value: "name", label: "Orden alfabético" }, { value: "recent", label: "Más recientes" }, { value: "city", label: "Por ciudad" }], query.sort)}
      </select>
    </div>
    <div id="cust-table"></div>`;

  let timer;
  root.querySelector("#cust-q").addEventListener("input", (e) => {
    clearTimeout(timer);
    timer = setTimeout(() => { query.q = e.target.value; query.page = 0; load(root); }, 250);
  });
  root.querySelectorAll("[data-status]").forEach((b) => b.addEventListener("click", () => {
    query.status = b.dataset.status;
    query.page = 0;
    root.querySelectorAll("[data-status]").forEach((x) => x.setAttribute("aria-pressed", String(x === b)));
    load(root);
  }));
  root.querySelector("#cust-sort").addEventListener("change", (e) => { query.sort = e.target.value; load(root); });
  root.querySelector("#new-customer").addEventListener("click", () => customerForm(null, () => load(root)));
  root.querySelector("#csv-export").addEventListener("click", () => download("/api/integrations/csv/customers", "clientes.csv"));
  root.querySelector("#csv-file").addEventListener("change", (e) => importCsv(e.target, root));

  await load(root);
}

async function load(root) {
  const params = new URLSearchParams({ page: query.page, size: query.size, sort: query.sort });
  if (query.q) params.set("q", query.q);
  if (query.status) params.set("status", query.status);
  const page = await api(`/api/customers?${params}`);
  const box = root.querySelector("#cust-table");

  if (!page.content.length) {
    box.innerHTML = `<div class="table-wrap"><div class="empty">
      <strong>Ningún cliente coincide con la búsqueda</strong>
      <span>Prueba con otro término o crea el cliente ahora.</span></div></div>`;
    return;
  }

  box.innerHTML = `
    <div class="table-wrap">
      <table class="table">
        <thead><tr>
          <th scope="col">Empresa</th><th scope="col">Contacto</th><th scope="col">Ubicación</th>
          <th scope="col">Industria</th><th scope="col">Estado</th><th scope="col">Responsable</th><th scope="col"><span class="sr-only">Acciones</span></th>
        </tr></thead>
        <tbody>${page.content.map(row).join("")}</tbody>
      </table>
    </div>
    <div class="pager">
      <span>${page.page * page.size + 1}–${page.page * page.size + page.content.length} de ${page.totalElements}</span>
      <button class="btn btn-sm" data-page="-1" ${page.page === 0 ? "disabled" : ""}>Anterior</button>
      <button class="btn btn-sm" data-page="1" ${page.page + 1 >= page.totalPages ? "disabled" : ""}>Siguiente</button>
    </div>`;

  box.querySelectorAll("[data-page]").forEach((b) => b.addEventListener("click", () => {
    query.page += Number(b.dataset.page);
    load(root);
  }));
  box.querySelectorAll("tbody tr").forEach((tr) => {
    const customer = page.content.find((c) => String(c.id) === tr.dataset.id);
    tr.addEventListener("click", (e) => {
      if (e.target.closest("button")) return;
      showCustomer(customer, root);
    });
    tr.querySelector("[data-edit]").addEventListener("click", () => customerForm(customer, () => load(root)));
    tr.querySelector("[data-delete]").addEventListener("click", async () => {
      if (!(await confirmDialog(`Se eliminará ${customer.companyName} y sus actividades.`))) return;
      try {
        await api(`/api/customers/${customer.id}`, { method: "DELETE" });
        toast("Cliente eliminado");
        load(root);
      } catch (err) {
        toast(err.message, "error");
      }
    });
  });
}

function row(c) {
  return `<tr data-id="${c.id}">
    <td><div class="company">${esc(c.companyName)}</div><div class="sub">${esc(c.sourceLabel)}</div></td>
    <td><div>${esc(c.contactName)}</div><div class="sub">${esc(c.email)}</div></td>
    <td>${esc([c.city, c.country].filter(Boolean).join(", "))}</td>
    <td>${esc(c.industry || "")}</td>
    <td><span class="status" data-status="${c.status}">${esc(c.statusLabel)}</span></td>
    <td>${esc(c.ownerName || "Sin asignar")}</td>
    <td class="actions-cell">
      <button class="icon-btn" data-edit aria-label="Editar ${esc(c.companyName)}">${icon("edit")}</button>
      <button class="icon-btn" data-delete aria-label="Eliminar ${esc(c.companyName)}">${icon("trash")}</button>
    </td>
  </tr>`;
}

async function showCustomer(c, root) {
  const [deals, activities] = await Promise.all([
    api(`/api/deals?customerId=${c.id}`),
    api(`/api/activities?customerId=${c.id}`),
  ]);
  const won = deals.filter((d) => d.stage === "WON").reduce((s, d) => s + Number(d.amount), 0);
  const open = deals.filter((d) => !["WON", "LOST"].includes(d.stage));
  const { overlay, close } = drawer(`
    <div class="modal-head">
      <div><h2>${esc(c.companyName)}</h2><span class="status" data-status="${c.status}">${esc(c.statusLabel)}</span></div>
      <button class="icon-btn" data-close aria-label="Cerrar">${icon("x")}</button>
    </div>
    <section style="padding-top:16px">
      <dl>
        <dt>Contacto</dt><dd>${esc(c.contactName)}</dd>
        <dt>Correo</dt><dd><a href="mailto:${esc(c.email)}">${esc(c.email)}</a></dd>
        <dt>Teléfono</dt><dd>${esc(c.phone || "Sin teléfono")}</dd>
        <dt>Ubicación</dt><dd>${esc([c.city, c.country].filter(Boolean).join(", ") || "Sin ubicación")}</dd>
        <dt>Industria</dt><dd>${esc(c.industry || "Sin industria")}</dd>
        <dt>Canal</dt><dd>${esc(c.sourceLabel)}</dd>
        <dt>Responsable</dt><dd>${esc(c.ownerName || "Sin asignar")}</dd>
        <dt>Cliente desde</dt><dd>${date(c.createdAt, { day: "numeric", month: "long", year: "numeric" })}</dd>
        <dt>Total comprado</dt><dd><strong>${money(won)}</strong></dd>
      </dl>
      ${c.notes ? `<p style="color:var(--muted);font-size:var(--t-sm)">${esc(c.notes)}</p>` : ""}
    </section>
    <section>
      <div style="display:flex;align-items:center;justify-content:space-between">
        <h3>Oportunidades (${deals.length})</h3>
        <button class="btn btn-sm" id="drawer-new-deal">${icon("plus")} Oportunidad</button>
      </div>
      ${deals.length ? `<ul class="mini-list">${deals.map((d) => `
        <li><span>${esc(d.title)}<br><small style="color:var(--muted)">${esc(d.stageLabel)}</small></span><strong>${money(d.amount)}</strong></li>`).join("")}</ul>`
        : `<p style="color:var(--muted);font-size:var(--t-sm)">Sin oportunidades todavía.</p>`}
      ${open.length ? `<p style="font-size:var(--t-sm);color:var(--muted)">${open.length} abierta(s) por ${money(open.reduce((s, d) => s + Number(d.amount), 0))}</p>` : ""}
    </section>
    <section>
      <h3>Actividades (${activities.length})</h3>
      ${activities.length ? `<ul class="mini-list">${activities.slice(0, 8).map((a) => `
        <li><span>${esc(a.subject)}<br><small style="color:var(--muted)">${esc(a.typeLabel)}${a.completed ? ", hecha" : ""}</small></span>
            <small style="color:${a.overdue ? "var(--brick)" : "var(--muted)"}">${relativeDay(a.dueAt)}</small></li>`).join("")}</ul>`
        : `<p style="color:var(--muted);font-size:var(--t-sm)">Sin actividades registradas.</p>`}
    </section>`);
  overlay.querySelector("#drawer-new-deal").addEventListener("click", () => {
    close();
    dealForm({ customerId: c.id }, () => load(root));
  });
}

function customerForm(c, done) {
  const editing = Boolean(c);
  formModal({
    title: editing ? `Editar ${c.companyName}` : "Nuevo cliente",
    submitLabel: editing ? "Guardar cambios" : "Crear cliente",
    body: `
      ${field("Empresa", "companyName", `<input class="input" name="companyName" value="${esc(c?.companyName)}" required>`)}
      <div class="two">
        ${field("Contacto", "contactName", `<input class="input" name="contactName" value="${esc(c?.contactName)}" required>`)}
        ${field("Correo", "email", `<input class="input" name="email" type="email" value="${esc(c?.email)}" required>`)}
      </div>
      <div class="two">
        ${field("Teléfono", "phone", `<input class="input" name="phone" value="${esc(c?.phone)}">`)}
        ${field("Industria", "industry", `<input class="input" name="industry" value="${esc(c?.industry)}" list="industries">`)}
      </div>
      <datalist id="industries">${["Agroindustria", "Construcción", "Educación", "Energía", "Finanzas", "Logística", "Manufactura", "Retail", "Salud", "Tecnología", "Turismo"].map((i) => `<option value="${i}">`).join("")}</datalist>
      <div class="two">
        ${field("Ciudad", "city", `<input class="input" name="city" value="${esc(c?.city)}">`)}
        ${field("País", "country", `<input class="input" name="country" value="${esc(c?.country ?? "Colombia")}">`)}
      </div>
      <div class="two">
        ${field("Estado", "status", `<select class="select" name="status">${options(state.meta.customerStatuses, c?.status || "LEAD")}</select>`)}
        ${field("Canal de origen", "source", `<select class="select" name="source">${options(state.meta.leadSources, c?.source || "WEBSITE")}</select>`)}
      </div>
      ${field("Responsable", "ownerId", `<select class="select" name="ownerId">${options(state.meta.salesReps, c?.ownerId, { empty: "Sin asignar" })}</select>`)}
      ${field("Notas", "notes", `<textarea class="textarea" name="notes">${esc(c?.notes)}</textarea>`)}`,
    onSubmit: async (data) => {
      const body = { ...data, ownerId: Number(data.ownerId) || null };
      await api(editing ? `/api/customers/${c.id}` : "/api/customers", { method: editing ? "PUT" : "POST", body });
      toast(editing ? "Cambios guardados" : "Cliente creado");
      done();
    },
  });
}

async function importCsv(input, root) {
  const file = input.files[0];
  input.value = "";
  if (!file) return;
  const form = new FormData();
  form.append("file", file);
  try {
    const result = DEMO
      ? await api("/api/integrations/csv/customers", { method: "POST", body: await file.text() })
      : await api("/api/integrations/csv/customers", { method: "POST", body: form });
    const errors = result.errors.slice(0, 6).map((e) => `<li>Fila ${e.row}: ${esc(e.message)}</li>`).join("");
    formModal({
      title: "Importación terminada",
      submitLabel: "Listo",
      body: `<p style="margin:0">Se importaron <strong>${result.imported}</strong> de ${result.totalRows} filas.</p>
        ${result.errors.length ? `<div><strong>${result.skipped} filas omitidas</strong><ul style="margin:6px 0 0;padding-left:18px;color:var(--muted);font-size:var(--t-sm)">${errors}</ul></div>` : ""}
        <p style="margin:0;color:var(--muted);font-size:var(--t-sm)">Columnas esperadas: empresa, contacto, email, telefono, industria, ciudad, pais, estado.</p>`,
      onSubmit: async () => {},
    });
    load(root);
  } catch (err) {
    toast(err.message, "error");
  }
}
