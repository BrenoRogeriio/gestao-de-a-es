import Card from '../ui/Card.jsx';
import { formatarValorFinanceiro } from '../../utils/dashboard.js';

export default function PortfolioDistribution({ distribuicao, moeda }) {
    return (
        <Card className="dashboard-panel" aria-labelledby="distribution-title">
            <div className="dashboard-panel-header">
                <div><span className="section-eyebrow">Composição atual</span><h2 id="distribution-title">Distribuição por ativo</h2></div>
            </div>

            {distribuicao.estado === 'ready' ? (
                <div className="distribution-list">
                    {distribuicao.itens.map(item => (
                        <div className="distribution-item" key={item.ticker}>
                            <div className="distribution-label">
                                <strong>{item.ticker}</strong>
                                <span>{item.percentual.toFixed(1).replace('.', ',')}% · {formatarValorFinanceiro(item.valorAtual, moeda)}</span>
                            </div>
                            <div className="distribution-track" aria-hidden="true">
                                <span style={{ width: `${item.percentual}%` }} />
                            </div>
                        </div>
                    ))}
                </div>
            ) : (
                <div className="dashboard-inline-state">
                    <strong>{distribuicao.estado === 'unavailable' ? 'Distribuição indisponível' : 'Sem ativos para distribuir'}</strong>
                    <p>{distribuicao.estado === 'unavailable'
                        ? 'Uma ou mais posições estão sem cotação atual. Nenhum percentual parcial foi exibido.'
                        : 'A distribuição aparecerá quando houver posições com valor atual.'}</p>
                </div>
            )}
        </Card>
    );
}
