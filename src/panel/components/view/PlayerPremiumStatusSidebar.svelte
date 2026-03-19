<div class="card mt-3 animate__animated animate__fadeIn">
  <div class="card-header d-flex align-items-center gap-2">
    <i class="fas fa-shield-alt text-secondary"></i>
    <span>{$_('sidebar.title')}</span>
  </div>
  <div class="card-body">
    {#if loading}
      <div class="d-flex justify-content-center py-2">
        <div class="spinner-border spinner-border-sm text-secondary" role="status"></div>
      </div>
    {:else}
      <!-- Account Type -->
      <div class="d-flex align-items-center gap-2 mb-2">
        <small class="text-muted">{$_('sidebar.account-type')}:</small>
        {#if accountType === 'PREMIUM'}
          <span class="badge bg-success">{$_('account-type.premium')}</span>
        {:else if accountType === 'CRACKED'}
          <span class="badge bg-warning text-dark">{$_('account-type.cracked')}</span>
        {:else}
          <span class="badge bg-secondary">{$_('account-type.auto')}</span>
        {/if}
      </div>

      <!-- Source -->
      {#if source}
        <div class="mb-2">
          <small class="text-muted">{$_('sidebar.source', { values: { source } })}</small>
        </div>
      {/if}

      <!-- Premium Link Status -->
      <div class="d-flex align-items-center gap-2">
        <small class="text-muted">{$_('sidebar.link-status')}:</small>
        {#if premiumLinked}
          <span class="badge bg-success"><i class="fas fa-check-circle me-1"></i>{$_('sidebar.linked')}</span>
        {:else}
          <span class="badge bg-secondary">{$_('sidebar.not-linked')}</span>
        {/if}
      </div>
    {/if}
  </div>
</div>

<script>
    import {_} from '../../../main.js';
    import ApiUtil from '@panomc/sdk/utils/api';

    export let playerData;

  let loading = true;
  let accountType = 'AUTO';
  let source = '';
  let premiumLinked = false;

  loadData();

  async function loadData() {
    try {
      const res = await ApiUtil.get({
        path: `/api/panel/premium-login/player/${playerData?.username}`,
      });

      if (res && !res.error) {
        accountType = res.accountType || 'AUTO';
        source = res.source || '';
        premiumLinked = res.premiumLinked || false;
      }
    } catch (e) {
      console.error('[pano-plugin-premium-login] Failed to load sidebar status', e);
    } finally {
      loading = false;
    }
  }
</script>
