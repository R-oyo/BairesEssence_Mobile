# BairesEssence — Auditoría de código (2026-06-10)

Revisión estática de todos los archivos Kotlin del proyecto. Sin runtime disponible (app Android), los hallazgos son análisis de código, no observaciones en ejecución.

---

## Resumen ejecutivo

| Prioridad | Cantidad | Descripción |
|-----------|----------|-------------|
| P0 — Crítico | 2 | Bloquean la navegación principal |
| P1 — Alto | 2 | Bugs visuales/funcionales en flujos core |
| P2 — Medio | 4 | Errores lógicos o UX confusa |
| P3 — Deuda técnica | 3 | Código muerto o configuración obsoleta |

---

## Bugs encontrados

---

### BUG-01 · P0 · `FavoritosScreen.kt` — Sin BottomBar

**Archivo:** `core/ui/screens/favoritos/FavoritosScreen.kt`

**Descripción:**
`FavoritosScreen` es el **segundo tab del BottomBar** (`favoritos`), pero su `Scaffold` no incluye `bottomBar`. Una vez que el usuario navega ahí desde el BottomBar, no puede cambiar a otro tab — solo puede volver usando la flecha del `TopAppBar`.

**Causa:**
```kotlin
Scaffold(
    topBar = { TopAppBar(...) },
    // falta: bottomBar = { BottomBar(navController) }
    containerColor = BEBackground
)
```

**Impacto:** El usuario queda atrapado en la pantalla. Toda la navegación por tabs se rompe al entrar a Favoritos.

---

### BUG-02 · P0 · `PerfilScreen.kt` — Sin BottomBar

**Archivo:** `core/ui/screens/perfil/PerfilScreen.kt`

**Descripción:**
`PerfilScreen` es el **quinto tab del BottomBar** (`perfil`), pero su `Scaffold` no tiene `bottomBar`. El usuario no puede navegar a otros tabs desde Perfil.

**Causa:**
```kotlin
Scaffold(
    topBar = { TopAppBar(...) },
    // falta: bottomBar = { BottomBar(navController) }
    containerColor = BEBackground
)
```

**Impacto:** Mismo efecto que BUG-01. Navegación por tabs rota al entrar a Perfil.

---

### BUG-03 · P1 · `DetalleScreen.kt` — Overlap visual entre precio y botón

**Archivo:** `core/ui/screens/detalle/DetalleScreen.kt:282`

**Descripción:**
El bottom panel flotante usa un `Box` como contenedor. Cuando `tienePrecio == true`, hay dos hijos directos del Box: un `Column` (precio) sin alignment y un `Button` con `align(Alignment.BottomCenter)`. Box los superpone en lugar de apilarlos verticalmente.

**Causa:**
```kotlin
Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()...) {
    if (s.tienePrecio) {
        Column { /* precio: ~70dp, posicionado en TopStart */ }
    }
    Button(
        modifier = Modifier...align(Alignment.BottomCenter)  // se superpone al Column
    )
}
```
El Box tiene altura `max(column_height, 52dp)`. El Column ocupa desde arriba y el Button desde abajo, solapándose visualmente.

**Impacto:** El precio ("Desde $X por persona") queda cubierto parcialmente o totalmente por el botón "Agregar al carrito".

---

### BUG-04 · P1 · `PagosScreen.kt` — `scope.launch` anidado dentro de `LaunchedEffect`

**Archivo:** `core/ui/screens/pagos/PagosScreen.kt:33`

**Descripción:**
`LaunchedEffect` ya ejecuta en una coroutine. Lanzar `scope.launch {}` adentro crea una coroutine anidada cuyo ciclo de vida no está ligado al `LaunchedEffect`, sino al `rememberCoroutineScope()` (vivo mientras el composable está en el backstack). Si el composable se destruye antes de que termine la carga, la inner coroutine sigue corriendo.

**Causa:**
```kotlin
LaunchedEffect(user?.uid) {
    if (user?.uid != null) {
        scope.launch {          // incorrecto — LaunchedEffect ya es una coroutine
            val snap = db.collection("reservas")...get().await()
            reservas = ...
            cargando = false
        }
    } else { cargando = false }  // este else SOLO corre cuando user == null,
}                                // no cuando el scope.launch termina con error
```

**Impacto:** Loading infinito si falla la query (el `else` no cubre ese caso). Posible write en composable destruido.

---

### BUG-05 · P2 · `MisReservasScreen.kt` — `canReview` habilita reseña en reservas "confirmadas"

**Archivo:** `core/ui/screens/reservas/MisReservasScreen.kt:112`

**Descripción:**
El botón "Dejar reseña" se muestra para reservas con estado `"confirmada"` (aprobadas pero no pagadas ni completadas). Un usuario no debería poder reseñar una experiencia que aún no vivió.

**Causa:**
```kotlin
val canReview = estado in listOf("confirmada", "pagada", "finalizada")
// Debería ser solo: listOf("pagada", "finalizada")
```

**Impacto:** El usuario puede enviar reseñas de experiencias no realizadas, contaminando las calificaciones.

---

### BUG-06 · P2 · `MisReservasScreen.kt` — Bug de índice en `PasajerosDialog` con servicios duplicados

**Archivo:** `core/ui/screens/reservas/MisReservasScreen.kt:391`

**Descripción:**
Al confirmar cambios en `PasajerosDialog`, se usa `servicios.indexOf(svc)` para recuperar el número de personas. Si una reserva tiene el mismo servicio dos veces (misma estructura de mapa), `indexOf` retorna siempre el **primer índice** encontrado, ignorando el índice real del elemento iterado.

**Causa:**
```kotlin
val actualizados = servicios.filterIndexed { i, _ -> i !in eliminados }
    .mapIndexed { _, svc ->
        val i = servicios.indexOf(svc)  // bug: retorna primer índice si hay duplicados
        svc.toMutableMap().apply {
            put("personas", personasPorServicio.getOrElse(i) { 1 }.toLong())
        }
    }
```

**Impacto:** Si se reserva el mismo servicio para grupos distintos, al editar pasajeros se asignan mal las cantidades.

---

### BUG-07 · P2 · `ReservasViewModel.kt` — Mutaciones silenciosas sin feedback al usuario

**Archivo:** `core/ui/screens/reservas/ReservasViewModel.kt:48-91`

**Descripción:**
`cancelarReserva`, `actualizarPasajeros` y `actualizarFechas` usan `runCatching` pero solo reaccionan en `.onSuccess`. Si Firestore devuelve un error (sin conexión, reglas de seguridad, etc.), el ViewModel no emite ningún estado de error y el usuario no recibe feedback.

**Causa:**
```kotlin
runCatching { FirestoreRepository.cancelarReserva(reservaId) }.onSuccess {
    _reservas.update { ... }
}
// falta .onFailure { _error.value = "No se pudo cancelar." }
```

**Impacto:** El usuario hace tap en "Cancelar reserva", el dialog desaparece, y nada cambia — sin saber si funcionó o no.

---

### BUG-08 · P2 · `PagoDetalleScreen.kt` — Navega a `"pagos"` en lugar de `"mis_reservas"` al confirmar pago

**Archivo:** `core/ui/screens/pagos/PagoDetalleScreen.kt:115`

**Descripción:**
Cuando el pago se confirma (estado `"pagada"`), el botón "Ver mis reservas" navega a la ruta `"pagos"` (`PagosScreen`). Pero el tab "Reservas" del BottomBar apunta a `"mis_reservas"` (`MisReservasScreen`). Esto deja el BottomBar sin ningún tab activo seleccionado.

**Causa:**
```kotlin
PagoExitosoContent(
    onVerReservas = {
        navController.navigate("pagos") {   // debería ser "mis_reservas"
            popUpTo("pagos") { inclusive = true }
        }
    }
)
```

**Impacto:** Confusión visual; ningún tab del BottomBar aparece seleccionado al llegar a la pantalla post-pago.

---

### BUG-09 · P3 · `TopBar.kt` — Componente sin usar (dead code)

**Archivo:** `core/ui/components/TopBar.kt`

**Descripción:**
El composable `TopBar` está definido pero no es importado ni usado en ningún archivo del proyecto. Todas las pantallas usan `TopAppBar` de Material3 directamente.

**Impacto:** Ninguno funcional. Ruido en el codebase.

---

### BUG-10 · P3 · `Reserva.kt` — Data class `Reserva` sin usar

**Archivo:** `data/model/Reserva.kt:18-29`

**Descripción:**
La data class `Reserva` está definida pero nunca se instancia. Todas las pantallas trabajan con `Map<String, Any>` para deserializar reservas de Firestore.

**Impacto:** Ninguno funcional. La clase tipada podría ser valiosa pero actualmente es dead code.

---

### BUG-11 · P3 · `build.gradle.kts` — `composeOptions` deprecado con Kotlin Compose plugin 2.x

**Archivo:** `app/build.gradle.kts:48`

**Descripción:**
Con `id("org.jetbrains.kotlin.plugin.compose") version "2.2.21"`, el bloque `composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }` es ignorado — la versión del compilador la controla el plugin directamente.

**Impacto:** Ninguno en compilación. Puede ocultar incompatibilidades a futuro.

---

## Plan de corrección

Las correcciones están ordenadas por impacto. P0 y P1 primero.

---

### Sprint 1 — Críticos (BUG-01, BUG-02, BUG-03, BUG-04)

#### Fix BUG-01 — Agregar BottomBar a FavoritosScreen

```kotlin
// core/ui/screens/favoritos/FavoritosScreen.kt
Scaffold(
    topBar = { TopAppBar(...) },
    bottomBar = { BottomBar(navController) },  // agregar esta línea
    containerColor = BEBackground
)
```

#### Fix BUG-02 — Agregar BottomBar a PerfilScreen

```kotlin
// core/ui/screens/perfil/PerfilScreen.kt
Scaffold(
    topBar = { TopAppBar(...) },
    bottomBar = { BottomBar(navController) },  // agregar esta línea
    containerColor = BEBackground
)
```

#### Fix BUG-03 — Reemplazar Box por Column en bottom panel de DetalleScreen

```kotlin
// core/ui/screens/detalle/DetalleScreen.kt:282
// Cambiar Box(...) por Column(...) para que precio y botón se apilen:
Column(
    modifier = Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth()
        .background(BESurface)
        .padding(16.dp)
) {
    if (s.tienePrecio) {
        Text("Desde", style = MaterialTheme.typography.labelSmall, color = BETextSecond)
        Text(s.precioFormateado, style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold, color = BETextPrimary)
        Text("por persona", style = MaterialTheme.typography.labelSmall, color = BETextSecond)
        Spacer(Modifier.height(8.dp))
    }
    if (!enCarrito) {
        Button(modifier = Modifier.fillMaxWidth().height(52.dp), ...) { ... }
    } else {
        OutlinedButton(modifier = Modifier.fillMaxWidth().height(52.dp), ...) { ... }
    }
}
```

#### Fix BUG-04 — Quitar scope.launch anidado en PagosScreen

```kotlin
// core/ui/screens/pagos/PagosScreen.kt:33
LaunchedEffect(user?.uid) {
    if (user?.uid != null) {
        // Sin scope.launch — LaunchedEffect ya corre en coroutine
        try {
            val snap = db.collection("reservas")
                .whereEqualTo("userId", user.uid).get().await()
            reservas = snap.documents.mapNotNull { doc ->
                val d = doc.data?.toMutableMap() ?: return@mapNotNull null
                if (!d.containsKey("estado")) d["estado"] = "pendiente"
                d["id"] = doc.id
                d
            }
        } catch (e: Exception) { e.printStackTrace() }
    }
    cargando = false  // siempre se ejecuta, sin importar el resultado
}
```

---

### Sprint 2 — Funcionales (BUG-05, BUG-06, BUG-07, BUG-08)

#### Fix BUG-05 — Restringir canReview

```kotlin
// core/ui/screens/reservas/MisReservasScreen.kt:112
val canReview = estado in listOf("pagada", "finalizada")
```

#### Fix BUG-06 — Corregir índice en PasajerosDialog

```kotlin
// core/ui/screens/reservas/MisReservasScreen.kt:391
// Construir mapa de índice original antes de filtrar
val indicesValidos = servicios.indices.filter { i -> i !in eliminados }

val actualizados = indicesValidos.map { originalIdx ->
    servicios[originalIdx].toMutableMap().apply {
        put("personas", personasPorServicio.getOrElse(originalIdx) { 1 }.toLong())
    }
}
```

#### Fix BUG-07 — Agregar StateFlow de error en ReservasViewModel

```kotlin
// core/ui/screens/reservas/ReservasViewModel.kt
private val _mutacionError = MutableStateFlow<String?>(null)
val mutacionError: StateFlow<String?> = _mutacionError.asStateFlow()

fun cancelarReserva(reservaId: String) {
    viewModelScope.launch {
        runCatching { FirestoreRepository.cancelarReserva(reservaId) }
            .onSuccess { _reservas.update { lista -> lista.map { r ->
                if (r["id"] == reservaId) r.toMutableMap().apply { put("estado", "cancelada") } else r
            }}}
            .onFailure { _mutacionError.value = "No se pudo cancelar la reserva. Intentá de nuevo." }
    }
}
// Mismo patrón para actualizarPasajeros y actualizarFechas

// En MisReservasScreen: observar mutacionError y mostrar Snackbar
val snackbarHostState = remember { SnackbarHostState() }
val mutacionError by vm.mutacionError.collectAsState()
LaunchedEffect(mutacionError) {
    mutacionError?.let {
        snackbarHostState.showSnackbar(it)
        vm.clearMutacionError()
    }
}
```

#### Fix BUG-08 — Corregir navegación post-pago

```kotlin
// core/ui/screens/pagos/PagoDetalleScreen.kt:115
estado == "pagada" -> PagoExitosoContent(
    onVerReservas = {
        navController.navigate("mis_reservas") {
            popUpTo(0) { inclusive = true }
        }
    }
)
```

---

### Sprint 3 — Deuda técnica (BUG-09, BUG-10, BUG-11)

#### Fix BUG-09 — Eliminar TopBar.kt
Borrar `core/ui/components/TopBar.kt` (ningún archivo lo importa).

#### Fix BUG-10 — Resolver Reserva data class
- Opción A (inmediata): Eliminar `data class Reserva` de `Reserva.kt`
- Opción B (mejor a futuro): Migrar `fetchReservasByUser` y pantallas para devolver `List<Reserva>` con tipado seguro

#### Fix BUG-11 — Eliminar composeOptions en build.gradle.kts
```kotlin
// Eliminar este bloque (ignorado con el plugin 2.x):
composeOptions {
    kotlinCompilerExtensionVersion = "1.5.14"
}
```

---

## Checklist de correcciones

- [x] BUG-01 · BottomBar en FavoritosScreen
- [x] BUG-02 · BottomBar en PerfilScreen
- [x] BUG-03 · Layout bottom panel DetalleScreen (Box → Column)
- [x] BUG-04 · scope.launch anidado en PagosScreen
- [x] BUG-05 · canReview solo para pagada/finalizada
- [x] BUG-06 · Índice original en PasajerosDialog
- [x] BUG-07 · Error feedback en mutaciones del ViewModel
- [x] BUG-08 · Navegación post-pago a mis_reservas
- [x] BUG-09 · Eliminar TopBar.kt
- [x] BUG-10 · Resolver Reserva data class
- [x] BUG-11 · Eliminar composeOptions deprecado
