import { useState, useEffect } from 'react';
import Carteira from './components/Carteira';
import HomeBroker from './components/HomeBroker';
import NovaAcao from './components/NovaAcao';
import NovaCorretora from './components/NovaCorretora';

function App() {
  const [abaAtiva, setAbaAtiva] = useState('carteira');

  // Lógica do Dark Mode (salva a preferência do usuário)
  const [temaEscuro, setTemaEscuro] = useState(() => {
    const salvo = localStorage.getItem('tema');
    return salvo ? salvo === 'escuro' : true; // Começa escuro por padrão!
  });

  useEffect(() => {
    localStorage.setItem('tema', temaEscuro ? 'escuro' : 'claro');
  }, [temaEscuro]);

  return (
      <div className={temaEscuro ? 'tema-escuro' : 'tema-claro'} style={{ minHeight: '100vh', transition: 'all 0.3s ease', backgroundColor: 'var(--bg-global)', color: 'var(--text-main)' }}>
        <style>
          {`
          @import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap');
          * { font-family: 'Inter', sans-serif; box-sizing: border-box; }
          body { margin: 0; background-color: #020617; } /* Fundo fallback */
          
          /* PALETA DE CORES DINÂMICA */
          .tema-claro {
            --bg-global: #f8fafc;
            --bg-nav: linear-gradient(135deg, #0f172a 0%, #1e293b 100%);
            --bg-card: #ffffff;
            --text-main: #0f172a;
            --text-muted: #64748b;
            --border-color: #e2e8f0;
            --input-bg: #f8fafc;
            --input-border: #cbd5e1;
            --hover-row: #f1f5f9;
            --shadow-card: 0 4px 20px -2px rgba(0,0,0,0.05);
          }
          .tema-escuro {
            --bg-global: #020617; /* Preto bem moderno */
            --bg-nav: linear-gradient(135deg, #020617 0%, #0f172a 100%);
            --bg-card: #0f172a; /* Azul ultra escuro para os cards */
            --text-main: #f8fafc;
            --text-muted: #94a3b8;
            --border-color: #1e293b;
            --input-bg: #1e293b;
            --input-border: #334155;
            --hover-row: #1e293b;
            --shadow-card: 0 4px 20px -2px rgba(0,0,0,0.5);
          }

          /* CLASSES REUTILIZÁVEIS */
          .btn-nav { background: transparent; color: #94a3b8; border: none; padding: 10px 16px; border-radius: 8px; cursor: pointer; font-size: 15px; font-weight: 500; transition: all 0.2s; }
          .btn-nav:hover { background-color: rgba(255, 255, 255, 0.1); color: white; }
          .btn-nav.ativa { color: white; background: rgba(255, 255, 255, 0.1); font-weight: 600; }
          
          .card-padrao { background: var(--bg-card); padding: 24px; border-radius: 16px; box-shadow: var(--shadow-card); border: 1px solid var(--border-color); transition: all 0.3s; }
          .lbl-card { margin: 0; font-size: 14px; text-transform: uppercase; font-weight: 600; color: var(--text-muted); }
          .titulo-form { margin: 0 0 20px 0; color: var(--text-main); font-size: 20px; text-align: center; }
          .lbl-form { display: block; color: var(--text-muted); font-size: 13px; font-weight: 600; margin-bottom: 8px; }
          
          /* O Novo Input e Dropdown Bonitão */
          .input-moderno { width: 100%; padding: 14px 16px; border-radius: 10px; border: 1px solid var(--input-border); font-size: 15px; background-color: var(--input-bg); color: var(--text-main); outline: none; transition: all 0.2s; }
          .input-moderno:focus { border-color: #6366f1; box-shadow: 0 0 0 4px rgba(99, 102, 241, 0.15); }
          
          select.input-moderno { appearance: none; cursor: pointer; background-image: url("data:image/svg+xml;charset=US-ASCII,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20width%3D%2224%22%20height%3D%2224%22%20fill%3D%22none%22%20stroke%3D%22%236366f1%22%20stroke-width%3D%222%22%20stroke-linecap%3D%22round%22%20stroke-linejoin%3D%22round%22%3E%3Cpolyline%20points%3D%226%209%2012%2015%2018%209%22%3E%3C%2Fpolyline%3E%3C%2Fsvg%3E"); background-repeat: no-repeat; background-position: right 14px center; background-size: 16px; padding-right: 40px; }
          select.input-moderno option { background-color: var(--bg-card); color: var(--text-main); }

          .btn-submit { width: 100%; padding: 16px; color: white; border: none; border-radius: 10px; font-weight: 600; font-size: 16px; cursor: pointer; transition: all 0.2s; margin-top: 10px; }
          .btn-submit:hover { filter: brightness(1.1); transform: translateY(-2px); }
          
          .btn-pill { flex: 1; padding: 12px; border: none; font-weight: 600; border-radius: 8px; cursor: pointer; transition: all 0.2s; }
          .btn-pill.inativo { background: transparent; color: var(--text-muted); }
          .btn-pill.compra { background: var(--bg-card); color: #10b981; box-shadow: 0 2px 8px rgba(0,0,0,0.1); border: 1px solid var(--border-color); }
          .btn-pill.venda { background: var(--bg-card); color: #ef4444; box-shadow: 0 2px 8px rgba(0,0,0,0.1); border: 1px solid var(--border-color); }
          
          table th { border-bottom: 2px solid var(--border-color); color: var(--text-muted); font-size: 13px; text-transform: uppercase; padding: 12px; }
          table td { padding: 16px 12px; border-bottom: 1px solid var(--border-color); color: var(--text-main); }
          .tr-hover:hover { background-color: var(--hover-row); }
        `}
        </style>

        <nav style={{ background: 'var(--bg-nav)', padding: '16px 50px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '15px', borderBottom: '1px solid var(--border-color)' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <div style={{ background: 'linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%)', color: 'white', padding: '8px 10px', borderRadius: '10px', fontWeight: 'bold', boxShadow: '0 2px 10px rgba(99, 102, 241, 0.4)' }}>GA</div>
            <h2 style={{ margin: 0, color: 'white', fontSize: '20px', letterSpacing: '-0.5px' }}>Gestão de Ativos</h2>
          </div>

          <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap', alignItems: 'center' }}>
            <button className={`btn-nav ${abaAtiva === 'carteira' ? 'ativa' : ''}`} onClick={() => setAbaAtiva('carteira')}>📊 Carteira</button>
            <button className={`btn-nav ${abaAtiva === 'operacoes' ? 'ativa' : ''}`} onClick={() => setAbaAtiva('operacoes')}>⚡ Home Broker</button>
            <button className={`btn-nav ${abaAtiva === 'acao' ? 'ativa' : ''}`} onClick={() => setAbaAtiva('acao')}>📈 Nova Ação</button>
            <button className={`btn-nav ${abaAtiva === 'corretora' ? 'ativa' : ''}`} onClick={() => setAbaAtiva('corretora')}>🏢 Nova Corretora</button>

            {/* BOTÃO MÁGICO DO TEMA */}
            <button onClick={() => setTemaEscuro(!temaEscuro)} style={{ background: 'var(--bg-card)', border: '1px solid var(--border-color)', color: 'var(--text-main)', padding: '8px 12px', borderRadius: '20px', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '8px', marginLeft: '15px', fontWeight: 'bold', transition: 'all 0.3s' }}>
              {temaEscuro ? '☀️ Claro' : '🌙 Escuro'}
            </button>
          </div>
        </nav>

        <main style={{ maxWidth: '1200px', margin: '40px auto', padding: '0 20px' }}>
          {abaAtiva === 'carteira' && <Carteira />}
          {abaAtiva === 'operacoes' && <HomeBroker />}
          {abaAtiva === 'acao' && <NovaAcao />}
          {abaAtiva === 'corretora' && <NovaCorretora />}
        </main>
      </div>
  );
}

export default App;