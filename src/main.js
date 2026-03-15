import {PanoPlugin, viewComponent} from '@panomc/sdk';
import {derived} from 'svelte/store';
import {_ as i18n} from '@panomc/sdk/utils/language';

const pluginId = 'pano-plugin-premium-login';

// this is to render plugin translations
export const _ = derived(i18n, ($_fn) => {
  return (key, options) => $_fn(`plugins.${pluginId}.${key}`, options);
});

export default class PremiumLoginPlugin extends PanoPlugin {
  onLoad() {
    const pano = this.pano;

    if (pano.isPanel) {
      // Register premium status edit component in player edit modal
      pano.ui.player.editModal.cardRows.edit((items) => {
        items.push({
          id: `${pluginId}-player-premium-status-edit`,
          component: viewComponent(
            () =>
              import(
                './panel/components/view/PlayerPremiumStatusEdit.svelte'
              ),
          ),
          permission: `pano.plugin.${pluginId}.manage.premium.status`,
          priority: 100,
        });
      });

      // Register sidebar block for player detail
      pano.ui.player.detailSidebar.cardRows.edit((items) => {
        items.push({
          id: `${pluginId}-player-sidebar-status`,
          component: viewComponent(
            () =>
              import(
                './panel/components/view/PlayerPremiumStatusSidebar.svelte'
              ),
          ),
          priority: 90,
          sortable: true,
          removable: true,
        });
      });
    } else {
      // Register premium status display in user settings
      pano.ui.settings.cardRows.edit((items) => {
        items.push({
          id: `${pluginId}-status-display`,
          component: viewComponent(
            () =>
              import('./theme/components/view/PremiumStatusDisplay.svelte'),
          ),
          priority: 95,
        });
      });

      // Register premium status display in player profile sidebar
      pano.ui.player.profileSidebar.cardRows.edit((items) => {
        items.push({
          id: `${pluginId}-profile-sidebar-status`,
          component: viewComponent(
            () =>
              import(
                './theme/components/view/PremiumStatusProfileSidebar.svelte'
              ),
          ),
          priority: 90,
          sortable: true,
          removable: true,
        });
      });
    }
  }

  onContextUpdate(ctx) {}

  onUnload() {}
}
