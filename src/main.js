import {PanoPlugin, viewComponent} from '@panomc/sdk';
import {derived, get} from 'svelte/store';
import {_ as i18n} from '@panomc/sdk/utils/language';
import { api } from '@panomc/sdk/plugin-api';
import { showToast } from '@panomc/sdk/toasts';
import { microsoftLoginEnabled, setPano } from './theme/state.js';

const pluginId = 'pano-plugin-premium-login';

// this is to render plugin translations
export const _ = derived(i18n, ($_fn) => {
  return (key, options) => $_fn(`plugins.${pluginId}.${key}`, options);
});

// Success/failure colouring for this plugin's toasts, matching the panel. showToast from
// @panomc/sdk/toasts is the host panel's ToastContainer `show`, whose signature is
// (text, params, toastComponent, options): passing undefined for toastComponent keeps the
// host's DefaultToast, and options.variant maps to Bootstrap's text-success / text-danger.
// These live here rather than in @panomc/sdk/toasts because this plugin is pinned to
// @panomc/sdk 1.0.0-dev.39, which predates the variants; they can be dropped for a direct
// SDK import once that pin moves. On an older panel build the extra argument is ignored and
// the toast renders neutral, so this degrades instead of breaking.
export function showSuccessToast(text, params = {}) {
  return showToast(text, params, undefined, { variant: 'success' });
}

export function showErrorToast(text, params = {}) {
  return showToast(text, params, undefined, { variant: 'danger' });
}

export let pano;
// The Microsoft switch is a plain store in theme/state.js (the theme views import it from there); re-exported here.
export { microsoftLoginEnabled };

async function loadMicrosoftConfig(event) {
  try {
    const config = await api.get({
      path: '/config',
      request: event,
    });
    if (!config.error && config.microsoftLoginEnabled) {
      microsoftLoginEnabled.set(true);
    }
  } catch (e) {
    console.error('[pano-plugin-premium-login] Failed to load config', e);
  }
}

/**
 * The Microsoft button and `?mcError=` on a login form. Shared by the theme's `/login` and the
 * panel's own sign-in page (a SERVERS install runs no theme), which expose the same
 * `pano.ui.auth.login` surface.
 */
function registerLoginContent(pano) {
  pano.ui.auth.login.onLoad(async (data, event) => {
    const url = new URL(event.url);
    const mcError = url.searchParams.get('mcError');
    if (mcError) {
      data.error = mcError;
    }

    await loadMicrosoftConfig(event);

    // Register Microsoft login button only if Microsoft login is enabled
    if (get(microsoftLoginEnabled)) {
      pano.ui.auth.login.alternativeMethods.add({
        id: `${pluginId}-microsoft-login`,
        // the theme names the view (a theme can restyle it); the panel's own sign-in page has no view registry
        ...(pano.isPanel
          ? {
              component: viewComponent(
                () => import('./theme/views/MicrosoftLoginButton.svelte'),
              ),
            }
          : {view: 'premium-login:MicrosoftLoginButton'}),
        priority: 50,
      });
    }
  });
}

/**
 * The pages the Microsoft round trip and the username-recovery mail land on, for the panel's own sign-in page. On the theme
 * they are the views premium-login:SetUsernamePage and premium-login:MicrosoftCallbackPage, which register themselves.
 */
function registerSignInPages(pano, options = {}) {
  // Register set-username page (for temp-user username recovery via email link)
  pano.ui.page.register({
    path: '/set-username',
    component: viewComponent(
      () => import('./theme/views/SetUsernamePage.svelte'),
    ),
    ...options,
  });

  // Register Microsoft OAuth callback processing page
  pano.ui.page.register({
    path: '/premium-login/callback',
    component: viewComponent(
      () => import('./theme/views/MicrosoftCallbackPage.svelte'),
    ),
    ...options,
  });
}

export default class PremiumLoginPlugin extends PanoPlugin {
  onLoad() {
    pano = this.pano;
    setPano(pano);

    if (pano.isPanel) {
      // Register premium status edit component in player edit modal
      pano.ui.player.editModal.cardRows.edit((items) => {
        items.push({
          id: `${pluginId}-player-premium-status-edit`,
          component: viewComponent(
            () =>
              import(
                './panel/components/view/PlayerPremiumStatusEdit.svelte'
              ),
          ),
          permission: `pano.plugin.${pluginId}.manage.premium.status`,
          priority: 100,
        });
      });

      // Register premium status sidebar in player detail page
      pano.ui.hook.register({
        name: 'panel:player-detail:sidebar',
        component: viewComponent(
          () =>
            import(
              './panel/components/view/PlayerPremiumStatusSidebar.svelte'
            ),
        ),
        permission: `pano.plugin.${pluginId}.manage.premium.status`,
      });

      // Load config for addon detail page
      pano.ui.addon.onLoad(async (data, event) => {
        if (data.addon.id !== pluginId) return;

        try {
          const config = await api.panel.get({
            path: '/config',
            request: event,
          });
          data.addon.config = config;
        } catch (e) {
          console.error('[pano-plugin-premium-login] Failed to load config', e);
        }
      });

      // Register plugin settings in addon detail page
      pano.ui.hook.register({
        name: `panel:plugin-detail:content:${pluginId}`,
        component: viewComponent(
          () =>
            import('./panel/components/view/PremiumLoginSettings.svelte'),
        ),
        permission: `pano.plugin.${pluginId}.manage.premium.login.settings`,
      });

      // A SERVERS install signs in on the panel itself, so the Microsoft button goes there too,
      // and the callback pages are served there (the platform moves them under /panel), open to
      // a visitor with no session yet. Panels older than `pano.ui.auth` skip this.
      if (pano.ui.auth?.login) {
        registerLoginContent(pano);
        registerSignInPages(pano, { public: true });
      }
    } else {
      // Check for Microsoft login success toast cookie
      checkMcLoginToast();

      // The sign-in pages (/set-username, /premium-login/callback), the premium status in the user settings (slot
      // settings-card-rows) and in the player profile (slot profile-card-rows) are views that register themselves:
      // premium-login:SetUsernamePage, :MicrosoftCallbackPage, :PremiumStatusDisplay, :PremiumStatusProfileSidebar.

      // Inject Microsoft callback errors into login page & load config
      registerLoginContent(pano);

      // Inject Microsoft callback errors and username into register page & load config
      pano.ui.auth.register.onLoad(async (data, event) => {
        const url = new URL(event.url);
        const mcError = url.searchParams.get('mcError');
        const mcUsername = url.searchParams.get('mcUsername');

        if (mcError) {
          data.error = mcError;
        }
        if (mcUsername) {
          data.username = mcUsername;
        }

        await loadMicrosoftConfig(event);

        // Register Microsoft register button only if Microsoft login is enabled
        if (get(microsoftLoginEnabled)) {
          pano.ui.auth.register.alternativeMethods.add({
            id: `${pluginId}-microsoft-register`,
            view: 'premium-login:MicrosoftRegisterButton',
            priority: 50,
          });
        }
      });
    }
  }

  onContextUpdate(ctx) {}

  onUnload() {}
}

function checkMcLoginToast() {
  if (typeof window === 'undefined') return;

  const match = document.cookie.match(/(?:^|;\s*)mc_login_toast=([^;]*)/);
  if (!match) return;

  const mcUsername = decodeURIComponent(match[1]);

  // Delete cookie
  document.cookie = 'mc_login_toast=; Path=/; Max-Age=0';

  // Wait for DOM to be ready, then show toast
  setTimeout(() => {
    // Get translated message
    let message = `Signed in with Minecraft premium account: ${mcUsername}`;

    const unsubscribe = _.subscribe((t) => {
      message = t('microsoft.login-success', { values: { username: mcUsername } });
    });
    unsubscribe();

    // Find existing toast container or create one
    let container = document.querySelector('.toast-container');
    if (!container) {
      container = document.createElement('div');
      container.className = 'toast-container position-fixed bottom-0 start-50 translate-middle-x mb-3';
      document.body.appendChild(container);
    }

    // Create toast element matching theme style
    const toastEl = document.createElement('div');
    toastEl.className = 'animate__animated animate__bounceInUp toast align-items-center shadow-lg text-bg-tertiary opacity-100';
    toastEl.setAttribute('role', 'alert');
    toastEl.setAttribute('aria-live', 'assertive');
    toastEl.setAttribute('aria-atomic', 'true');
    toastEl.innerHTML = `<div class="toast-body"><i class="fas fa-cube me-1"></i> ${message}</div>`;

    container.appendChild(toastEl);

    // Show via Bootstrap Toast API
    if (window.bootstrap?.Toast) {
      const bsToast = new window.bootstrap.Toast(toastEl);
      bsToast.show();

      toastEl.addEventListener('hidden.bs.toast', () => {
        toastEl.remove();
      });
    } else {
      // Fallback: manual show/hide
      toastEl.classList.add('show');
      setTimeout(() => {
        toastEl.classList.remove('show');
        setTimeout(() => toastEl.remove(), 300);
      }, 4000);
    }
  }, 500);
}
