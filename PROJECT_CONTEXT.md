# PROJECT_CONTEXT.md - AppKotlinIngGas

> Documento de contexto técnico generado automáticamente. No editar manualmente salvo actualizaciones mayores.

---

## 1. Visión General

**AppKotlinIngGas** — Aplicación Android (Kotlin + Jetpack Compose) para gestión de finanzas personales:
- Importación de CSV bancarios (cuentas y tarjetas)
- Categorización automática por reglas/excepciones
- Almacenamiento en **Supabase** (PostgreSQL + Auth + Storage)
- Backup cifrado (AES-256-GCM) a **Supabase Storage** + **Google Drive**
- Autenticación: Email/Password + Google OAuth (Supabase)
- UI: Material 3 + Navigation Compose + MVVM + StateFlow

**Stack**: Kotlin 2.0 + Compose BOM 2024.10 + AGP 8.7 + Supabase Kotlin 3.1 + Google Drive API v3 + Ktor 3.1 + KSP (serialization)

**Versión actual de la app**: 1.0.9 (`versionCode = 9`). La pantalla de Ajustes muestra `BuildConfig.VERSION_NAME`.

---

## 2. Arquitectura

### 2.1 Capas (Clean Architecture simplificada)

```
app/src/main/java/com/jexpop/appkotlininggas/
├── data/                    # Capa de datos (repositories, parsers, managers)
│   ├── model/               # Data classes @Serializable (mapeo 1:1 tablas Supabase)
│   ├── parser/              # Estrategias CSV por banco/tipo
│   │   ├── strategy/        # CsvParserStrategy + implementaciones BankA*
│   │   └── detector/        # CsvFormatDetector (registry hardcoded)
│   ├── repository/          # Wrappers suspend sobre Supabase PostgREST
│   └── EncryptionManager,
│       StorageManager,
│       DriveManager,
│       DriveAuthManager
├── domain/
│   └── usecase/             # Casos de uso (ImportCsvUseCase, CategorizationUseCase)
├── ui/
│   ├── screens/             # Composables por feature (MVVM + ViewModel)
│   │   ├── importcsv/
│   │   ├── transactions/
│   │   ├── categories/
│   │   ├── banks/
│   │   └── settings/
│   ├── components/          # Componentes reutilizables (DateFormatter)
│   ├── theme/               # Material3 Theme (Color, Type, Theme)
│   ├── AppNavigation.kt     # NavHost + BottomNavigation
│   └── Navigation.kt        # Rutas Sellado (sealed class Screen)
└── MainActivity.kt          # Entry point: auth state -> LoginScreen / AppNavigation
```

### 2.2 Flujo de datos principal (Import CSV)

```
ImportScreen (Compose)
    └─ ImportViewModel
         └─ ImportCsvUseCase.execute(content, bankId, context)
              ├─ CsvParser.parse(content, bankId)
              │    └─ CsvFormatDetector.detect(content) -> BankAAccountParser | BankACreditParser
              ├─ CategorizationUseCase.categorize(transactions, month)
              │    └─ CategorizationRepository.getRules() + getExceptions(month)
              ├─ PeriodRepository.getOrCreateYear/Month + setCurrentMonth
              ├─ TransactionRepository.deleteByMonthBankAndType / deleteByCreditMonthAndBank
              ├─ TransactionRepository.insertTransactions(categorized)
              └─ [Opcional] EncryptionManager.encrypt + StorageManager.uploadCsvBackup + DriveManager.uploadCsvBackup
```

### 2.3 Inyección de dependencias

**Actual**: Instanciación manual en constructores (`= Repository()`).  
**Planeado**: Hilt/Koin. No hay módulo `di/`.

---

## 3. Dependencias Principales (`app/build.gradle.kts`)

| Categoría | Librería | Versión |
|-----------|----------|---------|
| Android/Compose | `androidx.compose.bom` | 2024.10.00 |
| | `androidx.activity:activity-compose` | 1.9.3 |
| | `androidx.navigation:navigation-compose` | 2.7.7 |
| | `androidx.lifecycle:lifecycle-viewmodel-compose` | 2.8.7 |
| Supabase | `io.github.jan-tennert.supabase:bom` | 3.1.4 |
| | `postgrest-kt`, `auth-kt`, `storage-kt` | (bom) |
| Ktor | `io.ktor:ktor-client-android` | 3.1.3 |
| Serialización | `kotlinx-serialization-json` | 1.7.3 |
| Corrutinas | `kotlinx-coroutines-android` | 1.8.1 |
| Google Auth/Drive | `play-services-auth` | 21.2.0 |
| | `google-api-client-android` | 2.2.0 |
| | `google-api-services-drive` | v3-rev20220815-2.0.0 |
| Seguridad | `androidx.security:security-crypto` | 1.1.0-alpha06 |
| Testing | JUnit 4.13.2, Espresso, Compose Test | — |

> `local.properties` inyecta `SUPABASE_URL`, `SUPABASE_KEY`, `ADMIN_EMAIL` a `BuildConfig`.

---

## 4. Modelo de Datos (Supabase ↔ Kotlin `@Serializable`)

| Tabla Supabase | Data Class | Campos Clave |
|----------------|------------|--------------|
| `bank` | `Bank` | id, name, code, description, active |
| `transaction` | `Transaction` | id, transaction_date, concept, flow_type (H/D), amount, balance, payment_type (D/C), credit_month, group_id, period_month_id, bank_id |
| `transaction_view` | `TransactionView` | Vista join con category_group, bank, period_month |
| `category_group` | `CategoryGroup` | id, parent_id, description, periodicity, sort_order, flow_type |
| `period` | `Period` | year (PK) |
| `period_month` | `PeriodMonth` | id, month (YYYYMM), active, current |
| `categorization_rule` | `CategorizationRule` | id, rule_type (1,2,4,5,6,7,99), group_id, value1..4 |
| `categorization_exception` | `CategorizationException` | id, month, group_id, value1, value2 |
| app_param | AppParam | cond1, cond2, value (config: GitHub token, expiry, repos, username; ENCRYPTION/SALT para sincronización cifrado entre dispositivos) |

> **Convención**: `@SerialName("snake_case")` mapea snake_case ↔ camelCase.

---

## 5. Módulos y Componentes Clave

### 5.1 Parsers CSV (`data/parser/`)

| Parser | Detector (`canParse`) | Tipo CSV | Banco |
|--------|----------------------|----------|-------|
| `BankAAccountParser` | `date;value_date;concept` | Cuenta corriente | BankA |
| `BankACreditParser` | `Fecha;Comercio / Entidad;Importe;Tipo` | Tarjeta crédito | BankA |

**Estrategia**: `CsvParserStrategy` interface → `CsvFormatDetector` lista hardcodeada → `CsvParser.parse()` delega.

**Extensibilidad**: Añadir parser → implementar `CsvParserStrategy` + registrar en `CsvFormatDetector.parsers`.  
**Pendiente**: Registro dinámico por `bank.code`.

### 5.2.2 Copiar Concepto en Detalles de Movimiento (v1.0.8+)

**Funcionalidad**: Al expandir un movimiento, el usuario puede copiar el concepto completo al portapapeles con un click.

**Implementación** (`ui/screens/transactions/TransactionsScreen.kt`):
- Fila adicional en panel expandido con:
  - Etiqueta: `transactions_detail_concept`
  - Texto: concepto completo (hasta 3 líneas)
  - `IconButton` con icono `ContentCopy`
- Al click:
  - `ClipboardManager.setPrimaryClip(ClipData.newPlainText("concept", transaction.concept))`
  - `Toast`: "Concepto copiado al portapapeles" (`transactions_concept_copied`)
- **Propósito**: Minimizar errores tipográficos al crear reglas de categorización manualmente → copiar directamente del movimiento

**Strings** (`strings.xml`):
- `transactions_detail_concept`: "Concepto"
- `transactions_copy_concept`: "Copiar concepto"
- `transactions_concept_copied`: "Concepto copiado al portapapeles"

### 5.2.1 Filtros de Transacciones (`ui/screens/transactions/`)

**Estados de filtro** (`TransactionsViewModel`):
| Filtro | Parámetro | Valores | Efecto |
|--------|-----------|--------|--------|
| Mes | `selectedMonth` | `null` o YYYYMM | Carga transacciones del mes seleccionado o todos |
| Banco | `selectedBank` | `null` o `Bank` | Filtra por banco (solo si hay >1 banco) |
| Tipo de pago | `selectedPaymentType` | `null`, `"D"` (cuenta), `"C"` (tarjeta) | Filtra por tipo de pago |
| Categoría | `selectedCategoryFilter` | `null` o `"UNCATEGORIZED"` | Muestra todas categorías o solo sin categoría |

**Implementación de filtros**:
- **Repos**: `TransactionViewRepository.getByFilters()` acepta parámetros opcionales.
- **Filtro "Sin categoría"**: `onlyUncategorized=true` → aplica `exact("group_id", null)` **en la propia query de Supabase**, dentro del bloque `filter { }`, antes de `range()`. Genera `group_id=is.null` en la petición PostgREST. *(Corregido en v1.0.9: antes se filtraba en cliente tras `decodeList`, lo que provocaba que transacciones con `group_id = NULL` fuera de la página actual no se mostraran. Nota: en `postgrest-kt` 3.1.4 no existe `isNull()`; el método correcto para IS NULL es `exact(column, value: Boolean?)`.)*
- **Contador de movimientos**: al pie de la lista se muestra el nº de transacciones cargadas para el filtro activo (`pluralStringResource(R.plurals.transactions_count, ...)`), útil para detectar visualmente resultados inesperados de un filtro. Refleja solo lo cargado (no el total real si hay más páginas pendientes).
- **UI**: `FiltersPanel` con `FilterChip` por tipo de pago y categoría, `OutlinedButton` + `AlertDialog` para mes/banco.
- **Estado**: Se gestiona en `TransactionsViewModel` con `StateFlow<String?>` para cada filtro; cada cambio recarga transacciones desde offset 0.

---

### 5.2 Categorización (`domain/usecase/CategorizationUseCase`)

**Reglas (rule_type)**:
| Tipo | Lógica | Parámetros |
|------|--------|------------|
| 1 | Texto completo = value1 | value1 |
| 2 | Primeros 15 chars contienen value1 | value1 |
| 4 | Primeros 3 chars = value1 **Y** pos 18-30 contiene value2 | value1, value2 |
| 5 | Primeros 3 chars = value1 **Y** importe >/< value2 | value1, value2 (num), value3 (>,<,>=,<=) |
| 6 | Primeros 20 chars contienen value1 **Y** importe entre value2-value3 | value1, value2 (min), value3 (max) |
| 7 | Primeros 3 = value1 **Y** pos 18-30 contiene value2 **Y** importe entre value3-value4 | value1, value2, value3 (min), value4 (max) |
| 99 | Es tarjeta crédito (payment_type == "C") | — |

**Orden**: Excepciones manuales (por mes) → Reglas automáticas (orden BD).

**Notas técnicas** (v1.0.6+):
- **Tipos 4 y 7** (posiciones 18-30): Extracción segura con validación de longitud (`concept.length > 17`). Posiciones en numeración de usuario (1-basada): 18-30 → índices 0-basados: 17-30 → `substring(17, minOf(31, length))`.
- **Tipo 6** (primeros 20 chars): Requiere concepto con al menos 20 caracteres para evitar falsos positivos.

## 5.3 Cifrado (data/EncryptionManager)

- **Algoritmo**: AES-256-GCM + PBKDF2-HMAC-SHA256 (100k iter, salt aleatorio 16 bytes por usuario)
- **Storage local**: EncryptedSharedPreferences (AndroidX Security Crypto) para password maestra + salt
- **Sincronización salt**: `app_param (ENCRYPTION/SALT)` en Supabase — Base64 del salt, protegido por RLS
- **Flujo primer dispositivo**: configura contraseña → descarga salt Supabase → si no existe genera nuevo → sube a Supabase
- **Flujo segundo dispositivo**: arranca app → descarga salt Supabase automáticamente → configura misma contraseña → ficheros compatibles
- **Formato backup**: IV (12 bytes) || ciphertext → `.csv.enc`
- **Naming**: `{bankCode}_{c|a}_{YYYYMM}.csv.enc`
- **Exportación salt**: Settings (admin) → "Mostrar salt" → copia Base64 al portapapeles (para script Python externo)
- **Script Python descifrado**: usa salt exportado + contraseña → compatible con cualquier dispositivo autorizado

### 5.4 Backups

| Destino | Clase | Path/Bucket |
|---------|-------|-------------|
| Supabase Storage | `StorageManager` | `backups/csv/{filename}` |
| Google Drive | `DriveManager` | `ecogar/backups/csv/{filename}` |

Ambos usan `upsert=true` (sobrescriben si existe).

### 5.5 Autenticación

- `AuthRepository`: `signInWithEmail`, `signInWithGoogle`, `isAuthenticated()`, `getCurrentUserEmail()`
- `MainActivity`: `supabase.handleDeeplinks(intent)` en `onCreate`/`onNewIntent`
- Esquema OAuth: `ecogar://login-callback` (configurado en `SupabaseClient.kt`)

### 5.6 Períodos (`PeriodRepository`)

- `period` (años) + `period_month` (YYYYMM, active, current)
- `getOrCreateYear/Month` usan `upsert(onConflict=PK, ignoreDuplicates=true)`
- `setCurrentMonth` desmarca anterior → marca nuevo

---

## 6. Convenciones y Estilo

| Área | Convención |
|------|------------|
| Paquetes | `com.jexpop.appkotlininggas.{data\|domain\|ui}.feature` |
| Naming | PascalCase (clases), camelCase (props/funcs), UPPER_SNAKE (constantes) |
| Serialización | `@Serializable` + `@SerialName("snake_case")` en data classes |
| Corrutinas | `suspend` en repos/usecases; `viewModelScope.launch` en ViewModels |
| Resultado | `Result<T>` (stdlib) + `runCatching`; errores como `Exception("CODE")` |
| Logging | `android.util.Log.d("TAG", "msg")` en repos críticos (PeriodRepository) |
| UI State | Sealed class `XxxState { Idle, Loading, Success(data), Error(msg) }` |
| Navigation | Sealed class `Screen` (route, label, icon) + `NavHost` en `AppNavigation` |
| Temas | `AppKotlinIngGasTheme` (Material3), `Color.kt`/`Type.kt`/`Theme.kt` |
| Strings | `strings.xml` (es) — claves: `nav_*`, `import_*`, `settings_*`, `error_*`, `dialog_*` |
| Selectores/Pickers | `OutlinedButton` (muestra selección) + `AlertDialog` con lista scrollable de `TextButton` — **no** `ExposedDropdownMenuBox`/`DropdownMenu` (problemas de anclaje). Ver `GroupDialog`/`RuleDialog` en `CategoriesScreen.kt` y `FiltersPanel` en `TransactionsScreen.kt` |
| Listado de movimientos | Cada fila puede expandirse al tocarla para mostrar concepto completo y datos extra; solo una fila permanece expandida a la vez. La fila compacta mantiene iconos para cuenta/tarjeta. Se recarga automáticamente tras una importación correcta. |

---

## 7. Decisiones de Diseño Clave

| Decisión | Justificación | Trade-off / Deuda |
|----------|---------------|-------------------|
| **Supabase PostgREST directo** (sin ORM) | Tipado seguro con `@Serializable`, queries type-safe, sin migraciones | Repetir `.select().filter()...` en cada repo |
| **Parsers CSV por estrategia** | Extensible por banco/tipo sin tocar core | Detector hardcodeado; requiere recompilar |
| **Categorización en cliente** (no BD) | Reglas dinámicas por mes, excepciones usuario, offline-first | Reprocesar todo al cambiar reglas |
| **Cifrado AES-GCM + PBKDF2 local** | Zero-knowledge backup; password no sale del device | Salt fijo; sin rotación de clave |
| **Backup dual (Supabase + Drive)** | Redundancia; Drive para usuario, Storage para app | Complejidad sync; errores solo log, no reintentan |
| **MVVM + StateFlow + Compose** | Unidirectional data flow, testable, preview-friendly | ViewModels instancian repos (sin DI real) |
| **Navigation Compose + BottomBar** | Single Activity, type-safe routes via `Screen` sealed class | `bottomNavScreens` duplicado vs `Screen` enum |

---

## 8. Deuda Técnica y TODOs Priorizados

### 🔴 Crítico
- [x] **Salt fijo en PBKDF2** (`EncryptionManager.kt:18`) → Salt aleatorio por usuario + almacenar en EncryptedSharedPrefs ✅ **COMPLETADO 2025-07-06**
- [ ] **`TransactionRepository.deleteByMonthBankAndType`** usa `31` día fijo → falla feb/30d (`:25-26`)
- [ ] **Backups sin `await`** (`ImportCsvUseCase.kt:70-75`) → errores solo log, no reintento ni UI
- [ ] **`CategorizationUseCase` regla tipo 99** hardcodeada → mover a BD o config

### 🟡 Importante
- [ ] **DI real** (Hilt/Koin) → eliminar `= Repository()` en constructores
- [ ] **Registro dinámico parsers** por `bank.code` (no hardcodeado en `CsvFormatDetector`)
- [ ] **Result sealed class** propia vs `kotlin.Result` + string codes (`"FORMAT_NOT_RECOGNIZED"`)
- [ ] **Tests unitarios** parsers, categorización, repos (solo `ExampleUnitTest` existe)
- [ ] **Validación fecha** `transactionDate` antes de `substring(0,7)` (`ImportCsvUseCase.kt:30`)

### 🟢 Mejora
- [ ] Paginación real en `TransactionViewRepository` (ya tiene limit/offset)
- [ ] `PeriodRepository` logs → `Timber` o `Koin` logger
- [ ] `RuleType` enum en vez de `Int` mágicos (1,2,4,5,6,7,99)
- [ ] `DriveManager`/`StorageManager` unificar interfaz `BackupDestination`
- [ ] Compose Previews para screens principales

---

## 9. Estructura de Base de Datos (Supabase)

```sql
-- Tablas principales (resumen)
create table bank (
  id serial primary key,
  name text not null,
  code text unique,
  description text,
  active boolean default true,
  created_at timestamptz default now()
);

create table period (
  year text primary key
);

create table period_month (
  id serial primary key,
  month text unique not null, -- YYYYMM
  active boolean default true,
  current boolean default false
);

create table transaction (
  id bigserial primary key,
  transaction_date date not null,
  concept text not null,
  flow_type text not null check (flow_type in ('H','D')),
  amount numeric(12,2) not null,
  balance numeric(12,2),
  payment_type text not null check (payment_type in ('D','C')),
  credit_month text, -- YYYYMM para tarjetas
  group_id int references category_group(id),
  period_month_id int references period_month(id),
  bank_id int references bank(id),
  created_at timestamptz default now()
);

create table category_group (
  id serial primary key,
  parent_id int references category_group(id),
  description text not null,
  periodicity text, -- 'M','Q','Y','O'
  sort_order text not null, -- '001001'
  flow_type text not null check (flow_type in ('H','D'))
);

create table categorization_rule (
  id serial primary key,
  rule_type int not null, -- 1,2,4,5,6,7,99
  group_id int not null references category_group(id),
  value1 text, value2 text, value3 text, value4 text
);

create table categorization_exception (
  id serial primary key,
  month text not null, -- YYYY YYYYMM
  group_id int not null references category_group(id),
  value1 text, value2 text
);

create table app_param (
  cond1 text not null,
  cond2 text not null,
  value text,
  primary key (cond1, cond2)
);

-- Vista transacciones enriquecida
create view transaction_view as
select t.*, b.name as bank_name, b.code as bank_code,
       cg.description as group_description, cg.flow_type as group_flow_type,
       pm.month as period_month
from transaction t
left join bank b on t.bank_id = b.id
left join category_group cg on t.group_id = cg.id
left join period_month pm on t.period_month_id = pm.id;
```

---

## 10. Comandos Útiles

```bash
# Build debug
./gradlew assembleDebug

# Tests
./gradlew testDebugUnitTest

# Lint
./gradlew lint

# Generar BuildConfig (tras editar local.properties)
./gradlew generateDebugBuildConfig

# Limpiar
./gradlew clean
```

---

## 11. Variables de Entorno (`local.properties`)

```properties
SUPABASE_URL=https://xxx.supabase.co
SUPABASE_KEY=eyJhbGciOiJIUzI1NiIs...
ADMIN_EMAIL=admin@example.com
```

> No commitear. Inyectados a `BuildConfig` en `app/build.gradle.kts:20-26`.

---

## 12. Extensibilidad: Añadir Nuevo Banco

1. Crear `data/parser/strategy/BankXParser.kt` implementando `CsvParserStrategy`
2. Registrar en `CsvFormatDetector.parsers` (orden = prioridad)
3. Insertar en BD `bank` con `code` único (ej. `bankx`)
4. El backup generará nombre `{code}_{c|a}_{YYYYMM}.csv.enc`

---

## 13. Referencias Rápidas Archivos Clave

| Archivo | Propósito |
|---------|-----------|
| `SupabaseClient.kt` | Cliente singleton + config Auth/Storage |
| `MainActivity.kt` | Entry point, auth state, navigation root |
| `AppNavigation.kt` | NavHost + BottomNavigationBar |
| `ImportCsvUseCase.kt` | Orquestador importación completa |
| `CsvParser.kt` / `CsvFormatDetector.kt` / `*Parser.kt` | Pipeline CSV |
| `CategorizationUseCase.kt` | Motor reglas + excepciones |
| `EncryptionManager.kt` | Cifrado/descifrado AES-GCM + prefs cifradas |
| `StorageManager.kt` / `DriveManager.kt` | Backups remotos |
| `TransactionRepository.kt` | CRUD transacciones + deletes por mes/banco/tipo |
| `PeriodRepository.kt` | Gestión años/meses activos/current |
| `SettingsViewModel.kt` + `SettingsScreen.kt` | Config: cifrado, Drive, GitHub, logout |

---

*Generado: 2025-07-06 | Actualizado: 2026-07-10 | Proyecto: AppKotlinIngGas | Versión actual: 1.0.9 | Última sync: Fix filtro "Sin categoría" con exact() (paginación); contador de movimientos al pie de la lista*