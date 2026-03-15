<div class="row pt-2 pb-3 mb-3 border-bottom animate__animated animate__fadeIn">
  <label class="col-md-4 col-form-label pb-2">
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
        <select class="form-select form-select-sm" bind:value={accountType} style="max-width: 200px;" disabled={saving}>
          <option value="PREMIUM">{$_('account-type.premium')}</option>
          <option value="CRACKED">{$_('account-type.cracked')}</option>
          <option value="AUTO">{$_('account-type.auto')}</option>
        </select>

        {#if saving}
          <div class="spinner-border spinner-border-sm text-primary" role="status"></div>
        {:else if isDirty}
          <button class="btn btn-sm btn-primary" onclick={saveStatus}>
            <i class="fas fa-save me-1"></i>
            {$_('settings.save')}
          </button>
        {:else if saved}
          <span class="text-success small">
            <i class="fas fa-check me-1"></i>
            {$_('settings.saved')}
          </span>
        {/if}
      </div>
      <div class="text-muted small mt-2">
        <i class="fas fa-info-circle me-1"></i>
        {$_('settings.info')}
      </div>
    {/if}
  </div>
</div>

<script module>
    import ApiUtil from '@panomc/sdk/utils/api';

    export async function load(event) {
    try {
      const { session: { user } } = await event.parent();
      if (!user) return { status: null };

      const res = await ApiUtil.get({
        path: `/api/premium-login/player/${user.username}`,
        request: event,
      });

      return {
        status: res && !res.error ? res : null,
      };
    } catch (e) {
      console.error('[pano-plugin-premium-login] Failed to load status', e);
      return { status: null };
    }
  }
</script>

<script>
  import { _ } from '../../../main.js';

  let { data } = $props();

  let loading = $state(!data?.status);
  let accountType = $state(data?.status?.accountType || 'AUTO');
  let initialType = $state(data?.status?.accountType || 'AUTO');
  let saving = $state(false);
  let saved = $state(false);

  let isDirty = $derived(accountType !== initialType);

  async function saveStatus() {
    if (!isDirty) return;

    saving = true;
    saved = false;

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
      saved = true;

      setTimeout(() => { saved = false; }, 3000);
    } catch (e) {
      console.error('[pano-plugin-premium-login] Save error:', e);
    } finally {
      saving = false;
    }
  }
</script>
