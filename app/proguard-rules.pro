# ── Stack traces legibles en producción ────────────────────────────────────
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Kotlin ──────────────────────────────────────────────────────────────────
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keep class kotlin.Metadata { *; }

# ── Kotlin Coroutines ────────────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** { volatile <fields>; }

# ── Modelos de datos (Firestore reflection) ──────────────────────────────────
# Firestore necesita acceder a los campos via reflection en el mapeo de documentos
-keep class com.example.bairesessence.data.model.** { *; }

# ── PayPal SDK ───────────────────────────────────────────────────────────────
-keep class com.paypal.** { *; }
-dontwarn com.paypal.**

# ── CredentialManager / Google Identity ──────────────────────────────────────
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }

# ── Coil (image loading) ─────────────────────────────────────────────────────
-dontwarn coil.**
