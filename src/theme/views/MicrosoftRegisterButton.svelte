<style>

  .premium-login-microsoft-register-button {
    position: relative;
  }

  .premium-login-microsoft-register-button__button {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 0.5rem;
    width: 100%;
    padding: 0.65rem 1rem;
    font-weight: 500;
  }

  .premium-login-microsoft-register-button__verified {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.5rem 0.75rem;
    font-weight: 500;
    font-size: 0.9rem;
  }

  .premium-login-microsoft-register-button__shake {
    animation: premium-login-shake 250ms both;
  }

  @keyframes premium-login-shake {
    from, to { transform: translate3d(0, 0, 0); }
    10%, 30%, 50%, 70%, 90% { transform: translate3d(-10px, 0, 0); }
    20%, 40%, 60%, 80% { transform: translate3d(10px, 0, 0); }
  }
</style>

{#if $microsoftLoginEnabled}
  <div class="premium-login-microsoft-register-button">
    {#if mcUsername}
      <div class="premium-login-microsoft-register-button__verified mb-2">
        <i class="fas fa-check-circle"></i>
        <span>{$_('microsoft.verified', { values: { username: mcUsername } })}</span>
      </div>
    {:else}
      <button
        class="premium-login-microsoft-register-button__button btn"
        type="button"
        disabled={loading}
        onclick={startMicrosoftRegister}>
        {#if loading}
          <span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>
          <span>{$_('microsoft.login-loading')}</span>
        {:else}
          <i class="fas fa-cube"></i>
          <span>{$_('microsoft.register-button')}</span>
        {/if}
      </button>
    {/if}

    {#if errorMessage}
      <div class="alert alert-danger py-2 mt-2 small premium-login-microsoft-register-button__shake">{errorMessage}</div>
    {/if}
  </div>
{/if}

<script>
    import { derived } from 'svelte/store';
    import { _ as i18n } from '@panomc/sdk/utils/language';
    import { microsoftLoginEnabled } from '../state.js';
    import {onMount} from 'svelte';

    // plugin translations: $_('key') reads plugins.pano-plugin-premium-login.key
    const _ = derived(i18n, ($_fn) => (key, options) => $_fn(`plugins.pano-plugin-premium-login.${key}`, options));

    let loading = $state(false);
  let errorMessage = $state('');
  let mcUsername = $state('');

  onMount(() => {
    // Check for pre-filled Minecraft username from callback redirect
    const urlParams = new URLSearchParams(window.location.search);
    const username = urlParams.get('mcUsername');

    if (username) {
      mcUsername = username;
    }
  });

  async function startMicrosoftRegister() {
    loading = true;
    errorMessage = '';

    try {
      const res = await fetch('/api/plugins/pano-plugin-premium-login/microsoft/register');
      const body = await res.json();

      if (!body.error && body.redirectUrl) {
        window.location.href = body.redirectUrl;
        return;
      }

      errorMessage = body.error?.code || $_('microsoft.login-error');
    } catch (e) {
      console.error('[pano-plugin-premium-login] Microsoft register init failed', e);
      errorMessage = $_('microsoft.login-error');
    }
    loading = false;
  }
</script>
