import {rsbuildConfig} from '@halo-dev/ui-plugin-bundler-kit';
import Icons from 'unplugin-icons/rspack';
import {pluginSass} from '@rsbuild/plugin-sass';
import type {RsbuildConfig} from '@rsbuild/core';
import AutoImport from 'unplugin-auto-import/rspack';
import Components from 'unplugin-vue-components/rspack';
import {ElementPlusResolver} from 'unplugin-vue-components/resolvers';
import {FileSystemIconLoader} from "unplugin-icons/loaders";

export default rsbuildConfig({
  rsbuild: {
    resolve: {
      alias: {
        '@': './src',
      },
    },
    plugins: [pluginSass()],
    tools: {
      postcss: {
        postcssOptions: {
          plugins: ['@tailwindcss/postcss'],
        },
      },
      rspack: {
        plugins: [
          Icons({
            compiler: 'vue3',
            customCollections: {
              'my-icons': FileSystemIconLoader('./src/assets/icons', svg => svg),
            },
          }),
          AutoImport({
            resolvers: [ElementPlusResolver()],
          }),
          Components({
            resolvers: [ElementPlusResolver()],
          }),
        ],
      },
    },
  },
}) as RsbuildConfig;
