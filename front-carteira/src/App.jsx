import { useEffect, useState } from 'react';
import AppLayout from './components/layout/AppLayout.jsx';
import HomeBroker from './components/HomeBroker.jsx';
import Corretoras from './components/corretoras/Corretoras.jsx';
import ContentState from './components/ui/ContentState.jsx';
import Dashboard from './components/dashboard/Dashboard.jsx';
import MinhaCarteira from './components/carteira/MinhaCarteira.jsx';
import HistoricoOperacoes from './components/historico/HistoricoOperacoes.jsx';
import Acoes from './components/acoes/Acoes.jsx';

const PAGINAS = {
    dashboard: { eyebrow: 'Visão geral', titulo: 'Dashboard', descricao: 'Visão geral da sua carteira de investimentos.' },
    carteira: { eyebrow: 'Investimentos', titulo: 'Minha carteira', descricao: 'Consulte suas posições abertas e os principais indicadores.' },
    acoes: { eyebrow: 'Cadastros', titulo: 'Ações', descricao: 'Inclua ativos brasileiros e internacionais na sua base.' },
    corretoras: { eyebrow: 'Cadastros', titulo: 'Corretoras', descricao: 'Mantenha as instituições utilizadas nas suas operações.' },
    historico: { eyebrow: 'Movimentações', titulo: 'Histórico', descricao: 'Revise compras e vendas registradas na carteira.' },
    operacoes: { eyebrow: 'Movimentações', titulo: 'Lançamentos', descricao: 'Registre compras e vendas executadas na sua carteira.' },
    investidores: { eyebrow: 'Em preparação', titulo: 'Investidores', descricao: 'Espaço reservado para uma evolução futura do sistema.' }
};

function App() {
    const [paginaAtiva, setPaginaAtiva] = useState('dashboard');
    const [temaEscuro, setTemaEscuro] = useState(() => localStorage.getItem('tema') === 'escuro');

    useEffect(() => {
        localStorage.setItem('tema', temaEscuro ? 'escuro' : 'claro');
    }, [temaEscuro]);

    const renderizarPagina = () => {
        switch (paginaAtiva) {
            case 'dashboard':
                return <Dashboard onNavigate={setPaginaAtiva} />;
            case 'carteira':
                return <MinhaCarteira onNavigate={setPaginaAtiva} />;
            case 'historico':
                return <HistoricoOperacoes />;
            case 'operacoes':
                return <HomeBroker />;
            case 'acoes':
                return <Acoes />;
            case 'corretoras':
                return <Corretoras />;
            case 'investidores':
                return <ContentState title="Módulo de investidores" description="Esta área continua reservada para uma etapa futura. Nenhuma autenticação foi implementada." />;
            default:
                return null;
        }
    };

    return (
        <div className={temaEscuro ? 'theme-dark' : 'theme-light'}>
            <AppLayout
                activePage={paginaAtiva}
                onNavigate={setPaginaAtiva}
                page={PAGINAS[paginaAtiva]}
                darkMode={temaEscuro}
                onToggleTheme={() => setTemaEscuro(tema => !tema)}
            >
                {renderizarPagina()}
            </AppLayout>
        </div>
    );
}

export default App;
