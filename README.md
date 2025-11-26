# Baires Essence — Mobile App (Turistas)

La aplicación móvil de **Baires Essence** es la plataforma principal donde los turistas descubren, reservan y gestionan experiencias turísticas en Buenos Aires.
Está desarrollada en **Android Studio (Java/Kotlin)** y funciona conectada a Firebase para autenticación, gestión de datos y reservas en tiempo real.

---

## 🚀 Características Principales

* Exploración completa de experiencias turísticas en Buenos Aires.
* Sistema de reservas con conexión en vivo a Firestore.
* Pantalla de detalle con precio, descripción, fotos y disponibilidad.
* Autenticación mediante **Firebase Auth (Google OAuth)**.
* Sección de perfil del usuario con reservas activas.
* Formulario de contacto para consultas.
* Carga dinámica de imágenes desde Firebase Storage.

---

## 🏗️ Arquitectura Técnica

### 🧱 Android Studio

Proyecto nativo con estructura modular adaptable a Java o Kotlin.

### 🔥 Firebase Modular (v9+)

* **Auth** → Login con Google, manejo de sesiones y persistencia local.
* **Firestore** → Colecciones de experiencias, reservas y usuarios.
* **Storage** → Imágenes de experiencias y multimedia.

### 🧭 Navegación

* Actividades y Fragments organizados por vistas.
* Patrón MVVM opcional para mantener bajo acoplamiento.

---

## 📦 Instalación y Configuración

1. Clonar el repositorio del proyecto:

```bash
git clone https://github.com/GereeOK/proyecto-agencia
```

2. Abrir el directorio del proyecto en **Android Studio**.

3. Configurar Firebase (automáticamente mediante Firebase Assistant o manualmente).

4. Añadir el archivo `google-services.json` en:

```
/app
```

---

## 📚 Dependencias Principales (build.gradle)

```gradle
implementation 'com.google.firebase:firebase-auth'
implementation 'com.google.firebase:firebase-firestore'
implementation 'com.google.firebase:firebase-storage'
implementation 'com.google.android.material:material:1.12.0'
implementation 'androidx.recyclerview:recyclerview:1.3.2'
implementation 'androidx.cardview:cardview:1.0.0'
```

---

## 🧰 Estructura del Proyecto

```
/app/src/main/java/com/bairesessence
 ├── adapters/           # Adaptadores para listas (RecyclerView)
 ├── models/             # Clases de datos (Experiencia, Usuario, Reserva)
 ├── ui/
 │   ├── home/           # Lista de experiencias
 │   ├── detail/         # Detalle de experiencia
 │   ├── profile/        # Perfil del usuario
 │   └── auth/           # Pantalla de login (Google)
 ├── utils/              # Helpers, formateadores, validaciones
 └── MainActivity.java   # Control principal de navegación
```

---

## 🎨 Diseño UI

* Basado en Material Design 3 (MD3).
* Uso de `CardView`, `RecyclerView`, `ConstraintLayout` y `BottomNavigationView`.
* Paleta moderna y minimalista orientada al sector turístico.

---

## 🔐 Variables de Entorno (Firebase)

No se usan `.env` en Android, sino que la configuración está en:

```
app/google-services.json
```

Incluye:

* api_key
* auth_domain
* project_id
* storage_bucket
* messaging_sender_id
* app_id

---

## ✔️ Funcionalidades Integradas con la Web App

* Las experiencias se gestionan desde el **Panel Web (React)**.
* Las reservas generadas desde la app móvil se reflejan al instante en el dashboard.
* Los cambios de disponibilidad se sincronizan en tiempo real.

---

## 📄 Licencia

Proyecto académico / prototipo funcional — uso libre para fines educativos.

---

Si querés también puedo hacerte **una versión más breve, una más técnica**, o incluso una **versión pensada para portfolio**.
