// Shared state of the theme views. A view cannot import main.js (it holds the plugin class and the panel pages) nor a
// package, so the store lives here as a plain object that follows the Svelte store contract (`$microsoftLoginEnabled` works).

/** @type {Set<(value: boolean) => void>} */
const subscribers = new Set();
let enabled = false;

export const microsoftLoginEnabled = {
  /** @param {boolean} next */
  set(next) {
    if (Object.is(next, enabled)) return;

    enabled = next;

    for (const run of [...subscribers]) run(enabled);
  },
  /** @param {(value: boolean) => void} run */
  subscribe(run) {
    subscribers.add(run);
    run(enabled);

    return () => {
      subscribers.delete(run);
    };
  },
};

/** The host API, set by main.js in onLoad, for the pages that need it (the panel's `pano.ui.auth.login.complete`). */
export let pano;

export function setPano(value) {
  pano = value;
}
