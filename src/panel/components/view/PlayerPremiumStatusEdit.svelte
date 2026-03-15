<div class="row pt-2 pb-3 mb-3 border-bottom animate__animated animate__fadeIn">
  <label class="col-md-4 col-form-label pb-2">
    <div class="d-flex align-items-center gap-2 mb-2">
      <i class="fas fa-shield-alt text-secondary"></i>
      <span class="fw-bold">{$_('panel-edit.title')}</span>
    </div>
    <p class="text-secondary small mt-1 mb-0">{$_('panel-edit.desc')}</p>
  </label>

  <div class="col col-form-label">
    <div class="d-flex align-items-center gap-3">
      <select class="form-select form-select-sm" bind:value={accountType} style="max-width: 200px;">
        <option value="PREMIUM">{$_('account-type.premium')}</option>
        <option value="CRACKED">{$_('account-type.cracked')}</option>
        <option value="AUTO">{$_('account-type.auto')}</option>
      </select>

      {#if loading}
        <div class="spinner-border spinner-border-sm text-secondary" role="status"></div>
      {/if}
    </div>

    {#if currentSource}
      <div class="text-muted small mt-2">
        <i class="fas fa-info-circle me-1"></i>
        {$_('panel-edit.source-info', { values: { source: currentSource } })}
      </div>
    {/if}
  </div>
</div>

<script>
  import {_} from '../../../main.js';
  import ApiUtil from '@panomc/sdk/utils/api';

  export let playerData;
  export let onHookRegister;

  let loading = true;
  let accountType = 'AUTO';
  let initialType = 'AUTO';
  let currentSource = '';

  loadData();

  async function loadData() {
    try {
      const res = await ApiUtil.get({
        path: `/api/panel/premium-login/player/${playerData?.username}`,
      });

      if (res && !res.error) {
        accountType = res.accountType || 'AUTO';
        initialType = accountType;
        currentSource = res.source || '';
      }
    } catch (e) {
      console.error('[pano-plugin-premium-login] Failed to load status', e);
    } finally {
      loading = false;
    }
  }

  $: isDirty = accountType !== initialType;

  $: if (onHookRegister) {
    onHookRegister({
      _id: 'pano-plugin-premium-login-edit',
      isDirty: isDirty,
      save: saveStatus,
    });
  }

  async function saveStatus() {
    if (!isDirty) return;

    loading = true;

    try {
      const result = await ApiUtil.put({
        path: `/api/panel/premium-login/player/${playerData?.username}`,
        body: { accountType },
      });

      if (result && result.error) {
        console.error('[pano-plugin-premium-login] Save failed:', result.error);
        return;
      }

      initialType = accountType;
      currentSource = 'PANEL';
    } catch (e) {
      console.error('[pano-plugin-premium-login] Save error:', e);
    } finally {
      loading = false;
    }
  }
</script>
