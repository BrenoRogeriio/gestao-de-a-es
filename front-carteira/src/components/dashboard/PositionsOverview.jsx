import { ArrowRight } from 'lucide-react';
import Card from '../ui/Card.jsx';
import { formatarPercentualFinanceiro, formatarValorFinanceiro, tomFinanceiro } from '../../utils/dashboard.js';

export default function PositionsOverview({ posicoes, totalPosicoes, moeda, onViewAll, hasRealizedOnly = false }) {
    return (
        <Card className="dashboard-positions" aria-labelledby="positions-overview-title">
            <div className="dashboard-panel-header positions-header">
                <div>
                    <span className="section-eyebrow">Posições abertas</span>
                    <h2 id="positions-overview-title">Ativos em carteira</h2>
                    {totalPosicoes > 0 && <p>Exibindo {Math.min(totalPosicoes, 5)} de {totalPosicoes} posições em {moeda}.</p>}
                </div>
                <button className="button button-secondary" type="button" onClick={onViewAll}>
                    Ver carteira completa <ArrowRight size={17} aria-hidden="true" />
                </button>
            </div>

            {posicoes.length === 0 ? (
                <div className="dashboard-inline-state dashboard-inline-state-large">
                    <strong>Nenhuma posição aberta em {moeda}</strong>
                    <p>{hasRealizedOnly
                        ? 'Os resultados de operações encerradas continuam apresentados no resumo desta moeda.'
                        : 'As posições aparecerão aqui depois do primeiro lançamento de compra.'}</p>
                </div>
            ) : (
                <div className="table-scroll">
                    <table className="data-table dashboard-positions-table">
                        <thead>
                            <tr>
                                <th scope="col">Ativo</th><th scope="col">Quantidade</th><th scope="col">Preço médio</th><th scope="col">Cotação</th><th scope="col">Valor atual</th><th scope="col">Resultado</th><th scope="col">Rentabilidade</th>
                            </tr>
                        </thead>
                        <tbody>
                            {posicoes.map(posicao => {
                                const tone = tomFinanceiro(posicao.resultadoNaoRealizado);
                                return (
                                    <tr key={posicao.acaoId ?? `${posicao.ticker}-${posicao.mercado}`}>
                                        <td className="asset-cell"><strong>{posicao.ticker}</strong><small>{posicao.mercado} · {posicao.moeda}</small></td>
                                        <td className="number-cell">{posicao.quantidade ?? '—'}</td>
                                        <td className="number-cell">{formatarValorFinanceiro(posicao.precoMedio, moeda)}</td>
                                        <td className="number-cell">{formatarValorFinanceiro(posicao.cotacaoAtual, moeda)}</td>
                                        <td className="number-cell"><strong>{formatarValorFinanceiro(posicao.valorAtual, moeda)}</strong></td>
                                        <td className={`number-cell tone-${tone}`}>{formatarValorFinanceiro(posicao.resultadoNaoRealizado, moeda, true)}</td>
                                        <td><span className={`change-indicator change-${tone}`}><span className="sr-only">{tone === 'positive' ? 'Alta' : tone === 'negative' ? 'Queda' : 'Neutro ou indisponível'}: </span>{formatarPercentualFinanceiro(posicao.rentabilidadePercentual)}</span></td>
                                    </tr>
                                );
                            })}
                        </tbody>
                    </table>
                </div>
            )}
        </Card>
    );
}
