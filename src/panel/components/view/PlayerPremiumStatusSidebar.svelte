<div class="card mb-3 animate__animated animate__fadeIn">
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
      <div class="d-flex align-items-center gap-2">
        {#if accountType === 'PREMIUM'}
          <span class="badge bg-success">{$_('account-type.premium')}</span>
        {:else if accountType === 'CRACKED'}
          <span class="badge bg-warning text-dark">{$_('account-type.cracked')}</span>
        {:else}
          <span class="badge bg-secondary">{$_('account-type.auto')}</span>
        {/if}
      </div>
      {#if source}
        <small class="text-muted mt-1 d-block">
          {$_('sidebar.source', { values: { source } })}
        </small>
      {/if}
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

  loadData();

  async function loadData() {
    try {
      const res = await ApiUtil.get({
        path: `/api/panel/premium-login/player/${playerData?.username}`,
      });

      if (res && !res.error) {
        accountType = res.accountType || 'AUTO';
        source = res.source || '';
      }
    } catch (e) {
      console.error('[pano-plugin-premium-login] Failed to load sidebar status', e);
    } finally {
      loading = false;
    }
  }
</script>
