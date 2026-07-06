# Changelog - AppKotlinIngGas

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Added
- Initial project context documentation (`PROJECT_CONTEXT.md`)

### Changed
- Moved `PROJECT_CONTEXT.md` to project root for persistence across sessions

---

## [1.0.0] - 2025-07-06

### Added
- **Core Architecture**: Clean Architecture with data/domain/ui layers
- **Supabase Integration**: PostgREST (data), Auth (email/password + Google OAuth), Storage (backups)
- **Google Drive Integration**: Backup upload to user's Drive (DRIVE_FILE scope)
- **CSV Import Pipeline**:
  - Strategy pattern for bank-specific parsers
  - BankA account parser (`date;value_date;concept...`)
  - BankA credit card parser (`Fecha;Comercio / Entidad;Importe;Tipo`)
  - Auto-detection via `CsvFormatDetector`
- **Categorization Engine**:
  - 7 rule types (full text, prefix, position+substring, amount thresholds, ranges)
  - Manual exceptions per month
  - Priority: exceptions → automatic rules
- **Period Management**: Years + months (YYYYMM) with active/current flags
- **Encryption**: AES-256-GCM + PBKDF2 (100k iterations) for backup files
  - Password stored in EncryptedSharedPreferences (AndroidX Security Crypto)
  - IV prepended to ciphertext
- **UI (Compose + Material 3)**:
  - Login screen (email/password + Google)
  - Bottom navigation (Home, Transactions, Categories, Banks, Import, Settings)
  - Import screen with file picker + bank selector
  - Settings: encryption password, Drive connection, GitHub config, logout
- **Navigation**: Type-safe routes via sealed class `Screen`
- **Build Config**: Secrets injected from `local.properties` → `BuildConfig`

### Technical Details
- Kotlin 2.0, Compose BOM 2024.10, AGP 8.7
- KSP for kotlinx-serialization
- Ktor 3.1 HTTP client for Supabase
- Min SDK 24, Target SDK 36

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