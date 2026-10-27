/* global self */

self.addEventListener('push', function (event) {
  var data = event.data ? event.data.json() : { title: 'Me_lembra', body: 'Você tem um compromisso chegando.' }
  event.waitUntil(self.registration.showNotification(data.title, {
    body: data.body,
    icon: '/pwa-192.png',
    badge: '/pwa-192.png',
    data: { url: data.url || '/' },
    tag: data.tag || 'event-reminder'
  }))
})

self.addEventListener('notificationclick', function (event) {
  event.notification.close()
  event.waitUntil(self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then(function (clients) {
    var target = event.notification.data && event.notification.data.url ? event.notification.data.url : '/'
    var existing = clients.find(function (client) { return 'focus' in client })
    if (existing) return existing.navigate(target).then(function (navigated) { return navigated && navigated.focus() })
    return self.clients.openWindow(target)
  }))
})
