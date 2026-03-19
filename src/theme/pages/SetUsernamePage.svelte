<style>
  .set-username-container {
    max-width: 480px;
    margin: 0 auto;
    padding: 2rem 1rem;
  }

  .set-username-card {
    border-radius: 1rem;
    box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
    overflow: hidden;
  }

  .set-username-card .card-body {
    padding: 2rem;
  }
</style>

<div class="set-username-container animate__animated animate__fadeIn">
  <div class="card set-username-card">
    <div class="card-body">
      <h5 class="card-title text-center">{$_('set-username.title')}</h5>
      <p class="text-muted text-center">{$_('set-username.description')}</p>

      {#if error}
        <div class="alert alert-danger py-2" role="alert">{error}</div>
      {/if}

      {#if success}
        <div class="alert alert-success py-2 text-center" role="alert">
          {$_('set-username.success')}
        </div>
        <a href="/login" class="btn btn-secondary w-100 mt-2">
          {$_('set-username.go-to-login')}
        </a>
      {:else}
        <form onsubmit={(e) => { e.preventDefault(); onSubmit(); }}>
          <div class="vstack gap-3">
            <div class="form-floating">
              <input
                type="text"
                id="newUsername"
                class="form-control"
                disabled={loading}
                maxlength="16"
                bind:value={newUsername} />
              <label for="newUsername">{$_('set-username.input-label')}</label>
            </div>
            <button
              type="submit"
              class="btn btn-lg btn-secondary w-100"
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

<script>
    import {_} from '../../main.js';
    import {onMount} from 'svelte';
    import ApiUtil from '@panomc/sdk/utils/api';

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
      const body = await ApiUtil.post({
        path: '/api/premium-login/set-username',
        body: { token, newUsername },
      });

      if (body.result !== 'ok') {
        const errorCode = body.error || 'UNKNOWN';
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
