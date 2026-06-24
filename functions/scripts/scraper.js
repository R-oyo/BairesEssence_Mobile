/**
 * Scraper de actividades para BairesEssence.
 * Fuente preferida: Civitatis (API de afiliados — registrarse en civitatis.com/afiliados).
 * Fallback: Puppeteer scraping de la página pública.
 *
 * Uso: cd functions && node scripts/scraper.js
 *
 * Variables de entorno necesarias:
 *   GOOGLE_APPLICATION_CREDENTIALS → ruta al service account JSON de Firebase
 *   CIVITATIS_API_KEY               → API key de afiliados de Civitatis (opcional)
 */

const puppeteer = require("puppeteer");
const admin = require("firebase-admin");

// ── Firebase Admin
if (!admin.apps.length) {
  admin.initializeApp({
    credential: process.env.GOOGLE_APPLICATION_CREDENTIALS
      ? admin.credential.applicationDefault()
      : admin.credential.cert(
          JSON.parse(
            require("fs").readFileSync(
              require("path").resolve(__dirname, "../serviceAccount.json"),
              "utf8"
            )
          )
        ),
  });
}
const db = admin.firestore();

// ── Categoría mapper
function mapCategoria(rawCat = "") {
  const c = rawCat.toLowerCase();
  if (c.includes("tour") || c.includes("excursion") || c.includes("city")) return "Tours";
  if (c.includes("gastro") || c.includes("food") || c.includes("wine") || c.includes("comida")) return "Gastronomia";
  if (c.includes("transfer") || c.includes("traslado") || c.includes("airport")) return "Traslados";
  return "Experiencias";
}

// ── Normaliza una actividad al schema de servicios
function normalizar(act) {
  return {
    title: String(act.title || "").trim(),
    description: String(act.description || "").trim(),
    image: String(act.image || "").trim(),
    categoria: mapCategoria(act.categoria || act.category || ""),
    duracion: String(act.duracion || act.duration || "").trim(),
    ubicacion: String(act.ubicacion || act.location || "Buenos Aires").trim(),
    lat: Number(act.lat) || 0,
    lng: Number(act.lng) || 0,
    precio: Number(act.precio || act.price) || 0,
    activo: true,
  };
}

// ── Upsert con dedup por title + ubicacion
async function upsertActividad(act) {
  const { title, ubicacion } = act;
  if (!title) return;

  const existing = await db
    .collection("servicios")
    .where("title", "==", title)
    .where("ubicacion", "==", ubicacion)
    .get();

  if (existing.empty) {
    await db.collection("servicios").add(act);
    console.log("✅ Agregado:", title);
  } else {
    console.log("⏭  Duplicado, skip:", title);
  }
}

// ── Scraping con Puppeteer (Civitatis Buenos Aires)
async function scrapeCivitatis() {
  const browser = await puppeteer.launch({ headless: "new" });
  const page = await browser.newPage();
  await page.setUserAgent(
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
  );

  const actividades = [];

  try {
    await page.goto(
      "https://www.civitatis.com/es/buenos-aires/",
      { waitUntil: "networkidle2", timeout: 30000 }
    );

    // Extrae tarjetas de actividades de la página de listado
    const items = await page.$$eval(".comfort-card", (cards) =>
      cards.map((card) => ({
        title: card.querySelector(".comfort-card__title")?.textContent?.trim() ?? "",
        description:
          card.querySelector(".comfort-card__description")?.textContent?.trim() ?? "",
        image:
          card.querySelector("img")?.getAttribute("src") ??
          card.querySelector("img")?.getAttribute("data-src") ?? "",
        precio:
          parseFloat(
            (card.querySelector(".comfort-card__price")?.textContent ?? "0")
              .replace(/[^0-9,.]/g, "")
              .replace(",", ".")
          ) || 0,
        duracion:
          card.querySelector(".comfort-card__duration")?.textContent?.trim() ?? "",
        category:
          card.querySelector(".comfort-card__category")?.textContent?.trim() ?? "",
      }))
    );

    actividades.push(...items);
  } catch (err) {
    console.error("Error scraping Civitatis:", err.message);
  }

  await browser.close();
  return actividades;
}

// ── Main
async function main() {
  console.log("🚀 Iniciando scraper BairesEssence...");

  const rawActividades = await scrapeCivitatis();
  console.log(`📦 ${rawActividades.length} actividades encontradas`);

  for (const raw of rawActividades) {
    const act = normalizar(raw);
    await upsertActividad(act);
  }

  console.log("✅ Scraping completo");
  process.exit(0);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
