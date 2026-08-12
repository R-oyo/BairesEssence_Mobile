# BairesEssence — Backend de pagos (Firebase Functions)

API de pagos (MercadoPago + PayPal) desplegada como una única Cloud Function HTTP
(2ª gen, `exports.api`) que corre un app Express. Firebase Hosting expone una URL
estable y reescribe las rutas de la API hacia la función.

- **URL base estable:** `https://baires-essence.web.app`
- **Endpoints:** `/createMercadoPagoPreference`, `/createPaypalOrder`,
  `/webhook/mercadopago`, `/paypalWebhook`
- La app Android lee la URL desde Firestore `config/app.backendUrl` y, si no existe,
  usa el default `https://baires-essence.web.app` (`PagoDetalleScreen.kt`).

## Requisitos previos

- Node 22 y Firebase CLI (ya instalados).
- Sesión iniciada: `firebase login` (ya logueado como ramiro.royo@davinci.edu.ar).
- **Plan Blaze activado** en el proyecto (Functions lo requiere).

## Paso a paso

### 1. Activar el plan Blaze
Consola → https://console.firebase.google.com/project/baires-essence/usage/details
→ *Modificar plan* → **Blaze** (pago por uso). Asociá una tarjeta; el free tier
cubre el uso inicial sin cargo.

### 2. Obtener las credenciales de pago

**MercadoPago** — https://www.mercadopago.com.ar/developers → *Tus integraciones*
→ crear/abrir la app → *Credenciales*. Para probar usá las de **prueba (TEST)**;
para cobrar de verdad, las de **producción**. Copiá el **Access Token**.

**PayPal** — https://developer.paypal.com/dashboard → *Apps & Credentials*
→ **Sandbox** (pruebas) o **Live** (producción) → crear app → copiá **Client ID**
y **Secret**.

### 3. Cargar los secrets en Firebase
```bash
firebase functions:secrets:set MP_ACCESS_TOKEN
firebase functions:secrets:set PAYPAL_CLIENT_ID
firebase functions:secrets:set PAYPAL_CLIENT_SECRET
```
Pegá cada valor cuando lo pida. (Se guardan en Google Secret Manager.)

### 4. (Opcional) Config no sensible
PayPal arranca en **sandbox**. Para pasar a producción, creá `functions/.env`:
```
PAYPAL_SANDBOX=false
```
`APP_DOMAIN` y `MP_WEBHOOK_URL` ya tienen defaults correctos.

### 5. Deploy
```bash
firebase deploy --only functions,hosting
```
Al terminar, verificá que abra: https://baires-essence.web.app

### 6. Registrar los webhooks en los dashboards
- **MercadoPago** → en la app, *Webhooks/Notificaciones IPN* → URL:
  `https://baires-essence.web.app/webhook/mercadopago` (evento: *payments*).
- **PayPal** → app → *Webhooks* → *Add* → URL:
  `https://baires-essence.web.app/paypalWebhook` → evento
  `PAYMENT.CAPTURE.COMPLETED`.

### 7. Probar el flujo
1. Recompilar e instalar la app.
2. Crear una reserva y dejarla en estado `confirmada`.
3. Entrar a la pantalla de pago → probar **MercadoPago** y **PayPal** (con
   credenciales de prueba primero).
4. Confirmado el pago, el webhook actualiza la reserva a `pagada` y la pantalla
   lo detecta en vivo.

## Desarrollo local (emulador)
```bash
firebase emulators:start --only functions,hosting
```
Los secrets locales se toman de `functions/.secret.local` si existe.
