import {PanoPlugin, viewComponent} from '@panomc/sdk';
import {derived, get, writable} from 'svelte/store';
import {_ as i18n} from '@panomc/sdk/utils/language';
import ApiUtil from '@panomc/sdk/utils/api';

const pluginId = 'pano-plugin-premium-login';

// this is to render plugin translations
export const _ = derived(i18n, ($_fn) => {
  return (key, options) => $_fn(`plugins.${pluginId}.${key}`, options);
});

export let pano;
export const microsoftLoginEnabled = writable(false);

async function loadMicrosoftConfig(event) {
  try {
    const config = await ApiUtil.get({
      path: '/api/premium-login/config',
      request: event,
    });
    if (config.result === 'ok' && config.microsoftLoginEnabled) {
      microsoftLoginEnabled.set(true);
    }
  } catch (e) {
    console.error('[pano-plugin-premium-login] Failed to load config', e);
  }
}

export default class PremiumLoginPlugin extends PanoPlugin {
  onLoad() {
    pano = this.pano;

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
          const config = await ApiUtil.get({
            path: '/api/panel/premium-login/config',
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
    } else {
      // Check for Microsoft login success toast cookie
      checkMcLoginToast();

      // Register set-username page (for temp-user username recovery via email link)
      pano.ui.page.register({
        path: '/set-username',
        component: viewComponent(
          () => import('./theme/pages/SetUsernamePage.svelte'),
        ),
      });

      // Register Microsoft OAuth callback processing page
      pano.ui.page.register({
        path: '/premium-login/callback',
        component: viewComponent(
          () => import('./theme/pages/MicrosoftCallbackPage.svelte'),
        ),
      });

      // Register premium status display in user settings
      pano.ui.settings.cardRows.edit((items) => {
        items.push({
          id: `${pluginId}-status-display`,
          component: viewComponent(
            () =>
              import('./theme/components/view/PremiumStatusDisplay.svelte'),
          ),
          priority: 105,
        });
      });

      // Register premium status display in player profile sidebar
      pano.ui.profile.cardRows.edit((items) => {
        items.push({
          id: `${pluginId}-profile-sidebar-status`,
          component: viewComponent(
            () =>
              import(
                './theme/components/view/PremiumStatusProfileSidebar.svelte'
              ),
          ),
          priority: 90,
        });
      });

      // Inject Microsoft callback errors into login page & load config
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
            component: viewComponent(
              () =>
                import(
                  './theme/components/view/MicrosoftLoginButton.svelte'
                ),
            ),
            priority: 50,
          });
        }
      });

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
            component: viewComponent(
              () =>
                import(
                  './theme/components/view/MicrosoftRegisterButton.svelte'
                ),
            ),
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
