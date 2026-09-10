import { ArrowDownToLine, ArrowUpFromLine } from 'lucide-react';
import { formatarPreco, formatarQuantidade, rotuloMercado } from '../../utils/carteira.js';
import { formatarPercentualFinanceiro, formatarValorFinanceiro, tomFinanceiro, valorDisponivel } from '../../utils/dashboard.js';
import { formatarDataOperacao } from '../../utils/financeiro.js';
import { dataDaOperacao, formatarCorretora } from '../../utils/historico.js';
import Badge from '../ui/Badge.jsx';
import Card from '../ui/Card.jsx';

function TipoOperacao({ tipo }) {
    const compra = tipo === 'COMPRA';
    const Icon = compra ? ArrowDownToLine : ArrowUpFromLine;
    return <Badge tone={compra ? 'positive' : 'negative'}><Icon size={13} aria-hidden="true" /> {tipo}</Badge>;
}

function ResultadoOperacao({ operacao, compacto = false }) {
    const venda = operacao.tipo === 'VENDA';
    const disponivel = venda && valorDisponivel(operacao.resultadoRealizado);
    const tone = disponivel ? tomFinanceiro(operacao.resultadoRealizado) : 'neutral';

    if (!disponivel) {
        return <span className="history-unavailable" title={venda ? 'Resultado não disponível' : 'Compras não possuem resultado realizado'}>—</span>;
    }

    return (
        <div className={`history-result ${compacto ? 'history-result-compact' : ''}`}>
            <strong className={`tone-${tone}`}>
                <span className="sr-only">{tone === 'positive' ? 'Resultado positivo: ' : tone === 'negative' ? 'Resultado negativo: ' : 'Resultado neutro: '}</span>
                {formatarValorFinanceiro(operacao.resultadoRealizado, operacao.moeda, true)}
            </strong>
            <span className={`tone-${tomFinanceiro(operacao.rentabilidadeRealizada)}`}>
                {formatarPercentualFinanceiro(operacao.rentabilidadeRealizada)}
            </span>
        </div>
    );
}

function PrecoMedio({ operacao }) {
    if (operacao.tipo !== 'VENDA' || !valorDisponivel(operacao.precoMedioOperacao)) return <span className="history-unavailable">—</span>;
    return formatarPreco(operacao.precoMedioOperacao, operacao.moeda);
}

function Ativo({ operacao }) {
    return (
        <div className="asset-cell">
            <strong>{operacao.ticker}</strong>
            <small>{rotuloMercado(operacao.mercado)} · {operacao.moeda}</small>
            {operacao.corretoraCnpj && <small title="Corretora da operação">Corretora {formatarCorretora(operacao.corretoraCnpj)}</small>}
        </div>
    );
}

export default function HistoricoLista({ operacoes, resumo }) {
    return (
        <Card className="history-list" aria-labelledby="history-list-title">
            <div className="history-list-header">
                <div>
                    <span className="section-eyebrow">Movimentações</span>
                    <h2 id="history-list-title">Operações registradas</h2>
                </div>
                <p aria-live="polite">{resumo}</p>
            </div>

            <div className="history-table-wrap">
                <table className="data-table history-table">
                    <thead>
                        <tr>
                            <th scope="col">Data</th><th scope="col">Tipo</th><th scope="col">Ativo</th><th scope="col">Quantidade</th><th scope="col">Valor unitário</th><th scope="col">Preço médio usado</th><th scope="col">Resultado realizado</th>
                        </tr>
                    </thead>
                    <tbody>
                        {operacoes.map((operacao, indice) => (
                            <tr key={`${operacao.acaoId}-${operacao.data ?? operacao.dataOperacao}-${operacao.tipo}-${indice}`}>
                                <td className="number-cell">{formatarDataOperacao(dataDaOperacao(operacao))}</td>
                                <td><TipoOperacao tipo={operacao.tipo} /></td>
                                <td><Ativo operacao={operacao} /></td>
                                <td className="number-cell">{formatarQuantidade(operacao.quantidade)}</td>
                                <td className="number-cell">{formatarPreco(operacao.valorUnitario, operacao.moeda)}</td>
                                <td className="number-cell"><PrecoMedio operacao={operacao} /></td>
                                <td><ResultadoOperacao operacao={operacao} /></td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>

            <div className="history-mobile-list">
                {operacoes.map((operacao, indice) => (
                    <article className={`history-operation-card history-operation-${operacao.tipo?.toLowerCase()}`} key={`${operacao.acaoId}-${operacao.data ?? operacao.dataOperacao}-${operacao.tipo}-${indice}`}>
                        <header>
                            <div><TipoOperacao tipo={operacao.tipo} /><Ativo operacao={operacao} /></div>
                            <time dateTime={dataDaOperacao(operacao)}>{formatarDataOperacao(dataDaOperacao(operacao))}</time>
                        </header>
                        <dl>
                            <div><dt>Quantidade</dt><dd>{formatarQuantidade(operacao.quantidade)}</dd></div>
                            <div><dt>Valor unitário</dt><dd>{formatarPreco(operacao.valorUnitario, operacao.moeda)}</dd></div>
                            <div><dt>Preço médio usado</dt><dd><PrecoMedio operacao={operacao} /></dd></div>
                        </dl>
                        <div className="history-mobile-result">
                            <span>Resultado realizado</span>
                            <ResultadoOperacao operacao={operacao} compacto />
                        </div>
                    </article>
                ))}
            </div>
        </Card>
    );
}
