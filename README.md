# 📱 Baires Essence — Android

**Baires Essence Mobile** es la app Android de la plataforma de turismo porteño que conecta viajeros con experiencias auténticas gestionadas por guías locales de Buenos Aires.

## Stack Tecnológico

La app está construida con **Kotlin + Jetpack Compose** (Material 3) para la interfaz, **Firebase Auth + Firestore** para autenticación y base de datos en tiempo real, y **Navigation Compose** con **ViewModel** para la arquitectura. Los pagos se procesan con **MercadoPago Checkout Pro** usando **Firebase Cloud Functions** como backend seguro y **Chrome Custom Tabs** para el checkout.

## Funcionalidades

**Exploración y reservas:** Catálogo de experiencias con filtros por categoría y búsqueda en tiempo real, detalle con galería y reseñas, carrito multi-experiencia con selector de personas y fechas, y flujo de confirmación de reserva.

**Mi cuenta:** Historial de reservas con estados (pendiente / confirmada / pagada / cancelada), favoritos sincronizados por usuario en Firestore, perfil con estadísticas de reservas y sistema de reseñas con puntaje por estrellas.

**Itinerario personal:** Planificador de viaje con línea de tiempo visual, creación de actividades con hora y descripción, almacenamiento en tiempo real en Firestore por usuario.

**Pagos con MercadoPago:** Checkout Pro integrado vía Chrome Custom Tab, Cloud Functions para creación segura de preferencias de pago, webhook IPN que actualiza el estado de la reserva automáticamente en Firestore.

## Autenticación

Soporte para email/contraseña y **Google Sign-In**. Un listener de Firebase Auth redirige automáticamente al landing si el token expira, con protección de rutas en toda la navegación.

## Cloud Functions

| Función | Descripción |
|---|---|
| `createMercadoPagoPreference` | Crea la preferencia de pago y devuelve el `initPoint` |
| `mercadoPagoWebhook` | Recibe el IPN de MercadoPago y actualiza `estado` en Firestore |

## Accesos de prueba

Se pueden crear cuentas con email/contraseña o iniciar sesión con Google. Para probar el flujo de pagos se necesita un Access Token de MercadoPago en modo sandbox configurado en Firebase Secrets (`MP_ACCESS_TOKEN`).
