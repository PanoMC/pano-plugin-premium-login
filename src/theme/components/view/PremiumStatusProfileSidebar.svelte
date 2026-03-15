<div class="card mb-3 animate__animated animate__fadeIn">
  <div class="card-header d-flex align-items-center gap-2">
    <i class="fas fa-shield-alt text-secondary"></i>
    <span>{$_('profile-sidebar.title')}</span>
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
    {/if}
  </div>
</div>

<script module>
    import ApiUtil from '@panomc/sdk/utils/api';

    export async function load(event) {
    try {
      // Get the player username from the page params
      const { params } = await event;
      const username = params?.username;

      if (!username) return { status: null };

      const res = await ApiUtil.get({
        path: `/api/premium-login/player/${username}`,
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
  import { _ } from '../../../main.js';

  let { data } = $props();

  let loading = $state(!data?.status);
  let accountType = $state(data?.status?.accountType || 'AUTO');
</script>
