import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import * as faceapi from '@vladmandic/face-api';
import { Camera, CircleAlert, CircleCheck, Plus, RefreshCw, Save, Trash2 } from 'lucide-react';
import type { CameraConfiguration, Person, RecognitionReceipt } from '../../api/client';
import CameraScanner, { type CooldownStore, type KnownDescriptor } from './CameraScanner';

type DraftCamera = { key: string; deviceId: string; label: string; direction: 'ENTRY' | 'EXIT'; id?: number };
const labelDirection = (direction: DraftCamera['direction']) => direction === 'ENTRY' ? 'Entrada' : 'Saída';

export default function CameraPage({ known, cameras, showConfiguration, showScanners, onSaveCameras, onEnroll, onEvent }: {
  known: KnownDescriptor[]; cameras: CameraConfiguration[]; showConfiguration: boolean; showScanners: boolean;
  onSaveCameras: (cameras: Array<Pick<CameraConfiguration, 'deviceId' | 'label' | 'direction'>>) => Promise<CameraConfiguration[]>;
  onEnroll: (descriptor: Float32Array, cameraId: number) => Promise<Person>;
  onEvent: (personId: number, cameraId: number) => Promise<RecognitionReceipt>;
}) {
  const [devices, setDevices] = useState<MediaDeviceInfo[]>([]);
  const [permitted, setPermitted] = useState(false);
  const [draft, setDraft] = useState<DraftCamera[]>([]);
  const [modelsReady, setModelsReady] = useState(false);
  const [modelError, setModelError] = useState('');
  const [loadingDevices, setLoadingDevices] = useState(false);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const cooldowns = useRef<CooldownStore>({ lastSuccess: new Map(), lastAttempt: new Map(), pending: new Set() });
  const [activeIds, setActiveIds] = useState<Set<number>>(new Set());
  const handleRunningChange = useCallback((id: number, active: boolean) => {
    setActiveIds((current) => { const next = new Set(current); if (active) next.add(id); else next.delete(id); return next; });
  }, []);
  useEffect(() => {
    setDraft(cameras.map((camera) => ({ key: String(camera.id), id: camera.id, deviceId: camera.deviceId, label: camera.label, direction: camera.direction })));
  }, [cameras]);
  useEffect(() => {
    let alive = true;
    Promise.all([faceapi.nets.tinyFaceDetector.loadFromUri('/models'), faceapi.nets.faceLandmark68Net.loadFromUri('/models'), faceapi.nets.faceRecognitionNet.loadFromUri('/models')])
      .then(() => { if (alive) setModelsReady(true); })
      .catch(() => { if (alive) setModelError('Modelos locais não encontrados em /models/. A detecção permanece indisponível até que sejam instalados.'); });
    return () => { alive = false; };
  }, []);

  async function requestDevices() {
    setError(''); setMessage(''); setLoadingDevices(true);
    try {
      if (!navigator.mediaDevices?.getUserMedia) throw new Error('Este navegador não disponibiliza acesso à câmera.');
      const permissionStream = await navigator.mediaDevices.getUserMedia({ video: true, audio: false });
      try {
        const all = await navigator.mediaDevices.enumerateDevices();
        const videoInputs = all.filter((device) => device.kind === 'videoinput');
        setDevices(videoInputs); setPermitted(true);
        setDraft((current) => current.map((item) => {
          const match = videoInputs.find((device) => device.deviceId === item.deviceId);
          return match ? { ...item, label: match.label || item.label } : item;
        }));
        setMessage(`${videoInputs.length} dispositivo(s) de câmera disponível(is). A captura de reconhecimento não foi iniciada.`);
      } finally { permissionStream.getTracks().forEach((track) => track.stop()); }
    } catch (e) { setError(e instanceof Error ? e.message : 'Não foi possível autorizar ou listar câmeras.'); }
    finally { setLoadingDevices(false); }
  }

  function addCamera() {
    if (!permitted) { setError('Primeiro autorize e liste os dispositivos de câmera.'); return; }
    if (draft.length >= 2) return;
    const used = new Set(draft.map((item) => item.deviceId));
    const available = devices.find((device) => !used.has(device.deviceId));
    if (!available) { setError('Não há outro dispositivo disponível para adicionar.'); return; }
    const direction = draft.length === 1 ? (draft[0].direction === 'ENTRY' ? 'EXIT' : 'ENTRY') : 'ENTRY';
    setDraft((current) => [...current, { key: `new-${Date.now()}`, deviceId: available.deviceId, label: available.label || `Câmera ${current.length + 1}`, direction }]);
    setError('');
  }

  function updateDraft(key: string, updates: Partial<DraftCamera>) {
    setDraft((current) => current.map((item) => item.key === key ? { ...item, ...updates } : item));
  }

  async function save() {
    setError(''); setMessage('');
    if (activeIds.size > 0) { setError('Pare todos os feeds antes de alterar ou salvar a configuração.'); return; }
    if (cooldowns.current.pending.size > 0) { setError('Aguarde a confirmação dos eventos em andamento antes de alterar o sentido da câmera.'); return; }
    if (draft.length > 2 || new Set(draft.map((item) => item.deviceId)).size !== draft.length) { setError('Selecione dispositivos distintos e configure no máximo duas câmeras.'); return; }
    if (draft.length === 2 && draft[0].direction === draft[1].direction) { setError('Com duas câmeras, configure uma como Entrada e a outra como Saída.'); return; }
    if (draft.some((item) => !item.deviceId || !item.label.trim())) { setError('Cada câmera precisa ter um dispositivo selecionado.'); return; }
    setSaving(true);
    try { await onSaveCameras(draft.map(({ deviceId, label, direction }) => ({ deviceId, label, direction }))); setMessage('Configuração salva. Os sentidos exibidos abaixo são os sentidos autoritativos para os próximos eventos.'); }
    catch (e) { setError(e instanceof Error ? e.message : 'Não foi possível salvar a configuração.'); }
    finally { setSaving(false); }
  }

  const configured = useMemo(() => cameras.filter((camera) => Boolean(camera.id && camera.deviceId)), [cameras]);
  const isDirty = draft.length !== cameras.length || draft.some((item, index) => item.id !== cameras[index]?.id || item.deviceId !== cameras[index]?.deviceId || item.direction !== cameras[index]?.direction || item.label !== cameras[index]?.label);
  return <section className={`page camera-page ${showScanners && !showConfiguration ? 'camera-monitor-page' : ''}`}>
    <div className="page-heading"><div><div className="eyebrow">CENTRO DE DETECÇÃO <span>·</span> AMBIENTE LOCAL</div><h1>{showConfiguration && !showScanners ? 'Configurações' : 'Câmera'}</h1><p>{showConfiguration && !showScanners ? 'Altere os dispositivos e sentidos sem sair da visualização principal.' : showConfiguration ? 'Faça a configuração inicial para liberar os feeds de reconhecimento.' : 'Acompanhe as imagens e controle cada feed sem rolagem extra.'}</p></div>{showScanners && <div className={`camera-indicator ${activeIds.size ? 'on' : ''}`}><i /> {activeIds.size ? `${activeIds.size} FEED(S) ATIVO(S)` : 'FEEDS PAUSADOS'}</div>}</div>
    {showConfiguration && <section className="camera-config-panel" aria-labelledby="camera-config-title">
      <div className="panel-heading"><div><span className="eyebrow">CONFIGURAÇÃO AUTORITATIVA</span><h2 id="camera-config-title">Dispositivos e sentido</h2></div><span className="mono">0–2 CÂMERAS</span></div>
      <p className="camera-config-intro">Ação explícita necessária para solicitar permissão e listar dispositivos. Com duas câmeras, use um sentido de Entrada e outro de Saída. Com uma, pare o feed antes de alternar o sentido.</p>
      <div className="camera-config-actions"><button className="secondary-button" type="button" onClick={() => void requestDevices()} disabled={loadingDevices}>{loadingDevices ? <RefreshCw className="spin" size={16} /> : <Camera size={16} />}{loadingDevices ? 'Listando…' : permitted ? 'Atualizar lista de câmeras' : 'Autorizar e listar câmeras'}</button><span className="camera-hint">{permitted ? `${devices.length} dispositivo(s) listado(s)` : 'Nenhuma solicitação de permissão foi feita.'}</span></div>
      {draft.length === 0 && <div className="empty-detection camera-empty"><Camera size={20} /><span>Nenhuma câmera configurada. Não é possível iniciar reconhecimento nem registrar eventos.</span></div>}
      <div className="camera-config-list">{draft.map((item, index) => <div className="camera-config-row" key={item.key}>
        <label>Dispositivo<select value={item.deviceId} onChange={(event) => { const selected = devices.find((device) => device.deviceId === event.target.value); updateDraft(item.key, { deviceId: event.target.value, label: selected?.label || item.label }); }} disabled={!permitted || devices.length === 0 || activeIds.size > 0}>
          {!devices.some((device) => device.deviceId === item.deviceId) && <option value={item.deviceId}>{item.label} (salvo)</option>}
          {devices.map((device) => <option key={device.deviceId} value={device.deviceId}>{device.label || `Câmera ${devices.indexOf(device) + 1}`}</option>)}
        </select></label>
        <label>Sentido<select value={item.direction} onChange={(event) => updateDraft(item.key, { direction: event.target.value as DraftCamera['direction'] })} disabled={activeIds.size > 0}>
          <option value="ENTRY">Entrada</option><option value="EXIT">Saída</option>
        </select></label>
        <span className="camera-direction-badge">{labelDirection(item.direction)}{item.id ? ' · SALVO' : ' · NÃO SALVO'}</span>
        <button className="icon-button danger" type="button" aria-label={`Remover câmera ${index + 1}`} onClick={() => setDraft((current) => current.filter((candidate) => candidate.key !== item.key))} disabled={activeIds.size > 0}><Trash2 size={16} /> Remover</button>
      </div>)}</div>
      <div className="camera-config-footer"><button className="secondary-button" type="button" onClick={addCamera} disabled={!permitted || draft.length >= 2 || devices.length <= draft.length || activeIds.size > 0}><Plus size={16} /> Adicionar câmera</button><button className="primary-button" type="button" onClick={() => void save()} disabled={saving || !isDirty || activeIds.size > 0}><Save size={16} />{saving ? 'Salvando…' : 'Salvar configuração'}</button></div>
      {error && <div className="inline-alert error" role="alert"><CircleAlert size={17} />{error}</div>}{message && <div className="inline-alert success"><CircleCheck size={17} />{message}</div>}{modelError && <div className="inline-alert warning"><CircleAlert size={17} />{modelError}</div>}
    </section>}
    {showScanners && (configured.length === 0 ? <div className="empty-state scanner-empty"><Camera size={24} /><b>Feeds indisponíveis</b><p>Salve pelo menos uma câmera configurada para habilitar captura e eventos. Cada feed é iniciado e parado individualmente.</p></div> : <>
      <div className="scanner-grid">{configured.map((camera) => <CameraScanner key={`${camera.id}-${camera.direction}`} camera={camera} known={known} modelsReady={modelsReady} cooldowns={cooldowns.current} onEnroll={onEnroll} onEvent={onEvent} onRunningChange={handleRunningChange} />)}</div>
      <div className="privacy-mini list-privacy"><CircleAlert size={16} /><span>{configured.length === 1 ? 'Somente a câmera salva pode registrar eventos. Para alternar Entrada/Saída, pare o feed, altere e salve o sentido, então reinicie.' : 'Cada câmera tem controles independentes; ambas podem operar simultaneamente com sentidos opostos.'} O histórico mantém o sentido e rótulo salvos em cada evento.</span></div>
    </>)}
  </section>;
}
