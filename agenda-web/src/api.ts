export type Appointment = {
  id: string
  title: string
  category: 'HEALTH' | 'FINANCE' | 'WORK' | 'PERSONAL' | 'OTHER'
  location?: string | null
  destinationLatitude?: number | null
  destinationLongitude?: number | null
  notes?: string | null
  startAt: string
  endAt: string
  googleEventId?: string | null
  status: 'SCHEDULED' | 'CANCELLED'
}

export type AppointmentInput = Pick<Appointment, 'title' | 'category' | 'location' | 'notes' | 'startAt' | 'endAt' | 'destinationLatitude' | 'destinationLongitude'>

export type GeoPlace = { label: string; latitude: number; longitude: number }
export type LocationPreferences = {
  fallbackLabel: string | null
  gpsEnabled: boolean
  lastGpsAt: string | null
  gpsFresh: boolean
}

const apiUrl = import.meta.env.VITE_API_URL ?? '/api'
const apiOrigin = new URL(apiUrl, window.location.origin).origin

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const headers = new Headers(options?.headers)
  if (options?.body) headers.set('Content-Type', 'application/json')
  if (options?.method && !['GET', 'HEAD'].includes(options.method)) {
    const csrfResponse = await fetch(`${apiUrl}/auth/csrf`, { credentials: 'include' })
    if (!csrfResponse.ok) throw new Error('Não foi possível proteger esta operação. Tente novamente.')
    const csrf = await csrfResponse.json() as { token: string }
    headers.set('X-XSRF-TOKEN', csrf.token)
  }
  const response = await fetch(`${apiUrl}${path}`, {
    credentials: 'include',
    headers,
    ...options
  })
  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: 'Nao foi possivel concluir a operacao.' }))
    throw new Error(error.message ?? error.detail ?? 'Nao foi possivel concluir a operacao.')
  }
  return response.status === 204 ? (undefined as T) : response.json()
}

export function getAppointments(start: Date, end: Date) {
  const params = new URLSearchParams({ start: start.toISOString(), end: end.toISOString() })
  return request<Appointment[]>(`/appointments?${params}`)
}

export function createAppointment(input: AppointmentInput) {
  return request<Appointment>('/appointments', { method: 'POST', body: JSON.stringify(input) })
}

export function updateAppointment(id: string, input: AppointmentInput) {
  return request<Appointment>(`/appointments/${id}`, { method: 'PUT', body: JSON.stringify(input) })
}

export function cancelAppointment(id: string) {
  return request<void>(`/appointments/${id}`, { method: 'DELETE' })
}

export function getCurrentUser() {
  return request<{ id: string; name: string; email: string }>('/auth/me')
}

export function searchLocationPlaces(query: string) {
  return request<GeoPlace[]>(`/location/search?q=${encodeURIComponent(query)}`)
}

export function getLocationPreferences() {
  return request<LocationPreferences>('/location/preferences')
}

export function saveFallbackLocation(place: GeoPlace) {
  return request<LocationPreferences>('/location/fallback', { method: 'PUT', body: JSON.stringify(place) })
}

export function clearFallbackLocation() {
  return request<LocationPreferences>('/location/fallback', { method: 'DELETE' })
}

export function saveGpsLocation(latitude: number, longitude: number, accuracyMeters: number) {
  return request<LocationPreferences>('/location/gps', {
    method: 'POST', body: JSON.stringify({ latitude, longitude, accuracyMeters })
  })
}

export function disableGpsLocation() {
  return request<LocationPreferences>('/location/gps', { method: 'DELETE' })
}

export function logout() {
  return request<void>('/auth/logout', { method: 'POST' })
}

export function getVapidPublicKey() {
  return request<{ publicKey: string }>('/notifications/vapid-public-key')
}

export function savePushSubscription(subscription: { endpoint: string; keys: { p256dh?: string; auth?: string } }) {
  return request<void>('/notifications/subscriptions', {
    method: 'POST',
    body: JSON.stringify({ endpoint: subscription.endpoint, p256dh: subscription.keys.p256dh, auth: subscription.keys.auth })
  })
}

export function deletePushSubscription(endpoint: string) {
  return request<void>('/notifications/subscriptions', {
    method: 'DELETE',
    body: JSON.stringify({ endpoint })
  })
}

export function googleLoginUrl() {
  return `${apiOrigin}/oauth2/authorization/google`
}
