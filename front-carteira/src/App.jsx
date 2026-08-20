import { useState, useEffect } from 'react';
import Carteira from './components/Carteira';
import HomeBroker from './components/HomeBroker';
import NovaAcao from './components/NovaAcao';
import NovaCorretora from './components/NovaCorretora';

function App() {
    const [abaAtiva, setAbaAtiva] = useState('dashboard');

    const [temaEscuro, setTemaEscuro] = useState(() => {
        const salvo = localStorage.getItem('tema');
        return salvo ? salvo === 'escuro' : true;
    });

    useEffect(() => {
        localStorage.setItem('tema', temaEscuro ? 'escuro' : 'claro');
    }, [temaEscuro]);

    return (
        <div className={temaEscuro ? 'tema-escuro' : 'tema-claro'} style={{ display: 'flex', height: '100vh', width: '100%', overflow: 'hidden', backgroundColor: 'var(--bg-global)', color: 'var(--text-main)', transition: 'all 0.3s' }}>
            <style>
                {`
          @import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap');
          
          /* RESET PROFUNDO PARA ANULAR O VITE */
          * { font-family: 'Inter', sans-serif; box-sizing: border-box; }
          html, body { margin: 0; padding: 0; overflow-x: hidden; width: 100%; height: 100%; }
          #root { padding: 0 !important; margin: 0 !important; max-width: none !important; width: 100%; text-align: left !important; }
          
          .tema-claro {
            --bg-global: #f4f5f8;
            --bg-card: #ffffff;
            --bg-sidebar: #ffffff;
            --text-main: #111827;
            --text-muted: #6b7280;
            --border-color: #e5e7eb;
            --input-bg: #f9fafb;
            --input-border: #d1d5db;
            --hover-row: #f3f4f6;
            --primary: #10b981;
            --shadow-card: 0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px 0 rgba(0, 0, 0, 0.06);
          }
          .tema-escuro {
            --bg-global: #0f172a;
            --bg-card: #1e293b;
            --bg-sidebar: #0f172a;
            --text-main: #f8fafc;
            --text-muted: #94a3b8;
            --border-color: #334155;
            --input-bg: #1e293b;
            --input-border: #334155;
            --hover-row: #334155;
            --primary: #10b981;
            --shadow-card: 0 4px 6px -1px rgba(0, 0, 0, 0.5);
          }

          .menu-item { display: flex; align-items: center; gap: 12px; padding: 12px 16px; margin: 4px 12px; border-radius: 8px; color: var(--text-muted); cursor: pointer; font-weight: 500; transition: all 0.2s; border: none; background: transparent; width: calc(100% - 24px); text-align: left; font-size: 14px; }
          .menu-item:hover { background-color: var(--hover-row); color: var(--text-main); }
          .menu-item.ativo { background-color: rgba(99, 102, 241, 0.1); color: #6366f1; font-weight: 600; }
          
          .card-padrao { background: var(--bg-card); padding: 24px; border-radius: 12px; box-shadow: var(--shadow-card); border: 1px solid var(--border-color); }
          .input-moderno { width: 100%; padding: 12px 16px; border-radius: 8px; border: 1px solid var(--input-border); font-size: 14px; background-color: var(--input-bg); color: var(--text-main); outline: none; }
          .lbl-form { display: block; color: var(--text-muted); font-size: 13px; font-weight: 500; margin-bottom: 6px; }
        `}
            </style>

            {/* BARRA LATERAL (SIDEBAR) */}
            <aside style={{ width: '260px', minWidth: '260px', background: 'var(--bg-sidebar)', borderRight: '1px solid var(--border-color)', display: 'flex', flexDirection: 'column', padding: '20px 0', zIndex: 10 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '12px', padding: '0 24px', marginBottom: '30px' }}>
                    <div style={{ background: '#6366f1', color: 'white', padding: '6px 8px', borderRadius: '8px', fontWeight: 'bold', fontSize: '18px' }}>GA</div>
                    <h2 style={{ margin: 0, color: 'var(--text-main)', fontSize: '18px' }}>Gestão de Ativos</h2>
                </div>

                <div style={{ flex: 1 }}>
                    <p style={{ padding: '0 24px', fontSize: '11px', textTransform: 'uppercase', color: 'var(--text-muted)', fontWeight: '700', letterSpacing: '1px', marginBottom: '10px' }}>Principal</p>
                    <button className={`menu-item ${abaAtiva === 'dashboard' ? 'ativo' : ''}`} onClick={() => setAbaAtiva('dashboard')}><span>📊</span> Dashboard</button>
                    <button className={`menu-item ${abaAtiva === 'operacoes' ? 'ativo' : ''}`} onClick={() => setAbaAtiva('operacoes')}><span>⚡</span> Lançamentos</button>

                    <p style={{ padding: '0 24px', fontSize: '11px', textTransform: 'uppercase', color: 'var(--text-muted)', fontWeight: '700', letterSpacing: '1px', marginBottom: '10px', marginTop: '24px' }}>Cadastros</p>
                    <button className={`menu-item ${abaAtiva === 'acao' ? 'ativo' : ''}`} onClick={() => setAbaAtiva('acao')}><span>📈</span> Ações e Ativos</button>
                    <button className={`menu-item ${abaAtiva === 'corretora' ? 'ativo' : ''}`} onClick={() => setAbaAtiva('corretora')}><span>🏢</span> Corretoras</button>
                    <button className={`menu-item ${abaAtiva === 'investidor' ? 'ativo' : ''}`} onClick={() => setAbaAtiva('investidor')}><span>👥</span> Investidores</button>
                </div>

                <div style={{ padding: '0 12px' }}>
                    <button onClick={() => setTemaEscuro(!temaEscuro)} className="menu-item" style={{ justifyContent: 'center', border: '1px solid var(--border-color)' }}>
                        {temaEscuro ? '☀️ Mudar para Claro' : '🌙 Mudar para Escuro'}
                    </button>
                </div>
            </aside>

            {/* ÁREA CENTRAL DE CONTEÚDO */}
            <main style={{ flex: 1, overflowY: 'auto', overflowX: 'hidden', padding: '30px 40px' }}>
                {abaAtiva === 'dashboard' && <Carteira setAbaAtiva={setAbaAtiva} />}
                {abaAtiva === 'operacoes' && <HomeBroker />}
                {abaAtiva === 'acao' && <NovaAcao />}
                {abaAtiva === 'corretora' && <NovaCorretora />}
                {abaAtiva === 'investidor' && (
                    <div className="card-padrao">
                        <h3 style={{ margin: '0 0 20px 0' }}>Módulo de Investidores</h3>
                        <p style={{ color: 'var(--text-muted)' }}>Em breve: Tela para gerenciar os dados dos clientes da plataforma.</p>
                    </div>
                )}
            </main>
        </div>
    );
}

export default App;