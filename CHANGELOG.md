# Changelog - AppKotlinIngGas

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

---

## [1.0.1] - 2025-07-06

### Security
- **Fixed fixed salt in PBKDF2** (`EncryptionManager.kt`): Now generates unique random 16-byte salt per user on password setup, stored in EncryptedSharedPreferences. Legacy fallback retained for backward compatibility.
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