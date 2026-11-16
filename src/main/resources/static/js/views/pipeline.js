import { api, state, money, esc, avatar, icon, date, formModal, field, options, toast, confirmDialog, STAGE_COLORS } from "../core.js";

const OPEN = ["LEAD", "QUALIFIED", "PROPOSAL", "NEGOTIATION"];
const filters = { q: "", ownerId: "" };

export async function renderPipeline(root) {
  root.innerHTML = `
    <header class="page-head">
      <div>
        <h1>Pipeline</h1>
        <p>Arrastra una oportunidad a otra columna para cambiar su etapa. Ganar o perder dispara las integraciones configuradas.</p>
      </div>
      <div class="actions">
        <button class="btn btn-primary" id="new-deal">${icon("plus")} Nueva oportunidad</button>
      </div>
    </header>
    <div class="toolbar">
      <label class="search"><span class="sr-only">Buscar oportunidades</span>${icon("search")}
        <input class="input" id="deal-q" type="search" placeholder="Buscar por oportunidad o cliente" value="${esc(filters.q)}"></label>
      <select class="select" id="deal-owner" style="width:auto" aria-label="Filtrar por vendedor">
        ${options(state.meta.salesReps, filters.ownerId, { empty: "Todo el equipo" })}
      </select>
    </div>
    <div class="board" id="board"></div>`;

  let timer;
  root.querySelector("#deal-q").addEventListener("input", (e) => {
    clearTimeout(timer);
    timer = setTimeout(() => { filters.q = e.target.value; loadBoard(root); }, 250);
  });
  root.querySelector("#deal-owner").addEventListener("change", (e) => { filters.ownerId = e.target.value; loadBoard(root); });
  root.querySelector("#new-deal").addEventListener("click", () => dealForm(null, () => loadBoard(root)));

  await loadBoard(root);
}

async function loadBoard(root) {
  const params = new URLSearchParams();
  if (filters.q) params.set("q", filters.q);
  if (filters.ownerId) params.set("ownerId", filters.ownerId);
  const deals = await api(`/api/deals?${params}`);
  const board = root.querySelector("#board");
  const byStage = Object.groupBy ? Object.groupBy(deals, (d) => d.stage) : group(deals);

  const columns = OPEN.map((stage) => column(stage, byStage[stage] || [], false)).join("");
  const recent = (list) => (list || [])
    .filter((d) => Date.now() - new Date(d.closedAt) < 45 * 86400000)
    .sort((a, b) => new Date(b.closedAt) - new Date(a.closedAt));
  board.innerHTML = `${columns}
    <div class="closing-zone">
      ${column("WON", recent(byStage.WON), true)}
      ${column("LOST", recent(byStage.LOST), true)}
    </div>`;

  wireDragAndDrop(root, board, deals);
  board.querySelectorAll(".deal[data-id]").forEach((card) => {
    card.addEventListener("click", () => {
      const deal = deals.find((d) => String(d.id) === card.dataset.id);
      dealForm(deal, () => loadBoard(root));
    });
    card.addEventListener("keydown", (e) => {
      if (e.key === "Enter") card.click();
    });
  });
}

function column(stage, deals, closed) {
  const total = deals.reduce((s, d) => s + Number(d.amount), 0);
  const label = state.meta.stages.find((s) => s.value === stage);
  const hint = closed ? "45 días" : `Prob. ${label.probability} %`;
  return `
    <section class="column" data-stage="${stage}" aria-label="${esc(label.label)}">
      <div class="column-head" style="--stage-color:${STAGE_COLORS[stage]}">
        <div class="title">${esc(label.label)} <span class="badge">${deals.length}</span></div>
        <div class="sum">${money(total, { compact: true })} <small>${hint}</small></div>
      </div>
      <div class="column-body">
        ${deals.length ? deals.map((d) => card(d, closed)).join("")
          : `<div class="empty-col">${closed ? "Suelta aquí para marcarla como " + label.label.toLowerCase() : "Sin oportunidades en esta etapa"}</div>`}
      </div>
    </section>`;
}

function card(d, compact) {
  const today = new Date().toISOString().slice(0, 10);
  const late = d.expectedCloseDate && d.expectedCloseDate < today && !compact;
  const stale = d.daysInStage > 21 && !compact;
  return `
    <article class="deal ${compact ? "compact" : ""}" draggable="${!compact}" tabindex="0" data-id="${d.id}"
             aria-label="${esc(d.title)}, ${esc(d.customerName)}, ${money(d.amount)}">
      <div class="title">${esc(d.title)}</div>
      <div class="client">${esc(d.customerName)}</div>
      <div class="row">
        <span class="amount">${money(d.amount, { compact: true })}</span>
        ${d.ownerInitials ? avatar(d.ownerInitials, d.ownerId, "sm") : ""}
      </div>
      ${compact ? (d.lostReason ? `<div class="tags"><span class="tag">${esc(d.lostReason)}</span></div>` : "") : `
      <div class="tags">
        <span class="tag ${late ? "late" : ""}">${icon("calendar")} ${late ? "Vencida " : ""}${date(d.expectedCloseDate)}</span>
        <span class="tag ${stale ? "warn" : ""}" title="Días en esta etapa">${icon("clock")} ${d.daysInStage} d</span>
      </div>`}
    </article>`;
}

function wireDragAndDrop(root, board, deals) {
  let draggedId = null;
  board.querySelectorAll('.deal[draggable="true"]').forEach((el) => {
    el.addEventListener("dragstart", (e) => {
      draggedId = el.dataset.id;
      el.classList.add("dragging");
      e.dataTransfer.effectAllowed = "move";
      e.dataTransfer.setData("text/plain", draggedId);
    });
    el.addEventListener("dragend", () => el.classList.remove("dragging"));
  });

  board.querySelectorAll(".column").forEach((col) => {
    col.addEventListener("dragover", (e) => { e.preventDefault(); col.classList.add("drop-target"); });
    col.addEventListener("dragleave", (e) => { if (!col.contains(e.relatedTarget)) col.classList.remove("drop-target"); });
    col.addEventListener("drop", async (e) => {
      e.preventDefault();
      col.classList.remove("drop-target");
      const id = e.dataTransfer.getData("text/plain") || draggedId;
      const deal = deals.find((d) => String(d.id) === id);
      const stage = col.dataset.stage;
      if (!deal || deal.stage === stage) return;
      if (stage === "LOST") {
        lostReasonForm(deal, () => loadBoard(root));
        return;
      }
      await api(`/api/deals/${id}/stage`, { method: "PATCH", body: { stage } });
      toast(stage === "WON" ? `¡Ganada! ${deal.title} por ${money(deal.amount)}` : `Movida a ${stageName(stage)}`);
      window.dispatchEvent(new Event("nexo:changed"));
      loadBoard(root);
    });
  });
}

function lostReasonForm(deal, done) {
  formModal({
    title: "Marcar como perdida",
    submitLabel: "Marcar como perdida",
    body: `<p style="margin:0;color:var(--muted)">${esc(deal.title)} con ${esc(deal.customerName)}</p>
      ${field("¿Por qué se perdió?", "lostReason", `<select class="select" name="lostReason" required>
        ${options(["Eligió a la competencia", "Precio fuera de presupuesto", "Proyecto pospuesto", "Sin respuesta del cliente", "No cumplía requisitos técnicos"].map((v) => ({ value: v, label: v })), null, { empty: "Elige un motivo" })}
      </select>`)}`,
    onSubmit: async (data) => {
      if (!data.lostReason) throw Object.assign(new Error("Elige un motivo"), { fields: { lostReason: "Elige un motivo" } });
      await api(`/api/deals/${deal.id}/stage`, { method: "PATCH", body: { stage: "LOST", lostReason: data.lostReason } });
      toast("Oportunidad marcada como perdida");
      done();
    },
  });
}

export async function dealForm(deal, done) {
  const customers = await api("/api/customers/all");
  const editing = Boolean(deal?.id);
  const { overlay } = formModal({
    title: editing ? "Editar oportunidad" : "Nueva oportunidad",
    submitLabel: editing ? "Guardar cambios" : "Crear oportunidad",
    body: `
      ${field("Oportunidad", "title", `<input class="input" name="title" value="${esc(deal?.title)}" placeholder="Ej. Licencias CRM (25 usuarios)" required>`)}
      ${field("Cliente", "customerId", `<select class="select" name="customerId" required>${options(customers, deal?.customerId, { empty: "Elige un cliente" })}</select>`)}
      <div class="two">
        ${field("Monto (USD)", "amount", `<input class="input" name="amount" type="number" min="1" step="100" value="${esc(deal?.amount)}" required>`)}
        ${field("Etapa", "stage", `<select class="select" name="stage">${options(state.meta.stages, deal?.stage || "LEAD")}</select>`)}
      </div>
      <div class="two">
        ${field("Responsable", "ownerId", `<select class="select" name="ownerId">${options(state.meta.salesReps, deal?.ownerId, { empty: "Sin asignar" })}</select>`)}
        ${field("Cierre esperado", "expectedCloseDate", `<input class="input" name="expectedCloseDate" type="date" value="${esc(deal?.expectedCloseDate)}">`)}
      </div>
      ${editing ? `<div style="display:flex;justify-content:space-between;align-items:center">
        <small style="color:var(--muted)">Probabilidad actual ${deal.probability} %, ${deal.daysInStage} días en ${esc(deal.stageLabel.toLowerCase())}</small>
        <button type="button" class="btn btn-sm btn-danger" id="delete-deal">${icon("trash")} Eliminar</button>
      </div>` : ""}`,
    onSubmit: async (data) => {
      const body = { ...data, customerId: Number(data.customerId) || null, ownerId: Number(data.ownerId) || null, amount: data.amount ? Number(data.amount) : null };
      if (body.stage === "LOST" && deal?.stage !== "LOST") {
        throw Object.assign(new Error("Para marcarla como perdida, arrástrala a la columna Perdido e indica el motivo"), { fields: {} });
      }
      await api(editing ? `/api/deals/${deal.id}` : "/api/deals", { method: editing ? "PUT" : "POST", body });
      toast(editing ? "Cambios guardados" : "Oportunidad creada");
      window.dispatchEvent(new Event("nexo:changed"));
      done();
    },
  });
  overlay.querySelector("#delete-deal")?.addEventListener("click", async () => {
    if (!(await confirmDialog(`Se eliminará "${deal.title}". Las actividades relacionadas se conservan.`))) return;
    await api(`/api/deals/${deal.id}`, { method: "DELETE" });
    overlay.remove();
    toast("Oportunidad eliminada");
    done();
  });
}

const stageName = (stage) => state.meta.stages.find((s) => s.value === stage)?.label ?? stage;

function group(list) {
  return list.reduce((acc, d) => ((acc[d.stage] ||= []).push(d), acc), {});
}
