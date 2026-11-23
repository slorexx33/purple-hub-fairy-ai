# Changelog

## 5.6.3 — 2026-11-23
- Fixed request-body retry logic when switching between `max_tokens` and `max_completion_tokens`.
- Removed an invalid chained `JSONObject.remove().remove().put()` operation that caused a Kotlin compilation error.
- Kept the existing provider compatibility behavior and version code 26.

## 5.6.2
- API request compatibility and retry handling updates.

## 5.6.1
- Fixed a missing Material 3 import required for compilation.

## 5.6.0
- Improved API provider settings, optional Firebase/local startup, and API Monitor behavior.
