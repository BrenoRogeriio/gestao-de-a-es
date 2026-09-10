import StatCard from '../ui/StatCard.jsx';
import {
    formatarPercentualFinanceiro,
    formatarValorFinanceiro,
    tomFinanceiro,
    valorDisponivel
} from '../../utils/dashboard.js';

function valorComAcessibilidade(valor, moeda, comSinal = false) {
    const formatado = formatarValorFinanceiro(valor, moeda, comSinal);
    const tone = tomFinanceiro(valor);
    const situacao = !valorDisponivel(valor) ? 'indisponível' : tone === 'positive' ? 'positivo' : tone === 'negative' ? 'negativo' : 'neutro';
    return <span className={`dashboard-number tone-${tone}`} aria-label={`${formatado}, valor ${situacao}`}>{formatado}</span>;
}

export default function DashboardMetrics({ resumo, moeda }) {
    const resultadoTone = tomFinanceiro(resumo?.resultadoTotal);
    const rentabilidadeTone = tomFinanceiro(resumo?.rentabilidadePercentual);

    return (
        <section aria-labelledby="dashboard-metrics-title">
            <h2 className="sr-only" id="dashboard-metrics-title">Indicadores principais</h2>
            <div className="dashboard-metrics-grid">
                <StatCard
                    label="Valor investido"
                    value={valorComAcessibilidade(resumo?.valorInvestidoTotal, moeda)}
                    helper="Custo atual das posições abertas."
                    showIndicator={false}
                />
                <StatCard
                    label="Valor atual"
                    value={valorComAcessibilidade(resumo?.valorAtualTotal, moeda)}
                    helper={valorDisponivel(resumo?.valorAtualTotal) ? 'Valor das posições pelas cotações disponíveis.' : 'Indisponível porque há cotação ausente.'}
                    showIndicator={false}
                />
                <StatCard
                    label="Resultado total"
                    tone={resultadoTone}
                    statusLabel={`Resultado ${resultadoTone === 'positive' ? 'positivo' : resultadoTone === 'negative' ? 'negativo' : 'neutro ou indisponível'}`}
                    value={valorComAcessibilidade(resumo?.resultadoTotal, moeda, true)}
                    helper="Soma dos resultados realizado e não realizado."
                />
                <StatCard
                    label="Rentabilidade da posição"
                    tone={rentabilidadeTone}
                    statusLabel={`Rentabilidade ${rentabilidadeTone === 'positive' ? 'positiva' : rentabilidadeTone === 'negative' ? 'negativa' : 'neutra ou indisponível'}`}
                    value={<span className={`dashboard-number tone-${rentabilidadeTone}`}>{formatarPercentualFinanceiro(resumo?.rentabilidadePercentual)}</span>}
                    helper="Resultado não realizado dividido pelo valor investido."
                />
            </div>
        </section>
    );
}
