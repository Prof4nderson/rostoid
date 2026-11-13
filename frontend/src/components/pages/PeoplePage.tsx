import { useState } from 'react';
import { ChevronDown, ChevronUp, Eye, EyeOff, Save, Trash2, UserRound, X } from 'lucide-react';
import type { Person, Profile } from '../../api/client';

export default function PeoplePage({ people, onSaveProfile, onDelete, readProfile }: {
  people: Person[]; onSaveProfile: (id: number, profile: Profile) => Promise<void>; onDelete: (id: number) => Promise<void>; readProfile: (person: Person) => Promise<Profile | null>;
}) {
  const [openId, setOpenId] = useState<number | null>(null);
  const [profile, setProfile] = useState<Profile>({ fullName: '' });
  const [visible, setVisible] = useState<number | null>(null);
  const [confirmId, setConfirmId] = useState<number | null>(null);
  const [busyId, setBusyId] = useState<number | null>(null);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  async function toggleProfile(person: Person) {
    setMessage(''); setError('');
    if (openId === person.id) { setOpenId(null); return; }
    setOpenId(person.id); setBusyId(person.id);
    try { const saved = await readProfile(person); setProfile(saved ?? { fullName: '', email: '', phone: '' }); }
    catch { setProfile({ fullName: '', email: '', phone: '' }); setError(`Não foi possível descriptografar o perfil de ${person.publicId}.`); }
    finally { setBusyId(null); }
  }
  async function save(person: Person) {
    if (!profile.fullName.trim()) { setError('Nome completo é obrigatório.'); return; }
    setBusyId(person.id); setError(''); setMessage('');
    try { await onSaveProfile(person.id, { fullName: profile.fullName.trim(), email: profile.email?.trim() || undefined, phone: profile.phone?.trim() || undefined }); setMessage(`Perfil de ${person.publicId} cifrado e atualizado.`); setOpenId(null); }
    catch (e) { setError(e instanceof Error ? e.message : 'Falha ao salvar o perfil.'); }
    finally { setBusyId(null); }
  }
  async function remove(person: Person) {
    setBusyId(person.id); setError('');
    try { await onDelete(person.id); setConfirmId(null); setMessage(`${person.publicId} e seu histórico foram removidos.`); }
    catch (e) { setError(e instanceof Error ? e.message : 'Falha ao excluir.'); }
    finally { setBusyId(null); }
  }
  return <section className="page">
    <div className="page-heading"><div><div className="eyebrow">REGISTRO CRIPTOGRAFADO <span>·</span> SOMENTE PARTICIPANTES CONSENTIDOS</div><h1>Pessoas</h1><p>Perfis descriptografados apenas neste dispositivo; descritores biométricos nunca são exibidos.</p></div><div className="count-chip"><b>{String(people.length).padStart(2, '0')}</b> PARTICIPANTES</div></div>
    {message && <div className="inline-alert success">{message}</div>}{error && <div className="inline-alert error" role="alert">{error}</div>}
    <div className="people-list">{people.length === 0 ? <div className="empty-state"><UserRound size={24} /><b>Nenhuma pessoa cadastrada</b><p>Os cadastros consentidos aparecerão aqui. Nenhum rosto desconhecido é salvo automaticamente.</p></div> : people.map((person) => <article className="person-card" key={person.id}>
      <div className="person-main"><span className="person-avatar"><UserRound size={20} /></span><div className="person-identity"><span className="eyebrow">IDENTIFICADOR PÚBLICO</span><h2>{person.publicId}</h2><small>ID {person.id} <i /> Consentido em {new Date(person.consentAt).toLocaleDateString('pt-BR')}</small></div><div className="person-actions"><button className="icon-button" onClick={() => { setVisible(visible === person.id ? null : person.id); void toggleProfile(person); }} aria-label={visible === person.id ? 'Ocultar perfil e fechar edição' : 'Revelar e editar perfil'} title="Revelar/editar perfil">{visible === person.id ? <EyeOff size={17} /> : <Eye size={17} />}{openId === person.id ? <ChevronUp size={15} /> : <ChevronDown size={15} />}</button>
      {confirmId === person.id ? <button className="danger-button" onClick={() => void remove(person)} disabled={busyId === person.id}><Trash2 size={15} /> Confirmar exclusão</button> : <button className="icon-button danger" onClick={() => setConfirmId(person.id)} aria-label={`Excluir ${person.publicId}`} title="Excluir cadastro"><Trash2 size={16} /></button>}</div></div>
      {confirmId === person.id && <div className="delete-warning"><b>Excluir {person.publicId}?</b><span>Esta ação remove também todos os eventos associados e não pode ser desfeita.</span><button onClick={() => setConfirmId(null)}><X size={14} /> Cancelar</button></div>}
      {openId === person.id && <div className="profile-editor"><div className="profile-editor-heading"><b><Eye size={15} /> Perfil privado</b><span>Descriptografado neste dispositivo</span></div>{busyId === person.id ? <div className="loading-line">Lendo perfil cifrado…</div> : <div className="profile-fields"><label>Nome completo *<input value={profile.fullName} onChange={(e) => setProfile({ ...profile, fullName: e.target.value })} placeholder="Nome completo" autoComplete="off" /></label><label>E-mail (opcional)<input type="email" value={profile.email ?? ''} onChange={(e) => setProfile({ ...profile, email: e.target.value })} placeholder="nome@exemplo.com" autoComplete="off" /></label><label>Telefone (opcional)<input type="tel" value={profile.phone ?? ''} onChange={(e) => setProfile({ ...profile, phone: e.target.value })} placeholder="+55 …" autoComplete="off" /></label><button className="primary-button" onClick={() => void save(person)} disabled={busyId === person.id}><Save size={16} /> Salvar cifrado</button></div>}</div>}
    </article>)}</div>
    <div className="privacy-mini list-privacy"><EyeOff size={16} /><span>Perfis e descritores são cifrados com AES-GCM. A edição do perfil não altera o histórico de reconhecimento.</span></div>
  </section>;
}
