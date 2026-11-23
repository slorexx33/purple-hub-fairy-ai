# Purple Hub v5.6.3

This release fixes the provider-compatibility retry path in FAIRY.

### Fixed
- Corrected request-body mutation when retrying with `max_tokens` or `max_completion_tokens` after a provider reports an unsupported token-limit parameter.
- The retry now removes both possible token-limit fields independently before adding the selected parameter.

### Notes
- Firebase authentication remains optional; the app can run in local mode without a Firebase configuration file.
- AI requests require the user's own provider endpoint and API key. Provider pricing and data policies apply.
- This source archive does not include a prebuilt APK, Firebase configuration, or signing keys.
