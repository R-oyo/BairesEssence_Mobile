const { onCall, onRequest, HttpsError } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const admin = require("firebase-admin");
const axios = require("axios");

admin.initializeApp();
const db = admin.firestore();

// Configurar con: firebase functions:secrets:set MP_ACCESS_TOKEN
const mpToken = defineSecret("MP_ACCESS_TOKEN");

// Crea una preferencia de pago en MercadoPago y devuelve el initPoint
exports.createMercadoPagoPreference = onCall(
  { secrets: [mpToken], region: "us-central1" },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "Autenticación requerida.");
    }

    const { reservaId } = request.data;
    if (!reservaId) {
      throw new HttpsError("invalid-argument", "reservaId requerido.");
    }

    const snap = await db.collection("reservas").doc(reservaId).get();
    if (!snap.exists) {
      throw new HttpsError("not-found", "Reserva no encontrada.");
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
        : [
            {
              title: "Reserva BairesEssence",
              quantity: 1,
              unit_price: total,
              currency_id: "ARS",
            },
          ];

    // TODO: Reemplazar APP_DOMAIN con tu dominio de Firebase Hosting después del deploy
    // Ejemplo: "https://baires-essence.web.app"
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
      const res = await axios.post(
        "https://api.mercadopago.com/checkout/preferences",
        preference,
        { headers: { Authorization: `Bearer ${mpToken.value()}` } }
      );
      return {
        preferenceId: res.data.id,
        initPoint: res.data.init_point,
      };
    } catch (err) {
      console.error("MP API error:", err.response?.data || err.message);
      throw new HttpsError("internal", "Error al crear preferencia de pago.");
    }
  }
);

// Webhook IPN: MercadoPago notifica el resultado y actualizamos Firestore
exports.mercadoPagoWebhook = onRequest(
  { secrets: [mpToken], region: "us-central1" },
  async (req, res) => {
    if (req.method !== "POST") {
      res.status(405).send("Method Not Allowed");
      return;
    }

    const { type, data } = req.body;
    if (type !== "payment" || !data?.id) {
      res.status(200).send("OK");
      return;
    }

    try {
      const payRes = await axios.get(
        `https://api.mercadopago.com/v1/payments/${data.id}`,
        { headers: { Authorization: `Bearer ${mpToken.value()}` } }
      );
      const { status: mpStatus, external_reference: reservaId } = payRes.data;
      if (!reservaId) {
        res.status(200).send("OK");
        return;
      }

      const estado =
        mpStatus === "approved"
          ? "pagada"
          : mpStatus === "rejected"
          ? "cancelada"
          : "pendiente";

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
  }
);
