<style>
  .callback-container {
    max-width: 480px;
    margin: 0 auto;
    padding: 2rem 1rem;
  }

  .callback-card {
    border-radius: 1rem;
    box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
    overflow: hidden;
  }

  .callback-card .card-body {
    padding: 2rem;
  }

  .status-icon {
    font-size: 3rem;
    margin-bottom: 1rem;
  }

  .status-icon.success {
    color: var(--bs-success);
  }

  .status-icon.error {
    color: var(--bs-danger);
  }

  .status-icon.loading {
    color: var(--bs-primary);
  }
</style>

{#if !isBrowser}
  <!-- SSR: show loading placeholder -->
  <div class="callback-container">
    <div class="card callback-card">
      <div class="card-body text-center">
        <div class="status-icon loading">
          <div class="spinner-border" role="status"></div>
        </div>
      </div>
    </div>
  </div>
{:else}
  <div class="callback-container animate__animated animate__fadeIn">
    <div class="card callback-card">
      <div class="card-body text-center">
        {#if callbackState === 'loading'}
          <div class="status-icon loading">
            <div class="spinner-border" role="status"></div>
          </div>
          <h5>{$_('microsoft.login-loading')}</h5>
        {:else if callbackState === 'success'}
          <div class="status-icon success animate__animated animate__bounceIn">
            <i class="fas fa-check-circle"></i>
          </div>
          <h5 class="text-success">{$_('microsoft.login-success', { values: { username: mcUsername } })}</h5>
          <p class="text-muted">{$_('microsoft.redirecting')}</p>
        {:else if callbackState === 'linked'}
          <div class="status-icon success animate__animated animate__bounceIn">
            <i class="fas fa-link"></i>
          </div>
          <h5 class="text-success">{$_('microsoft.link-success', { values: { username: mcUsername } })}</h5>
          <p class="text-muted">{$_('microsoft.redirecting')}</p>
        {:else}
          <div class="status-icon error animate__animated animate__shakeX">
            <i class="fas fa-times-circle"></i>
          </div>
          <h5 class="text-danger">{$_('microsoft.login-error')}</h5>
          <p class="text-muted">{errorMessage}</p>
          <a href={loginHref} class="btn btn-secondary">
            {$_('microsoft.back-to-login')}
          </a>
        {/if}
      </div>
    </div>
  </div>
{/if}

<script>
    import {_, pano} from '../../main.js';
    import {getContext, onMount} from 'svelte';
    import {goto} from '@panomc/sdk/svelte';

    let { data } = $props();

  const isBrowser = typeof window !== 'undefined';

  // On a SERVERS install this page is served by the panel, which has its own sign-in page and
  // finishes the session itself (panel-access check included).
  const inPanel = !!(pano?.isPanel && pano.ui.auth?.login?.complete);
  const loginHref = pano?.isPanel ? '/panel/login' : '/login';

  let session;
  let callbackState = $state('loading');
  let errorMessage = $state('');
  let mcUsername = $state('');

  try {
    session = getContext('session');
  } catch (e) {
    // SSR or no context available — session will be null
  }

  onMount(async () => {
    const url = new URL(window.location.href);
    const code = url.searchParams.get('code');
    const state = url.searchParams.get('state');
    const errorParam = url.searchParams.get('error');

    if (errorParam) {
      callbackState = 'error';
      errorMessage = url.searchParams.get('error_description') || errorParam;
      return;
    }

    if (!code) {
      callbackState = 'error';
      errorMessage = 'No authorization code received.';
      return;
    }

    try {
      const res = await fetch('/api/premium-login/microsoft/process', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ code, state: state || '' }),
      });
      const body = await res.json();

      if (body.error) {
        callbackState = 'error';
        errorMessage = body.error;
        return;
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

        // Update session with new credentials
        const csrfToken = body.csrfToken;
        const credsRes = await fetch('/api/auth/credentials', {
          headers: { 'X-CSRF-Token': csrfToken }
        });
        const creds = await credsRes.json();

        if (session) {
          session.update((sessionData) => {
            sessionData.user = {
              ...Object.keys(creds)
                .filter((key) => !['result'].includes(key))
                .reduce((object, key) => {
                  object[key] = creds[key];
                  return object;
                }, {})
            };
            sessionData.csrfToken = csrfToken;
            return sessionData;
          });
        }

        setTimeout(() => goto('/'), 1500);
      } else if (body.action === 'link') {
        mcUsername = body.mcUsername || '';
        callbackState = 'linked';
        setTimeout(() => (inPanel ? goto('/panel') : goto('/profile/settings')), 1500);
      }
    } catch (e) {
      console.error('[pano-plugin-premium-login] Process error:', e);
      callbackState = 'error';
      errorMessage = 'Connection error';
    }
  });
</script>
