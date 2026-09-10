import { useEffect, useState } from 'react';
import { consultarHistorico } from '../../services/carteira.js';
import { criarModeloHistorico, FILTRO_TODOS, resumoOperacoes } from '../../utils/historico.js';
import EmptyState from '../ui/EmptyState.jsx';
import ErrorState from '../ui/ErrorState.jsx';
import LoadingState from '../ui/LoadingState.jsx';
import PageHeader from '../ui/PageHeader.jsx';
import HistoricoFiltros from './HistoricoFiltros.jsx';
import HistoricoLista from './HistoricoLista.jsx';

const FILTROS_INICIAIS = {
    tipo: FILTRO_TODOS,
    moeda: FILTRO_TODOS,
    busca: '',
    dataInicial: '',
    dataFinal: ''
};

export default function HistoricoOperacoes() {
    const [historico, setHistorico] = useState([]);
    const [filtros, setFiltros] = useState(FILTROS_INICIAIS);
    const [carregando, setCarregando] = useState(true);
    const [erro, setErro] = useState(false);

    const carregar = async signal => {
        setCarregando(true);
        try {
            setHistorico(await consultarHistorico(signal));
            setErro(false);
        } catch (falha) {
            if (falha.name !== 'AbortError') {
                console.error(falha);
                setErro(true);
            }
        } finally {
            if (!signal?.aborted) setCarregando(false);
        }
    };

    useEffect(() => {
        const controller = new AbortController();
        consultarHistorico(controller.signal)
            .then(dados => {
                setHistorico(dados);
                setErro(false);
            })
            .catch(falha => {
                if (falha.name !== 'AbortError') {
                    console.error(falha);
                    setErro(true);
                }
            })
            .finally(() => {
                if (!controller.signal.aborted) setCarregando(false);
            });
        return () => controller.abort();
    }, []);

    const modelo = criarModeloHistorico({ loading: carregando, error: erro, historico, ...filtros });

    if (modelo.estado === 'loading') {
        return <LoadingState title="Carregando o histórico" description="Buscando suas compras e vendas mais recentes." />;
    }

    if (modelo.estado === 'error') {
        return <ErrorState title="Não foi possível carregar o histórico" description="Verifique a conexão com o servidor e tente novamente." onRetry={() => carregar()} />;
    }

    if (modelo.estado === 'empty') {
        return <EmptyState title="Histórico vazio" description="Compras e vendas registradas aparecerão aqui." />;
    }

    return (
        <div className="history-page">
            <PageHeader title="Histórico de operações" description="Consulte compras, vendas e resultados realizados sem misturar moedas." />
            <HistoricoFiltros filtros={filtros} moedas={modelo.moedas} onChange={setFiltros} onClear={() => setFiltros(FILTROS_INICIAIS)} />
            {modelo.estado === 'filtered-empty' ? (
                <EmptyState
                    title="Nenhuma operação encontrada"
                    description="Revise o tipo, a moeda, o ticker ou o período selecionado."
                    action={<button className="button button-secondary" type="button" onClick={() => setFiltros(FILTROS_INICIAIS)}>Limpar filtros</button>}
                />
            ) : (
                <HistoricoLista operacoes={modelo.operacoes} resumo={resumoOperacoes(modelo.total, filtros.tipo)} />
            )}
        </div>
    );
}
