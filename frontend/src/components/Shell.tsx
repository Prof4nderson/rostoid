import { Aperture, ContactRound, History, LockKeyhole, Settings, ShieldCheck } from 'lucide-react';
import type { ReactNode } from 'react';

export type PageId = 'camera' | 'people' | 'history' | 'settings';
const links: { id: PageId; label: string; icon: typeof Aperture }[] = [
  { id: 'camera', label: 'Câmera', icon: Aperture },
  { id: 'people', label: 'Pessoas', icon: ContactRound },
  { id: 'history', label: 'Histórico', icon: History },
  { id: 'settings', label: 'Configurações', icon: Settings },
];

export default function Shell({ active, onNavigate, onLock, children }: {
  active: PageId; onNavigate: (page: PageId) => void; onLock: () => void; children: ReactNode;
}) {
  return <div className="app-shell">
    <aside className="sidebar">
      <a className="brand" href="#camera" aria-label="RostoID, início" onClick={(e) => { e.preventDefault(); onNavigate('camera'); }}>
        <span className="brand-mark"><Aperture size={21} strokeWidth={2.2} /></span>
        <span><strong>rosto<span>ID</span></strong><small>CONSOLE LOCAL</small></span>
      </a>
      <div className="nav-label">ÁREA DE TRABALHO</div>
      <nav aria-label="Navegação principal">
        {links.map(({ id, label, icon: Icon }) => <button key={id} className={`nav-link ${active === id ? 'selected' : ''}`} onClick={() => onNavigate(id)} aria-current={active === id ? 'page' : undefined}>
          <Icon size={18} /><span>{label}</span>{active === id && <i />}
        </button>)}
      </nav>
      <div className="sidebar-foot"><div className="privacy-stamp"><ShieldCheck size={17} /><span>Processamento<br /><b>neste dispositivo</b></span></div>
        <button className="lock-button" onClick={onLock}><LockKeyhole size={16} /> Bloquear cofre <kbd>↵</kbd></button>
      </div>
    </aside>
    <main className="main-area">
      <header className="topbar"><div className="breadcrumbs"><span>ROSTOID</span><b>/</b><strong>{links.find((l) => l.id === active)?.label.toUpperCase()}</strong></div><div className="topbar-actions"><span className="mode-badge"><i /> HOLO MODE <b>ACTIVE</b></span><div className="session-state"><i /> Cofre desbloqueado</div><button className="mobile-lock-button" onClick={onLock}><LockKeyhole size={14} /> Bloquear</button></div></header>
      <div className="content">{children}</div>
    </main>
  </div>;
}
