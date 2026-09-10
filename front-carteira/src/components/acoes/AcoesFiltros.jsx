import { Search } from 'lucide-react';
import { MERCADOS, MERCADO_TODOS, ORDENACOES_ACOES } from '../../utils/acoes.js';

export default function AcoesFiltros({ filtros, onChange, onClear }) {
    const alterar = (campo, valor) => onChange({ ...filtros, [campo]: valor });

    return (
        <section className="assets-filters" aria-label="Filtros de ações">
            <div className="assets-market-filter" role="group" aria-label="Filtrar por mercado">
                {[{ value: MERCADO_TODOS, label: 'Todos' }, ...MERCADOS].map(opcao => (
                    <button
                        className={`assets-market-button ${filtros.mercado === opcao.value ? 'is-active' : ''}`}
                        type="button"
                        key={opcao.value}
                        aria-pressed={filtros.mercado === opcao.value}
                        onClick={() => alterar('mercado', opcao.value)}
                    >
                        {opcao.label}
                    </button>
                ))}
            </div>
            <div className="assets-filter-tools">
                <label className="assets-search">
                    <span className="sr-only">Buscar por ticker ou nome da empresa</span>
                    <Search size={16} aria-hidden="true" />
                    <input type="search" value={filtros.busca} placeholder="Buscar ticker ou empresa" onChange={evento => alterar('busca', evento.target.value)} />
                </label>
                <label className="assets-sort">
                    <span className="sr-only">Ordenar ações</span>
                    <select value={filtros.ordenacao} onChange={evento => alterar('ordenacao', evento.target.value)}>
                        {ORDENACOES_ACOES.map(opcao => <option value={opcao.value} key={opcao.value}>{opcao.label}</option>)}
                    </select>
                </label>
                <button className="button button-secondary assets-clear" type="button" onClick={onClear}>Limpar filtros</button>
            </div>
        </section>
    );
}
