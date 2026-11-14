import { useCallback, useEffect, useState } from 'react';
import { api, type CameraConfiguration, type EventRecord, type Person, type Profile, type RecognitionReceipt } from './api/client';
import { decryptJson, encryptJson } from './crypto/vault';
import SecurityGate, { type Session } from './components/SecurityGate';
import Shell, { type PageId } from './components/Shell';
import CameraPage from './components/pages/CameraPage';
import type { KnownDescriptor } from './components/pages/CameraScanner';
import PeoplePage from './components/pages/PeoplePage';
import HistoryPage from './components/pages/HistoryPage';

export default function App() {
  const [session, setSession] = useState<Session | null>(null);
  const [page, setPage] = useState<PageId>('camera');
  const [people, setPeople] = useState<Person[]>([]);
  const [events, setEvents] = useState<EventRecord[]>([]);
  const [cameras, setCameras] = useState<CameraConfiguration[]>([]);
  const [known, setKnown] = useState<KnownDescriptor[]>([]);
  const [theme, setTheme] = useState<'light' | 'dark'>(() => (localStorage.getItem('rostoid-theme') as 'light' | 'dark') || 'dark');
  const [loadError, setLoadError] = useState('');
  const [refreshing, setRefreshing] = useState(false);

  const refresh = useCallback(async (active: Session) => {
    setRefreshing(true); setLoadError('');
    try {
      const [list, history, cameraList] = await Promise.all([api.people(active.token), api.events(active.token), api.cameras(active.token)]);
      const descriptors: KnownDescriptor[] = [];
      for (const person of list) {
        try {
          const values = await decryptJson<number[]>({ ciphertext: person.faceCiphertext, iv: person.faceIv }, active.key);
          if (Array.isArray(values) && values.length > 0 && values.every(Number.isFinite)) descriptors.push({ person, descriptor: new Float32Array(values) });
        } catch { /* Um cadastro não descriptografável não é usado para reconhecimento. */ }
      }
      setPeople(list); setEvents(history); setCameras(cameraList); setKnown(descriptors);
    } catch (e) { setLoadError(e instanceof Error ? e.message : 'Falha ao carregar dados protegidos.'); }
    finally { setRefreshing(false); }
  }, []);

  useEffect(() => { if (session) void refresh(session); else { setPeople([]); setEvents([]); setCameras([]); setKnown([]); } }, [session, refresh]);
  const addPerson = async (descriptor: Float32Array, cameraId: number) => {
    if (!session) throw new Error('Sessão encerrada.');
    if (!cameras.some((camera) => camera.id === cameraId)) throw new Error('Configure uma câmera antes de cadastrar.');
    const encrypted = await encryptJson(Array.from(descriptor), session.key);
    const created = await api.createPerson(session.token, { faceCiphertext: encrypted.ciphertext, faceIv: encrypted.iv, consentConfirmed: true, cameraId });
    setPeople((previous) => [...previous, created]);
    setKnown((previous) => [...previous, { person: created, descriptor: new Float32Array(descriptor) }]);
    void refresh(session);
    return created;
  };
  const createEvent = async (personId: number, cameraId: number): Promise<RecognitionReceipt> => {
    if (!session) throw new Error('Sessão encerrada.');
    if (!cameras.some((camera) => camera.id === cameraId)) throw new Error('A câmera não está configurada; nenhum evento foi enviado.');
    const receipt = await api.createEvent(session.token, personId, cameraId);
    const history = await api.events(session.token);
    setEvents(history);
    return receipt;
  };
  const saveCameras = async (drafts: Array<Pick<CameraConfiguration, 'deviceId' | 'label' | 'direction'>>) => {
    if (!session) throw new Error('Sessão encerrada.');
    if (drafts.length > 2 || (drafts.length === 2 && drafts[0].direction === drafts[1].direction)) throw new Error('Configure no máximo duas câmeras, uma Entrada e outra Saída.');
    const saved = await api.saveCameras(session.token, drafts);
    setCameras(saved);
    return saved;
  };
  const saveProfile = async (id: number, profile: Profile) => {
    if (!session) throw new Error('Sessão encerrada.');
    const encrypted = await encryptJson(profile, session.key);
    const updated = await api.saveProfile(session.token, id, encrypted);
    setPeople((previous) => previous.map((person) => person.id === id ? updated : person));
  };
  const readProfile = async (person: Person): Promise<Profile | null> => {
    if (!session || !person.profileCiphertext || !person.profileIv) return null;
    return decryptJson<Profile>({ ciphertext: person.profileCiphertext, iv: person.profileIv }, session.key);
  };
  const deletePerson = async (id: number) => {
    if (!session) throw new Error('Sessão encerrada.');
    await api.deletePerson(session.token, id);
    setPeople((previous) => previous.filter((person) => person.id !== id));
    setKnown((previous) => previous.filter((item) => item.person.id !== id));
    setEvents((previous) => previous.filter((event) => event.personId !== id));
  };
  function lock() { setSession(null); setPage('camera'); }
  function toggleTheme() { const next = theme === 'dark' ? 'light' : 'dark'; setTheme(next); localStorage.setItem('rostoid-theme', next); }

  if (!session) return <SecurityGate onUnlock={setSession} />;
  return <div className={`theme-root theme-${theme}`}><Shell active={page} onNavigate={setPage} onLock={lock} username={session.username} theme={theme} onToggleTheme={toggleTheme}>
    {loadError && <div className="inline-alert error app-load-error" role="alert">{loadError}<button onClick={() => void refresh(session)}>Tentar novamente</button></div>}
    {refreshing && <div className="sync-note">Sincronizando registros cifrados…</div>}
    {page === 'camera' && <CameraPage known={known} cameras={cameras} showConfiguration={cameras.length === 0} showScanners onSaveCameras={saveCameras} onEnroll={addPerson} onEvent={createEvent} />}
    {page === 'settings' && <CameraPage known={known} cameras={cameras} showConfiguration showScanners={false} onSaveCameras={saveCameras} onEnroll={addPerson} onEvent={createEvent} />}
    {page === 'people' && <PeoplePage people={people} onSaveProfile={saveProfile} onDelete={deletePerson} readProfile={readProfile} />}
    {page === 'history' && <HistoryPage events={events} people={people} readProfile={readProfile} />}
  </Shell></div>;
}
