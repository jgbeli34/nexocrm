// Backend simulado para el modo demo (GitHub Pages).
// Reproduce los mismos endpoints y reglas de negocio que la API Java para que la
// interfaz pueda probarse sin servidor. Los datos viven en memoria y se reinician al recargar.

const STAGES = [
  ["LEAD", "Prospecto", 10], ["QUALIFIED", "Calificado", 25], ["PROPOSAL", "Propuesta", 50],
  ["NEGOTIATION", "Negociación", 75], ["WON", "Ganado", 100], ["LOST", "Perdido", 0],
];
const STATUSES = [["LEAD", "Lead"], ["ACTIVE", "Activo"], ["INACTIVE", "Inactivo"]];
const SOURCES = [["WEBSITE", "Sitio web"], ["REFERRAL", "Referido"], ["LINKEDIN", "LinkedIn"], ["EVENT", "Evento"],
  ["COLD_OUTREACH", "Prospección en frío"], ["IMPORT", "Importación CSV"]];
const TYPES = [["CALL", "Llamada"], ["EMAIL", "Correo"], ["MEETING", "Reunión"], ["TASK", "Tarea"]];
const EVENTS = [["DEAL_WON", "Negocio ganado"], ["DEAL_LOST", "Negocio perdido"],
  ["DEAL_STAGE_CHANGED", "Cambio de etapa"], ["CUSTOMER_CREATED", "Cliente creado"]];
const RATES = { USD: 1, COP: 4050, MXN: 18.4, EUR: 0.92, PEN: 3.75, CLP: 940, BRL: 5.4 };

const label = (list, v) => list.find((x) => x[0] === v)?.[1] ?? v;
const prob = (stage) => STAGES.find((s) => s[0] === stage)[2];
const isOpen = (stage) => stage !== "WON" && stage !== "LOST";
const DAY = 86400000;

// Generador pseudoaleatorio con semilla: la demo siempre muestra los mismos datos
let seed = 20261116;
const rand = () => { seed |= 0; seed = (seed + 0x6d2b79f5) | 0; let t = Math.imul(seed ^ (seed >>> 15), 1 | seed); t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t; return ((t ^ (t >>> 14)) >>> 0) / 4294967296; };
const pick = (arr) => arr[Math.floor(rand() * arr.length)];
const int = (n) => Math.floor(rand() * n);

const db = { reps: [], customers: [], deals: [], activities: [], hooks: [], deliveries: [], seq: 1000 };
const nextId = () => ++db.seq;

const COMPANIES = [
  ["Café Andino S.A.S.", "Agroindustria", "Manizales", "Colombia"], ["Logística del Pacífico", "Logística", "Buenaventura", "Colombia"],
  ["Textiles La Montaña", "Manufactura", "Medellín", "Colombia"], ["Clínica Santa Lucía", "Salud", "Bogotá", "Colombia"],
  ["Grupo Educativo Horizonte", "Educación", "Cali", "Colombia"], ["Fintech Cóndor", "Finanzas", "Bogotá", "Colombia"],
  ["Hoteles Caribe Azul", "Turismo", "Cartagena", "Colombia"], ["Distribuidora El Puerto", "Retail", "Barranquilla", "Colombia"],
  ["Constructora Vértice", "Construcción", "Bucaramanga", "Colombia"], ["AgroValle Orgánicos", "Agroindustria", "Palmira", "Colombia"],
  ["Laboratorios Bioandes", "Salud", "Medellín", "Colombia"], ["Moda Urbana Co.", "Retail", "Bogotá", "Colombia"],
  ["Transportes Ruta 40", "Logística", "Pereira", "Colombia"], ["Seguros Altamira", "Finanzas", "Bogotá", "Colombia"],
  ["Panadería La Espiga", "Retail", "Cali", "Colombia"], ["Instituto Técnico Nova", "Educación", "Medellín", "Colombia"],
  ["Energía Solar Llanos", "Energía", "Villavicencio", "Colombia"], ["Metalmecánica Andes", "Manufactura", "Cali", "Colombia"],
  ["Inmobiliaria Prisma", "Construcción", "Bogotá", "Colombia"], ["Ópticas Visión Clara", "Salud", "Pereira", "Colombia"],
  ["Mercados Frescos", "Retail", "Ciudad de México", "México"], ["Grupo Industrial Maya", "Manufactura", "Monterrey", "México"],
  ["Viajes Sol y Mar", "Turismo", "Cancún", "México"], ["Banca Comunal Azteca", "Finanzas", "Guadalajara", "México"],
  ["Pesquera del Sur", "Agroindustria", "Lima", "Perú"], ["Universidad Inca Digital", "Educación", "Lima", "Perú"],
  ["Minera Altiplano", "Energía", "Arequipa", "Perú"], ["Vinos del Valle Central", "Agroindustria", "Santiago", "Chile"],
  ["Retail Patagonia", "Retail", "Santiago", "Chile"], ["Salud Integral Quito", "Salud", "Quito", "Ecuador"],
  ["Cacao Esmeralda", "Agroindustria", "Guayaquil", "Ecuador"], ["Software Ceiba", "Tecnología", "Medellín", "Colombia"],
  ["Datos & Nube Ltda.", "Tecnología", "Bogotá", "Colombia"], ["Ferretería Industrial Norte", "Retail", "Cúcuta", "Colombia"],
  ["Agencia Creativa Pulpo", "Tecnología", "Bogotá", "Colombia"], ["Lácteos San Pedro", "Agroindustria", "Ubaté", "Colombia"],
  ["Farmacias Vida", "Salud", "Barranquilla", "Colombia"], ["Coworking Colmena", "Construcción", "Medellín", "Colombia"],
  ["Envíos Express Ya", "Logística", "Bogotá", "Colombia"], ["Colegio Bilingüe Arcoíris", "Educación", "Chía", "Colombia"],
  ["Aerolínea Regional Cóndor", "Turismo", "Rionegro", "Colombia"], ["Cooperativa Financiera Unión", "Finanzas", "Tunja", "Colombia"],
];
const FIRST = ["María", "Juan", "Daniela", "Carlos", "Paula", "Felipe", "Sofía", "Alejandro", "Natalia", "Diego", "Isabela", "Mateo", "Juliana", "Sebastián", "Mariana", "Tomás", "Catalina", "Nicolás", "Gabriela", "Andrés"];
const LAST = ["García", "Rodríguez", "Martínez", "López", "Hernández", "Gómez", "Díaz", "Restrepo", "Ospina", "Castro", "Vargas", "Rojas", "Mejía", "Torres", "Salazar", "Cárdenas"];
const PRODUCTS = ["Licencias CRM (25 usuarios)", "Implementación ERP", "Soporte anual premium", "Migración a la nube", "Tablero de BI comercial",
  "Integración e-commerce", "Plan Enterprise", "Renovación anual", "Capacitación equipo comercial", "Automatización de facturación",
  "App móvil de pedidos", "Licencias CRM (50 usuarios)", "Consultoría de datos", "Portal de clientes"];
const LOST_REASONS = ["Eligió a la competencia", "Precio fuera de presupuesto", "Proyecto pospuesto", "Sin respuesta del cliente", "No cumplía requisitos técnicos"];
const SRC_POOL = ["WEBSITE", "REFERRAL", "LINKEDIN", "LINKEDIN", "EVENT", "COLD_OUTREACH", "REFERRAL", "WEBSITE"];

const strip = (s) => s.normalize("NFD").replace(/\p{M}/gu, "").toLowerCase().replace(/[^a-z0-9]/g, "");
const shortName = (c) => c.replace(" S.A.S.", "").replace(" Ltda.", "").replace(" Co.", "");
// Fecha y hora local sin zona, igual que LocalDateTime en Java
const iso = (d) => { const x = new Date(d); return new Date(x - x.getTimezoneOffset() * 60000).toISOString().slice(0, 19); };
const atHour = (d) => { const x = new Date(d); x.setHours(8 + int(10), rand() < 0.5 ? 0 : 30, 0, 0); return x; };

function seedData() {
  const now = new Date();
  db.reps = [["Valentina Ríos", 60000], ["Andrés Morales", 55000], ["Camila Herrera", 50000], ["Santiago Vélez", 45000], ["Laura Gómez", 40000]]
    .map(([name, quota], i) => ({ id: i + 1, name, email: `${strip(name.split(" ")[0])}@nexocrm.dev`, monthlyQuota: quota }));

  db.customers = COMPANIES.map(([company, industry, city, country], i) => {
    const first = pick(FIRST); const last = pick(LAST);
    return {
      id: i + 1, companyName: company, contactName: `${first} ${last}`,
      email: `${strip(first)}.${strip(last)}@${strip(shortName(company))}.example`,
      phone: `+57 3${10 + int(89)} ${100 + int(899)} ${1000 + int(8999)}`,
      industry, city, country, status: "LEAD", source: pick(SRC_POOL),
      ownerId: db.reps[i % db.reps.length].id, notes: null,
      createdAt: iso(now - (30 + int(400)) * DAY),
    };
  });

  const newDeal = (created, growth) => {
    const c = pick(db.customers);
    const ownerId = rand() < 0.8 ? c.ownerId : pick(db.reps).id;
    const amount = Math.round(((2500 + Math.pow(rand(), 1.8) * 42000) * growth) / 500) * 500;
    return { id: nextId(), title: pick(PRODUCTS), customerId: c.id, ownerId, amount, stage: "LEAD", probability: 10,
      expectedCloseDate: null, createdAt: iso(created), stageChangedAt: iso(created), closedAt: null, lostReason: null };
  };

  for (let i = 11; i >= 0; i--) {
    const ym = new Date(now.getFullYear(), now.getMonth() - i, 1);
    const daysInMonth = new Date(ym.getFullYear(), ym.getMonth() + 1, 0).getDate();
    const lastDay = i === 0 ? now.getDate() : daysInMonth;
    let won = 4 + Math.floor((11 - i) / 2) + int(3);
    if (i === 0) won = Math.max(4, Math.round((won * lastDay) / daysInMonth) + 3);
    const lost = 2 + int(3);
    for (let k = 0; k < won + lost; k++) {
      let closed = atHour(new Date(ym.getFullYear(), ym.getMonth(), 1 + int(lastDay)));
      if (closed > now) closed = new Date(now - 2 * 3600000);
      const d = newDeal(new Date(closed - (12 + int(60)) * DAY), 1 + (11 - i) * 0.04);
      d.expectedCloseDate = iso(closed - (int(10) - 5) * DAY).slice(0, 10);
      move(d, k < won ? "WON" : "LOST", k < won ? null : pick(LOST_REASONS), closed);
      db.deals.push(d);
    }
  }

  [["LEAD", 11], ["QUALIFIED", 9], ["PROPOSAL", 8], ["NEGOTIATION", 6]].forEach(([stage, n], s) => {
    for (let k = 0; k < n; k++) {
      const created = atHour(new Date(now - (3 + int(80)) * DAY));
      const d = newDeal(created, 1.15);
      if (stage !== "LEAD") move(d, stage, null, new Date(+created + int(Math.max(1, (now - created) / DAY)) * DAY));
      d.expectedCloseDate = iso(+now + (-6 + int(90) - s * 6) * DAY).slice(0, 10);
      db.deals.push(d);
    }
  });
  db.customers.filter((c) => c.status === "LEAD").slice(0, 4).forEach((c) => (c.status = "INACTIVE"));
  // Un cliente existe antes que su primera oportunidad
  db.deals.forEach((d) => {
    const c = customer(d.customerId);
    const limit = new Date(d.createdAt) - 7 * DAY;
    if (new Date(c.createdAt) > limit) c.createdAt = iso(limit - int(20) * DAY);
  });

  const open = db.deals.filter((d) => isOpen(d.stage));
  const templates = [["CALL", "Llamada de seguimiento"], ["MEETING", "Demo del producto"], ["EMAIL", "Enviar propuesta actualizada"],
    ["TASK", "Preparar cotización"], ["MEETING", "Reunión de cierre"], ["CALL", "Validar presupuesto con finanzas"],
    ["EMAIL", "Compartir caso de éxito"], ["TASK", "Revisar contrato con jurídica"]];
  for (let i = 0; i < 40; i++) {
    const deal = pick(open);
    const [type, subject] = pick(templates);
    let due; let done = false;
    if (i < 14) { due = atHour(new Date(+now + int(9) * DAY)); if (due < now) due = new Date(+due + DAY); }
    else if (i < 18) due = atHour(new Date(now - (1 + int(4)) * DAY));
    else { due = atHour(new Date(now - (1 + int(40)) * DAY)); done = true; }
    db.activities.push({ id: nextId(), type, subject: `${subject} con ${shortName(customer(deal.customerId).companyName)}`,
      description: `Relacionada con: ${deal.title}`, dueAt: iso(due), completed: done, completedAt: done ? iso(+due + 3600000) : null,
      customerId: deal.customerId, dealId: deal.id, ownerId: deal.ownerId });
  }

  db.hooks = [
    { id: 1, name: "Sandbox interno: negocios ganados", targetUrl: `${location.origin}/api/webhooks/receiver`, event: "DEAL_WON", active: true },
    { id: 2, name: "Sandbox interno: cambios de etapa", targetUrl: `${location.origin}/api/webhooks/receiver`, event: "DEAL_STAGE_CHANGED", active: true },
    { id: 3, name: "Canal #ventas en Slack (ejemplo)", targetUrl: "https://hooks.slack.com/services/T000/B000/REEMPLAZAR", event: "DEAL_WON", active: false },
  ].map((h) => ({ ...h, secret: "whsec_" + Math.random().toString(16).slice(2).padEnd(48, "0"), createdAt: iso(now) }));
}

function move(d, stage, reason, when = new Date()) {
  if (d.stage === stage) return;
  d.stage = stage;
  d.probability = prob(stage);
  d.stageChangedAt = iso(when);
  d.closedAt = isOpen(stage) ? null : iso(when);
  d.lostReason = stage === "LOST" ? reason : null;
  if (stage === "WON") customer(d.customerId).status = "ACTIVE";
}

const customer = (id) => db.customers.find((c) => c.id === Number(id));
const rep = (id) => db.reps.find((r) => r.id === Number(id));
const initials = (name) => (name || "?").split(/\s+/).slice(0, 2).map((p) => p[0].toUpperCase()).join("");

// ---------- Mapeo a DTO (mismo formato que la API Java) ----------

const customerDto = (c) => ({ ...c, statusLabel: label(STATUSES, c.status), sourceLabel: label(SOURCES, c.source), ownerName: rep(c.ownerId)?.name ?? null });

function dealDto(d) {
  const c = customer(d.customerId); const r = rep(d.ownerId);
  const until = d.closedAt ? new Date(d.closedAt) : new Date();
  return { ...d, customerName: c.companyName, ownerName: r?.name ?? null, ownerInitials: r ? initials(r.name) : null,
    stageLabel: label(STAGES, d.stage), weightedAmount: (d.amount * d.probability) / 100,
    daysInStage: Math.max(0, Math.floor((until - new Date(d.stageChangedAt)) / DAY)) };
}

function activityDto(a) {
  return { ...a, typeLabel: label(TYPES, a.type), overdue: !a.completed && new Date(a.dueAt) < new Date(),
    customerName: customer(a.customerId)?.companyName ?? null, dealTitle: db.deals.find((d) => d.id === a.dealId)?.title ?? null,
    ownerName: rep(a.ownerId)?.name ?? null };
}

const hookDto = (h, reveal = false) => ({ ...h, eventLabel: label(EVENTS, h.event), secret: reveal ? h.secret : h.secret.slice(0, 10) + "••••••••" });

// ---------- Analítica (equivalente a AnalyticsService.buildDashboard) ----------

const ym = (d) => { const x = new Date(d); return x.getFullYear() * 12 + x.getMonth(); };
const sum = (list) => list.reduce((s, d) => s + d.amount, 0);
const rate = (p, t) => (t ? Math.round((p * 1000) / t) / 10 : 0);
const monthLabel = (n) => { const s = new Date(Math.floor(n / 12), n % 12, 1).toLocaleDateString("es-CO", { month: "short", year: "2-digit" }).replace(".", "").replace(" de ", " "); return s[0].toUpperCase() + s.slice(1); };

function dashboard() {
  const now = new Date(); const cur = ym(now);
  const won = db.deals.filter((d) => d.stage === "WON"); const lost = db.deals.filter((d) => d.stage === "LOST");
  const open = db.deals.filter((d) => isOpen(d.stage));
  const inMonth = (list, m) => list.filter((d) => d.closedAt && ym(d.closedAt) === m);
  const thisM = sum(inMonth(won, cur));
  const lastM = sum(inMonth(won, cur - 1).filter((d) => new Date(d.closedAt).getDate() <= now.getDate())); // mismo corte (MTD)
  const weighted = (list) => list.reduce((s, d) => s + (d.amount * d.probability) / 100, 0);
  const cycle = won.length ? won.reduce((s, d) => s + (new Date(d.closedAt) - new Date(d.createdAt)) / DAY, 0) / won.length : 0;

  const byIndustry = {};
  won.forEach((d) => { const k = customer(d.customerId).industry || "Sin industria"; (byIndustry[k] ||= []).push(d); });

  return {
    kpis: {
      revenueWon: sum(won), revenueThisMonth: thisM, revenueLastMonth: lastM,
      monthOverMonthGrowth: lastM ? Math.round(((thisM - lastM) * 1000) / lastM) / 10 : null,
      openPipeline: sum(open), weightedPipeline: weighted(open), winRate: rate(won.length, won.length + lost.length),
      averageDealSize: won.length ? sum(won) / won.length : 0, averageSalesCycleDays: Math.round(cycle * 10) / 10,
      openDeals: open.length, activeCustomers: db.customers.filter((c) => c.status === "ACTIVE").length,
      overdueActivities: db.activities.filter((a) => !a.completed && new Date(a.dueAt) < now).length,
    },
    revenueByMonth: Array.from({ length: 12 }, (_, i) => {
      const m = cur - 11 + i; const list = inMonth(won, m);
      return { month: String(m), label: monthLabel(m), revenue: sum(list), dealsWon: list.length };
    }),
    pipelineByStage: STAGES.filter(([s]) => isOpen(s)).map(([stage, lbl]) => {
      const list = open.filter((d) => d.stage === stage);
      return { stage, label: lbl, count: list.length, amount: sum(list), weighted: weighted(list) };
    }),
    leaderboard: db.reps.map((r) => {
      const mine = db.deals.filter((d) => d.ownerId === r.id);
      const w = mine.filter((d) => d.stage === "WON"); const l = mine.filter((d) => d.stage === "LOST");
      const month = sum(inMonth(w, cur));
      return { id: r.id, name: r.name, initials: initials(r.name), revenueWon: sum(w), revenueThisMonth: month,
        monthlyQuota: r.monthlyQuota, quotaAttainment: Math.round((month * 1000) / r.monthlyQuota) / 10,
        dealsWon: w.length, winRate: rate(w.length, w.length + l.length) };
    }).sort((a, b) => (b.revenueThisMonth - a.revenueThisMonth) || (b.revenueWon - a.revenueWon)),
    wonByIndustry: Object.entries(byIndustry).map(([k, list]) => ({ label: k, amount: sum(list), count: list.length }))
      .sort((a, b) => b.amount - a.amount),
    conversionBySource: SOURCES.map(([source, lbl]) => {
      const closed = db.deals.filter((d) => customer(d.customerId).source === source && !isOpen(d.stage));
      const w = closed.filter((d) => d.stage === "WON").length;
      return { source, label: lbl, total: closed.length, won: w, conversionRate: rate(w, closed.length) };
    }).filter((s) => s.total > 0),
    forecast: [0, 1, 2].map((i) => {
      const m = cur + i;
      const list = open.filter((d) => d.expectedCloseDate && (i === 0 ? ym(d.expectedCloseDate + "T00:00") <= m : ym(d.expectedCloseDate + "T00:00") === m));
      return { month: String(m), label: monthLabel(m), weighted: weighted(list), bestCase: sum(list) };
    }),
  };
}

// ---------- Validación y errores ----------

function fail(status, message, fields) {
  const e = new Error(message);
  e.status = status;
  e.fields = fields || {};
  throw e;
}

function validate(body, rules) {
  const fields = {};
  Object.entries(rules).forEach(([name, check]) => { const msg = check(body[name]); if (msg) fields[name] = msg; });
  if (Object.keys(fields).length) fail(400, "Revisa los campos marcados", fields);
}

const required = (msg) => (v) => (v === null || v === undefined || String(v).trim() === "" ? msg : null);

// ---------- Webhooks simulados ----------

function deliver(hook, event, data, test = false) {
  const sandbox = hook.targetUrl.includes("/api/webhooks/receiver");
  const ok = sandbox || /webhook\.site|zapier|n8n|make\.com/.test(hook.targetUrl);
  const payload = JSON.stringify({ id: crypto.randomUUID?.() ?? String(Math.random()), event, test, occurredAt: new Date().toISOString(), data });
  const delivery = {
    id: nextId(), subscriptionId: hook.id, subscriptionName: hook.name, event, eventLabel: label(EVENTS, event),
    statusCode: ok ? 204 : 404, success: ok, attempts: 1, errorMessage: ok ? null : "HTTP 404",
    durationMs: 25 + int(120), deliveredAt: iso(new Date()), payload,
  };
  db.deliveries.unshift(delivery);
  return delivery;
}

function publish(event, data) {
  db.hooks.filter((h) => h.active && h.event === event).forEach((h) => deliver(h, event, data));
}

// ---------- CSV ----------

function csvLine(values) {
  return values.map((v) => { const s = v == null ? "" : String(v); return /[",\n]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s; }).join(",");
}

function parseCsv(text) {
  const rows = []; let row = []; let cell = ""; let quoted = false;
  for (let i = 0; i < text.length; i++) {
    const ch = text[i];
    if (quoted) {
      if (ch === '"' && text[i + 1] === '"') { cell += '"'; i++; } else if (ch === '"') quoted = false; else cell += ch;
    } else if (ch === '"') quoted = true;
    else if (ch === ",") { row.push(cell); cell = ""; }
    else if (ch === "\n" || ch === "\r") { if (ch === "\r" && text[i + 1] === "\n") i++; row.push(cell); rows.push(row); row = []; cell = ""; }
    else cell += ch;
  }
  if (cell || row.length) { row.push(cell); rows.push(row); }
  return rows.filter((r) => r.some((c) => c.trim() !== ""));
}

export function file(path) {
  const today = new Date().toISOString().slice(0, 10);
  if (path.endsWith("/csv/customers")) {
    const lines = [csvLine(["empresa", "contacto", "email", "telefono", "industria", "ciudad", "pais", "estado"]),
      ...[...db.customers].sort((a, b) => a.companyName.localeCompare(b.companyName))
        .map((c) => csvLine([c.companyName, c.contactName, c.email, c.phone, c.industry, c.city, c.country, c.status]))];
    return { name: `clientes-${today}.csv`, type: "text/csv", content: "﻿" + lines.join("\r\n") };
  }
  if (path.endsWith("/csv/deals")) {
    const lines = [csvLine(["id", "titulo", "cliente", "vendedor", "monto_usd", "etapa", "probabilidad", "fecha_cierre_esperada", "creado", "cerrado"]),
      ...db.deals.map((d) => csvLine([d.id, d.title, customer(d.customerId).companyName, rep(d.ownerId)?.name, d.amount, d.stage,
        d.probability, d.expectedCloseDate, d.createdAt.slice(0, 10), d.closedAt?.slice(0, 10)]))];
    return { name: `oportunidades-${today}.csv`, type: "text/csv", content: "﻿" + lines.join("\r\n") };
  }
  return null;
}

// ---------- Enrutador ----------

seedData();

export async function handle(method, fullPath, body) {
  await new Promise((r) => setTimeout(r, 60 + Math.random() * 90)); // latencia realista
  const url = new URL(fullPath, location.origin);
  const p = url.pathname; const q = url.searchParams;
  const id = Number(p.split("/").filter(Boolean).find((s) => /^\d+$/.test(s)));
  const clone = (x) => JSON.parse(JSON.stringify(x));

  // Catálogos
  if (p === "/api/meta") {
    const opt = (list) => list.map(([value, lbl, probability]) => ({ value, label: lbl, probability: probability ?? null }));
    return { stages: opt(STAGES), customerStatuses: opt(STATUSES), leadSources: opt(SOURCES), activityTypes: opt(TYPES),
      webhookEvents: opt(EVENTS), salesReps: db.reps.map((r) => ({ ...r, initials: initials(r.name) })) };
  }
  if (p === "/api/sales-reps") return db.reps.map((r) => ({ ...r, initials: initials(r.name) }));
  if (p === "/api/analytics/dashboard") return dashboard();
  if (p === "/api/integrations/exchange-rates") return { base: "USD", rates: RATES, source: "demo", fetchedAt: new Date().toISOString() };

  // Clientes
  if (p === "/api/customers/all") return [...db.customers].sort((a, b) => a.companyName.localeCompare(b.companyName)).map(customerDto);
  if (p === "/api/customers" && method === "GET") {
    const term = (q.get("q") || "").toLowerCase(); const status = q.get("status");
    let list = db.customers.filter((c) => (!status || c.status === status)
      && [c.companyName, c.contactName, c.email, c.city].some((v) => (v || "").toLowerCase().includes(term)));
    const sort = q.get("sort");
    list = list.sort(sort === "recent" ? (a, b) => b.createdAt.localeCompare(a.createdAt)
      : sort === "city" ? (a, b) => (a.city || "").localeCompare(b.city || "") || a.companyName.localeCompare(b.companyName)
        : (a, b) => a.companyName.localeCompare(b.companyName));
    const page = Number(q.get("page") || 0); const size = Number(q.get("size") || 20);
    return { content: list.slice(page * size, page * size + size).map(customerDto), page, size, totalElements: list.length, totalPages: Math.ceil(list.length / size) };
  }
  if (p === "/api/customers" && method === "POST" || /^\/api\/customers\/\d+$/.test(p) && method === "PUT") {
    validate(body, {
      companyName: required("La empresa es obligatoria"), contactName: required("El contacto es obligatorio"),
      email: (v) => required("El correo es obligatorio")(v) || (/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(v) ? null : "El correo no es válido"),
    });
    const data = { ...body, ownerId: body.ownerId ? Number(body.ownerId) : null, source: body.source || "WEBSITE", status: body.status || "LEAD" };
    if (method === "POST") {
      const c = { id: nextId(), createdAt: iso(new Date()), ...data };
      db.customers.push(c);
      publish("CUSTOMER_CREATED", customerDto(c));
      return customerDto(c);
    }
    const c = customer(id) || fail(404, "Cliente no encontrado");
    Object.assign(c, data);
    return customerDto(c);
  }
  if (/^\/api\/customers\/\d+$/.test(p) && method === "DELETE") {
    if (db.deals.some((d) => d.customerId === id)) fail(422, "No se puede eliminar un cliente con oportunidades asociadas");
    db.customers = db.customers.filter((c) => c.id !== id);
    db.activities = db.activities.filter((a) => a.customerId !== id);
    return null;
  }

  // Oportunidades
  if (p === "/api/deals" && method === "GET") {
    const term = (q.get("q") || "").toLowerCase();
    return db.deals.filter((d) => (!q.get("stage") || d.stage === q.get("stage"))
      && (!q.get("ownerId") || d.ownerId === Number(q.get("ownerId")))
      && (!q.get("customerId") || d.customerId === Number(q.get("customerId")))
      && (d.title.toLowerCase().includes(term) || customer(d.customerId).companyName.toLowerCase().includes(term)))
      .sort((a, b) => b.stageChangedAt.localeCompare(a.stageChangedAt)).map(dealDto);
  }
  if (p === "/api/deals" && method === "POST" || /^\/api\/deals\/\d+$/.test(p) && method === "PUT") {
    validate(body, {
      title: required("El título es obligatorio"), customerId: required("El cliente es obligatorio"),
      amount: (v) => required("El monto es obligatorio")(v) || (Number(v) > 0 ? null : "El monto debe ser mayor que cero"),
    });
    const isNew = method === "POST";
    const d = isNew ? { id: nextId(), stage: "LEAD", probability: 10, createdAt: iso(new Date()), stageChangedAt: iso(new Date()), closedAt: null, lostReason: null }
      : db.deals.find((x) => x.id === id) || fail(404, "Oportunidad no encontrada");
    Object.assign(d, { title: body.title, customerId: Number(body.customerId), ownerId: body.ownerId ? Number(body.ownerId) : null,
      amount: Number(body.amount), expectedCloseDate: body.expectedCloseDate || null });
    if (body.stage && body.stage !== d.stage) {
      const from = d.stage;
      move(d, body.stage);
      if (!isNew) stageEvents(d, from);
    }
    if (isNew) db.deals.push(d);
    return dealDto(d);
  }
  if (/^\/api\/deals\/\d+\/stage$/.test(p)) {
    const d = db.deals.find((x) => x.id === id) || fail(404, "Oportunidad no encontrada");
    if (body.stage === "LOST" && !body.lostReason) fail(422, "Indica el motivo por el que se perdió la oportunidad");
    const from = d.stage;
    move(d, body.stage, body.lostReason);
    stageEvents(d, from);
    return dealDto(d);
  }
  if (/^\/api\/deals\/\d+$/.test(p) && method === "DELETE") {
    db.deals = db.deals.filter((d) => d.id !== id);
    db.activities.forEach((a) => { if (a.dealId === id) a.dealId = null; });
    return null;
  }
  if (/^\/api\/deals\/\d+$/.test(p)) return dealDto(db.deals.find((x) => x.id === id) || fail(404, "Oportunidad no encontrada"));

  // Actividades
  if (p === "/api/activities" && method === "GET") {
    const completed = q.get("completed"); const cid = q.get("customerId");
    return db.activities.filter((a) => (completed === null || String(a.completed) === completed) && (!cid || a.customerId === Number(cid)))
      .sort((a, b) => (a.completed - b.completed) || a.dueAt.localeCompare(b.dueAt)).map(activityDto);
  }
  if (p === "/api/activities" && method === "POST" || /^\/api\/activities\/\d+$/.test(p) && method === "PUT") {
    validate(body, { subject: required("El asunto es obligatorio"), dueAt: required("La fecha es obligatoria") });
    const a = method === "POST" ? { id: nextId(), completed: false, completedAt: null } : db.activities.find((x) => x.id === id);
    const deal = db.deals.find((d) => d.id === Number(body.dealId));
    Object.assign(a, { type: body.type, subject: body.subject, description: body.description, dueAt: body.dueAt,
      dealId: deal?.id ?? null, customerId: body.customerId ? Number(body.customerId) : deal?.customerId ?? null,
      ownerId: body.ownerId ? Number(body.ownerId) : null });
    if (method === "POST") db.activities.push(a);
    return activityDto(a);
  }
  if (/^\/api\/activities\/\d+\/toggle$/.test(p)) {
    const a = db.activities.find((x) => x.id === id);
    a.completed = !a.completed;
    a.completedAt = a.completed ? iso(new Date()) : null;
    return activityDto(a);
  }
  if (/^\/api\/activities\/\d+$/.test(p) && method === "DELETE") {
    db.activities = db.activities.filter((a) => a.id !== id);
    return null;
  }

  // Integraciones
  if (p === "/api/integrations/csv/customers" && method === "POST") return importCsv(String(body || ""));
  if (p === "/api/integrations/webhooks" && method === "GET") return db.hooks.map((h) => hookDto(h));
  if (p === "/api/integrations/webhooks" && method === "POST") {
    validate(body, { name: required("El nombre es obligatorio"),
      targetUrl: (v) => required("La URL es obligatoria")(v) || (/^https?:\/\/.+/.test(v) ? null : "La URL debe empezar por http:// o https://") });
    const h = { id: nextId(), name: body.name, targetUrl: body.targetUrl, event: body.event, active: true, createdAt: iso(new Date()),
      secret: "whsec_" + Array.from(crypto.getRandomValues(new Uint8Array(24)), (b) => b.toString(16).padStart(2, "0")).join("") };
    db.hooks.push(h);
    return hookDto(h, true);
  }
  if (p === "/api/integrations/webhooks/deliveries") return clone(db.deliveries.slice(0, 30));
  if (/\/webhooks\/\d+\/toggle$/.test(p)) { const h = db.hooks.find((x) => x.id === id); h.active = !h.active; return hookDto(h); }
  if (/\/webhooks\/\d+\/test$/.test(p)) {
    const h = db.hooks.find((x) => x.id === id);
    return deliver(h, h.event, { message: "Evento de prueba enviado desde NexoCRM", subscription: h.name }, true);
  }
  if (/\/webhooks\/\d+$/.test(p) && method === "DELETE") {
    db.hooks = db.hooks.filter((h) => h.id !== id);
    db.deliveries = db.deliveries.filter((d) => d.subscriptionId !== id);
    return null;
  }

  fail(404, `Ruta no disponible en modo demo: ${method} ${p}`);
}

function stageEvents(d, from) {
  const dto = dealDto(d);
  publish("DEAL_STAGE_CHANGED", { from, to: d.stage, deal: dto });
  if (d.stage === "WON") publish("DEAL_WON", dto);
  if (d.stage === "LOST") publish("DEAL_LOST", dto);
}

function importCsv(text) {
  const rows = parseCsv(text.replace(/^﻿/, ""));
  if (!rows.length) fail(422, "El archivo está vacío");
  const headers = rows[0].map((h) => h.trim().toLowerCase());
  for (const col of ["empresa", "contacto", "email"]) {
    if (!headers.includes(col)) fail(422, `Falta la columna obligatoria '${col}'`);
  }
  const get = (r, col) => (r[headers.indexOf(col)] ?? "").trim();
  const errors = []; const seen = new Set(); let imported = 0;
  rows.slice(1).forEach((r, i) => {
    const row = i + 2;
    const company = get(r, "empresa"); const contact = get(r, "contacto"); const email = get(r, "email");
    if (!company || !contact || !email) return errors.push({ row, message: "Empresa, contacto y email son obligatorios" });
    if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email)) return errors.push({ row, message: `Email inválido: ${email}` });
    const key = email.toLowerCase();
    if (seen.has(key) || db.customers.some((c) => c.email.toLowerCase() === key)) return errors.push({ row, message: `Ya existe un cliente con el email ${email}` });
    seen.add(key);
    const status = STATUSES.find(([v, l]) => [v.toLowerCase(), l.toLowerCase()].includes(get(r, "estado").toLowerCase()))?.[0] || "LEAD";
    db.customers.push({ id: nextId(), companyName: company, contactName: contact, email, phone: get(r, "telefono") || null,
      industry: get(r, "industria") || null, city: get(r, "ciudad") || null, country: get(r, "pais") || null,
      status, source: "IMPORT", ownerId: null, notes: null, createdAt: iso(new Date()) });
    imported++;
  });
  const total = rows.length - 1;
  return { totalRows: total, imported, skipped: total - imported, errors };
}
