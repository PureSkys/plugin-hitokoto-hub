import {definePlugin} from '@halo-dev/ui-shared'
import IconBob from '~icons/my-icons/bob';
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
          title: '轻言管理',
          searchable: true,
          permissions: ['plugin:hitokoto-hub:view'],
          menu: {
            name: '轻言管理',
            icon: markRaw(IconBob),
            priority: 0,
          },
        },
      },
    },
  ],
  extensionPoints: {},
})
