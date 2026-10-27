import { useCallback, useEffect, useState } from 'react'
import AddressPicker from './AddressPicker'
import {
  GeoPlace, LocationPreferences, clearFallbackLocation, disableGpsLocation,
  getLocationPreferences, saveFallbackLocation, saveGpsLocation
} from './api'

export default function LocationSettings() {
  const [preferences, setPreferences] = useState<LocationPreferences | null>(null)
  const [fallbackQuery, setFallbackQuery] = useState('')
  const [fallbackPicked, setFallbackPicked] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [info, setInfo] = useState('')

  useEffect(() => {
    getLocationPreferences().then(value => {
      setPreferences(value)
      setFallbackQuery(value.fallbackLabel ?? '')
      setFallbackPicked(Boolean(value.fallbackLabel))
    }).catch(reason => setError(reason instanceof Error ? reason.message : 'Não foi possível carregar suas localizações.'))
  }, [])

  const refreshGps = useCallback(async (silent = false) => {
    if (!navigator.geolocation) {
      if (!silent) setError('Seu navegador não oferece localização por GPS.')
      return
    }
    if (!silent) { setBusy(true); setInfo(''); setError('') }
    try {
      const position = await new Promise<GeolocationPosition>((resolve, reject) => {
        navigator.geolocation.getCurrentPosition(resolve, reject, {
          enableHighAccuracy: true, maximumAge: 0, timeout: 10000
        })
      })
      const updated = await saveGpsLocation(
        position.coords.latitude, position.coords.longitude, position.coords.accuracy
      )
      setPreferences(updated)
      if (!silent) setInfo('Localização atual registrada. Será usada enquanto estiver recente (até 30 minutos).')
    } catch (reason) {
      if (!silent) setError(reason instanceof Error && 'code' in reason
        ? 'Não foi possível acessar o GPS. Confira a permissão do navegador.'
        : reason instanceof Error ? reason.message : 'Falha ao atualizar sua localização.')
    } finally { if (!silent) setBusy(false) }
  }, [])

  // A PWA fechada não consegue acompanhar o GPS. Atualizamos só com consentimento prévio,
  // durante uso em primeiro plano e se o navegador já concedeu a permissão.
  useEffect(() => {
    if (!preferences?.gpsEnabled) return
    let active = true
    async function maybeRefresh() {
      if (!active || document.visibilityState !== 'visible' || !navigator.permissions) return
      try {
        const permission = await navigator.permissions.query({ name: 'geolocation' })
        if (active && permission.state === 'granted') await refreshGps(true)
      } catch { /* Sem atualização automática se a API de permissões não estiver disponível. */ }
    }
    void maybeRefresh()
    const interval = window.setInterval(() => { void maybeRefresh() }, 10 * 60 * 1000)
    document.addEventListener('visibilitychange', maybeRefresh)
    return () => { active = false; window.clearInterval(interval); document.removeEventListener('visibilitychange', maybeRefresh) }
  }, [preferences?.gpsEnabled, refreshGps])

  async function chooseFallback(place: GeoPlace) {
    setBusy(true); setError(''); setInfo('')
    try {
      const updated = await saveFallbackLocation(place)
      setPreferences(updated); setFallbackQuery(place.label); setFallbackPicked(true)
      setInfo('Endereço de reserva salvo.')
    } catch (reason) { setError(reason instanceof Error ? reason.message : 'Falha ao salvar endereço.') }
    finally { setBusy(false) }
  }

  async function removeFallback() {
    setBusy(true); setError(''); setInfo('')
    try {
      const updated = await clearFallbackLocation()
      setPreferences(updated); setFallbackQuery(''); setFallbackPicked(false)
      setInfo('Endereço de reserva removido.')
    } catch (reason) { setError(reason instanceof Error ? reason.message : 'Falha ao remover endereço.') }
    finally { setBusy(false) }
  }

  async function turnOffGps() {
    setBusy(true); setError(''); setInfo('')
    try {
      setPreferences(await disableGpsLocation())
      setInfo('GPS desativado e última localização apagada do servidor.')
    } catch (reason) { setError(reason instanceof Error ? reason.message : 'Falha ao desativar GPS.') }
    finally { setBusy(false) }
  }

  return <section className="location-settings panel" aria-labelledby="location-title">
    <div className="section-heading"><div><span className="overline">LEMBRETES INTELIGENTES</span>
      <h2 id="location-title">📍 Origem para o trânsito</h2></div></div>
    <p className="muted-copy">No aviso de 1h antes, calculamos o tempo de viagem de carro e a previsão no destino. Usamos o GPS recente; se estiver desatualizado, usamos o endereço de reserva.</p>
    <div className="location-settings-grid">
      <div>
        <h3>Localização do celular</h3>
        <p className="muted-copy">Só acessamos o GPS com sua permissão, enquanto o app estiver aberto.</p>
        <p className="location-state">{preferences?.gpsEnabled
          ? preferences.gpsFresh ? '🟢 GPS recente disponível' : '🟠 GPS desatualizado — usando endereço salvo, se houver'
          : '⚪ GPS não ativado'}</p>
        {preferences?.lastGpsAt && <small>Última atualização: {new Date(preferences.lastGpsAt).toLocaleString('pt-BR')}</small>}
        <div className="location-actions">
          <button type="button" className="secondary-button" disabled={busy} onClick={() => void refreshGps()}>
            {busy ? 'Aguarde…' : 'Usar minha localização atual'}
          </button>
          {preferences?.gpsEnabled && <button className="text-action" disabled={busy} onClick={() => void turnOffGps()}>Desativar GPS</button>}
        </div>
      </div>
      <div>
        <h3>Endereço de reserva</h3>
        <p className="muted-copy">Sua casa ou ponto de partida padrão. Usado quando não há GPS recente.</p>
        <AddressPicker value={fallbackQuery} selected={fallbackPicked && Boolean(preferences?.fallbackLabel)}
          placeholder="Ex.: Valparaíso de Goiás, GO" onTextChange={value => { setFallbackQuery(value); setFallbackPicked(false) }}
          onSelect={place => void chooseFallback(place)} />
        {preferences?.fallbackLabel && <div className="location-actions"><button className="text-action" disabled={busy}
          onClick={() => void removeFallback()}>Remover endereço salvo</button></div>}
      </div>
    </div>
    {info && <p className="location-feedback" role="status">✓ {info}</p>}
    {error && <p className="form-error" role="alert">{error}</p>}
    <small className="muted-copy">O GPS não é acessível quando o PWA está fechado. Para rotas confiáveis mesmo assim, cadastre um endereço de reserva. A estimativa de trânsito não substitui o mapa em tempo real.</small>
  </section>
}
