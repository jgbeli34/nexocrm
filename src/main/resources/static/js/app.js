import { api, state, DEMO, toast } from "./core.js";
import { renderDashboard } from "./views/dashboard.js";
import { renderPipeline } from "./views/pipeline.js";
import { renderCustomers } from "./views/customers.js";
import { renderActivities } from "./views/activities.js";
import { renderIntegrations } from "./views/integrations.js";

const routes = {
  panel: { title: "Panel", render: renderDashboard },
  pipeline: { title: "Pipeline", render: renderPipeline },
  clientes: { title: "Clientes", render: renderCustomers },
  actividades: { title: "Actividades", render: renderActivities },
  integraciones: { title: "Integraciones", render: renderIntegrations },
};

const view = document.getElementById("view");
const sidebar = document.getElementById("sidebar");
const menuToggle = document.getElementById("menu-toggle");

async function boot() {
  if (DEMO) {
    document.getElementById("demo-flag").hidden = false;
    document.getElementById("api-link").closest("div").hidden = true;
  }
  try {
    const [meta, rates] = await Promise.all([api("/api/meta"), api("/api/integrations/exchange-rates")]);
    state.meta = meta;
    state.rates = rates.rates;
    if (!state.rates[state.currency]) state.currency = "USD";
  } catch (e) {
    view.innerHTML = `<div class="empty"><h2>No hay conexión con el servidor</h2><p>Comprueba que la aplicación Java esté en ejecución y recarga la página.</p></div>`;
    return;
  }
  window.addEventListener("hashchange", navigate);
  navigate();
  refreshOverdueBadge();
}

async function navigate() {
  const key = location.hash.replace(/^#\/?/, "").split("?")[0] || "panel";
  const route = routes[key] || routes.panel;
  document.querySelectorAll(".nav a").forEach((a) => {
    if (a.dataset.route === key) a.setAttribute("aria-current", "page");
    else a.removeAttribute("aria-current");
  });
  document.title = `${route.title} · NexoCRM`;
  closeMenu();
  view.innerHTML = `<div class="skeleton" style="height:64px;margin-bottom:18px"></div><div class="skeleton" style="height:360px"></div>`;
  try {
    await route.render(view);
  } catch (e) {
    console.error(e);
    view.innerHTML = `<div class="empty"><h2>No se pudo cargar esta sección</h2><p>${e.message}</p></div>`;
  }
  view.focus({ preventScroll: true });
}

export async function refreshOverdueBadge() {
  try {
    const pending = await api("/api/activities?completed=false");
    const overdue = pending.filter((a) => a.overdue).length;
    const badge = document.getElementById("overdue-count");
    badge.hidden = overdue === 0;
    badge.textContent = overdue;
    badge.setAttribute("aria-label", `${overdue} vencidas`);
  } catch { /* el contador es opcional */ }
}

function closeMenu() {
  sidebar.classList.remove("open");
  menuToggle.setAttribute("aria-expanded", "false");
}

menuToggle.addEventListener("click", () => {
  const open = sidebar.classList.toggle("open");
  menuToggle.setAttribute("aria-expanded", String(open));
});

window.addEventListener("nexo:changed", refreshOverdueBadge);
window.addEventListener("unhandledrejection", (e) => toast(e.reason?.message || "Algo salió mal", "error"));

boot();
