const { onRequest } = require("firebase-functions/v2/https");
const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { defineSecret, defineString } = require("firebase-functions/params");
const admin = require("firebase-admin");
const express = require("express");
const axios = require("axios");

// En Cloud Functions las credenciales de Admin salen del service account
// del entorno (Application Default Credentials); no hace falta cargar un JSON.
admin.initializeApp();
const db = admin.firestore();

// ── Secrets (Google Secret Manager). Se cargan con:
//    firebase functions:secrets:set MP_ACCESS_TOKEN   (idem PAYPAL_*)
const MP_ACCESS_TOKEN = defineSecret("MP_ACCESS_TOKEN");
const PAYPAL_CLIENT_ID = defineSecret("PAYPAL_CLIENT_ID");
const PAYPAL_CLIENT_SECRET = defineSecret("PAYPAL_CLIENT_SECRET");
// Opcional: solo hace falta si la cuenta de EmailJS tiene "API calls" en
// modo estricto (ver nota en onReservaCreada más abajo).
const EMAILJS_PRIVATE_KEY = defineSecret("EMAILJS_PRIVATE_KEY");

// ── Config no sensible (parámetros con default; se pueden fijar en deploy).
const PAYPAL_SANDBOX = defineString("PAYPAL_SANDBOX", { default: "true" });
const APP_DOMAIN = defineString("APP_DOMAIN", { default: "https://www.mercadopago.com.ar" });
const MP_WEBHOOK_URL = defineString("MP_WEBHOOK_URL", {
  default: "https://baires-essence.web.app/webhook/mercadopago",
});

// EmailJS: vacíos por defecto -> onReservaCreada no manda nada hasta que
// se configuren (ver instrucciones de deploy al final del archivo).
const EMAILJS_SERVICE_ID = defineString("EMAILJS_SERVICE_ID", { default: "" });
const EMAILJS_TEMPLATE_ID = defineString("EMAILJS_TEMPLATE_ID", { default: "" });
const EMAILJS_PUBLIC_KEY = defineString("EMAILJS_PUBLIC_KEY", { default: "" });

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

  const appDomain = APP_DOMAIN.value();
  const webhookUrl = MP_WEBHOOK_URL.value();

  const preference = {
    items,
    external_reference: reservaId,
    back_urls: {
      success: `${appDomain}/pago/success`,
      failure: `${appDomain}/pago/failure`,
      pending: `${appDomain}/pago/pending`,
    },
    auto_return: "approved",
    ...(webhookUrl && { notification_url: webhookUrl }),
  };

  try {
    const mpRes = await axios.post(
      "https://api.mercadopago.com/checkout/preferences",
      preference,
      { headers: { Authorization: `Bearer ${MP_ACCESS_TOKEN.value()}` } }
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
      { headers: { Authorization: `Bearer ${MP_ACCESS_TOKEN.value()}` } }
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

  const paypalBase = PAYPAL_SANDBOX.value() === "false"
    ? "https://api-m.paypal.com"
    : "https://api-m.sandbox.paypal.com";

  try {
    // 1. Obtener access token
    const tokenRes = await axios.post(
      `${paypalBase}/v1/oauth2/token`,
      "grant_type=client_credentials",
      {
        auth: { username: PAYPAL_CLIENT_ID.value(), password: PAYPAL_CLIENT_SECRET.value() },
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
      }
    );
    const accessToken = tokenRes.data.access_token;

    // 2. Crear orden — monto en USD (aproximado; ajustar según moneda real)
    const amountUSD = (total / 1000).toFixed(2);
    const orderRes = await axios.post(
      `${paypalBase}/v2/checkout/orders`,
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

// Exporta el Express app como una única Cloud Function HTTP (2ª gen).
exports.api = onRequest(
  {
    region: "us-central1",
    secrets: [MP_ACCESS_TOKEN, PAYPAL_CLIENT_ID, PAYPAL_CLIENT_SECRET],
  },
  app
);

// ── Email de confirmación — dispara con cualquier reserva nueva, sin
// importar si la creó la web o el móvil (antes cada plataforma tenía su
// propia lógica de email del lado cliente y ninguna corría en
// producción). Si faltan los parámetros EMAILJS_*, no hace nada: es
// seguro deployar esto ya, no manda nada hasta que se configure.
//
// Para activarlo:
//   firebase deploy --only functions \
//     --set-env-vars EMAILJS_SERVICE_ID=service_xxx,EMAILJS_TEMPLATE_ID=template_xxx,EMAILJS_PUBLIC_KEY=xxx
//   (si la cuenta de EmailJS tiene "API calls" en modo estricto, además:
//    firebase functions:secrets:set EMAILJS_PRIVATE_KEY)
//
// El template de EmailJS debe aceptar: fullname, email, checkin,
// checkout, servicios (títulos separados por coma).
exports.onReservaCreada = onDocumentCreated(
  { document: "reservas/{reservaId}", region: "us-central1", secrets: [EMAILJS_PRIVATE_KEY] },
  async (event) => {
    const serviceId = EMAILJS_SERVICE_ID.value();
    const templateId = EMAILJS_TEMPLATE_ID.value();
    const publicKey = EMAILJS_PUBLIC_KEY.value();

    if (!serviceId || !templateId || !publicKey) {
      console.log("EmailJS no configurado — se omite el email de confirmación.");
      return;
    }

    const reserva = event.data?.data();
    if (!reserva?.email) return;

    const privateKey = EMAILJS_PRIVATE_KEY.value();

    try {
      await axios.post("https://api.emailjs.com/api/v1.0/email/send", {
        service_id: serviceId,
        template_id: templateId,
        user_id: publicKey,
        ...(privateKey && { accessToken: privateKey }),
        template_params: {
          fullname: reserva.fullname || "",
          email: reserva.email,
          checkin: reserva.checkin || "",
          checkout: reserva.checkout || "",
          servicios: (reserva.servicios || []).map((s) => s.title).filter(Boolean).join(", "),
        },
      });
      console.log(`✅ Email de confirmación enviado para la reserva ${event.params.reservaId}`);
    } catch (err) {
      console.error("Error enviando email de confirmación:", err.response?.data || err.message);
    }
  }
);
