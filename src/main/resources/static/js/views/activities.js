import { api, state, esc, icon, relativeDay, dateTime, formModal, field, options, toast, confirmDialog } from "../core.js";

let showDone = false;

export async function renderActivities(root) {
  root.innerHTML = `
    <header class="page-head">
      <div>
        <h1>Actividades</h1>
        <p>Llamadas, reuniones y tareas que mantienen vivas las oportunidades.</p>
      </div>
      <div class="actions">
        <button class="btn btn-primary" id="new-activity">${icon("plus")} Nueva actividad</button>
      </div>
    </header>
    <div id="activity-list" class="activity-groups"></div>`;
  root.querySelector("#new-activity").addEventListener("click", () => activityForm(null, () => load(root)));
  await load(root);
}

async function load(root) {
  const all = await api("/api/activities");
  const pending = all.filter((a) => !a.completed);
  const done = all.filter((a) => a.completed).sort((a, b) => new Date(b.completedAt) - new Date(a.completedAt));
  const today = new Date().toDateString();

  const groups = [
    { title: "Vencidas", cls: "late", items: pending.filter((a) => a.overdue) },
    { title: "Hoy", items: pending.filter((a) => !a.overdue && new Date(a.dueAt).toDateString() === today) },
    { title: "Próximas", items: pending.filter((a) => !a.overdue && new Date(a.dueAt).toDateString() !== today) },
  ];

  const box = root.querySelector("#activity-list");
  box.innerHTML = groups.filter((g) => g.items.length).map((g) => `
      <section>
        <h2 class="group-title ${g.cls || ""}">${g.title} <small>${g.items.length}</small></h2>
        <div class="task-list">${g.items.map(task).join("")}</div>
      </section>`).join("")
    + (pending.length ? "" : `<div class="empty"><strong>Todo al día</strong><span>No hay actividades pendientes. Programa el siguiente contacto con un cliente.</span></div>`)
    + `<section>
        <h2 class="group-title">Completadas <small>${done.length}</small>
          <button class="btn btn-sm btn-ghost" id="toggle-done" aria-expanded="${showDone}">${showDone ? "Ocultar" : "Mostrar"}</button></h2>
        ${showDone ? `<div class="task-list">${done.slice(0, 25).map(task).join("")}</div>` : ""}
      </section>`;

  box.querySelector("#toggle-done").addEventListener("click", () => { showDone = !showDone; load(root); });
  box.querySelectorAll(".task").forEach((el) => {
    const activity = all.find((a) => String(a.id) === el.dataset.id);
    el.querySelector(".check").addEventListener("change", async () => {
      await api(`/api/activities/${activity.id}/toggle`, { method: "PATCH" });
      toast(activity.completed ? "Actividad reabierta" : "Actividad completada");
      window.dispatchEvent(new Event("nexo:changed"));
      load(root);
    });
    el.querySelector("[data-edit]").addEventListener("click", () => activityForm(activity, () => load(root)));
    el.querySelector("[data-delete]").addEventListener("click", async () => {
      if (!(await confirmDialog(`Se eliminará "${activity.subject}".`))) return;
      await api(`/api/activities/${activity.id}`, { method: "DELETE" });
      toast("Actividad eliminada");
      window.dispatchEvent(new Event("nexo:changed"));
      load(root);
    });
  });
}

function task(a) {
  const context = [a.dealTitle || a.customerName, a.ownerName && `a cargo de ${a.ownerName}`].filter(Boolean).join(", ");
  return `<div class="task ${a.completed ? "done" : ""}" data-id="${a.id}">
    <input type="checkbox" class="check" ${a.completed ? "checked" : ""} aria-label="Marcar ${esc(a.subject)} como ${a.completed ? "pendiente" : "hecha"}">
    <span class="type-icon" data-type="${a.type}" title="${esc(a.typeLabel)}">${icon(a.type)}</span>
    <div style="min-width:0">
      <div class="subject">${esc(a.subject)}</div>
      <div class="context">${esc(context || a.typeLabel)}</div>
    </div>
    <span class="due ${a.overdue ? "late" : ""}" title="${esc(dateTime(a.dueAt))}">${relativeDay(a.dueAt)}, ${new Date(a.dueAt).toLocaleTimeString("es-CO", { hour: "numeric", minute: "2-digit" })}</span>
    <span>
      <button class="icon-btn" data-edit aria-label="Editar">${icon("edit")}</button>
      <button class="icon-btn" data-delete aria-label="Eliminar">${icon("trash")}</button>
    </span>
  </div>`;
}

async function activityForm(a, done) {
  const [customers, deals] = await Promise.all([api("/api/customers/all"), api("/api/deals")]);
  const openDeals = deals.filter((d) => !["WON", "LOST"].includes(d.stage));
  const editing = Boolean(a);
  const defaultDue = new Date(Date.now() + 86400000);
  defaultDue.setHours(10, 0, 0, 0);
  const due = (a ? new Date(a.dueAt) : defaultDue);
  const local = new Date(due.getTime() - due.getTimezoneOffset() * 60000).toISOString().slice(0, 16);

  formModal({
    title: editing ? "Editar actividad" : "Nueva actividad",
    submitLabel: editing ? "Guardar cambios" : "Crear actividad",
    body: `
      <div class="two">
        ${field("Tipo", "type", `<select class="select" name="type">${options(state.meta.activityTypes, a?.type || "CALL")}</select>`)}
        ${field("Fecha y hora", "dueAt", `<input class="input" type="datetime-local" name="dueAt" value="${local}" required>`)}
      </div>
      ${field("Asunto", "subject", `<input class="input" name="subject" value="${esc(a?.subject)}" placeholder="Ej. Llamada para revisar la propuesta" required>`)}
      ${field("Oportunidad", "dealId", `<select class="select" name="dealId">${options(openDeals.map((d) => ({ value: d.id, label: `${d.title}, ${d.customerName}` })), a?.dealId, { empty: "Ninguna" })}</select>`)}
      ${field("Cliente", "customerId", `<select class="select" name="customerId">${options(customers, a?.customerId, { empty: "Se toma de la oportunidad" })}</select>`)}
      ${field("Responsable", "ownerId", `<select class="select" name="ownerId">${options(state.meta.salesReps, a?.ownerId, { empty: "Sin asignar" })}</select>`)}
      ${field("Detalle", "description", `<textarea class="textarea" name="description">${esc(a?.description)}</textarea>`)}`,
    onSubmit: async (data) => {
      const body = {
        ...data,
        dueAt: data.dueAt ? `${data.dueAt}:00` : null,
        dealId: Number(data.dealId) || null,
        customerId: Number(data.customerId) || null,
        ownerId: Number(data.ownerId) || null,
      };
      await api(editing ? `/api/activities/${a.id}` : "/api/activities", { method: editing ? "PUT" : "POST", body });
      toast(editing ? "Cambios guardados" : "Actividad creada");
      window.dispatchEvent(new Event("nexo:changed"));
      done();
    },
  });
}
