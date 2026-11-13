import { useEffect, useState, type FormEvent } from 'react';
import { AlertTriangle, Aperture, ArrowRight, KeyRound, ShieldCheck } from 'lucide-react';
import { api, type SecurityState } from '../api/client';
import { deriveVault } from '../crypto/vault';

export interface Session { token: string; key: CryptoKey }
export default function SecurityGate({ onUnlock }: { onUnlock: (session: Session) => void }) {
  const [security, setSecurity] = useState<SecurityState | null>(null);
  const [loading, setLoading] = useState(true);
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [bootstrapKey, setBootstrapKey] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  useEffect(() => { api.securityState().then(setSecurity).catch((e: unknown) => setError(`Não foi possível conectar ao serviço: ${e instanceof Error ? e.message : 'erro de rede'}`)).finally(() => setLoading(false)); }, []);

  async function submit(event: FormEvent) {
    event.preventDefault(); setError('');
    if (!security) return;
    if (password.length < 12) { setError('Use uma senha com pelo menos 12 caracteres.'); return; }
    if (!security.configured && bootstrapKey.length < 24) { setError('Informe a chave de provisionamento fornecida pelo responsável do projeto.'); return; }
    if (!security.configured && password !== confirm) { setError('As senhas não coincidem.'); return; }
    if (security.encryptionSalt === security.authSalt) { setError('O serviço retornou salts iguais. A configuração foi interrompida por segurança.'); return; }
    setBusy(true);
    try {
      const derived = await deriveVault(password, security.encryptionSalt, security.authSalt);
      if (security.configured) await api.login(derived.token); else await api.setup(derived.token, bootstrapKey);
      setPassword(''); setConfirm(''); setBootstrapKey(''); onUnlock(derived);
    } catch (e) { setError(e instanceof Error ? e.message : 'Não foi possível validar o cofre.'); }
    finally { setBusy(false); }
  }

  return <main className="gate-page"><div className="gate-grid" aria-hidden="true" /><section className="gate-card">
    <div className="gate-brand"><span className="brand-mark"><Aperture size={24} /></span><span>rosto<strong>ID</strong></span></div>
    <div className="gate-eyebrow"><span className="live-dot" /> BIOMETRIA LOCAL · ACESSO RESTRITO</div>
    <h1>{loading ? 'Iniciando cofre…' : security?.configured ? 'Desbloqueie seu cofre' : 'Configure seu cofre'}</h1>
    <p className="gate-intro">Descritores biométricos e perfis permanecem cifrados no serviço. A chave de leitura é derivada localmente da sua senha.</p>
    {!security && !loading && <div className="inline-alert error"><AlertTriangle size={18} />{error || 'Serviço indisponível. Verifique a conexão com a API.'}</div>}
    {security && <form onSubmit={submit} className="gate-form">
      <label htmlFor="vault-password">Senha do cofre</label>
      <input id="vault-password" type="password" autoComplete={security.configured ? 'current-password' : 'new-password'} minLength={12} value={password} onChange={(e) => setPassword(e.target.value)} placeholder="Mínimo de 12 caracteres" required disabled={busy} />
      {!security.configured && <>
        <label htmlFor="bootstrap-key">Chave de provisionamento</label>
        <input id="bootstrap-key" type="password" autoComplete="off" minLength={24} value={bootstrapKey} onChange={(e) => setBootstrapKey(e.target.value)} placeholder="Fornecida pelo responsável do projeto" required disabled={busy} />
        <div className="inline-alert warning"><AlertTriangle size={18} /><span>A configuração inicial só será aceita com a chave protegida do projeto. Não compartilhe esta chave em mensagens.</span></div>
        <label htmlFor="vault-confirm">Confirme a senha</label>
        <input id="vault-confirm" type="password" autoComplete="new-password" value={confirm} onChange={(e) => setConfirm(e.target.value)} placeholder="Digite novamente" required disabled={busy} />
        <div className="inline-alert warning"><AlertTriangle size={18} /><span><b>Esta senha não pode ser recuperada.</b> Se esquecida, não será possível descriptografar descritores nem perfis salvos.</span></div>
      </>}
      {error && security && <div className="inline-alert error" role="alert">{error}</div>}
      <button className="primary-button wide" type="submit" disabled={busy || loading}>{busy ? 'Validando…' : security.configured ? 'Desbloquear' : 'Criar cofre'}<ArrowRight size={17} /></button>
    </form>}
    <div className="gate-assurance"><KeyRound size={15} /> Chaves e senha ficam apenas na memória desta sessão.</div>
    <div className="gate-foot"><ShieldCheck size={16} /> Protótipo · avalie privacidade e LGPD antes de uso real.</div>
  </section><div className="gate-index">ROSTOID / AMBIENTE LOCAL <span>·</span> 01</div></main>;
}
