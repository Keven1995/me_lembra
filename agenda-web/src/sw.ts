/// <reference lib="webworker" />

import { precacheAndRoute } from 'workbox-precaching'

declare const self: ServiceWorkerGlobalScope & { __WB_MANIFEST: Array<unknown> }

precacheAndRoute(self.__WB_MANIFEST)

self.addEventListener('push', event => {
  const data = event.data?.json() ?? { title: 'Me_lembra', body: 'Você tem um compromisso chegando.' }
  event.waitUntil(self.registration.showNotification(data.title, {
    body: data.body,
    icon: '/pwa-192.png',
    badge: '/pwa-192.png',
    data: { url: data.url ?? '/' },
    tag: data.tag ?? 'event-reminder'
  }))
})

self.addEventListener('notificationclick', event => {
  event.notification.close()
  event.waitUntil(self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then(clients => {
    const target = event.notification.data?.url ?? '/'
    const existing = clients.find(client => 'focus' in client)
    if (existing) return existing.navigate(target).then(navigated => navigated?.focus())
    return self.clients.openWindow(target)
  }))
})
