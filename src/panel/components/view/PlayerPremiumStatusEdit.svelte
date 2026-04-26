<div class="pt-2 pb-3 mb-3 border-bottom animate__animated animate__fadeIn">
  <!-- Title & Description (full width) -->
  <div class="d-flex align-items-center gap-2 mb-1">
    <i class="fas fa-shield-alt text-secondary"></i>
    <span class="fw-bold">{$_('panel-edit.title')}</span>
  </div>
  <p class="text-secondary small mb-3">{$_('panel-edit.desc')}</p>

  <!-- Account Type Select (2-column row) -->
  <div class="row mb-2">
    <label class="col-md-4 col-form-label" for="panelPremiumStatusSelect">
      {$_('sidebar.account-type')}
    </label>
    <div class="col col-form-label">
      <div class="d-flex align-items-center gap-3">
        <select class="form-select form-select-sm" id="panelPremiumStatusSelect" bind:value={accountType} style="max-width: 200px;">
          <option value="PREMIUM">{$_('account-type.premium')}</option>
          <option value="CRACKED">{$_('account-type.cracked')}</option>
          <option value="AUTO">{$_('account-type.auto')}</option>
        </select>

        {#if loading}
          <div class="spinner-border spinner-border-sm text-secondary" role="status"></div>
        {/if}
      </div>

      {#if currentSource}
        <div class="text-muted small mt-1">
          <i class="fas fa-info-circle me-1"></i>
          {$_('panel-edit.source-info', { values: { source: currentSource } })}
        </div>
      {/if}
    </div>
  </div>

  <!-- Premium Link Status + Unlink (2-column row) -->
  <div class="row">
    <label class="col-md-4 col-form-label">
      {$_('sidebar.link-status')}
    </label>
    <div class="col col-form-label d-flex align-items-center gap-2">
      {#if premiumLinked && !pendingUnlink}
        <span class="badge bg-success"><i class="fas fa-check-circle me-1"></i>{$_('sidebar.linked')}</span>
        <button
          class="btn btn-outline-danger btn-sm"
          type="button"
          on:click={() => pendingUnlink = true}>
          <i class="fas fa-unlink me-1"></i>
          {$_('sidebar.unlink-button')}
        </button>
      {:else if premiumLinked && pendingUnlink}
        <span class="badge bg-warning text-dark"><i class="fas fa-exclamation-triangle me-1"></i>{$_('sidebar.will-unlink')}</span>
        <button
          class="btn btn-outline-secondary btn-sm"
          type="button"
          on:click={() => pendingUnlink = false}>
          <i class="fas fa-undo me-1"></i>
          {$_('sidebar.undo')}
        </button>
      {:else}
        <span class="badge bg-secondary">{$_('sidebar.not-linked')}</span>
      {/if}
    </div>
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
  let premiumLinked = false;
  let pendingUnlink = false;

  /** Edit modal reuses the same card row instance for every player; reload when the user changes. */
  let lastLoadedUsername = null;

  $: if (playerData?.username) {
    if (playerData.username !== lastLoadedUsername) {
      lastLoadedUsername = playerData.username;
      pendingUnlink = false;
      loadData();
    }
  }

  async function loadData() {
    if (!playerData?.username) return;
    loading = true;
    try {
      const res = await ApiUtil.get({
        path: `/api/panel/premium-login/player/${playerData?.username}`,
      });

      if (res && !res.error) {
        accountType = res.accountType || 'AUTO';
        initialType = accountType;
        currentSource = res.source || '';
        premiumLinked = res.premiumLinked || false;
      }
    } catch (e) {
      console.error('[pano-plugin-premium-login] Failed to load status', e);
    } finally {
      loading = false;
    }
  }

  $: isDirty = accountType !== initialType || pendingUnlink;

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
      // Save account type if changed
      if (accountType !== initialType) {
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
      }

      // Unlink if pending
      if (pendingUnlink) {
        const res = await ApiUtil.post({
          path: `/api/panel/premium-login/player/${playerData?.username}/unlink`,
        });

        if (res && !res.error) {
          premiumLinked = false;
          pendingUnlink = false;
        }
      }
    } catch (e) {
      console.error('[pano-plugin-premium-login] Save error:', e);
    } finally {
      loading = false;
    }
  }
</script>
