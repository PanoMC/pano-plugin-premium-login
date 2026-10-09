<style>
  .premium-login-premium-status-profile-sidebar__fade-in {
    animation: premium-login-fade-in 250ms both;
  }

  @keyframes premium-login-fade-in {
    from { opacity: 0; }
    to { opacity: 1; }
  }
</style>

<div class="premium-login-premium-status-profile-sidebar card mb-3 premium-login-premium-status-profile-sidebar__fade-in">
  <div class="premium-login-premium-status-profile-sidebar__header card-header d-flex align-items-center gap-2">
    <i class="fas fa-shield-alt text-secondary"></i>
    <span>{$_('profile-sidebar.title')}</span>
  </div>
  <div class="premium-login-premium-status-profile-sidebar__body card-body">
    {#if loading}
      <div class="d-flex justify-content-center py-2">
        <div class="spinner-border spinner-border-sm text-secondary" role="status"></div>
      </div>
    {:else}
      <div class="d-flex align-items-center gap-2">
        {#if accountType === 'PREMIUM'}
          <span class="premium-login-premium-status-profile-sidebar__badge badge bg-success">{$_('account-type.premium')}</span>
        {:else if accountType === 'CRACKED'}
          <span class="premium-login-premium-status-profile-sidebar__cracked badge bg-warning text-dark">{$_('account-type.cracked')}</span>
        {:else}
          <span class="premium-login-premium-status-profile-sidebar__auto badge bg-secondary">{$_('account-type.auto')}</span>
        {/if}
      </div>
    {/if}
  </div>
</div>

<script module>
    import { api } from '@panomc/sdk/plugin-api';

    // The premium status in the player profile sidebar (slot profile-card-rows).
    export const view = { slot: 'profile-card-rows', id: 'pano-plugin-premium-login-profile-sidebar-status', priority: 90 };

    export async function load(event) {
    try {
      // Get the player username from the page params
      const { params } = await event;
      const username = params?.username;

      if (!username) return { status: null };

      const res = await api.get({
        path: `/player/${username}`,
        request: event,
      });

      return {
        status: res && !res.error ? res : null,
      };
    } catch (e) {
      console.error('[pano-plugin-premium-login] Failed to load profile status', e);
      return { status: null };
    }
  }
</script>

<script>
  import { derived } from 'svelte/store';
  import { _ as i18n } from '@panomc/sdk/utils/language';

  // plugin translations: $_('key') reads plugins.pano-plugin-premium-login.key
  const _ = derived(i18n, ($_fn) => (key, options) => $_fn(`plugins.pano-plugin-premium-login.${key}`, options));

  let { data } = $props();

  let loading = $derived(!data?.status);
  let accountType = $derived(data?.status?.accountType || 'AUTO');
</script>
