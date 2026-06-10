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

app.listen(PORT, () => console.log(`BairesEssence API listening on port ${PORT}`));
