// Utilidades compartidas: acceso a la API, formato de moneda, modales y avisos.

export const DEMO = new URLSearchParams(location.search).has("demo")
  || location.hostname.endsWith("github.io")
  || location.protocol === "file:";

let mock = null;

/** Llama a la API REST. En modo demo, la misma llamada la atiende un backend simulado en el navegador. */
export async function api(path, { method = "GET", body, raw = false } = {}) {
  if (DEMO) {
    mock ??= await import("./demo/mock-api.js");
    return mock.handle(method, path, body);
  }
  const isForm = body instanceof FormData;
  const res = await fetch(path, {
    method,
    headers: body && !isForm ? { "Content-Type": "application/json" } : undefined,
    body: body ? (isForm ? body : JSON.stringify(body)) : undefined,
  });
  if (!res.ok) {
    let problem = {};
    try { problem = await res.json(); } catch { /* sin cuerpo */ }
    const error = new Error(problem.detail || `Error ${res.status}`);
    error.status = res.status;
    error.fields = problem.fields || {};
    throw error;
  }
  if (raw) return res;
  if (res.status === 204) return null;
  return res.json();
}

// ---------- Estado global ----------

export const state = {
  meta: null,
  currency: localStore("nexo.currency") || "USD",
  rates: { USD: 1 },
};

function localStore(key, value) {
  try {
    if (value === undefined) return localStorage.getItem(key);
    localStorage.setItem(key, value);
  } catch { /* almacenamiento no disponible */ }
  return null;
}

export function setCurrency(code) {
  state.currency = code;
  localStore("nexo.currency", code);
}

// ---------- Formato ----------

const NO_DECIMALS = new Set(["COP", "CLP"]);

/** Convierte un monto en USD a la moneda elegida y lo formatea. */
export function money(usd, { compact = false, currency = state.currency } = {}) {
  const rate = Number(state.rates[currency] ?? 1);
  const value = Number(usd || 0) * rate;
  if (compact) return compactMoney(value, currency);
  // En un CRM los centavos no aportan: se muestran montos enteros
  return new Intl.NumberFormat("es-CO", { style: "currency", currency, minimumFractionDigits: 0, maximumFractionDigits: 0 }).format(value);
}

const symbols = {};

/** Formato abreviado consistente: US$12 k, $48,6 M, $2,1 mil M (Intl mezcla "k" y "K" en es-CO). */
function compactMoney(value, currency) {
  symbols[currency] ??= new Intl.NumberFormat("es-CO", { style: "currency", currency })
    .formatToParts(0).find((p) => p.type === "currency").value;
  const abs = Math.abs(value);
  const fmt = (n, digits) => n.toLocaleString("es-CO", { maximumFractionDigits: digits });
  let body;
  if (abs >= 1e9) body = `${fmt(value / 1e9, 1)} mil M`;
  else if (abs >= 1e6) body = `${fmt(value / 1e6, 1)} M`;
  else if (abs >= 1e3) body = `${fmt(value / 1e3, abs >= 1e5 ? 0 : 1)} k`;
  else body = fmt(value, 0);
  return `${symbols[currency]}\u00a0${body}`;
}

export const pct = (v, digits = 1) => `${Number(v || 0).toLocaleString("es-CO", { maximumFractionDigits: digits })} %`;
export const num = (v) => Number(v || 0).toLocaleString("es-CO");

export function date(value, opts = { day: "numeric", month: "short" }) {
  if (!value) return "Sin fecha";
  const d = typeof value === "string" && value.length === 10 ? new Date(value + "T00:00:00") : new Date(value);
  return d.toLocaleDateString("es-CO", opts).replace(".", "");
}

export function dateTime(value) {
  const d = new Date(value);
  return `${date(d)}, ${d.toLocaleTimeString("es-CO", { hour: "numeric", minute: "2-digit" })}`;
}

/** "hoy", "mañana", "hace 3 días"... */
export function relativeDay(value) {
  const d = new Date(value);
  const today = new Date();
  const a = Date.UTC(d.getFullYear(), d.getMonth(), d.getDate());
  const b = Date.UTC(today.getFullYear(), today.getMonth(), today.getDate());
  const diff = Math.round((a - b) / 86400000);
  if (diff === 0) return "Hoy";
  if (diff === 1) return "Mañana";
  if (diff === -1) return "Ayer";
  if (diff < 0) return `Hace ${-diff} días`;
  if (diff < 7) return d.toLocaleDateString("es-CO", { weekday: "long" }).replace(/^./, (c) => c.toUpperCase());
  return date(d);
}

export function esc(value) {
  return String(value ?? "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
}

const AVATAR_TONES = ["#d6ebe2", "#dde8f2", "#f6e6c9", "#e8e1f1", "#f3dcd5", "#dcebd0"];

export function avatar(initials, id, size = "") {
  const tone = AVATAR_TONES[(Number(id) || 0) % AVATAR_TONES.length];
  return `<span class="avatar ${size}" style="--avatar:${tone}" aria-hidden="true">${esc(initials || "?")}</span>`;
}

export const STAGE_COLORS = {
  LEAD: "#a9bac2",
  QUALIFIED: "#7a9fae",
  PROPOSAL: "#4b8796",
  NEGOTIATION: "#237b6f",
  WON: "#0b7a5e",
  LOST: "#b94e3a",
};

export function stageLabel(stage) {
  return state.meta?.stages.find((s) => s.value === stage)?.label ?? stage;
}

// ---------- Iconos (trazos propios, estilo línea) ----------

const ICON_PATHS = {
  plus: '<path d="M12 5v14M5 12h14"/>',
  search: '<circle cx="11" cy="11" r="7"/><path d="m20 20-3.5-3.5"/>',
  x: '<path d="M18 6 6 18M6 6l12 12"/>',
  edit: '<path d="M12 20h9"/><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4Z"/>',
  trash: '<path d="M3 6h18M8 6V4h8v2M19 6l-1 14H6L5 6"/>',
  download: '<path d="M12 3v12M7 10l5 5 5-5M5 21h14"/>',
  upload: '<path d="M12 21V9M7 14l5-5 5 5M5 3h14"/>',
  check: '<path d="M20 6 9 17l-5-5"/>',
  alert: '<path d="M12 9v4M12 17h.01"/><path d="M10.3 3.9 1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0Z"/>',
  clock: '<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>',
  calendar: '<rect x="3" y="4" width="18" height="18" rx="2"/><path d="M16 2v4M8 2v4M3 10h18"/>',
  CALL: '<path d="M22 16.9v3a2 2 0 0 1-2.2 2 19.8 19.8 0 0 1-8.6-3.1 19.5 19.5 0 0 1-6-6A19.8 19.8 0 0 1 2.1 4.2 2 2 0 0 1 4.1 2h3a2 2 0 0 1 2 1.7c.1.9.4 1.8.7 2.7a2 2 0 0 1-.5 2.1L8 9.8a16 16 0 0 0 6 6l1.3-1.3a2 2 0 0 1 2.1-.4c.9.3 1.8.6 2.7.7a2 2 0 0 1 1.7 2Z"/>',
  EMAIL: '<rect x="2" y="4" width="20" height="16" rx="2"/><path d="m22 7-10 6L2 7"/>',
  MEETING: '<path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.9M16 3.1a4 4 0 0 1 0 7.8"/>',
  TASK: '<path d="M9 11l3 3L22 4"/><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/>',
  api: '<path d="m16 18 6-6-6-6M8 6l-6 6 6 6"/>',
  file: '<path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8Z"/><path d="M14 2v6h6M8 13h8M8 17h8"/>',
  sheet: '<rect x="3" y="3" width="18" height="18" rx="2"/><path d="M3 9h18M3 15h18M9 3v18"/>',
  globe: '<circle cx="12" cy="12" r="10"/><path d="M2 12h20M12 2a15 15 0 0 1 0 20 15 15 0 0 1 0-20"/>',
  bolt: '<path d="M13 2 3 14h9l-1 8 10-12h-9l1-8Z"/>',
  send: '<path d="m22 2-7 20-4-9-9-4Z"/><path d="M22 2 11 13"/>',
  refresh: '<path d="M21 12a9 9 0 1 1-2.6-6.4L21 8"/><path d="M21 3v5h-5"/>',
  power: '<path d="M18.4 6.6a9 9 0 1 1-12.8 0M12 2v10"/>',
};

export function icon(name) {
  return `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${ICON_PATHS[name] || ""}</svg>`;
}

// ---------- Avisos ----------

export function toast(message, type = "ok") {
  const box = document.getElementById("toasts");
  const el = document.createElement("div");
  el.className = `toast ${type}`;
  el.innerHTML = `${icon(type === "error" ? "alert" : "check")}<span>${esc(message)}</span>`;
  box.append(el);
  setTimeout(() => el.remove(), 4200);
}

// ---------- Modales y panel lateral ----------

let lastFocus = null;

function mountOverlay(html, { drawer = false } = {}) {
  lastFocus = document.activeElement;
  const overlay = document.createElement("div");
  overlay.className = `overlay${drawer ? " drawer-overlay" : ""}`;
  overlay.innerHTML = html;
  document.body.append(overlay);
  const close = () => {
    overlay.remove();
    document.removeEventListener("keydown", onKey);
    lastFocus?.focus?.();
  };
  const onKey = (e) => { if (e.key === "Escape") close(); };
  document.addEventListener("keydown", onKey);
  overlay.addEventListener("mousedown", (e) => { if (e.target === overlay) close(); });
  overlay.querySelectorAll("[data-close]").forEach((b) => b.addEventListener("click", close));
  setTimeout(() => overlay.querySelector("input, select, textarea, button:not([data-close])")?.focus(), 30);
  return { overlay, close };
}

/**
 * Abre un formulario modal. `onSubmit` recibe los datos del formulario; si lanza un error con
 * `fields`, los mensajes se muestran junto a cada campo.
 */
export function formModal({ title, body, submitLabel = "Guardar", onSubmit }) {
  const { overlay, close } = mountOverlay(`
    <form class="modal" role="dialog" aria-modal="true" aria-labelledby="modal-title" novalidate>
      <div class="modal-head">
        <h2 id="modal-title">${esc(title)}</h2>
        <button type="button" class="icon-btn" data-close aria-label="Cerrar">${icon("x")}</button>
      </div>
      <div class="modal-body">${body}</div>
      <div class="modal-foot">
        <button type="button" class="btn btn-ghost" data-close>Cancelar</button>
        <button type="submit" class="btn btn-primary">${esc(submitLabel)}</button>
      </div>
    </form>`);
  const form = overlay.querySelector("form");
  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    form.querySelectorAll(".error").forEach((el) => (el.textContent = ""));
    form.querySelectorAll("[aria-invalid]").forEach((el) => el.removeAttribute("aria-invalid"));
    const submit = form.querySelector("[type=submit]");
    submit.disabled = true;
    try {
      await onSubmit(formData(form));
      close();
    } catch (err) {
      const fields = err.fields || {};
      Object.entries(fields).forEach(([name, msg]) => {
        const input = form.elements[name];
        input?.setAttribute("aria-invalid", "true");
        const slot = form.querySelector(`[data-error-for="${name}"]`);
        if (slot) slot.textContent = msg;
      });
      if (!Object.keys(fields).length) toast(err.message, "error");
    } finally {
      submit.disabled = false;
    }
  });
  return { overlay, close, form };
}

export function drawer(html) {
  return mountOverlay(`<aside class="drawer" role="dialog" aria-modal="true">${html}</aside>`, { drawer: true });
}

export function confirmDialog(message, { confirmLabel = "Eliminar" } = {}) {
  return new Promise((resolve) => {
    const { overlay, close } = mountOverlay(`
      <div class="modal" role="alertdialog" aria-modal="true" aria-labelledby="confirm-title" style="width:min(420px,100%)">
        <div class="modal-head"><h2 id="confirm-title">¿Seguro?</h2></div>
        <div class="modal-body"><p style="margin:0">${esc(message)}</p></div>
        <div class="modal-foot">
          <button type="button" class="btn btn-ghost" data-close>Cancelar</button>
          <button type="button" class="btn btn-danger" data-ok>${esc(confirmLabel)}</button>
        </div>
      </div>`);
    overlay.querySelector("[data-ok]").addEventListener("click", () => { close(); resolve(true); });
    overlay.querySelectorAll("[data-close]").forEach((b) => b.addEventListener("click", () => resolve(false)));
  });
}

/** Campo de formulario con etiqueta y espacio para el error de validación. */
export function field(label, name, input) {
  return `<label class="field"><span>${esc(label)}</span>${input}<small class="error" data-error-for="${name}"></small></label>`;
}

export function options(list, selected, { empty } = {}) {
  const head = empty !== undefined ? `<option value="">${esc(empty)}</option>` : "";
  return head + list.map((o) => {
    const value = o.value ?? o.id;
    const label = o.label ?? o.name ?? o.companyName;
    return `<option value="${esc(value)}" ${String(value) === String(selected ?? "") ? "selected" : ""}>${esc(label)}</option>`;
  }).join("");
}

function formData(form) {
  const data = {};
  for (const el of form.elements) {
    if (!el.name) continue;
    const v = el.value.trim();
    data[el.name] = v === "" ? null : v;
  }
  return data;
}

/** Descarga un archivo generado por la API (o por el modo demo). */
export async function download(path, fallbackName) {
  if (DEMO) {
    mock ??= await import("./demo/mock-api.js");
    const file = mock.file(path);
    if (!file) {
      toast("El reporte Excel se genera en el servidor Java. Ejecuta el backend para descargarlo.", "error");
      return;
    }
    saveBlob(new Blob([file.content], { type: file.type }), file.name);
    return;
  }
  const res = await fetch(path);
  if (!res.ok) { toast("No se pudo generar el archivo", "error"); return; }
  const disposition = res.headers.get("Content-Disposition") || "";
  const name = /filename="?([^";]+)"?/.exec(disposition)?.[1] || fallbackName;
  saveBlob(await res.blob(), name);
}

function saveBlob(blob, name) {
  const url = URL.createObjectURL(blob);
  const a = Object.assign(document.createElement("a"), { href: url, download: name });
  document.body.append(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

/** Lee el valor de un parámetro CSS (para que los gráficos usen la misma paleta). */
export const cssVar = (name) => getComputedStyle(document.documentElement).getPropertyValue(name).trim();
