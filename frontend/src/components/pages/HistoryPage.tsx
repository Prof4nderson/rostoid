import { useEffect, useState } from 'react';
import { Clock3, History as HistoryIcon } from 'lucide-react';
import type { EventRecord, Person, Profile } from '../../api/client';

export default function HistoryPage({ events, people, readProfile }: { events: EventRecord[]; people: Person[]; readProfile: (person: Person) => Promise<Profile | null> }) {
  const [names, setNames] = useState<Record<number, string>>({});
  useEffect(() => {
    let current = true;
    void (async () => {
      const resolved: Record<number, string> = {};
      await Promise.all(people.map(async (person) => {
        if (!person.profileCiphertext || !person.profileIv) return;
        try {
          const profile = await readProfile(person);
          if (profile?.fullName?.trim()) resolved[person.id] = profile.fullName.trim();
        } catch { /* Perfil indisponível ou não descriptografável: exibir ID genérico. */ }
      }));
      if (current) setNames(resolved);
    })();
    return () => { current = false; };
  }, [people, readProfile]);
  const sorted = [...events].sort((a, b) => new Date(b.recognizedAt).getTime() - new Date(a.recognizedAt).getTime());
  return <section className="page">
    <div className="page-heading"><div><div className="eyebrow">AUDITORIA LOCAL <span>·</span> ÚLTIMAS 200 OCORRÊNCIAS</div><h1>Histórico</h1><p>Os nomes são descriptografados apenas neste dispositivo; horários no fuso local.</p></div><div className="count-chip"><b>{String(events.length).padStart(2, '0')}</b> EVENTOS</div></div>
    <div className="history-table"><div className="table-heading history-heading"><span>PARTICIPANTE</span><span>SENTIDO</span><span>CÂMERA</span><span>DATA E HORÁRIO LOCAL</span></div>
      {sorted.length === 0 ? <div className="empty-state"><HistoryIcon size={23} /><b>Nenhum evento registrado</b><p>Reconhecimentos consentidos serão listados aqui.</p></div> : sorted.map((event) => <article className="history-row history-detail-row" key={event.id}>
        <span className="history-person"><strong>{names[event.personId] || event.publicId}</strong><small>{event.publicId} · EVT-{String(event.id).padStart(5, '0')}</small></span>
        <span className={`history-direction ${event.direction ? 'known' : 'unknown'}`}>{event.direction === 'ENTRY' ? 'Entrada' : event.direction === 'EXIT' ? 'Saída' : 'Não informado'}</span>
        <span className="history-camera">{event.cameraLabel?.trim() || '—'}</span>
        <time dateTime={event.recognizedAt}><Clock3 size={15} />{new Date(event.recognizedAt).toLocaleString('pt-BR')}</time>
      </article>)}
    </div><div className="privacy-mini list-privacy"><Clock3 size={16} /><span>Sentido e câmera vêm do snapshot do evento; registros antigos sem esses metadados permanecem como “Não informado” e “—”. Atualizações de perfil não alteram eventos.</span></div>
  </section>;
}
