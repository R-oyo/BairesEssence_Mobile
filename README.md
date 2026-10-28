# Baires Essence — Android

> App Android de la plataforma de turismo porteño que conecta viajeros con experiencias auténticas gestionadas por guías y agencias locales de Buenos Aires.

![Kotlin](https://img.shields.io/badge/Kotlin-2.2-7F52FF?logo=kotlin&logoColor=white&style=flat-square)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white&style=flat-square)
![Firebase](https://img.shields.io/badge/Firebase-Firestore%20%2B%20Functions-FFCA28?logo=firebase&logoColor=black&style=flat-square)
![Android](https://img.shields.io/badge/minSdk-24-3DDC84?logo=android&logoColor=white&style=flat-square)

---

## Plataforma

Baires Essence tiene dos clientes que comparten el mismo proyecto Firebase (`baires-essence`):

| Repo | Cliente | Usuarios |
|---|---|---|
| [BairesEssence_Mobile](https://github.com/R-oyo/BairesEssence_Mobile) (este) | App Android | Turistas |
| [proyecto-agencia](https://github.com/GereeOK/proyecto-agencia) | Web React | Turistas, sellers y admins |

Ambos leen y escriben las mismas colecciones de Firestore (`users`, `servicios`, `reservas`, `resenas`, etc.). **Un cambio de esquema en uno de los dos repos tiene que replicarse en el otro.**

Las reglas de Firestore (`firestore.rules`) están duplicadas en ambos repos y hoy son idénticas: si se modifican, hay que actualizar las dos copias. Firebase Hosting se despliega **solo desde este repo**, porque es el que expone la API de pagos (ver [Despliegue](#despliegue)).

---

## Funcionalidades

**Exploración**
- Catálogo por categoría (Tours, Gastronomía, Traslados, Experiencias) con búsqueda en tiempo real y filtro por fechas de viaje
- Detalle de cada experiencia: duración, idioma, ubicación, disponibilidad, qué incluye, reseñas y contacto por WhatsApp
- Mapa de actividades (Google Maps) con filtro por favoritos o reservas
- Paquetes especiales armados por agencia

**Reservas**
- Carrito multi-experiencia con cantidad de personas y fechas
- Pasajeros (nombre y DNI) y grupo familiar guardado en el perfil
- Confirmación en dos pasos: el turista confirma su parte y luego el seller confirma la reserva
- Cancelación de reservas; si la canceló el seller, se muestra su motivo
- Chat por reserva con el equipo del seller
- Email de confirmación al crear la reserva (trigger `onReservaCreada`)
- Prevención de sobreventa y de reservas duplicadas

**Mi cuenta**
- Historial de reservas con su estado, favoritos y perfil con estadísticas
- Reseñas con puntaje por estrellas para reservas finalizadas
- Itinerario personal con línea de tiempo y actividades propias

**Pagos**
- MercadoPago Checkout Pro y PayPal, abiertos en Chrome Custom Tabs
- Backend en Firebase Cloud Functions con webhooks que actualizan el estado de la reserva

**Autenticación**
- Email/contraseña y Google Sign-In (Credential Manager)
- Si la sesión expira, un listener de Firebase Auth redirige al landing

---

## Stack

| Capa | Tecnología |
|---|---|
| Lenguaje | Kotlin 2.2 |
| UI | Jetpack Compose + Material 3 + Material Icons Extended |
| Arquitectura | ViewModel + Navigation Compose |
| Auth | Firebase Auth (email + Google vía Credential Manager) |
| Base de datos | Cloud Firestore |
| Imágenes | Coil |
| Mapas | Maps Compose |
| Pagos | MercadoPago Checkout Pro + PayPal (Chrome Custom Tabs) |
| Backend | Firebase Cloud Functions 2ª gen (Node 22, Express) |

SDK: `minSdk 24`, `targetSdk 35`, `compileSdk 37`.

---

## Instalación

### Requisitos
- Android Studio (versión reciente; incluye el JDK que usa Gradle)
- Un emulador o dispositivo con Android 7.0 (API 24) o superior

### Pasos

```bash
git clone https://github.com/R-oyo/BairesEssence_Mobile
```

1. Abrir el proyecto en Android Studio y dejar que sincronice Gradle.
2. `app/google-services.json` ya está incluido y apunta al proyecto `baires-essence`. Para usar otro proyecto Firebase, reemplazarlo por el propio.
3. Agregar la API key de Google Maps en `local.properties` (no se commitea):
   ```properties
   MAPS_API_KEY=tu_api_key
   ```
   Sin esta key la app funciona, pero el mapa se muestra vacío.
4. Compilar e instalar:
   ```bash
   ./gradlew :app:installDebug
   ```

### URL del backend de pagos

La app usa por defecto `https://baires-essence.web.app` (`PagoDetalleScreen.kt`). Para apuntar a otra URL sin recompilar, crear en Firestore el documento `config/app` con el campo `backendUrl`.

---

## Estados de una reserva

```
pendiente → confirmada_usuario → confirmada → pagada → finalizada
                                      ↓
                                  cancelada
```

| Estado | Significado |
|---|---|
| `pendiente` | Creada; el turista todavía puede editar pasajeros, cantidades y fechas |
| `confirmada_usuario` | El turista confirmó su parte; espera la confirmación del seller |
| `confirmada` | El seller confirmó; se habilita el pago |
| `pagada` | Pago acreditado por webhook |
| `finalizada` | Experiencia realizada; se habilitan las reseñas |
| `cancelada` | Cancelada por el turista o por el seller (el seller puede indicar un motivo) |

---

## Pantallas

| Ruta | Pantalla |
|---|---|
| `splash`, `landing`, `login`, `register` | Arranque y autenticación |
| `home` | Catálogo, búsqueda, filtro por fechas y carrito |
| `detalle/{servicioId}` | Detalle de una experiencia |
| `favoritos` | Experiencias guardadas |
| `mis_reservas`, `reserva_exitosa` | Historial y confirmación de reserva |
| `pago_detalle/{reservaId}` | Pago con MercadoPago o PayPal |
| `chat/{reservaId}` | Chat de la reserva |
| `itinerary` | Itinerario personal |
| `mapa`, `mapa_filtrado/{filter}` | Mapa de actividades |
| `paquetes`, `paquete_detalle/{paqueteId}` | Paquetes por agencia |
| `perfil` | Perfil, grupo familiar y estadísticas |

---

## Backend (Cloud Functions)

El código está en `functions/`. Hay dos funciones:

| Función | Tipo | Descripción |
|---|---|---|
| `api` | HTTP (Express) | Expone las rutas de pago, detalladas abajo |
| `onReservaCreada` | Trigger de Firestore | Envía el email de confirmación al crearse una reserva |

| Ruta de `api` | Descripción |
|---|---|
| `POST /createMercadoPagoPreference` | Crea la preferencia de MercadoPago y devuelve el `initPoint` (requiere token de Firebase) |
| `POST /webhook/mercadopago` | Recibe la notificación de MercadoPago y actualiza el `estado` de la reserva |
| `POST /createPaypalOrder` | Crea la orden de PayPal (requiere token de Firebase) |
| `POST /paypalWebhook` | Recibe la notificación de PayPal y actualiza la reserva |

Secrets (Firebase Secret Manager): `MP_ACCESS_TOKEN`, `PAYPAL_CLIENT_ID`, `PAYPAL_CLIENT_SECRET`, `EMAILJS_PRIVATE_KEY`.

**Estado actual:** el código está migrado a Cloud Functions pero **todavía no está desplegado**. Para desplegarlo hace falta activar el plan **Blaze** en el proyecto, cargar los secrets y registrar los webhooks. El paso a paso completo está en [`functions/README.md`](functions/README.md).

---

## Despliegue

```bash
firebase deploy --only functions,hosting
```

Firebase Hosting reescribe las rutas de la API hacia la función `api`, lo que da una URL estable (`https://baires-essence.web.app`).

> **Importante:** el Hosting se despliega **solo desde este repo**. La web (`proyecto-agencia`) se publica en Vercel y su `firebase.json` solo configura las reglas de Firestore, para que un deploy desde allá no pise las rutas de la API de pagos. Si alguna vez se agrega un bloque `hosting` en el repo web, los pagos de la app dejan de funcionar.

---

## Cuentas de prueba

Se pueden crear cuentas con email/contraseña o iniciar sesión con Google. Las cuentas de prueba con reservas en distintos estados son las mismas de la web (ver el README de [proyecto-agencia](https://github.com/GereeOK/proyecto-agencia#cuentas-de-prueba)). Las contraseñas no se publican en el repo; pedirlas al equipo.

Para probar los pagos se necesitan credenciales de prueba (sandbox) de MercadoPago y PayPal cargadas como secrets.

---

## Licencia

Proyecto académico — uso libre con fines educativos.
