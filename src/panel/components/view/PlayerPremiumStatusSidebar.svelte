<script module>
  /**
   * Called from panel `executeHookLoad` during layout `load` so `request: event` is used
   * (CSRF, cookies, `event.fetch`). See panel-ui api.util.
   * (ApiUtil is only imported in the instance script; same-name imports in both scripts error in Svelte 5.)
   * @param {import('@sveltejs/kit').LayoutLoadEvent} event
   */
  export async function load(event) {
    const { api } = await import('@panomc/sdk/plugin-api');
    const username = event.params?.username;
    if (!username || typeof username !== 'string') {
      return {};
    }
    try {
      const res = await api.panel.get({
        path: `/player/${encodeURIComponent(username)}`,
        request: event,
      });
      if (res && !res.error) {
        return {
          premiumPreload: {
            _ok: true,
            accountType: res.accountType || 'AUTO',
            source: res.source || '',
            premiumLinked: !!res.premiumLinked,
          },
        };
      }
    } catch (e) {
      console.error('[pano-plugin-premium-login] premium sidebar load() failed', e);
    }
    return {};
  }
</script>

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
  import { _ } from '../../../main.js';
  import { api } from '@panomc/sdk/plugin-api';

  export let playerData;

  /** @type {{ _ok: true, accountType: string, source: string, premiumLinked: boolean } | undefined} */
  export let premiumPreload = undefined;

  let loading = true;
  let accountType = 'AUTO';
  let source = '';
  let premiumLinked = false;

  let lastFetchedUsername = null;

  $: if (premiumPreload?._ok) {
    accountType = premiumPreload.accountType;
    source = premiumPreload.source;
    premiumLinked = premiumPreload.premiumLinked;
    loading = false;
    lastFetchedUsername = playerData?.username ?? null;
  } else if (playerData?.username && playerData.username !== lastFetchedUsername) {
    lastFetchedUsername = playerData.username;
    loadData();
  }

  async function loadData() {
    if (!playerData?.username) return;
    loading = true;
    try {
      const res = await api.panel.get({
        path: `/player/${playerData?.username}`,
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
