<style>

  .premium-login-set-username-page {
    max-width: 480px;
    margin: 0 auto;
    padding: 2rem 1rem;
  }

  .premium-login-set-username-page__body {
    padding: 2rem;
  }

  .premium-login-set-username-page__fade-in {
    animation: premium-login-fade-in 250ms both;
  }

  @keyframes premium-login-fade-in {
    from { opacity: 0; }
    to { opacity: 1; }
  }
</style>

<div class="premium-login-set-username-page premium-login-set-username-page__fade-in">
  <div class="card">
    <div class="premium-login-set-username-page__body card-body">
      <h5 class="premium-login-set-username-page__title card-title text-center">{$_('set-username.title')}</h5>
      <p class="text-muted text-center">{$_('set-username.description')}</p>

      {#if error}
        <div class="premium-login-set-username-page__alert alert alert-danger py-2" role="alert">{error}</div>
      {/if}

      {#if success}
        <div class="premium-login-set-username-page__success alert alert-success py-2 text-center" role="alert">
          {$_('set-username.success')}
        </div>
        <a href={pano?.isPanel ? '/panel/login' : '/login'} class="premium-login-set-username-page__action btn btn-secondary w-100 mt-2">
          {$_('set-username.go-to-login')}
        </a>
      {:else}
        <form onsubmit={(e) => { e.preventDefault(); onSubmit(); }}>
          <div class="vstack gap-3">
            <div class="form-floating">
              <input
                type="text"
                id="newUsername"
                class="premium-login-set-username-page__input form-control"
                disabled={loading}
                maxlength="16"
                bind:value={newUsername} />
              <label for="newUsername">{$_('set-username.input-label')}</label>
            </div>
            <button
              type="submit"
              class="premium-login-set-username-page__saving btn btn-lg btn-secondary w-100"
              class:disabled={loading || !newUsername || newUsername.length < 3}
              disabled={loading || !newUsername || newUsername.length < 3}>
              {#if loading}
                <span class="spinner-border spinner-border-sm me-2" role="status" aria-label="Loading"></span>
                <span>{$_('set-username.saving')}...</span>
              {:else}
                {$_('set-username.save')}
              {/if}
            </button>
          </div>
        </form>
      {/if}
    </div>
  </div>
</div>

<script module>
  // The page the username-recovery mail links to.
  export const view = { path: '/set-username' };
</script>

<script>
    import { derived } from 'svelte/store';
    import { _ as i18n } from '@panomc/sdk/utils/language';
    import { pano } from '../state.js';
    import {onMount} from 'svelte';
    import { api } from '@panomc/sdk/plugin-api';

    // plugin translations: $_('key') reads plugins.pano-plugin-premium-login.key
    const _ = derived(i18n, ($_fn) => (key, options) => $_fn(`plugins.pano-plugin-premium-login.${key}`, options));

    const ERROR_KEYS = {
    REGISTER_USERNAME_NOT_AVAILABLE: 'set-username.error.REGISTER_USERNAME_NOT_AVAILABLE',
    REGISTER_USERNAME_TOO_SHORT: 'set-username.error.REGISTER_USERNAME_TOO_SHORT',
    REGISTER_USERNAME_TOO_LONG: 'set-username.error.REGISTER_USERNAME_TOO_LONG',
    REGISTER_INVALID_USERNAME: 'set-username.error.REGISTER_INVALID_USERNAME',
    INVALID_TOKEN: 'set-username.error.INVALID_TOKEN',
  };

  let newUsername = $state('');
  let loading = $state(false);
  let error = $state(null);
  let success = $state(false);
  let token = $state(null);

  onMount(() => {
    const url = new URL(window.location.href);
    token = url.searchParams.get('token');

    if (!token) {
      error = $_('set-username.error.INVALID_TOKEN');
    }
  });

  async function onSubmit() {
    if (!token) {
      error = $_('set-username.error.INVALID_TOKEN');
      return;
    }

    loading = true;
    error = null;

    try {
      const body = await api.post({
        path: '/set-username',
        body: { token, newUsername },
      });

      if (body.error) {
        const errorCode = body.error.code || 'UNKNOWN';
        const translationKey = ERROR_KEYS[errorCode];
        error = translationKey ? $_(translationKey) : errorCode;
        loading = false;
        return;
      }

      success = true;
    } catch (err) {
      console.error(err);
      error = $_('set-username.error.CONNECTION_ERROR');
    } finally {
      loading = false;
    }
  }
</script>
