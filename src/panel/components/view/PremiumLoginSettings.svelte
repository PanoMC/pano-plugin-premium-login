{#if addon?.id === 'pano-plugin-premium-login'}
  <div class="card">
    <div class="card-header">
      {$_('plugin-settings.title')}
    </div>
    <div class="card-body">
      <!-- Setup Guide -->
      <div class="alert alert-info mb-4">
        <h6 class="alert-heading mb-2"><i class="fas fa-info-circle me-1"></i> {$_('plugin-settings.setup-guide-title')}</h6>
        <ol class="mb-2 ps-3">
          <li><a href="https://portal.azure.com/#blade/Microsoft_AAD_RegisteredApps/ApplicationsListBlade" target="_blank" rel="noopener">Azure Portal → App Registrations</a> {$_('plugin-settings.setup-step-1')}</li>
          <li>{$_('plugin-settings.setup-step-2')}</li>
          <li>{$_('plugin-settings.setup-step-3')}</li>
          <li>{$_('plugin-settings.setup-step-4')}</li>
          <li>{$_('plugin-settings.setup-step-5')}</li>
        </ol>
        <hr />
        <p class="mb-0 small">
          <i class="fas fa-exclamation-triangle text-warning me-1"></i>
          {@html $_('plugin-settings.verification-notice')}
        </p>
      </div>

      <!-- Microsoft Login Enabled -->
      <div class="row mb-3">
        <label class="col-md-6 col-form-label" for="microsoftLoginEnabled">
          <span class="d-block">
            {$_('plugin-settings.enabled')}
          </span>
          <small>
            {$_('plugin-settings.enabled-desc')}
          </small>
        </label>
        <div class="col-md-6 d-flex align-items-center">
          <div class="form-check form-switch">
            <input
              class="form-check-input"
              type="checkbox"
              id="microsoftLoginEnabled"
              bind:checked={config.microsoftLoginEnabled} />
          </div>
        </div>
      </div>

      {#if config.microsoftLoginEnabled}
        <!-- Client ID -->
        <div class="row mb-3 animate__animated animate__fadeIn">
          <label class="col-md-6 col-form-label" for="microsoftClientId">
            {$_('plugin-settings.client-id')}
          </label>
          <div class="col-md-6">
            <input
              type="text"
              class="form-control"
              id="microsoftClientId"
              bind:value={config.microsoftClientId} />
            <small class="d-block mt-2">
              {$_('plugin-settings.client-id-desc')}
            </small>
          </div>
        </div>

        <!-- Client Secret -->
        <div class="row mb-3 animate__animated animate__fadeIn">
          <label class="col-md-6 col-form-label" for="microsoftClientSecret">
            {$_('plugin-settings.client-secret')}
          </label>
          <div class="col-md-6">
            {#if hasSecret && !secretRevealed && !editingSecret}
              <!-- Secret is saved but not revealed -->
              <div class="d-flex align-items-center gap-2">
                <input
                  type="password"
                  class="form-control"
                  value="••••••••••••••••"
                  disabled />
                <button
                  class="btn btn-outline-secondary"
                  type="button"
                  on:click={openRevealModal}
                  disabled={revealLoading}>
                  {#if revealLoading}
                    <span class="spinner-border spinner-border-sm" role="status"></span>
                  {:else}
                    <i class="fas fa-eye"></i>
                  {/if}
                </button>
                <button
                  class="btn btn-outline-primary btn-sm"
                  type="button"
                  aria-label="Edit secret"
                  on:click={() => { editingSecret = true; config.microsoftClientSecret = ''; }}>
                  <i class="fas fa-edit"></i>
                </button>
              </div>
            {:else}
              <!-- Revealed or editing new secret -->
              <div class="input-group">
                <input
                  type={showSecret ? 'text' : 'password'}
                  class="form-control"
                  id="microsoftClientSecret"
                  placeholder={hasSecret && editingSecret ? $_('plugin-settings.client-secret-new-placeholder') : ''}
                  bind:value={config.microsoftClientSecret} />
                <button
                  class="btn btn-outline-secondary"
                  type="button"
                  aria-label="Toggle secret visibility"
                  on:click={() => showSecret = !showSecret}>
                  <i class="fas {showSecret ? 'fa-eye-slash' : 'fa-eye'}"></i>
                </button>
                {#if editingSecret}
                  <button
                    class="btn btn-outline-danger"
                    type="button"
                    aria-label="Cancel edit"
                    on:click={() => { editingSecret = false; config.microsoftClientSecret = ''; }}>
                    <i class="fas fa-times"></i>
                  </button>
                {/if}
              </div>
            {/if}
            <small class="d-block mt-2">
              {$_('plugin-settings.client-secret-desc')}
            </small>
          </div>
        </div>

        <!-- Reveal Secret Modal -->
        <div aria-hidden="true" class="modal fade" bind:this={revealModalElement} role="dialog" tabindex="-1">
          <div class="modal-dialog modal-dialog-centered" role="dialog">
            <div class="modal-content">
              <form on:submit|preventDefault={revealSecret}>
                <div class="modal-body text-center">
                  <div class="pb-3">
                    <i class="fas fa-lock fa-3x d-block m-auto text-warning"></i>
                  </div>
                  {$_('plugin-settings.reveal-desc')}

                  <input
                    class="form-control mt-3"
                    placeholder={$_('plugin-settings.reveal-password-placeholder')}
                    type="password"
                    bind:value={revealPassword}
                    bind:this={revealPasswordInput}
                    class:border-danger={revealError} />
                </div>

                <div class="modal-footer flex-nowrap">
                  <button class="btn btn-link col-6 m-0" type="button" on:click={closeRevealModal} disabled={revealLoading}>
                    {$_('plugin-settings.reveal-cancel')}
                  </button>
                  <button
                    class="btn btn-warning col-6 m-0"
                    type="submit"
                    disabled={!revealPassword || revealLoading}>
                    {$_('plugin-settings.reveal-confirm')}
                    {#if revealLoading}
                      <i class="fas fa-sync fa-spin ms-2"></i>
                    {/if}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>

        <!-- Redirect URI -->
        <div class="row mb-3 animate__animated animate__fadeIn">
          <label class="col-md-6 col-form-label" for="microsoftRedirectUri">
            {$_('plugin-settings.redirect-uri')}
          </label>
          <div class="col-md-6">
            <input
              type="text"
              class="form-control"
              id="microsoftRedirectUri"
              placeholder={defaultRedirectUri}
              bind:value={config.microsoftRedirectUri} />
            <small class="d-block mt-2">
              {$_('plugin-settings.redirect-uri-desc')}
            </small>
          </div>
        </div>
      {/if}

      <!-- Debug Logging -->
      <div class="row mb-3">
        <label class="col-md-6 col-form-label" for="debugLogging">
          <span class="d-block">
            {$_('plugin-settings.debug-logging')}
          </span>
          <small>
            {$_('plugin-settings.debug-logging-desc')}
          </small>
        </label>
        <div class="col-md-6 d-flex align-items-center">
          <div class="form-check form-switch">
            <input
              class="form-check-input"
              type="checkbox"
              id="debugLogging"
              bind:checked={config.debugLogging} />
          </div>
        </div>
      </div>

      <div class="mt-4">
        <button class="btn btn-secondary" on:click={saveConfig} disabled={saving || !hasChanges}>
          {#if saving}
            <span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"
            ></span>
          {:else}{/if}
          {$_('settings.save')}
        </button>
      </div>
    </div>
  </div>
{/if}

<script>
    import { _, showSuccessToast, showErrorToast } from '../../../main.js';
    import ApiUtil from '@panomc/sdk/utils/api';

    export let addon;

  let showSecret = false;
  let hasSecret = addon?.config?.hasSecret || false;
  let secretRevealed = false;
  let editingSecret = false;
  let revealModalElement;
  let revealPasswordInput;
  let revealModal;
  let revealPassword = '';
  let revealError = false;
  let revealLoading = false;

  let config = {
    microsoftLoginEnabled: addon?.config?.microsoftLoginEnabled || false,
    microsoftClientId: addon?.config?.microsoftClientId || '',
    microsoftClientSecret: '',
    microsoftRedirectUri: addon?.config?.microsoftRedirectUri || '',
    debugLogging: addon?.config?.debugLogging || false,
  };
  let saving = false;

  // Auto-fill redirect URI if empty
  const defaultRedirectUri = typeof window !== 'undefined'
    ? `${window.location.origin}/api/premium-login/microsoft/callback`
    : '';

  if (!config.microsoftRedirectUri && defaultRedirectUri) {
    config.microsoftRedirectUri = defaultRedirectUri;
  }

  // Keep track of initial config to determine if there are changes
  let initialConfig = JSON.parse(JSON.stringify(config));

  $: hasChanges = JSON.stringify(config) !== JSON.stringify(initialConfig);

  function openRevealModal() {
    revealModal = new window.bootstrap.Modal(revealModalElement, {
      backdrop: 'static',
      keyboard: false,
    });
    revealLoading = false;
    revealError = false;
    revealPassword = '';
    revealModal.show();

    setTimeout(() => {
      if (revealPasswordInput) revealPasswordInput.focus();
    }, 500);
  }

  function closeRevealModal() {
    if (revealModal) revealModal.hide();
    revealPassword = '';
    revealError = false;
  }

  async function revealSecret() {
    if (!revealPassword || revealLoading) return;
    revealLoading = true;
    revealError = false;

    try {
      const res = await ApiUtil.post({
        path: '/api/panel/premium-login/config/reveal-secret',
        body: { password: revealPassword },
      });

      if (res.error) {
        revealError = true;
        revealLoading = false;
        return;
      }

      config.microsoftClientSecret = res.microsoftClientSecret;
      initialConfig.microsoftClientSecret = res.microsoftClientSecret;
      secretRevealed = true;
      showSecret = true;
      closeRevealModal();
    } catch (e) {
      revealError = true;
    } finally {
      revealLoading = false;
    }
  }

  async function saveConfig() {
    if (saving) return;
    saving = true;
    try {
      const body = { ...config };

      // If secret is not changed (not editing and not revealed with changes), don't send it
      if (hasSecret && !editingSecret && !secretRevealed) {
        delete body.microsoftClientSecret;
      }

      await ApiUtil.put({
        path: '/api/panel/premium-login/config',
        body,
      });
      addon.config = { ...config, hasSecret: config.microsoftClientSecret ? true : hasSecret };
      hasSecret = addon.config.hasSecret;
      editingSecret = false;
      initialConfig = JSON.parse(JSON.stringify(config));
      await showSuccessToast('components.toasts.settings-save-success');
    } catch (e) {
      console.error('Failed to save premium login config', e);
      await showErrorToast('components.toasts.settings-save-error');
    } finally {
      saving = false;
    }
  }
</script>
