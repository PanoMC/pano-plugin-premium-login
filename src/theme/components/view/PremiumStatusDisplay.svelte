<div class="row pt-2 pb-3 mb-3 border-bottom animate__animated animate__fadeIn">
  <label class="col-md-4 col-form-label pb-2" for="premiumStatusSelect">
    <div class="d-flex align-items-center gap-2 mb-2">
      <i class="fas fa-shield-alt text-secondary"></i>
      <span class="fw-bold">{$_('settings.title')}</span>
    </div>
    <p class="text-secondary small mt-1 mb-0">{$_('settings.desc')}</p>
  </label>

  <div class="col col-form-label">
    {#if loading}
      <div class="d-flex align-items-center gap-2">
        <div class="spinner-border spinner-border-sm text-secondary" role="status"></div>
      </div>
    {:else}
      <div class="d-flex align-items-center gap-3">
        <select class="form-select form-select-sm" id="premiumStatusSelect" bind:value={accountType} style="max-width: 200px;" disabled={saving}>
          <option value="PREMIUM">{$_('account-type.premium')}</option>
          <option value="CRACKED">{$_('account-type.cracked')}</option>
          <option value="AUTO">{$_('account-type.auto')}</option>
        </select>

        {#if saving}
          <div class="spinner-border spinner-border-sm text-primary" role="status"></div>
        {/if}
      </div>
      <div class="text-muted small mt-2">
        <i class="fas fa-info-circle me-1"></i>
        {#if accountType === 'PREMIUM'}
          {$_('settings.info-premium')}
        {:else if accountType === 'CRACKED'}
          {$_('settings.info-cracked')}
        {:else}
          {$_('settings.info-auto')}
        {/if}
      </div>

      {#if microsoftLoginEnabled}
        {#if premiumLinked}
          <div class="text-success small mt-2">
            <i class="fas fa-check-circle me-1"></i>
            {$_('settings.linked')}
            <button
              class="btn btn-link btn-sm p-0 ms-2 text-danger"
              type="button"
              disabled={unlinking}
              onclick={unlinkAccount}>
              {#if unlinking}
                <span class="spinner-border spinner-border-sm me-1" role="status"></span>
              {/if}
              {$_('settings.unlink-button')}
            </button>
          </div>
        {:else}
          <div class="text-muted small mt-2">
            <i class="fas fa-link me-1"></i>
            {$_('settings.link-desc')}
            <button
              class="btn btn-link btn-sm p-0 ms-1"
              type="button"
              disabled={linking}
              onclick={startLinkAccount}>
              {#if linking}
                <span class="spinner-border spinner-border-sm me-1" role="status"></span>
              {/if}
              {$_('settings.link-button')}
            </button>
          </div>
        {/if}
      {/if}
    {/if}
  </div>
</div>

<script module>
    import ApiUtil from '@panomc/sdk/utils/api';

    export async function load(event) {
    try {
      const { session: { user } } = await event.parent();
      if (!user) return { status: null, microsoftLoginEnabled: false };

      const [statusRes, configRes] = await Promise.all([
        ApiUtil.get({
          path: `/api/premium-login/player/${user.username}`,
          request: event,
        }),
        ApiUtil.get({
          path: '/api/premium-login/config',
          request: event,
        }),
      ]);

      return {
        status: statusRes && !statusRes.error ? statusRes : null,
        microsoftLoginEnabled: configRes?.microsoftLoginEnabled || false,
      };
    } catch (e) {
      console.error('[pano-plugin-premium-login] Failed to load status', e);
      return { status: null, microsoftLoginEnabled: false };
    }
  }
</script>

<script>
  import { untrack } from 'svelte';
  import { _ } from '../../../main.js';

  let { onRegister, data } = $props();

  let loading = $derived(!data?.status);
  let initialType = $state(data?.status?.accountType || 'AUTO');
  let accountType = $state(data?.status?.accountType || 'AUTO');
  let premiumLinked = $derived(data?.status?.premiumLinked || false);
  let microsoftLoginEnabled = $derived(data?.microsoftLoginEnabled || false);
  let saving = $state(false);
  let linking = $state(false);
  let unlinking = $state(false);

  $effect(() => {
    const status = data?.status;
    untrack(() => {
      if (status?.accountType) {
        const apiType = status.accountType;
        if (initialType !== apiType) initialType = apiType;
        if (!isDirty && accountType !== apiType) {
          accountType = apiType;
        }
      }
    });
  });

  let isDirty = $derived(accountType !== initialType);

  // Register with parent save form (same pattern as avatar plugin)
  $effect(() => {
    const _dirty = isDirty;
    if (onRegister) {
      onRegister({
        get isDirty() { return isDirty },
        save: saveStatus,
      });
    }
  });

  async function saveStatus() {
    if (!isDirty) return;

    saving = true;

    try {
      const res = await fetch('/api/premium-login/me', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ accountType }),
      });

      const result = await res.json();

      if (result && result.error) {
        console.error('[pano-plugin-premium-login] Save failed:', result.error);
        return;
      }

      initialType = accountType;

      // Notify parent to update dirty totals after save
      if (onRegister) {
        onRegister({
          get isDirty() { return isDirty },
          save: saveStatus,
        });
      }
    } catch (e) {
      console.error('[pano-plugin-premium-login] Save error:', e);
    } finally {
      saving = false;
    }
  }

  async function startLinkAccount() {
    linking = true;
    try {
      const res = await ApiUtil.get({
        path: '/api/premium-login/microsoft/link',
      });

      if (res?.redirectUrl) {
        window.location.href = res.redirectUrl;
        return; // Don't reset linking since we're navigating away
      }
    } catch (e) {
      console.error('[pano-plugin-premium-login] Link error:', e);
    }
    linking = false;
  }

  async function unlinkAccount() {
    unlinking = true;
    try {
      const res = await fetch('/api/premium-login/microsoft/unlink', {
        method: 'POST',
      });
      const result = await res.json();

      if (result?.result === 'ok') {
        // Reload to reflect changes
        window.location.reload();
        return;
      }
    } catch (e) {
      console.error('[pano-plugin-premium-login] Unlink error:', e);
    }
    unlinking = false;
  }
</script>
