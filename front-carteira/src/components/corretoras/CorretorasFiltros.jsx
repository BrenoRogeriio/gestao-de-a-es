import { Search } from 'lucide-react';
import {
    ORDENACOES_CORRETORAS,
    SITUACAO_ATIVAS,
    SITUACAO_OUTRAS,
    SITUACAO_TODAS
} from '../../utils/corretoras.js';

const SITUACOES = [
    { value: SITUACAO_TODAS, label: 'Todas' },
    { value: SITUACAO_ATIVAS, label: 'Ativas' },
    { value: SITUACAO_OUTRAS, label: 'Outras' }
];

export default function CorretorasFiltros({ filtros, onChange, onClear }) {
    const alterar = (campo, valor) => onChange({ ...filtros, [campo]: valor });
    return (
        <section className="brokers-filters" aria-label="Filtros de corretoras">
            <div className="brokers-status-filter" role="group" aria-label="Filtrar por situação cadastral">
                {SITUACOES.map(opcao => (
                    <button className={`brokers-status-button ${filtros.situacao === opcao.value ? 'is-active' : ''}`} type="button" key={opcao.value} aria-pressed={filtros.situacao === opcao.value} onClick={() => alterar('situacao', opcao.value)}>{opcao.label}</button>
                ))}
            </div>
            <div className="brokers-filter-tools">
                <label className="brokers-search">
                    <span className="sr-only">Buscar por nome ou CNPJ</span>
                    <Search size={16} aria-hidden="true" />
                    <input type="search" value={filtros.busca} placeholder="Buscar nome ou CNPJ" onChange={evento => alterar('busca', evento.target.value)} />
                </label>
                <label className="brokers-sort">
                    <span className="sr-only">Ordenar corretoras</span>
                    <select value={filtros.ordenacao} onChange={evento => alterar('ordenacao', evento.target.value)}>
                        {ORDENACOES_CORRETORAS.map(opcao => <option value={opcao.value} key={opcao.value}>{opcao.label}</option>)}
                    </select>
                </label>
                <button className="button button-secondary brokers-clear" type="button" onClick={onClear}>Limpar filtros</button>
            </div>
        </section>
    );
}
