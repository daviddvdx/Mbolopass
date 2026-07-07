import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { VitePWA } from 'vite-plugin-pwa';

export default defineConfig({
  plugins: [
    react(),
    VitePWA({
      registerType: 'autoUpdate',
      includeAssets: ['README-logo.txt'],
      manifest: {
        name: 'MboloPass',
        short_name: 'MboloPass',
        lang: 'fr',
        description: 'Votre passeport de sante numerique.',
        theme_color: '#17304D',
        background_color: '#F8FCFD',
        display: 'standalone',
        start_url: '/',
        scope: '/',
        icons: [
          { src: '/mbolopass-logo.png', sizes: '1040x914', type: 'image/png', purpose: 'any' }
        ]
      },
      workbox: {
        globPatterns: ['**/*.{js,css,html,svg,ico,webmanifest}'],
        globIgnores: ['**/ScannerPage-*.js'],
        navigateFallback: '/',
        navigateFallbackDenylist: [/^\/api\//, /^\/urgence\//, /^\/emergency\//],
        runtimeCaching: [
          {
            urlPattern: ({ url }) => url.pathname.startsWith('/api/'),
            handler: 'NetworkOnly',
            options: { cacheName: 'mbolopass-api-never-cache' }
          },
          {
            urlPattern: ({ request, url }) => url.origin === self.location.origin && ['style', 'script', 'image', 'font'].includes(request.destination),
            handler: 'CacheFirst',
            options: {
              cacheName: 'mbolopass-static-assets',
              expiration: { maxEntries: 80, maxAgeSeconds: 60 * 60 * 24 * 30 }
            }
          }
        ]
      }
    })
  ],
  server: { port: 5173 }
});
