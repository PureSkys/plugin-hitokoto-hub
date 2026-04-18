import {definePlugin} from '@halo-dev/ui-shared'
import {IconPlug} from '@halo-dev/components'
import {markRaw} from 'vue'

export default definePlugin({
  components: {},
  routes: [
    {
      parentName: 'ToolsRoot',
      route: {
        path: '/hitokoto-hub',
        name: 'Hitokoto',
        component: () => import(/* webpackChunkName: "HomeView" */ './views/HomeView.vue'),
        meta: {
          title: '一言管理',
          searchable: true,
          menu: {
            name: '一言管理',
            icon: markRaw(IconPlug),
            priority: 0,
          },
        },
      },
    },
  ],
  extensionPoints: {},
})
