import { FormEvent, useEffect, useMemo, useState } from 'react'
import {
  Appointment, AppointmentInput, cancelAppointment, createAppointment, deletePushSubscription, getAppointments,
  getCurrentUser, getVapidPublicKey, googleLoginUrl, logout, savePushSubscription, updateAppointment
} from './api'
import AddressPicker from './AddressPicker'
import LocationSettings from './LocationSettings'

type Category = Appointment['category']
type User = { id: string; name: string; email: string }
type EventForm = {
  title: string; category: Category; location: string; notes: string; date: string; start: string; end: string
  destinationLatitude: number | null; destinationLongitude: number | null
}
const categories: { value: Category; label: string; icon: string }[] = [
  { value: 'PERSONAL', label: 'Pessoal', icon: '✨' },
  { value: 'HEALTH', label: 'Saúde', icon: '🩺' },
  { value: 'FINANCE', label: 'Finanças', icon: '🏦' },
  { value: 'WORK', label: 'Trabalho', icon: '💼' },
  { value: 'OTHER', label: 'Outros', icon: '📌' }
]
const today = () => new Date()
const dateKey = (date: Date) => [
  date.getFullYear(), String(date.getMonth() + 1).padStart(2, '0'), String(date.getDate()).padStart(2, '0')
].join('-')
const fromKey = (value: string) => new Date(`${value}T12:00:00`)
const timeFormat = new Intl.DateTimeFormat('pt-BR', { hour: '2-digit', minute: '2-digit' })
const dayFormat = new Intl.DateTimeFormat('pt-BR', { day: 'numeric', month: 'long', weekday: 'long' })
const monthFormat = new Intl.DateTimeFormat('pt-BR', { month: 'long', year: 'numeric' })
const formatTime = (value: string) => timeFormat.format(new Date(value))
const categoryFor = (value: Category) => categories.find(item => item.value === value) ?? categories[4]
const emptyForm = (date: Date): EventForm => ({
  title: '', category: 'PERSONAL', location: '', notes: '', date: dateKey(date), start: '09:00', end: '10:00',
  destinationLatitude: null, destinationLongitude: null
})
const toInput = (form: EventForm): AppointmentInput => ({
  title: form.title.trim(),
  category: form.category,
  location: form.location.trim() || null,
  destinationLatitude: form.destinationLatitude,
  destinationLongitude: form.destinationLongitude,
  notes: form.notes.trim() || null,
  startAt: new Date(`${form.date}T${form.start}:00`).toISOString(),
  endAt: new Date(`${form.date}T${form.end}:00`).toISOString()
})
const asForm = (event: Appointment): EventForm => {
  const start = new Date(event.startAt)
  return {
    title: event.title, category: event.category, location: event.location ?? '', notes: event.notes ?? '',
    date: dateKey(start), start: formatTime(event.startAt), end: formatTime(event.endAt),
    destinationLatitude: event.destinationLatitude ?? null,
    destinationLongitude: event.destinationLongitude ?? null
  }
}
function base64ToBytes(value: string) {
  const padded = (value + '='.repeat((4 - value.length % 4) % 4)).replace(/-/g, '+').replace(/_/g, '/')
  return Uint8Array.from(atob(padded), char => char.charCodeAt(0))
}
function ReminderPreview({ form }: { form: EventForm }) {
  const start = new Date(`${form.date}T${form.start}:00`)
  if (Number.isNaN(start.getTime())) return null
  const before = new Date(start)
  before.setDate(before.getDate() - 1)
  before.setHours(18, 0, 0, 0)
  const morning = new Date(start)
  morning.setHours(9, 0, 0, 0)
  const latest = new Date(start.getTime() - 3600000)
  const unique = new Set<number>()
  const times = [
    { label: '1h antes', at: latest },
    { label: '3h antes', at: new Date(start.getTime() - 10800000) },
    { label: 'No dia', at: morning },
    { label: 'Véspera', at: before }
  ].filter(item => {
    if (item.at <= new Date() || item.at > latest || unique.has(item.at.getTime())) return false
    unique.add(item.at.getTime())
    return true
  }).sort((a, b) => a.at.getTime() - b.at.getTime())
  return <div className="preview-reminders">
    <strong>🔔 Seus lembretes</strong>
    <p>O Me_lembra avisa automaticamente nestes horários:</p>
    <div className="reminder-chips">{times.length ? times.map(item =>
      <span key={item.label}>{item.label} · {formatTime(item.at.toISOString())}</span>
    ) : <span>Sem horários futuros para avisar</span>}</div>
  </div>
}
function Login() {
  return <main className="login-layout">
    <section className="login-intro">
      <div className="wordmark"><span className="app-symbol">✳</span><strong>Me_lembra</strong></div>
      <div className="login-hero">
        <div className="floating-bell">🔔</div>
        <span className="overline light">MENOS PREOCUPAÇÃO. MAIS PRESENÇA.</span>
        <h1>Deixa que a gente te lembra.</h1>
        <p>Seus compromissos organizados, com avisos inteligentes no celular antes de cada evento.</p>
        <div className="hero-notice"><span>🦷</span><div><strong>Amanhã tem dentista!</strong><small>Sua consulta é às 14h. Tudo pronto?</small></div><span className="notice-check">✓</span></div>
      </div>
      <small>Organize seu tempo. Viva o que importa.</small>
    </section>
    <section className="login-right">
      <div className="login-box">
        <div className="login-icon">✳</div>
        <span className="overline">BEM-VINDO AO ME_LEMBRA</span>
        <h2>Seu tempo merece cuidado.</h2>
        <p>Entre com sua conta Google para começar a organizar seus compromissos e receber lembretes personalizados.</p>
        <a className="google-button" href={googleLoginUrl()}><span className="google-mark">G</span> Continuar com Google <span>↗</span></a>
        <small>Seus eventos ficam vinculados à sua conta. As notificações só são ativadas com sua permissão.</small>
      </div>
    </section>
  </main>
}
function AppShell({ user }: { user: User }) {
  const [selected, setSelected] = useState(today)
  const [month, setMonth] = useState(() => new Date(today().getFullYear(), today().getMonth(), 1))
  const [events, setEvents] = useState<Appointment[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [form, setForm] = useState<EventForm | null>(null)
  const [editing, setEditing] = useState<Appointment | null>(null)
  const [formError, setFormError] = useState('')
  const [saving, setSaving] = useState(false)
  const [notificationOn, setNotificationOn] = useState(false)
  const [pushBusy, setPushBusy] = useState(false)
  const [showMenu, setShowMenu] = useState(false)
  const [toast, setToast] = useState('')

  const days = useMemo(() => {
    const start = new Date(month.getFullYear(), month.getMonth(), 1)
    start.setDate(start.getDate() - start.getDay())
    return Array.from({ length: 42 }, (_, i) => {
      const date = new Date(start)
      date.setDate(start.getDate() + i)
      return date
    })
  }, [month])
  const grouped = useMemo(() => events.reduce<Record<string, Appointment[]>>((acc, event) => {
    (acc[dateKey(new Date(event.startAt))] ??= []).push(event)
    return acc
  }, {}), [events])
  const daily = (grouped[dateKey(selected)] ?? []).sort((a, b) => a.startAt.localeCompare(b.startAt))
  const upcoming = events.filter(event => new Date(event.startAt) >= today())
    .sort((a, b) => a.startAt.localeCompare(b.startAt)).slice(0, 3)

  useEffect(() => {
    let active = true
    setLoading(true)
    getAppointments(new Date(month.getFullYear(), month.getMonth(), 1), new Date(month.getFullYear(), month.getMonth() + 1, 1))
      .then(items => { if (active) { setEvents(items); setError('') } })
      .catch(reason => { if (active) setError(reason instanceof Error ? reason.message : 'Não foi possível carregar a agenda.') })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [month])
  useEffect(() => {
    if (!('serviceWorker' in navigator)) return
    navigator.serviceWorker.getRegistrations()
      .then(registrations => registrations[0]?.pushManager?.getSubscription())
      .then(subscription => setNotificationOn(Boolean(subscription)))
      .catch(() => {})
  }, [])
  const showToast = (message: string) => { setToast(message); setError('') }
  const openNew = (day = selected) => { setEditing(null); setForm(emptyForm(day)); setFormError('') }
  const openEdit = (event: Appointment) => { setEditing(event); setForm(asForm(event)); setFormError('') }
  function shiftMonth(shift: number) {
    const next = new Date(month.getFullYear(), month.getMonth() + shift, 1)
    setMonth(next); setSelected(next)
  }
  function selectDate(day: Date) { setSelected(day); if (day.getMonth() !== month.getMonth()) setMonth(new Date(day.getFullYear(), day.getMonth(), 1)) }
  async function submit(event: FormEvent) {
    event.preventDefault()
    if (!form) return
    if (!form.title.trim()) { setFormError('Dê um nome ao compromisso.'); return }
    if (form.end <= form.start) { setFormError('O horário de término deve ser posterior ao início.'); return }
    if (new Date(`${form.date}T${form.start}:00`) <= new Date()) { setFormError('Escolha uma data e um horário futuros.'); return }
    setSaving(true)
    try {
      const saved = editing ? await updateAppointment(editing.id, toInput(form)) : await createAppointment(toInput(form))
      if (new Date(saved.startAt).getMonth() === month.getMonth() && new Date(saved.startAt).getFullYear() === month.getFullYear()) {
        setEvents(current => [...current.filter(item => item.id !== saved.id), saved])
      } else {
        setMonth(new Date(new Date(saved.startAt).getFullYear(), new Date(saved.startAt).getMonth(), 1))
      }
      setSelected(fromKey(form.date)); setForm(null); setEditing(null)
      showToast(editing ? 'Compromisso atualizado e lembretes recalculados.' : 'Compromisso salvo! Seus lembretes estão programados.')
    } catch (reason) { setFormError(reason instanceof Error ? reason.message : 'Não foi possível salvar o compromisso.') }
    finally { setSaving(false) }
  }
  async function removeEvent() {
    if (!editing || !window.confirm(`Cancelar "${editing.title}" e todos os lembretes futuros?`)) return
    setSaving(true)
    try {
      await cancelAppointment(editing.id)
      setEvents(current => current.filter(event => event.id !== editing.id))
      setForm(null); setEditing(null); showToast('Compromisso cancelado. Nenhum outro lembrete será enviado.')
    } catch (reason) { setFormError(reason instanceof Error ? reason.message : 'Falha ao cancelar compromisso.') }
    finally { setSaving(false) }
  }
  async function enablePush() {
    setPushBusy(true)
    try {
      if (!('serviceWorker' in navigator) || !('PushManager' in window) || !('Notification' in window))
        throw new Error('Este navegador não oferece notificações push para PWAs.')
      if (Notification.permission === 'denied') throw new Error('Libere as notificações nas configurações do navegador.')
      const { publicKey } = await getVapidPublicKey()
      if (!publicKey) throw new Error('O servidor ainda precisa das chaves VAPID para enviar notificações.')
      const permission = await Notification.requestPermission()
      if (permission !== 'granted') throw new Error('Você não permitiu as notificações neste dispositivo.')
      const path = import.meta.env.PROD ? '/sw.js' : '/push-sw.js'
      await navigator.serviceWorker.register(path, { scope: '/', type: import.meta.env.PROD ? 'module' : 'classic' })
      const registration = await Promise.race([
        navigator.serviceWorker.ready,
        new Promise<never>((_, reject) => setTimeout(() => reject(new Error('O PWA ainda está iniciando. Tente novamente.')), 10000))
      ])
      const subscription = await registration.pushManager.subscribe({
        userVisibleOnly: true, applicationServerKey: base64ToBytes(publicKey)
      })
      const payload = subscription.toJSON()
      await savePushSubscription({ endpoint: payload.endpoint ?? '', keys: payload.keys ?? {} })
      setNotificationOn(true); showToast('Notificações ativadas neste dispositivo! 🔔')
    } catch (reason) { setError(reason instanceof Error ? reason.message : 'Não foi possível ativar os avisos.') }
    finally { setPushBusy(false) }
  }
  async function disablePush() {
    setPushBusy(true)
    try {
      const registrations = await navigator.serviceWorker.getRegistrations()
      const subscription = await registrations[0]?.pushManager.getSubscription()
      if (subscription) {
        await deletePushSubscription(subscription.endpoint)
        await subscription.unsubscribe()
      }
      setNotificationOn(false)
      showToast('Notificações desativadas neste dispositivo.')
    } catch (reason) { setError(reason instanceof Error ? reason.message : 'Falha ao desativar notificações.') }
    finally { setPushBusy(false) }
  }
  async function signOut() {
    try {
      if (notificationOn) {
        const registrations = await navigator.serviceWorker.getRegistrations()
        const subscription = await registrations[0]?.pushManager.getSubscription()
        if (subscription) {
          await deletePushSubscription(subscription.endpoint)
          await subscription.unsubscribe()
        }
      }
      await logout()
      location.reload()
    } catch (reason) { setError(reason instanceof Error ? reason.message : 'Não foi possível sair da conta.') }
  }

  return <div className="app-frame">
    <aside className="sidebar">
      <div className="wordmark"><span className="app-symbol">✳</span><strong>Me_lembra</strong></div>
      <div className="side-section">SEU ESPAÇO</div>
      <a className="side-link active" href="/"><span>▦</span> Minha agenda</a>
      <button className="side-link" onClick={() => openNew()}><span>＋</span> Novo compromisso</button>
      <div className="side-divider" />
      <div className="side-section">LEMBRETES</div>
      <div className="side-helper"><span>🔔</span><strong>A gente avisa você.</strong><p>Com antecedência, contexto e tranquilidade.</p></div>
      <div className="side-bottom"><span className="avatar">{user.name.slice(0, 1).toUpperCase()}</span><div><strong>{user.name}</strong><small>Conta pessoal</small></div><button aria-label="Sair" onClick={signOut}>↪</button></div>
    </aside>
    <main className="main-panel">
      <header className="mobile-top"><div className="wordmark"><span className="app-symbol">✳</span><strong>Me_lembra</strong></div><button className="text-action" onClick={() => setShowMenu(value => !value)}>☰</button></header>
      {showMenu && <div className="mobile-menu"><button onClick={notificationOn ? disablePush : enablePush}>{notificationOn ? 'Desativar notificações' : 'Ativar notificações'}</button><button onClick={signOut}>Sair</button></div>}
      <div className="page-heading">
        <div><span className="overline">SUA VIDA, MAIS ORGANIZADA</span><h1>Olá, {user.name.split(' ')[0]} <span>👋</span></h1><p>Vamos cuidar dos seus próximos compromissos?</p></div>
        <button className="primary-button" onClick={() => openNew()}>＋ Novo compromisso</button>
      </div>
      {toast && <div className="alert success" role="status"><span>✓ {toast}</span><button onClick={() => setToast('')} aria-label="Fechar">×</button></div>}
      {error && <div className="alert error" role="alert"><span>{error}</span><button onClick={() => setError('')} aria-label="Fechar">×</button></div>}
      <div className="notification-banner"><span className="banner-icon">🔔</span><div><strong>{notificationOn ? 'Seus lembretes estão ativados!' : 'Não perca o que é importante.'}</strong><p>{notificationOn ? 'Este aparelho está preparado para receber avisos push.' : 'Ative as notificações para receber avisos mesmo com o aplicativo fechado.'}</p></div><button onClick={notificationOn ? disablePush : enablePush} disabled={pushBusy}>{pushBusy ? 'Aguarde...' : notificationOn ? 'Desativar avisos' : 'Ativar avisos'}</button></div>
      <div className="dashboard-grid">
        <section className="panel calendar-panel">
          <div className="section-heading"><div><span className="overline">CALENDÁRIO</span><h2>{monthFormat.format(month)}</h2></div><div className="month-nav"><button aria-label="Mês anterior" onClick={() => shiftMonth(-1)}>‹</button><button aria-label="Próximo mês" onClick={() => shiftMonth(1)}>›</button></div></div>
          <div className="calendar-weekdays">{['DOM','SEG','TER','QUA','QUI','SEX','SÁB'].map(day => <span key={day}>{day}</span>)}</div>
          <div className="calendar-days">{days.map(day => {
            const key = dateKey(day), active = dateKey(selected) === key, current = dateKey(today()) === key
            return <button key={key} onClick={() => selectDate(day)} className={`${day.getMonth() !== month.getMonth() ? 'faded' : ''} ${active ? 'selected' : ''} ${current ? 'current' : ''}`} aria-label={dayFormat.format(day)} aria-pressed={active}><span>{day.getDate()}</span>{grouped[key]?.length ? <i className="day-dot" /> : null}</button>
          })}</div>
          <div className="calendar-bottom"><span><i className="day-dot" /> Com compromisso</span><button onClick={() => { const now = today(); setSelected(now); setMonth(new Date(now.getFullYear(), now.getMonth(), 1)) }}>Ir para hoje</button></div>
        </section>
        <section className="panel day-panel">
          <div className="section-heading"><div><span className="overline">AGENDA DO DIA</span><h2>{dayFormat.format(selected)}</h2></div><button className="icon-add" aria-label="Adicionar compromisso" onClick={() => openNew()}>＋</button></div>
          {loading ? <div className="empty-day">Carregando seus compromissos...</div> : daily.length ? <div className="daily-list">{daily.map(item => <button key={item.id} className="event-item" onClick={() => openEdit(item)}>
            <span className="event-category">{categoryFor(item.category).icon}</span><div className="event-main"><strong>{item.title}</strong><small>{formatTime(item.startAt)} – {formatTime(item.endAt)} · {categoryFor(item.category).label}</small>{item.location && <small>📍 {item.location}</small>}</div><span className="event-chevron">›</span>
          </button>)}</div> : <div className="empty-day"><span className="empty-illustration">☀️</span><h3>Dia livre por aqui!</h3><p>Nenhum compromisso marcado. Aproveite o tempo ou adicione algo novo.</p><button onClick={() => openNew()}>＋ Adicionar compromisso</button></div>}
        </section>
      </div>
      <section className="upcoming-block"><div className="section-heading"><div><span className="overline">FIQUE TRANQUILO</span><h2>Próximos compromissos</h2></div><span className="subtle-label">Avisamos você na hora certa</span></div>
        <div className="upcoming-list">{upcoming.length ? upcoming.map(item => <button key={item.id} onClick={() => openEdit(item)} className="upcoming-card"><span>{categoryFor(item.category).icon}</span><div><strong>{item.title}</strong><small>{dayFormat.format(new Date(item.startAt))} · {formatTime(item.startAt)}</small></div><span className="pill">🔔 Lembretes</span></button>) : <p className="muted-copy">Seus próximos compromissos aparecerão aqui assim que forem cadastrados.</p>}</div>
      </section>
      <LocationSettings />
      <footer className="app-footer">Me_lembra ✳ &nbsp; Feito para deixar seus dias mais leves. <span>Para iPhone: adicione à Tela de Início antes de ativar o push.</span></footer>
    </main>
    {form && <div className="modal-overlay" onClick={() => !saving && setForm(null)}><form className="event-modal" onClick={event => event.stopPropagation()} onSubmit={submit} aria-label="Formulário de compromisso">
      <div className="modal-heading"><div><span className="overline">SUA AGENDA</span><h2>{editing ? 'Editar compromisso' : 'Novo compromisso'}</h2><p>{editing ? 'Ao salvar, os lembretes serão atualizados.' : 'Conta pra gente o que você não quer esquecer.'}</p></div><button type="button" onClick={() => setForm(null)} disabled={saving} aria-label="Fechar">×</button></div>
      <label>O que você precisa fazer?<input required maxLength={160} autoFocus placeholder="Ex.: Consulta no dentista" value={form.title} onChange={event => setForm({ ...form, title: event.target.value })} /></label>
      <label>Categoria<select value={form.category} onChange={event => setForm({ ...form, category: event.target.value as Category })}>{categories.map(item => <option key={item.value} value={item.value}>{item.icon} {item.label}</option>)}</select></label>
      <label>Data<input required type="date" value={form.date} onChange={event => setForm({ ...form, date: event.target.value })} /></label>
      <div className="form-row"><label>Começa às<input required type="time" value={form.start} onChange={event => setForm({ ...form, start: event.target.value })} /></label><label>Termina às<input required type="time" value={form.end} onChange={event => setForm({ ...form, end: event.target.value })} /></label></div>
      <label>Onde? <em>(opcional)</em>
        <AddressPicker value={form.location} selected={form.destinationLatitude !== null && form.destinationLongitude !== null}
          placeholder="Ex.: Clínica, centro da cidade"
          onTextChange={value => setForm({ ...form, location: value, destinationLatitude: null, destinationLongitude: null })}
          onSelect={place => setForm({ ...form, location: place.label,
            destinationLatitude: place.latitude, destinationLongitude: place.longitude })} />
      </label>
      <label>Observações <em>(opcional)</em><textarea maxLength={1000} rows={2} placeholder="O que você precisa lembrar?" value={form.notes} onChange={event => setForm({ ...form, notes: event.target.value })} /></label>
      <ReminderPreview form={form} />
      {formError && <p className="form-error" role="alert">{formError}</p>}
      <div className="modal-actions">{editing && <button className="delete-button" type="button" onClick={removeEvent} disabled={saving}>Cancelar evento</button>}<button className="primary-button" type="submit" disabled={saving}>{saving ? 'Salvando...' : editing ? 'Salvar alterações' : 'Criar compromisso'}</button></div>
    </form></div>}
  </div>
}
export default function App() {
  const [user, setUser] = useState<User | null>()
  useEffect(() => { getCurrentUser().then(setUser).catch(() => setUser(null)) }, [])
  if (user === undefined) return <div className="app-loading"><span>✳</span> Preparando o Me_lembra...</div>
  return user ? <AppShell user={user} /> : <Login />
}
