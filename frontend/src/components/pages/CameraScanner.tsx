import { useCallback, useEffect, useRef, useState } from 'react';
import * as faceapi from '@vladmandic/face-api';
import { Aperture, Camera, CameraOff, CircleAlert, CircleCheck, ScanFace, ShieldCheck, UserPlus } from 'lucide-react';
import type { CameraConfiguration, Person, RecognitionReceipt } from '../../api/client';

export interface KnownDescriptor { person: Person; descriptor: Float32Array }
export type Direction = CameraConfiguration['direction'];
export type CooldownStore = { lastSuccess: Map<string, number>; lastAttempt: Map<string, number>; pending: Set<string> };

export default function CameraScanner({ camera, known, modelsReady, cooldowns, onEnroll, onEvent, onRunningChange }: {
  camera: CameraConfiguration; known: KnownDescriptor[]; modelsReady: boolean; cooldowns: CooldownStore;
  onEnroll: (descriptor: Float32Array, cameraId: number) => Promise<Person>;
  onEvent: (personId: number, cameraId: number) => Promise<RecognitionReceipt>;
  onRunningChange: (cameraId: number, active: boolean) => void;
}) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const rafRef = useRef<number | null>(null);
  const lastDetection = useRef(0);
  const currentDescriptor = useRef<Float32Array | null>(null);
  const knownRef = useRef(known);
  const consentedDescriptor = useRef<Float32Array | null>(null);
  const [cameraOn, setCameraOn] = useState(false);
  const [status, setStatus] = useState<'idle' | 'loading' | 'scanning' | 'unknown' | 'recognized' | 'error'>('idle');
  const [match, setMatch] = useState<Person | null>(null);
  const [consent, setConsent] = useState(false);
  const [enrolling, setEnrolling] = useState(false);
  const [notice, setNotice] = useState('');
  const [error, setError] = useState('');
  useEffect(() => { knownRef.current = known; }, [known]);

  const stopCamera = useCallback(() => {
    if (rafRef.current !== null) cancelAnimationFrame(rafRef.current);
    rafRef.current = null;
    streamRef.current?.getTracks().forEach((track) => track.stop());
    streamRef.current = null;
    if (videoRef.current) videoRef.current.srcObject = null;
    currentDescriptor.current = null;
    consentedDescriptor.current = null;
    setCameraOn(false); setStatus('idle'); setMatch(null); setConsent(false);
    onRunningChange(camera.id, false);
  }, [camera.id, onRunningChange]);
  useEffect(() => () => stopCamera(), [stopCamera]);

  async function startCamera() {
    setError(''); setNotice(''); setStatus('loading');
    if (!modelsReady) { setError('Aguarde o carregamento dos modelos faciais locais.'); setStatus('error'); return; }
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ video: { deviceId: { exact: camera.deviceId } }, audio: false });
      streamRef.current = stream;
      const video = videoRef.current;
      if (!video) { stream.getTracks().forEach((track) => track.stop()); return; }
      video.srcObject = stream;
      await video.play();
      setCameraOn(true); setStatus('scanning');
      onRunningChange(camera.id, true);
      const scan = async () => {
        if (!streamRef.current || !videoRef.current) return;
        const now = Date.now();
        if (videoRef.current.readyState >= 2 && now - lastDetection.current > 350) {
          lastDetection.current = now;
          try {
            const result = await faceapi.detectSingleFace(videoRef.current, new faceapi.TinyFaceDetectorOptions({ inputSize: 320, scoreThreshold: 0.5 })).withFaceLandmarks().withFaceDescriptor();
            if (result) {
              const descriptor = result.descriptor;
              currentDescriptor.current = descriptor;
              let best: KnownDescriptor | undefined;
              let bestDistance = Infinity;
              for (const candidate of knownRef.current) {
                const distance = faceapi.euclideanDistance(descriptor, candidate.descriptor);
                if (distance < bestDistance) { bestDistance = distance; best = candidate; }
              }
              if (best && bestDistance < 0.52) {
                const person = best.person; setMatch(person); setStatus('recognized');
                if (consentedDescriptor.current) { consentedDescriptor.current = null; setConsent(false); }
                const key = `${person.id}:${camera.id}:${camera.direction}`;
                const eventAt = cooldowns.lastSuccess.get(key) ?? 0;
                const attemptAt = cooldowns.lastAttempt.get(key) ?? 0;
                if (now - eventAt >= 30_000 && now - attemptAt >= 5_000 && !cooldowns.pending.has(key)) {
                  cooldowns.lastAttempt.set(key, now);
                  cooldowns.pending.add(key);
                  void onEvent(person.id, camera.id).then((receipt) => {
                    const direction = receipt.direction ?? camera.direction;
                    const actualKey = `${person.id}:${receipt.cameraId ?? camera.id}:${direction}`;
                    const recordedAt = Date.parse(receipt.recognizedAt);
                    if (Number.isFinite(recordedAt)) cooldowns.lastSuccess.set(actualKey, recordedAt);
                    setNotice(receipt.created ? `${direction === 'ENTRY' ? 'Entrada' : 'Saída'} registrada em ${receipt.cameraLabel || camera.label}.` : `Evento já existente nos últimos 30 segundos para esta câmera e sentido.`);
                  }).catch((e: unknown) => setError(e instanceof Error ? e.message : 'Falha ao registrar ocorrência.'))
                    .finally(() => cooldowns.pending.delete(key));
                }
              } else {
                setMatch(null); setStatus('unknown');
                const approved = consentedDescriptor.current;
                if (approved && faceapi.euclideanDistance(descriptor, approved) > 0.32) {
                  consentedDescriptor.current = null; setConsent(false);
                  setNotice('O rosto mudou desde a confirmação. Confirme novamente antes de cadastrar.');
                }
              }
            } else {
              currentDescriptor.current = null; setMatch(null); setStatus('scanning');
              if (consentedDescriptor.current) {
                consentedDescriptor.current = null; setConsent(false);
                setNotice('O rosto saiu do enquadramento. Confirme novamente antes de cadastrar.');
              }
            }
          } catch { setError('A análise local falhou. Verifique os modelos em /models/.'); setStatus('error'); }
        }
        if (streamRef.current) rafRef.current = requestAnimationFrame(scan);
      };
      rafRef.current = requestAnimationFrame(scan);
    } catch (e) {
      streamRef.current?.getTracks().forEach((track) => track.stop()); streamRef.current = null;
      setStatus('error'); setError(e instanceof Error ? `Câmera indisponível: ${e.message}` : 'Permissão da câmera negada ou dispositivo indisponível.');
    }
  }

  async function enroll() {
    const approved = consentedDescriptor.current;
    const video = videoRef.current;
    if (!approved || !video || !consent || !camera.id) return;
    const pendingKey = `enroll:${camera.id}:${camera.direction}`;
    if (cooldowns.pending.has(pendingKey)) return;
    cooldowns.pending.add(pendingKey);
    setEnrolling(true); setError('');
    try {
      const live = await faceapi.detectSingleFace(video, new faceapi.TinyFaceDetectorOptions({ inputSize: 320, scoreThreshold: 0.5 })).withFaceLandmarks().withFaceDescriptor();
      if (!live || consentedDescriptor.current !== approved || faceapi.euclideanDistance(live.descriptor, approved) > 0.32) {
        consentedDescriptor.current = null; setConsent(false);
        setError('O rosto atual não corresponde ao rosto confirmado. Marque o consentimento novamente para esta pessoa.');
        return;
      }
      const person = await onEnroll(new Float32Array(approved), camera.id);
      const initialEventAt = Date.parse(person.lastRecognizedAt || person.createdAt);
      const key = `${person.id}:${camera.id}:${camera.direction}`;
      if (Number.isFinite(initialEventAt)) cooldowns.lastSuccess.set(key, initialEventAt);
      consentedDescriptor.current = null;
      setNotice(`Participante ${person.publicId} cadastrado. O primeiro evento foi criado em ${camera.direction === 'ENTRY' ? 'Entrada' : 'Saída'}.`); setConsent(false);
    } catch (e) { setError(e instanceof Error ? e.message : 'Não foi possível cadastrar.'); }
    finally { cooldowns.pending.delete(pendingKey); setEnrolling(false); }
  }

  const label = status === 'recognized' ? 'Pessoa reconhecida' : status === 'unknown' ? 'Rosto não cadastrado' : status === 'scanning' ? 'Aguardando rosto' : status === 'loading' ? 'Iniciando câmera' : status === 'error' ? 'Atenção necessária' : 'Câmera desligada';
  return <article className="scanner-card" aria-label={`Feed ${camera.label}, ${camera.direction === 'ENTRY' ? 'Entrada' : 'Saída'}`}>
    <div className="scanner-heading"><div><span className="eyebrow">{camera.direction === 'ENTRY' ? 'ENTRADA' : 'SAÍDA'} · CÂMERA CONFIGURADA</span><h2>{camera.label}</h2></div><span className={`camera-indicator ${cameraOn ? 'on' : ''}`}><i />{cameraOn ? 'ATIVA' : 'PAUSADA'}</span></div>
    <div className="scanner-body">
      <div className="scanner-feed-column">
        <div className="video-stage"><video ref={videoRef} muted playsInline aria-label={`Pré-visualização local: ${camera.label}`} />
          {!cameraOn && <div className="video-placeholder"><span className="scan-glyph"><Aperture size={36} strokeWidth={1.2} /></span><b>Câmera pausada</b><p>Inicie este feed explicitamente.</p></div>}
          {cameraOn && <div className="viewfinder" aria-hidden="true"><i /><i /><i /><i /></div>}
          <div className={`scan-state ${status}`}><i />{label}</div>
        </div>
        <div className="camera-controls"><button className={cameraOn ? 'secondary-button' : 'primary-button'} onClick={cameraOn ? stopCamera : startCamera} disabled={!modelsReady && !cameraOn}>{cameraOn ? <><CameraOff size={17} /> Parar este feed</> : <><Camera size={17} /> Iniciar este feed</>}</button><span className="camera-hint">Sentido atual: <b>{camera.direction === 'ENTRY' ? 'Entrada' : 'Saída'}</b> · processamento local</span></div>
      </div>
      <aside className="scanner-side-panel" aria-label="Confirmação e resultado do feed">
        <div className="side-panel-label"><ScanFace size={15} /> LEITURA DO FEED</div>
        {error && <div className="inline-alert error" role="alert"><CircleAlert size={17} />{error}</div>}
        {match ? <div className="match-card"><div className="match-icon"><CircleCheck size={19} /></div><div><span className="eyebrow">PARTICIPANTE IDENTIFICADO</span><strong>{match.publicId}</strong><small>{camera.direction === 'ENTRY' ? 'Entrada' : 'Saída'} · {camera.label}</small></div></div> : <div className="empty-detection"><ScanFace size={19} /><span>{status === 'unknown' ? 'Rosto sem correspondência' : 'Nenhuma identificação ativa'}</span></div>}
        {status === 'unknown' && <div className="enroll-card"><div className="enroll-title"><UserPlus size={17} /><b>Confirmação única</b></div><p>Confirme uma vez que a pessoa foi informada e consentiu. A confirmação vale para este rosto enquanto ele permanecer no enquadramento.</p>
          <label className="consent-check"><input type="checkbox" checked={consent} onChange={(e) => { const checked = e.target.checked; const descriptor = currentDescriptor.current; if (checked && descriptor) { consentedDescriptor.current = new Float32Array(descriptor); setConsent(true); } else { consentedDescriptor.current = null; setConsent(false); } }} /><span>Confirmo o consentimento para coleta e processamento biométrico deste rosto.</span></label>
          <button className="primary-button wide" onClick={enroll} disabled={!consent || enrolling || !currentDescriptor.current}>{enrolling ? 'Cadastrando…' : 'Cadastrar pessoa consentida'}<UserPlus size={16} /></button></div>}
        {notice && <div className="inline-alert success"><CircleCheck size={17} />{notice}</div>}
        <div className="privacy-mini"><ShieldCheck size={16} /><span>Sem gravação de fotos ou vídeo; somente biometria cifrada é armazenada após consentimento.</span></div>
      </aside>
    </div>
  </article>;
}
