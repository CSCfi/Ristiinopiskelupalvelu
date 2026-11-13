import { defineConfig, loadEnv } from 'vite';
import vue from '@vitejs/plugin-vue';
import vuetify from 'vite-plugin-vuetify';
import path from 'path';

export default defineConfig(({ mode }) => {
  loadEnv(mode, process.cwd(), '');

  return {
    base: '/admin-ui/',
    plugins: [
      vue(),
      vuetify({
        autoImport: true
      })
    ],
    resolve: {
      alias: {
        '@': path.resolve(__dirname, 'src')
      }
    },
    define: {
      'import.meta.env.VITE_APP_VERSION': JSON.stringify(
        process.env.npm_package_version || ''
      )
    },
    server: {
      port: 3000,
      proxy: {
        '/admin-ui/api': {
          target: 'http://localhost:8081/admin-ui/api',
          changeOrigin: true,
          rewrite: pathStr => pathStr.replace(/^\/admin-ui\/api/, '')
        }
      }
    },
    build: {
      outDir: 'target/dist'
    },
    test: {
      environment: 'jsdom'
    }
  };
});
