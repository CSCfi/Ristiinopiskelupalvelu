import { defineComponent, h } from 'vue';
import { VListItem, VListItemTitle } from 'vuetify/components';

const legacyLayout = defineComponent({
  name: 'LegacyLayout',
  setup(_, { slots, attrs }) {
    const style = {
      display: 'flex',
      flexWrap: attrs.wrap !== undefined ? 'wrap' : undefined,
      flexDirection: attrs.column !== undefined ? 'column' : 'row',
      alignItems: attrs['align-center'] ? 'center' : undefined,
      justifyContent: attrs['justify-center']
        ? 'center'
        : attrs['justify-start']
        ? 'flex-start'
        : attrs['justify-space-between']
        ? 'space-between'
        : attrs['justify-end']
        ? 'flex-end'
        : undefined
    };

    return () => h('div', { class: ['legacy-layout', attrs.class], style }, slots.default?.());
  }
});

const legacyFlex = defineComponent({
  name: 'LegacyFlex',
  setup(_, { slots, attrs }) {
    const sizeOrder = ['xl', 'lg', 'md', 'sm', 'xs'];
    let widthKey = null;
    sizeOrder.some(size => {
      const match = Object.keys(attrs).find(key => key.startsWith(size));
      if (match) {
        widthKey = match;
        return true;
      }
      return false;
    });
    let basis;
    if (widthKey && widthKey.length > 2) {
      const size = sizeOrder.find(s => widthKey.startsWith(s)) || 'xs';
      const cols = parseInt(widthKey.replace(size, ''), 10);
      if (!isNaN(cols) && cols > 0) {
        basis = `${(cols / 12) * 100}%`;
      }
    }
    const style = {
      flex: '1 1 auto',
      flexBasis: basis,
      maxWidth: basis
    };

    return () => h('div', { class: ['legacy-flex', attrs.class], style }, slots.default?.());
  }
});

const legacyListTile = defineComponent({
  name: 'LegacyListTile',
  setup(_, { slots, attrs }) {
    return () => h(VListItem, attrs, slots);
  }
});

const legacyListTileContent = defineComponent({
  name: 'LegacyListTileContent',
  setup(_, { slots, attrs }) {
    return () =>
      h('div', { class: ['v-list-item__content', attrs.class], ...attrs }, slots.default?.());
  }
});

const legacyListTileTitle = defineComponent({
  name: 'LegacyListTileTitle',
  setup(_, { slots, attrs }) {
    return () =>
      h(
        VListItemTitle,
        {
          class: attrs.class,
          ...attrs
        },
        slots.default ? slots.default() : []
      );
  }
});

const legacyListTileAction = defineComponent({
  name: 'LegacyListTileAction',
  setup(_, { slots, attrs }) {
    return () =>
      h(
        'div',
        { class: ['v-list-item__append', attrs.class], ...attrs },
        slots.default ? slots.default() : []
      );
  }
});

const legacyToolbarItems = defineComponent({
  name: 'LegacyToolbarItems',
  setup(_, { slots, attrs }) {
    return () =>
      h('div', { class: ['v-toolbar-items', attrs.class], ...attrs }, slots.default?.());
  }
});

export function registerLegacyComponents(app) {
  app.component('v-layout', legacyLayout);
  app.component('v-flex', legacyFlex);
  app.component('v-list-tile', legacyListTile);
  app.component('v-list-tile-content', legacyListTileContent);
  app.component('v-list-tile-title', legacyListTileTitle);
  app.component('v-list-tile-action', legacyListTileAction);
  app.component('v-toolbar-items', legacyToolbarItems);
}
