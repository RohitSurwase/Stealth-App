/**
 * Local copy of Anubis' `sha256-webcrypto.mjs` worker.
 *
 * Some Redlib instances sit behind Cloudflare, which returns 403 for the
 * original worker script request from an Android WebView. That makes Anubis
 * fail with "Failed to calculate challenge: undefined". Serving the (tiny,
 * upstream) worker script locally lets the proof-of-work finish; only the
 * script is replaced, the challenge and validation still come from the server.
 *
 * Protocol and message format are part of Anubis 1.x (see
 * https://github.com/TecharoHQ/anubis/blob/v1.24.0/web/js/worker/sha256-webcrypto.ts).
 */

const encoder = new TextEncoder();

const calculateSHA256 = async (input) => {
  const data = encoder.encode(input);
  return await crypto.subtle.digest("SHA-256", data);
};

const toHexString = (byteArray) => {
  return byteArray.reduce(
    (str, byte) => str + byte.toString(16).padStart(2, "0"),
    ""
  );
};

addEventListener("message", async ({ data: eventData }) => {
  const { data, difficulty, threads } = eventData;
  let nonce = eventData.nonce;
  const isMainThread = nonce === 0;
  let iterations = 0;

  const requiredZeroBytes = Math.floor(difficulty / 2);
  const isDifficultyOdd = difficulty % 2 !== 0;

  for (;;) {
    const hashBuffer = await calculateSHA256(data + nonce);
    const hashArray = new Uint8Array(hashBuffer);

    let isValid = true;
    for (let i = 0; i < requiredZeroBytes; i++) {
      if (hashArray[i] !== 0) {
        isValid = false;
        break;
      }
    }

    if (isValid && isDifficultyOdd) {
      if (hashArray[requiredZeroBytes] >> 4 !== 0) {
        isValid = false;
      }
    }

    if (isValid) {
      postMessage({
        hash: toHexString(hashArray),
        data,
        difficulty,
        nonce,
      });
      return;
    }

    nonce += threads;
    iterations++;

    if (nonce % 1 !== 0) {
      nonce = Math.trunc(nonce);
    }

    if (isMainThread && (iterations & 1023) === 0) {
      postMessage(nonce);
    }
  }
});
