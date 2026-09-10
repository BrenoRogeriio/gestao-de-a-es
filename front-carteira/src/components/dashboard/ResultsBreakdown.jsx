import Card from '../ui/Card.jsx';
import { formatarValorFinanceiro, tomFinanceiro, valorDisponivel } from '../../utils/dashboard.js';

function ResultRow({ label, value, currency, total = false }) {
    const tone = tomFinanceiro(value);
    const formatted = formatarValorFinanceiro(value, currency, true);
    const unavailable = !valorDisponivel(value);
    return (
        <div className={`result-row ${total ? 'result-row-total' : ''}`}>
            <span>{label}</span>
            <strong className={`tone-${tone}`} title={unavailable ? 'Valor não disponível porque uma ou mais cotações estão ausentes.' : undefined}>
                <span className="sr-only">{unavailable ? 'Valor indisponível' : tone === 'positive' ? 'Positivo: ' : tone === 'negative' ? 'Negativo: ' : 'Neutro: '}</span>
                {formatted}
            </strong>
        </div>
    );
}

export default function ResultsBreakdown({ resumo, moeda }) {
    return (
        <Card className="dashboard-panel" aria-labelledby="results-title">
            <div className="dashboard-panel-header">
                <div><span className="section-eyebrow">Desempenho</span><h2 id="results-title">Resultado da carteira</h2></div>
            </div>
            <div className="results-list">
                <ResultRow label="Não realizado" value={resumo?.resultadoNaoRealizadoTotal} currency={moeda} />
                <ResultRow label="Realizado" value={resumo?.resultadoRealizadoTotal} currency={moeda} />
                <ResultRow label="Resultado total" value={resumo?.resultadoTotal} currency={moeda} total />
            </div>
        </Card>
    );
}
