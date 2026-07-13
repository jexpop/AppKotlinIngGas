# AppKotlinIngGas

Aplicación Android (Kotlin + Jetpack Compose) para gestión de finanzas personales con importación CSV, categorización automática, backup cifrado y sincronización Supabase/Google Drive.

**Versión actual:** 1.0.15

---

## Documentación

- **[PROJECT_CONTEXT.md](PROJECT_CONTEXT.md)** — Contexto técnico completo: arquitectura, modelo de datos, módulos clave, convenciones, deuda técnica, estructura BD y referencias rápidas.
- **[CHANGELOG.md](CHANGELOG.md)** — Historial de versiones y cambios (formato Keep a Changelog).

---

## Configuración del Proyecto

Para que esta app funcione, necesitas crear una base de datos en Supabase.

Variables requeridas en `local.properties`:
```properties
SUPABASE_URL=https://xxx.supabase.co
SUPABASE_KEY=eyJhbGciOiJIUzI1NiIs...
ADMIN_EMAIL=admin@example.com
```

---

## Stack Principal

- **Kotlin 2.0** + **Compose BOM 2024.10** + **AGP 8.7**
- **Supabase Kotlin 3.1** (PostgREST, Auth, Storage)
- **Google Drive API v3** + **Ktor 3.1**
- **KSP** (serialización)
- **Min SDK 24**, **Target SDK 36**

---

## Build

```bash
# Debug
./gradlew assembleDebug

# Tests
./gradlew testDebugUnitTest

# Lint
./gradlew lint
```
