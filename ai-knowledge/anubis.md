# Anubis proof-of-work worker
AI-generated, verified against the files listed below on 2026-11-22. Check before relying on it.

## Summary
`app/src/main/assets/anubis/sha256-webcrypto.mjs` is a local copy of the Anubis 1.x SHA-256 proof-of-work worker. Redlib instances sit behind Cloudflare, which returns `403` for the original worker script request from an Android WebView; serving the tiny upstream worker locally lets the proof-of-work finish. Only the script is replaced — the challenge and validation still come from the server. It is served by `RedlibWebView` (see project-overview-and-principles).

## Key files
- `app/src/main/assets/anubis/sha256-webcrypto.mjs`: the worker. `calculateSHA256` uses `crypto.subtle.digest`; `toHexString` renders the digest.

## Entry points and flows
The WebView posts `{ data, difficulty, threads, nonce }` to the worker. It hashes `data + nonce` and needs `floor(difficulty/2)` zero bytes (odd difficulty also checks the next nibble). It advances `nonce += threads` for parallelism and `postMessage`s the winning `{ hash, data, difficulty, nonce }`, or progress `nonce` every 1024 iterations on the main thread.

## Gotchas
- Anubis difficulty is a byte/nibble zero-count, not a hash-count — `requiredZeroBytes = floor(difficulty/2)`.
- The worker is a static asset, not executed by the app directly; it runs inside the verification WebView.

## Covers
- app/src/main/assets/anubis
Listing: fe7022f7b991

## Files
- app/src/main/assets/anubis/sha256-webcrypto.mjs 21f96f70c9fa
