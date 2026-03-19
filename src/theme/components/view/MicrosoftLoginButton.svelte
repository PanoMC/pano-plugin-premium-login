<style>
  .microsoft-login-wrapper {
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
</style>

{#if $microsoftLoginEnabled}
  <div class="microsoft-login-wrapper">
    <button
      class="btn btn-microsoft"
      type="button"
      disabled={loading}
      onclick={startMicrosoftLogin}>
      {#if loading}
        <span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>
        <span>{$_('microsoft.login-loading')}</span>
      {:else}
        <i class="fas fa-cube"></i>
        <span>{$_('microsoft.login-button')}</span>
      {/if}
    </button>
    {#if errorMessage}
      <div class="alert alert-danger py-2 mt-2 small animate__animated animate__shakeX">{errorMessage}</div>
    {/if}
  </div>
{/if}

<script>
    import {_, microsoftLoginEnabled} from '../../../main.js';

    let loading = $state(false);
  let errorMessage = $state('');

  async function startMicrosoftLogin() {
    loading = true;
    errorMessage = '';

    try {
      const res = await fetch('/api/premium-login/microsoft/login');
      const body = await res.json();

      if (body.result === 'ok' && body.redirectUrl) {
        window.location.href = body.redirectUrl;
        return;
      }

      errorMessage = body.error || $_('microsoft.login-error');
    } catch (e) {
      console.error('[pano-plugin-premium-login] Microsoft login init failed', e);
      errorMessage = $_('microsoft.login-error');
    }
    loading = false;
  }
</script>
