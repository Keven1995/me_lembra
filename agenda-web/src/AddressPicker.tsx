import { useState } from 'react'
import { GeoPlace, searchLocationPlaces } from './api'

type Props = {
  value: string
  selected: boolean
  placeholder: string
  onTextChange: (value: string) => void
  onSelect: (place: GeoPlace) => void
}

export default function AddressPicker({ value, selected, placeholder, onTextChange, onSelect }: Props) {
  const [matches, setMatches] = useState<GeoPlace[]>([])
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  async function lookup() {
    if (value.trim().length < 4) { setError('Digite pelo menos 4 caracteres.'); return }
    setBusy(true)
    setError('')
    try {
      const result = await searchLocationPlaces(value)
      setMatches(result)
      if (!result.length) setError('Nenhum endereço encontrado. Tente ser mais específico.')
    } catch (reason) {
      setMatches([])
      setError(reason instanceof Error ? reason.message : 'Falha ao pesquisar endereços.')
    } finally { setBusy(false) }
  }

  return <div className="address-picker">
    <div className="address-search-row">
      <input maxLength={240} placeholder={placeholder} value={value} onChange={event => {
        onTextChange(event.target.value); setMatches([]); setError('')
      }} />
      <button type="button" onClick={lookup} disabled={busy || value.trim().length < 4}>
        {busy ? 'Buscando…' : 'Buscar'}
      </button>
    </div>
    {matches.length > 0 && <div className="address-results" role="listbox" aria-label="Resultados de endereços">
      {matches.map((place, index) => <button type="button" key={`${place.latitude},${place.longitude},${index}`}
        onClick={() => { onSelect(place); setMatches([]); setError('') }}>📍 {place.label}</button>)}
    </div>}
    {selected ? <small className="location-confirmed">✓ Endereço confirmado para previsão e rota</small>
      : <small className="muted-copy">Pesquise e selecione um resultado para ativar tempo e trânsito.</small>}
    {error && <small className="form-error" role="alert">{error}</small>}
  </div>
}
