const express = require("express");
const admin = require("firebase-admin");
const axios = require("axios");

// Firebase Admin — credentials via env var (base64 del service account JSON)
const serviceAccount = JSON.parse(
  Buffer.from(process.env.FIREBASE_SERVICE_ACCOUNT_B64 || "", "base64").toString()
);
admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });
const db = admin.firestore();

const MP_ACCESS_TOKEN = process.env.MP_ACCESS_TOKEN;
const PORT = process.env.PORT || 3000;

const app = express();
app.use(express.json());

// ── Middleware: verifica el ID token de Firebase en el header Authorization
async function verifyFirebaseToken(req, res, next) {
  const auth = req.headers.authorization;
  if (!auth?.startsWith("Bearer ")) {
    return res.status(401).json({ error: "Token requerido." });
  }
  try {
    const decoded = await admin.auth().verifyIdToken(auth.split("Bearer ")[1]);
    req.uid = decoded.uid;
    next();
  } catch {
    res.status(401).json({ error: "Token inválido." });
  }
}

// ── POST /createMercadoPagoPreference
// Body: { reservaId: string }
// Header: Authorization: Bearer <firebase_id_token>
app.post("/createMercadoPagoPreference", verifyFirebaseToken, async (req, res) => {
  const { reservaId } = req.body;
  if (!reservaId) {
    return res.status(400).json({ error: "reservaId requerido." });
  }

  const snap = await db.collection("reservas").doc(reservaId).get();
  if (!snap.exists) {
    return res.status(404).json({ error: "Reserva no encontrada." });
  }

  const reserva = snap.data();
  const servicios = reserva.servicios || [];
  const total = Math.max(1, Number(reserva.total) || 0);

  const items =
    servicios.length > 0
      ? servicios.map((s) => ({
          title: String(s.title || "Experiencia BairesEssence"),
          quantity: Math.max(1, Number(s.personas) || 1),
          unit_price: Math.max(1, Number(s.price) || total / servicios.length),
          currency_id: "ARS",
        }))
      : [{ title: "Reserva BairesEssence", quantity: 1, unit_price: total, currency_id: "ARS" }];

  const APP_DOMAIN = process.env.APP_DOMAIN || "https://www.mercadopago.com.ar";
  const WEBHOOK_URL = process.env.MP_WEBHOOK_URL || "";

  const preference = {
    items,
    external_reference: reservaId,
    back_urls: {
      success: `${APP_DOMAIN}/pago/success`,
      failure: `${APP_DOMAIN}/pago/failure`,
      pending: `${APP_DOMAIN}/pago/pending`,
    },
    auto_return: "approved",
    ...(WEBHOOK_URL && { notification_url: WEBHOOK_URL }),
  };

  try {
    const mpRes = await axios.post(
      "https://api.mercadopago.com/checkout/preferences",
      preference,
      { headers: { Authorization: `Bearer ${MP_ACCESS_TOKEN}` } }
    );
    res.json({ preferenceId: mpRes.data.id, initPoint: mpRes.data.init_point });
  } catch (err) {
    console.error("MP API error:", err.response?.data || err.message);
    res.status(500).json({ error: "Error al crear preferencia de pago." });
  }
});

// ── POST /webhook/mercadopago
// Recibe notificaciones IPN de MercadoPago y actualiza el estado de la reserva
app.post("/webhook/mercadopago", async (req, res) => {
  const { type, data } = req.body;
  if (type !== "payment" || !data?.id) {
    return res.status(200).send("OK");
  }

  try {
    const payRes = await axios.get(
      `https://api.mercadopago.com/v1/payments/${data.id}`,
      { headers: { Authorization: `Bearer ${MP_ACCESS_TOKEN}` } }
    );
    const { status: mpStatus, external_reference: reservaId } = payRes.data;
    if (!reservaId) return res.status(200).send("OK");

    const estado =
      mpStatus === "approved" ? "pagada" :
      mpStatus === "rejected" ? "cancelada" : "pendiente";

    await db.collection("reservas").doc(reservaId).update({
      estado,
      mpPaymentId: String(data.id),
      mpStatus,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });
  } catch (err) {
    console.error("Webhook error:", err.message);
  }

  res.status(200).send("OK");
});

// ── POST /createPaypalOrder
// Body: { reservaId: string }
// Header: Authorization: Bearer <firebase_id_token>
app.post("/createPaypalOrder", verifyFirebaseToken, async (req, res) => {
  const { reservaId } = req.body;
  if (!reservaId) return res.status(400).json({ error: "reservaId requerido." });

  const snap = await db.collection("reservas").doc(reservaId).get();
  if (!snap.exists) return res.status(404).json({ error: "Reserva no encontrada." });

  const reserva = snap.data();
  const total = Math.max(1, Number(reserva.total) || 0);

  const PAYPAL_CLIENT_ID = process.env.PAYPAL_CLIENT_ID;
  const PAYPAL_CLIENT_SECRET = process.env.PAYPAL_CLIENT_SECRET;
  const PAYPAL_BASE = process.env.PAYPAL_SANDBOX === "false"
    ? "https://api-m.paypal.com"
    : "https://api-m.sandbox.paypal.com";

  try {
    // 1. Obtener access token
    const tokenRes = await axios.post(
      `${PAYPAL_BASE}/v1/oauth2/token`,
      "grant_type=client_credentials",
      {
        auth: { username: PAYPAL_CLIENT_ID, password: PAYPAL_CLIENT_SECRET },
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
      }
    );
    const accessToken = tokenRes.data.access_token;

    // 2. Crear orden — monto en USD (aproximado; ajustar según moneda real)
    const amountUSD = (total / 1000).toFixed(2);
    const orderRes = await axios.post(
      `${PAYPAL_BASE}/v2/checkout/orders`,
      {
        intent: "CAPTURE",
        purchase_units: [
          {
            reference_id: reservaId,
            amount: { currency_code: "USD", value: amountUSD },
            description: "Reserva BairesEssence",
          },
        ],
        application_context: { user_action: "PAY_NOW" },
      },
      { headers: { Authorization: `Bearer ${accessToken}`, "Content-Type": "application/json" } }
    );

    const approveUrl = orderRes.data.links.find((l) => l.rel === "approve")?.href;
    if (!approveUrl) throw new Error("No se encontró la URL de aprobación de PayPal.");

    res.json({ approveUrl, orderId: orderRes.data.id });
  } catch (err) {
    console.error("PayPal API error:", err.response?.data || err.message);
    res.status(500).json({ error: "Error al crear orden de pago con PayPal." });
  }
});

// ── POST /paypalWebhook
// Recibe evento PAYMENT.CAPTURE.COMPLETED y actualiza estado de reserva
app.post("/paypalWebhook", async (req, res) => {
  const { event_type, resource } = req.body;
  if (event_type === "PAYMENT.CAPTURE.COMPLETED") {
    const reservaId = resource?.purchase_units?.[0]?.reference_id;
    if (reservaId) {
      try {
        await db.collection("reservas").doc(reservaId).update({
          estado: "pagada",
          paypalCaptureId: resource?.id ?? null,
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        });
      } catch (err) {
        console.error("PayPal webhook error:", err.message);
      }
    }
  }
  res.status(200).send("OK");
});

app.listen(PORT, () => console.log(`BairesEssence API listening on port ${PORT}`));
