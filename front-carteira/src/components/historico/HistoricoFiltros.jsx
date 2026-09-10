import { Search } from 'lucide-react';
import { FILTRO_TODOS } from '../../utils/historico.js';

const TIPOS = [
    { value: FILTRO_TODOS, label: 'Todos' },
    { value: 'COMPRA', label: 'Compras' },
    { value: 'VENDA', label: 'Vendas' }
];

export default function HistoricoFiltros({ filtros, moedas, onChange, onClear }) {
    const alterar = (campo, valor) => onChange({ ...filtros, [campo]: valor });

    return (
        <section className="history-filters" aria-label="Filtros do histórico">
            <div className="history-filter-types" role="group" aria-label="Filtrar por tipo de operação">
                {TIPOS.map(opcao => (
                    <button
                        className={`history-type-button ${filtros.tipo === opcao.value ? 'is-active' : ''}`}
                        type="button"
                        key={opcao.value}
                        aria-pressed={filtros.tipo === opcao.value}
                        onClick={() => alterar('tipo', opcao.value)}
                    >
                        {opcao.label}
                    </button>
                ))}
            </div>

            <div className="history-filter-grid">
                <label className="history-search">
                    <span>Buscar ticker</span>
                    <span className="history-input-with-icon">
                        <Search size={16} aria-hidden="true" />
                        <input type="search" value={filtros.busca} placeholder="Ex.: WEGE3" onChange={evento => alterar('busca', evento.target.value)} />
                    </span>
                </label>
                <label>
                    <span>Moeda</span>
                    <select value={filtros.moeda} onChange={evento => alterar('moeda', evento.target.value)}>
                        <option value={FILTRO_TODOS}>Todas</option>
                        {moedas.map(moeda => <option value={moeda} key={moeda}>{moeda}</option>)}
                    </select>
                </label>
                <label>
                    <span>Data inicial</span>
                    <input type="date" value={filtros.dataInicial} onChange={evento => alterar('dataInicial', evento.target.value)} />
                </label>
                <label>
                    <span>Data final</span>
                    <input type="date" value={filtros.dataFinal} onChange={evento => alterar('dataFinal', evento.target.value)} />
                </label>
                <button className="button button-secondary history-clear" type="button" onClick={onClear}>Limpar filtros</button>
            </div>
        </section>
    );
}
