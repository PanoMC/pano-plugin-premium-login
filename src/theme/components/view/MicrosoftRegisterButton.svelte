<style>
  .microsoft-register-wrapper {
    position: relative;
  }

  .btn-microsoft {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 0.5rem;
    width: 100%;
    padding: 0.65rem 1rem;
    border-radius: 0.5rem;
    font-weight: 500;
    background: linear-gradient(135deg, #107c10 0%, #0e6b0e 100%);
    border: none;
    color: #fff;
    transition: all 0.2s ease;
    box-shadow: 0 2px 8px rgba(16, 124, 16, 0.25);
  }

  .btn-microsoft:hover:not(:disabled) {
    background: linear-gradient(135deg, #0e6b0e 0%, #0c5c0c 100%);
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(16, 124, 16, 0.35);
    color: #fff;
  }

  .btn-microsoft:active:not(:disabled) {
    transform: translateY(0);
  }

  .btn-microsoft:disabled {
    opacity: 0.65;
    cursor: not-allowed;
  }

  .mc-verified {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.5rem 0.75rem;
    border-radius: 0.5rem;
    background: rgba(16, 124, 16, 0.1);
    border: 1px solid rgba(16, 124, 16, 0.3);
    color: #107c10;
    font-weight: 500;
    font-size: 0.9rem;
  }
</style>

{#if $microsoftLoginEnabled}
  <div class="microsoft-register-wrapper">
    {#if mcUsername}
      <div class="mc-verified mb-2">
        <i class="fas fa-check-circle"></i>
        <span>{$_('microsoft.verified', { values: { username: mcUsername } })}</span>
      </div>
    {:else}
      <button
        class="btn btn-microsoft"
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
      <div class="alert alert-danger py-2 mt-2 small animate__animated animate__shakeX">{errorMessage}</div>
    {/if}
  </div>
{/if}

<script>
    import {_, microsoftLoginEnabled} from '../../../main.js';
    import {onMount} from 'svelte';

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
      const res = await fetch('/api/premium-login/microsoft/register');
      const body = await res.json();

      if (body.result === 'ok' && body.redirectUrl) {
        window.location.href = body.redirectUrl;
        return;
      }

      errorMessage = body.error || $_('microsoft.login-error');
    } catch (e) {
      console.error('[pano-plugin-premium-login] Microsoft register init failed', e);
      errorMessage = $_('microsoft.login-error');
    }
    loading = false;
  }
</script>
