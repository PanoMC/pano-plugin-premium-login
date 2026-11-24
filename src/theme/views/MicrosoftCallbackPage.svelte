<style>

  .premium-login-microsoft-callback-page {
    max-width: 480px;
    margin: 0 auto;
    padding: 2rem 1rem;
  }

  /* Hosts the login-content widgets of a step-up (they show their own overlay), never inline UI. */
  .premium-login-microsoft-callback-page__slot-mount {
    display: none;
  }

  .premium-login-microsoft-callback-page__body {
    padding: 2rem;
  }

  .premium-login-microsoft-callback-page__icon {
    font-size: 3rem;
    margin-bottom: 1rem;
  }

  .premium-login-microsoft-callback-page__icon.is-success {
    color: var(--pano-color-success);
  }

  .premium-login-microsoft-callback-page__icon.is-error {
    color: var(--pano-color-danger);
  }

  .premium-login-microsoft-callback-page__icon.is-loading {
    color: var(--pano-color-primary);
  }

  .premium-login-microsoft-callback-page__fade-in {
    animation: premium-login-fade-in 250ms both;
  }

  @keyframes premium-login-fade-in {
    from { opacity: 0; }
    to { opacity: 1; }
  }

  .premium-login-microsoft-callback-page__shake {
    animation: premium-login-shake 250ms both;
  }

  @keyframes premium-login-shake {
    from, to { transform: translate3d(0, 0, 0); }
    10%, 30%, 50%, 70%, 90% { transform: translate3d(-10px, 0, 0); }
    20%, 40%, 60%, 80% { transform: translate3d(10px, 0, 0); }
  }

  .premium-login-microsoft-callback-page__bounce-in {
    animation: premium-login-bounce-in 250ms both;
  }

  @keyframes premium-login-bounce-in {
    from, 20%, 40%, 60%, 80%, to { animation-timing-function: cubic-bezier(0.215, 0.61, 0.355, 1); }
    0% { opacity: 0; transform: scale3d(0.3, 0.3, 0.3); }
    20% { transform: scale3d(1.1, 1.1, 1.1); }
    40% { transform: scale3d(0.9, 0.9, 0.9); }
    60% { opacity: 1; transform: scale3d(1.03, 1.03, 1.03); }
    80% { transform: scale3d(0.97, 0.97, 0.97); }
    to { opacity: 1; transform: scale3d(1, 1, 1); }
  }
</style>

{#if !isBrowser}
  <!-- SSR: show loading placeholder -->
  <div class="premium-login-microsoft-callback-page">
    <div class="card">
      <div class="premium-login-microsoft-callback-page__body card-body text-center">
        <div class="premium-login-microsoft-callback-page__icon is-loading">
          <div class="spinner-border" role="status"></div>
        </div>
      </div>
    </div>
  </div>
{:else}
  <div class="premium-login-microsoft-callback-page premium-login-microsoft-callback-page__fade-in">
    <div class="card">
      <div class="premium-login-microsoft-callback-page__body card-body text-center">
        {#if callbackState === 'loading'}
          <div class="premium-login-microsoft-callback-page__icon is-loading">
            <div class="spinner-border" role="status"></div>
          </div>
          <h5>{$_('microsoft.login-loading')}</h5>
        {:else if callbackState === 'success'}
          <div class="premium-login-microsoft-callback-page__icon is-success premium-login-microsoft-callback-page__bounce-in">
            <i class="fas fa-check-circle"></i>
          </div>
          <h5 class="premium-login-microsoft-callback-page__title text-success">{$_('microsoft.login-success', { values: { username: mcUsername } })}</h5>
          <p class="text-muted">{$_('microsoft.redirecting')}</p>
        {:else if callbackState === 'linked'}
          <div class="premium-login-microsoft-callback-page__icon is-success premium-login-microsoft-callback-page__bounce-in">
            <i class="fas fa-link"></i>
          </div>
          <h5 class="premium-login-microsoft-callback-page__link-success text-success">{$_('microsoft.link-success', { values: { username: mcUsername } })}</h5>
          <p class="text-muted">{$_('microsoft.redirecting')}</p>
        {:else}
          <div class="premium-login-microsoft-callback-page__icon is-error premium-login-microsoft-callback-page__shake">
            <i class="fas fa-times-circle"></i>
          </div>
          <h5 class="premium-login-microsoft-callback-page__login-error text-danger">{$_('microsoft.login-error')}</h5>
          <p class="text-muted">{errorMessage}</p>
          <a href={loginHref} class="premium-login-microsoft-callback-page__action btn btn-secondary">
            {$_('microsoft.back-to-login')}
          </a>
        {/if}
      </div>
    </div>
    <div class="premium-login-microsoft-callback-page__slot-mount" aria-hidden="true">
      {#each contentItems as item (item.id)}
        {#if item.id !== 'login-form' && item.component && typeof item.component !== 'function'}
          {@const Field = item.component.default || item.component}
          <Field data={{ pageType: 'login' }} />
        {/if}
      {/each}
    </div>
  </div>
{/if}

<script module>
  // The page the Microsoft sign-in returns to (the panel's own sign-in page registers it from main.js).
  export const view = { path: '/premium-login/callback' };
</script>

<script>
    import { derived } from 'svelte/store';
    import { _ as i18n } from '@panomc/sdk/utils/language';
    import { pano } from '../state.js';
    import {onMount, tick} from 'svelte';
    import {goto, invalidateAll} from '@panomc/sdk/svelte';

    // plugin translations: $_('key') reads plugins.pano-plugin-premium-login.key
    const _ = derived(i18n, ($_fn) => (key, options) => $_fn(`plugins.pano-plugin-premium-login.${key}`, options));

    let { data } = $props();

  const isBrowser = typeof window !== 'undefined';

  // On a SERVERS install this page is served by the panel, which has its own sign-in page and
  // finishes the session itself (panel-access check included).
  const inPanel = !!(pano?.isPanel && pano.ui.auth?.login?.complete);
  const loginHref = pano?.isPanel ? '/panel/login' : '/login';

  let callbackState = $state('loading');
  let errorMessage = $state('');
  let mcUsername = $state('');

  /** `key` translated, or null when the plugin has no such key. */
  function translated(key) {
    const text = $_(key);
    return text && text !== `plugins.pano-plugin-premium-login.${key}` ? text : null;
  }

  /** The visitor's text for an API error: its reason code, then its code, else the generic failure. */
  function errorText(error) {
    const reason = error?.details?.reason;
    return (reason && translated(`microsoft.errors.${reason}`))
      || (error?.code && translated(`fallback.errors.${error.code}`))
      || $_('fallback.errors.MICROSOFT_LOGIN_FAILED');
  }

  // Login-content items of the active UI (e.g. auth-guard's two-factor prompt). Mounted (hidden) only while a
  // sign-in waits for a step-up, so their fetch hooks can answer /api/v1/auth/complete-pending.
  let contentItems = $state([]);
  let contentUnsub = null;

  $effect(() => {
    return () => {
      if (contentUnsub) contentUnsub();
    };
  });

  // The account asks for more than the Microsoft sign-in (two-factor, …): finish through the core pending session.
  async function completePending(pendingToken) {
    if (pano?.ui?.auth?.login?.load) {
      try {
        await pano.ui.auth.login.load();
        contentUnsub = pano.ui.auth.login.content.get().subscribe((items) => {
          contentItems = items || [];
        });
      } catch (e) {
        console.error('[pano-plugin-premium-login] Failed to load login content', e);
      }
    }
    await tick();

    const res = await fetch('/api/v1/auth/complete-pending', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ pendingToken }),
    });
    return res.json();
  }

  onMount(async () => {
    const url = new URL(window.location.href);
    const code = url.searchParams.get('code');
    const state = url.searchParams.get('state');
    const errorParam = url.searchParams.get('error');

    if (errorParam) {
      callbackState = 'error';
      // Microsoft's own description is English: keep it in the console only.
      console.error('[pano-plugin-premium-login] Microsoft sign-in error:', errorParam, url.searchParams.get('error_description'));
      errorMessage = $_('fallback.errors.MICROSOFT_LOGIN_FAILED');
      return;
    }

    if (!code) {
      callbackState = 'error';
      errorMessage = $_('microsoft.no-code');
      return;
    }

    try {
      const res = await fetch('/api/plugins/pano-plugin-premium-login/microsoft/process', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ code, state: state || '' }),
      });
      const body = await res.json();

      if (body.error) {
        callbackState = 'error';
        errorMessage = errorText(body.error);
        return;
      }

      if (body.action === 'complete-required') {
        const completed = await completePending(body.pendingToken || '');
        if (completed.error) {
          callbackState = 'error';
          errorMessage = errorText(completed.error);
          return;
        }
        Object.assign(body, completed, { action: 'login' });
      }

      if (body.action === 'login' && inPanel) {
        mcUsername = body.mcUsername || '';
        callbackState = 'success';

        const result = await pano.ui.auth.login.complete(body.csrfToken);
        if (result === 'NO_PANEL_ACCESS') {
          callbackState = 'error';
          errorMessage = $_('microsoft.no-panel-access');
        }
      } else if (body.action === 'login') {
        mcUsername = body.mcUsername || '';
        callbackState = 'success';

        // The login cookie is set: reload the layout data so the session (user, CSRF token) is the new one.
        // (A view cannot read the host's session store from a Svelte context, so it asks the host to refresh it.)
        await invalidateAll();

        setTimeout(() => goto('/'), 1500);
      } else if (body.action === 'link') {
        mcUsername = body.mcUsername || '';
        callbackState = 'linked';
        setTimeout(() => (inPanel ? goto('/panel') : goto('/profile/settings')), 1500);
      }
    } catch (e) {
      console.error('[pano-plugin-premium-login] Process error:', e);
      callbackState = 'error';
      errorMessage = $_('set-username.error.CONNECTION_ERROR');
    }
  });
</script>
