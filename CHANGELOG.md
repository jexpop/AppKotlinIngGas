# Changelog - AppKotlinIngGas

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.0.9] - 2026-07-10

### Fixed
- **Filtro "Sin categoría" en Movimientos** (`data/repository/TransactionViewRepository.kt`): El filtro `onlyUncategorized` se aplicaba en cliente **después** de que Supabase ya había recortado los resultados con `range(offset, offset+limit-1)`. Esto provocaba que transacciones con `group_id = NULL` fuera de la página actual (paginación) nunca se descargaran ni, por tanto, se mostraran, aunque cumplieran la condición.
  - `getByFilters()`: el filtro ahora se aplica en servidor mediante `exact("group_id", null)` dentro del bloque `filter { }`, junto al resto de condiciones, antes de `range()`. `exact()` es el método nativo de `postgrest-kt` 3.1.4 para generar `column=is.null` (no existe `isNull()` ni hace falta `FilterOperator` en esta versión). Se elimina el `.let { transactions -> transactions.filter { it.groupId == null } }` posterior al `decodeList`.

### Added
- **Contador de movimientos** (`ui/screens/transactions/TransactionsScreen.kt`): Al pie de la lista de Movimientos se muestra el número de transacciones actualmente cargadas para el filtro activo (ej. "23 movimientos"), como ayuda para detectar visualmente resultados inesperados de un filtro.
  - Nuevo `Text` centrado tras el `LazyColumn`, separado por `HorizontalDivider()`.
  - `LazyColumn` cambia de `Modifier.fillMaxSize()` a `Modifier.weight(1f)` para dejar espacio fijo al contador dentro del `Column` padre.
  - Usa `pluralStringResource(R.plurals.transactions_count, ...)` con singular/plural.
  - **Nota**: al haber paginación (`hasMore`/`loadMore`), el contador refleja los movimientos **cargados hasta el momento**, no el total real en BD que cumple el filtro. Si se carga solo la primera página, el número puede ser menor que el total real.

### Strings (`strings.xml`)
- Nuevo `<plurals name="transactions_count">`: `"%d movimiento"` (one) / `"%d movimientos"` (other).

### Changed
- `app/build.gradle.kts`: versión de app actualizada a `1.0.9` (`versionCode = 9`).

---

## [1.0.8] - 2026-07-10

### Added
- **Botón copiar concepto en detalles de Movimiento** (`ui/screens/transactions/TransactionsScreen.kt`): Al expandir un movimiento, se muestra el concepto completo con un botón de copia junto a él. Al hacer click, copia el concepto al portapapeles y muestra confirmación con Toast.
  - Nueva fila en panel expandido con concepto + `IconButton` con icono `ContentCopy`
  - `ClipboardManager` copia texto al clipboard
  - Toast de confirmación: "Concepto copiado al portapapeles"
  - Útil para minimizar errores al crear reglas de categorización manualmente

### Changed
- `app/build.gradle.kts`: versión de app actualizada a `1.0.8` (`versionCode = 8`).

---

## [1.0.7] - 2026-07-10

### Added
- **Filtro de categoría en Movimientos** (`ui/screens/transactions/`): Nuevo filtro "Sin categoría" para visualizar rápidamente transacciones sin categorizar.
  - `TransactionsViewModel.kt`: Nuevo estado `selectedCategoryFilter` + método `selectCategoryFilter()`
  - `TransactionViewRepository.kt`: Parámetro `onlyUncategorized` en `getByFilters()` que filtra transacciones donde `group_id IS NULL`
  - `TransactionsScreen.kt`: Nuevo `FilterChip` en `FiltersPanel` para seleccionar "Sin categoría"
  - `strings.xml`: Nuevas claves `transactions_payment_type`, `transactions_category`, `transactions_uncategorized_filter`

### Changed
- `app/build.gradle.kts`: versión de app actualizada a `1.0.7` (`versionCode = 7`).

---

## [1.0.6] - 2026-07-10

### Fixed
- **CategorizationUseCase - Reglas tipo 4 y 7** (`domain/usecase/CategorizationUseCase.kt`): Corregido error en extracción de posiciones 18-30 del concepto. Antes usaba `substring(17, minOf(30, concept.length))` que fallaba o truncaba. Ahora usa `substring(17, minOf(31, concept.length))` para capturar correctamente hasta posición 30 (numeración de usuario).
- **CategorizationUseCase - Regla tipo 6** (`domain/usecase/CategorizationUseCase.kt`): Agregada validación `if (concept.length < 20) return false` para evitar falsos positivos cuando el concepto tiene menos de 20 caracteres. Garantiza que la regla solo se aplica si hay suficientes caracteres para extraer los primeros 20.

### Changed
- `app/build.gradle.kts`: versión de app actualizada a `1.0.6` (`versionCode = 6`).

---

## [1.0.5] - 2026-07-09

### Fixed
- **Refresco automático de Movimientos**: tras una importación correcta, la pantalla de Movimientos se recarga sola sin necesidad de cerrar y volver a abrir la app.

### Changed
- `app/build.gradle.kts`: versión de app actualizada a `1.0.5` (`versionCode = 5`).

---

## [1.0.4] - 2026-07-09

### Changed
- **Listado de Movimientos** (`TransactionsScreen.kt`): al tocar un movimiento se expande para mostrar la descripción completa y datos extra; solo puede haber un movimiento expandido a la vez.
- La fila compacta de Movimientos mantiene los iconos visuales de cuenta/tarjeta.
- **Ajustes** (`SettingsScreen.kt`): la versión mostrada ahora se obtiene de `BuildConfig.VERSION_NAME` en vez de estar escrita a mano.
- `app/build.gradle.kts`: versión de app actualizada a `1.0.4` (`versionCode = 4`).

---

## [1.0.3] - 2026-07-09

### Added
- **Sincronización de salt de cifrado entre dispositivos** (`EncryptionManager.kt`): El salt PBKDF2 ahora se sincroniza con Supabase (`app_param: ENCRYPTION/SALT`) para permitir descifrar backups en múltiples dispositivos.
- `EncryptionManager.uploadSaltToSupabase(context)`: sube el salt en Base64 a `app_param`.
- `EncryptionManager.downloadSaltFromSupabase(context)`: descarga el salt si no existe localmente.
- `EncryptionManager.initializeSaltIfNeeded(context)`: genera salt aleatorio solo si no existe.
- `MainActivity.kt`: descarga automática del salt al arrancar si el usuario está autenticado y no tiene salt local.

### Changed
- `SettingsViewModel.saveEncryptionPassword()`: antes de guardar la contraseña, intenta descargar el salt existente de Supabase. Solo genera uno nuevo si no existe en ningún sitio.
- `EncryptionManager.savePassword()`: ya no genera salt (responsabilidad movida a `initializeSaltIfNeeded`).

### Security
- El salt almacenado en Supabase (`app_param`) está protegido por RLS (whitelist de emails). Sin la contraseña maestra el salt no permite descifrar nada.

---

## [1.0.2] - 2025-07-07

### Fixed
- **Selector de mes/banco en filtros de Transacciones** (`TransactionsScreen.kt`): el `ExposedDropdownMenuBox` no respondía correctamente al pulsar. Reemplazado por el patrón `OutlinedButton` + `AlertDialog`.
- Import incorrecto de `TextOverflow` (`androidx.compose.ui.text.TextOverflow` → `androidx.compose.ui.text.style.TextOverflow`).

### Added
- Strings `transactions_select_month` y `transactions_select_bank` (`strings.xml`).

---

## [1.0.1] - 2025-07-06

### Security
- **Fixed fixed salt in PBKDF2** (`EncryptionManager.kt`): Now generates unique random 16-byte salt per user on password setup, stored in EncryptedSharedPreferences. Legacy fallback retained for backwards compatibility.
- Added salt export in Settings (admin only): "Show salt" button copies Base64 to clipboard for external decryption scripts.

### Changed
- `EncryptionManager.encrypt()` and `decrypt()` now require `Context` parameter to access per-user salt.
- `ImportCsvUseCase` updated to pass context to encryption calls.
- Settings screen: new "Salt de cifrado (Base64)" section visible to admin when encryption is configured, with copy-to-clipboard button.

---

## Template for Future Entries

## [X.Y.Z] - YYYY-MM-DD

### Added
- Feature descriptions

### Changed
- Changes to existing functionality

### Deprecated
- Soon-to-be-removed features

### Removed
- Removed features

### Fixed
- Bug fixes

### Security
- Vulnerability patches