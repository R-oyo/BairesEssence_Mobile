# Baires Essence — Mobile App (Turistas)

La aplicación móvil de **Baires Essence** es la herramienta principal para los turistas que desean explorar, descubrir y reservar experiencias auténticas en la Ciudad de Buenos Aires.
Está desarrollada en **Kotlin** utilizando **Android Studio**, Firebase y Google Maps API.

---

## 📱 Características Principales

* Exploración de experiencias turísticas con información detallada.
* Búsqueda filtrada por categorías, zonas y tipo de actividad.
* Sistema de **Reserva transaccional (RF03)** con validación de disponibilidad en tiempo real.
* Mapa interactivo integrado mediante **Google Maps API**.
* Inicio de sesión con **Firebase Auth (Google OAuth)**.
* Acceso al historial de reservas.
* Compatible con **Android 10 o superior (RNF04)**.

---

## 🏗️ Arquitectura Técnica

* **Kotlin 100%**
* **Android Studio (última versión estable)**.
* **Arquitectura recomendada:**

  * MVVM + LiveData / StateFlow
  * ViewModels para manejo del estado
  * Repositorios conectados a Firebase
* **Firebase (modular)**

  * Firestore (lectura/escritura y sincronización en tiempo real)
  * Firebase Auth (Google OAuth)
* **Google Maps SDK for Android** para geolocalización.
* **Material Design 3** para una UI moderna y consistente.

---

## 📦 Instalación y Ejecución del Proyecto

Cloná el repositorio:

```bash
git clone https://github.com/R-oyo/BairesEssence_Mobile
cd BairesEssence_Mobile
```

Abrí el proyecto en **Android Studio**:

1. Abrir *Android Studio > Open Project*
2. Seleccionar la carpeta `BairesEssence_Mobile`
3. Esperar a que Gradle sincronice las dependencias automáticamente
4. Ejecutar la app con **Run ▶** en un emulador o dispositivo físico Android

---

## 📱 Requisitos del Sistema

* **Android 10 (API 29)** o superior — *obligatorio por RNF04*
* Google Play Services actualizado
* API key válida para Google Maps

---

## 🗂️ Estructura del Proyecto

```bash
/app
 ├── java/com/bairesessence/       # Código principal (Kotlin)
 │     ├── ui/                      # Activities, Fragments, ViewModels
 │     ├── data/                    # Repositorios y modelos
 │     ├── firebase/                # Integración Firebase (Auth/Firestore)
 │     └── utils/                   # Helpers, validaciones, constantes
 ├── res/                           # Layouts XML, drawables, estilos
 └── AndroidManifest.xml            # Permisos, providers y configuración general
```

---

## 🔐 Configuración de Firebase (Obligatoria)

Colocar el archivo **`google-services.json`** en:

```
app/google-services.json
```

Sin este archivo, la app **no podrá compilar ni autenticarse**.

---

## 🌍 Configuración de Google Maps API

Agregar la API Key en `local.properties` (no versionado):

```properties
MAPS_API_KEY=TU_API_KEY
```

Y en `AndroidManifest.xml`:

```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="${MAPS_API_KEY}" />
```

---

## 📸 UI y Diseño

* Interfaz construida con **Material Design 3**
* Componentes nativos + vistas personalizadas
* Mapas integrados con markers dinámicos para cada experiencia
* Soporte para dark mode según configuración del dispositivo

---

## 🧪 Pruebas y Debugging

* Logging con `Logcat` (Android Studio)
* Pruebas de reserva simulando disponibilidad en Firestore
* Uso de Firebase Emulator Suite (opcional)

---

## 📄 Licencia

Proyecto académico / prototipo funcional — uso libre para fines educativos.

---

Si querés, puedo también hacerte la **versión en inglés**, o un README con **badges** (shields.io) completamente estilizado.
