import { useEffect, useRef, useState } from 'react';
import { Check, CheckCircle2, Info, LoaderCircle, RotateCcw, ShoppingCart, TrendingDown, X } from 'lucide-react';
import { consultarAcoes } from '../services/acoes.js';
import { consultarCarteira, registrarOperacao } from '../services/carteira.js';
import { consultarCorretoras } from '../services/corretoras.js';
import { gerarChaveIdempotencia, obterTentativaIdempotente, prepararOperacao } from '../utils/carteira.js';
import { rotuloOpcaoCorretora } from '../utils/corretoras.js';
import { formatarMoeda, simboloMoeda } from '../utils/financeiro.js';
import {
    criarFormularioLancamento,
    criarModeloLancamentos,
    descricaoSaldoVenda,
    limparLancamentoConcluido,
    obterQuantidadeDisponivel,
    rotuloOpcaoAcao,
    selecionarAcao,
    validarLancamento
} from '../utils/lancamentos.js';
import Card from './ui/Card.jsx';
import ErrorState from './ui/ErrorState.jsx';
import LoadingState from './ui/LoadingState.jsx';
import PageHeader from './ui/PageHeader.jsx';

async function consultarDadosLancamento(signal) {
    const [resultadoAcoes, resultadoCorretoras, resultadoCarteira] = await Promise.all([
        consultarAcoes(signal),
        consultarCorretoras(signal),
        consultarCarteira(signal)
    ]);
    return {
        acoes: resultadoAcoes.acoes,
        corretoras: resultadoCorretoras.corretoras,
        posicoes: resultadoCarteira.posicoes
    };
}

export default function HomeBroker() {
    const [dados, setDados] = useState({ acoes: [], corretoras: [], posicoes: [] });
    const [form, setForm] = useState(() => criarFormularioLancamento());
    const [carregando, setCarregando] = useState(true);
    const [erroCarga, setErroCarga] = useState(false);
    const [enviando, setEnviando] = useState(false);
    const [erros, setErros] = useState({});
    const [feedback, setFeedback] = useState(null);
    const tentativaRef = useRef(null);
    const enviandoRef = useRef(false);
    const ativoRef = useRef(null);

    const carregar = async signal => {
        setCarregando(true);
        setErroCarga(false);
        try {
            setDados(await consultarDadosLancamento(signal));
        } catch (falha) {
            if (falha.name !== 'AbortError') setErroCarga(true);
        } finally {
            if (!signal?.aborted) setCarregando(false);
        }
    };

    useEffect(() => {
        const controller = new AbortController();
        consultarDadosLancamento(controller.signal)
            .then(resultado => {
                setDados(resultado);
                setErroCarga(false);
            })
            .catch(falha => {
                if (falha.name !== 'AbortError') setErroCarga(true);
            })
            .finally(() => {
                if (!controller.signal.aborted) setCarregando(false);
            });
        return () => controller.abort();
    }, []);

    const modelo = criarModeloLancamentos({ loading: carregando, error: erroCarga, ...dados });

    const atualizarCampo = (campo, valor) => {
        setForm(atual => ({ ...atual, [campo]: valor }));
        setErros(atuais => ({ ...atuais, [campo]: undefined }));
        setFeedback(null);
        tentativaRef.current = null;
    };

    const alterarTipo = tipo => {
        setForm(atual => ({ ...atual, tipo }));
        setErros({});
        setFeedback(null);
        tentativaRef.current = null;
    };

    const alterarAcao = evento => {
        const acao = dados.acoes.find(item => String(item.id) === evento.target.value);
        setForm(atual => selecionarAcao(atual, acao));
        setErros(atuais => ({ ...atuais, acaoId: undefined, preco: undefined, quantidade: undefined }));
        setFeedback(null);
        tentativaRef.current = null;
    };

    const limpar = () => {
        setForm(atual => criarFormularioLancamento({ tipo: atual.tipo }));
        setErros({});
        setFeedback(null);
        tentativaRef.current = null;
        ativoRef.current?.focus();
    };

    const enviar = async evento => {
        evento.preventDefault();
        if (enviandoRef.current) return;
        const validacao = validarLancamento(form, dados.posicoes);
        setErros(validacao);
        if (Object.keys(validacao).length > 0) return;

        const payload = prepararOperacao(form);
        const tentativa = obterTentativaIdempotente(tentativaRef.current, payload, gerarChaveIdempotencia);
        tentativaRef.current = tentativa;
        enviandoRef.current = true;
        setEnviando(true);
        setFeedback(null);
        const acao = dados.acoes.find(item => String(item.id) === form.acaoId);

        try {
            await registrarOperacao(form.tipo, payload, tentativa.chave);
            setFeedback({ tone: 'success', message: `${form.tipo === 'VENDA' ? 'Venda' : 'Compra'} de ${acao?.ticker ?? 'ativo'} registrada com sucesso.` });
            setForm(atual => limparLancamentoConcluido(atual));
            setErros({});
            tentativaRef.current = null;
            try {
                const carteira = await consultarCarteira();
                setDados(atuais => ({ ...atuais, posicoes: carteira.posicoes }));
            } catch {
                setFeedback({ tone: 'warning', message: 'Lançamento registrado, mas não foi possível atualizar as posições agora.' });
            }
            window.setTimeout(() => ativoRef.current?.focus(), 0);
        } catch (falha) {
            setFeedback({ tone: 'error', message: falha.message || 'Não foi possível registrar a operação.' });
        } finally {
            enviandoRef.current = false;
            setEnviando(false);
        }
    };

    if (modelo.estado === 'loading') return <LoadingState title="Carregando lançamentos" description="Buscando ativos, corretoras e posições disponíveis." />;
    if (modelo.estado === 'error') return <ErrorState title="Não foi possível preparar o lançamento" description="Verifique a conexão com o servidor e tente novamente." onRetry={() => carregar()} />;

    const saldoVenda = descricaoSaldoVenda(form, dados.posicoes);
    const quantidadeDisponivel = obterQuantidadeDisponivel(dados.posicoes, form.acaoId);
    const valorTotal = Number(form.quantidade) * Number(String(form.preco).replace(',', '.'));
    const semAcoes = dados.acoes.length === 0;
    const semCorretoras = dados.corretoras.length === 0;

    return (
        <div className="launch-page">
            <PageHeader title="Registrar lançamento" description="Registre compras e vendas executadas na sua carteira." />

            {(semAcoes || semCorretoras) && (
                <div className="inline-feedback feedback-warning" role="status">
                    <Info size={18} aria-hidden="true" />
                    <span>{semAcoes ? 'Cadastre ao menos um ativo antes de registrar uma operação.' : 'Cadastre ao menos uma corretora antes de registrar uma operação.'}</span>
                </div>
            )}

            {feedback && (
                <div className={`inline-feedback feedback-${feedback.tone}`} role={feedback.tone === 'success' ? 'status' : 'alert'}>
                    {feedback.tone === 'success' && <CheckCircle2 size={18} aria-hidden="true" />}
                    <span>{feedback.message}</span>
                    <button type="button" onClick={() => setFeedback(null)} aria-label="Fechar mensagem"><X size={16} aria-hidden="true" /></button>
                </div>
            )}

            <Card className="launch-card">
                <header className="launch-card-header">
                    <div>
                        <span className="section-eyebrow">Nova movimentação</span>
                        <h2>Dados da operação</h2>
                        <p>Informe os dados da ordem executada pela instituição financeira.</p>
                    </div>
                    <span className={`launch-kind-badge is-${form.tipo.toLowerCase()}`}>{form.tipo === 'VENDA' ? 'Venda' : 'Compra'}</span>
                </header>

                <form className="launch-form" onSubmit={enviar} noValidate>
                    <div className="segmented-control launch-type" role="group" aria-label="Tipo de operação">
                        <button className={`segmented-button is-buy ${form.tipo === 'COMPRA' ? 'is-active' : ''}`} type="button" aria-pressed={form.tipo === 'COMPRA'} disabled={enviando} onClick={() => alterarTipo('COMPRA')}><ShoppingCart size={17} aria-hidden="true" /> Comprar</button>
                        <button className={`segmented-button is-sell ${form.tipo === 'VENDA' ? 'is-active' : ''}`} type="button" aria-pressed={form.tipo === 'VENDA'} disabled={enviando} onClick={() => alterarTipo('VENDA')}><TrendingDown size={17} aria-hidden="true" /> Vender</button>
                    </div>

                    <div className="launch-form-grid">
                        <div className="form-field-full">
                            <label className="lbl-form" htmlFor="launch-asset">Ativo</label>
                            <select ref={ativoRef} className="input-moderno" id="launch-asset" value={form.acaoId} disabled={enviando || semAcoes} aria-invalid={Boolean(erros.acaoId)} aria-describedby={erros.acaoId ? 'launch-asset-error' : undefined} onChange={alterarAcao}>
                                <option value="">Selecione um ativo</option>
                                {dados.acoes.map(acao => <option key={acao.id} value={acao.id}>{rotuloOpcaoAcao(acao)}</option>)}
                            </select>
                            {erros.acaoId && <span className="field-error" id="launch-asset-error">{erros.acaoId}</span>}
                            {saldoVenda && <span className={`launch-field-helper ${quantidadeDisponivel === 0 ? 'tone-negative' : ''}`}>{saldoVenda}</span>}
                        </div>

                        <div className="form-field-full">
                            <label className="lbl-form" htmlFor="launch-broker">Instituição financeira</label>
                            <select className="input-moderno" id="launch-broker" value={form.corretoraId} disabled={enviando || semCorretoras} aria-invalid={Boolean(erros.corretoraId)} aria-describedby={erros.corretoraId ? 'launch-broker-error' : undefined} onChange={evento => atualizarCampo('corretoraId', evento.target.value)}>
                                <option value="">Selecione uma corretora</option>
                                {dados.corretoras.map(corretora => <option key={corretora.id} value={corretora.id}>{rotuloOpcaoCorretora(corretora)}</option>)}
                            </select>
                            {erros.corretoraId && <span className="field-error" id="launch-broker-error">{erros.corretoraId}</span>}
                        </div>

                        <div>
                            <label className="lbl-form" htmlFor="launch-quantity">Quantidade</label>
                            <input className="input-moderno" id="launch-quantity" type="number" min="1" max={form.tipo === 'VENDA' && form.acaoId ? quantidadeDisponivel : undefined} step="1" value={form.quantidade} disabled={enviando} aria-invalid={Boolean(erros.quantidade)} aria-describedby={erros.quantidade ? 'launch-quantity-error' : undefined} onChange={evento => atualizarCampo('quantidade', evento.target.value)} />
                            {erros.quantidade && <span className="field-error" id="launch-quantity-error">{erros.quantidade}</span>}
                        </div>

                        <div>
                            <label className="lbl-form" htmlFor="launch-date">Data da operação <span className="label-optional">(opcional)</span></label>
                            <input className="input-moderno" id="launch-date" type="date" value={form.data} disabled={enviando} aria-invalid={Boolean(erros.data)} aria-describedby={erros.data ? 'launch-date-error' : undefined} onChange={evento => atualizarCampo('data', evento.target.value)} />
                            {erros.data && <span className="field-error" id="launch-date-error">{erros.data}</span>}
                        </div>

                        <div className="form-field-full">
                            <label className="lbl-form" htmlFor="launch-price">Valor unitário</label>
                            <div className={`input-with-prefix ${erros.preco ? 'input-invalid' : ''}`}>
                                <span className="input-prefix">{simboloMoeda(form.moeda)}</span>
                                <input id="launch-price" type="number" min="0.0001" step="0.0001" value={form.preco} disabled={enviando} aria-invalid={Boolean(erros.preco)} aria-describedby={erros.preco ? 'launch-price-error' : undefined} onChange={evento => atualizarCampo('preco', evento.target.value)} />
                            </div>
                            {erros.preco && <span className="field-error" id="launch-price-error">{erros.preco}</span>}
                        </div>
                    </div>

                    <div className="launch-summary" aria-live="polite">
                        <div><span>Tipo</span><strong>{form.tipo === 'VENDA' ? 'Venda' : 'Compra'}</strong></div>
                        <div><span>Valor total informado</span><strong>{Number.isFinite(valorTotal) ? formatarMoeda(valorTotal, form.moeda) : '—'}</strong></div>
                    </div>

                    <footer className="launch-actions">
                        <button className="button button-secondary" type="button" disabled={enviando} onClick={limpar}><RotateCcw size={17} aria-hidden="true" /> Limpar</button>
                        <button className="button button-primary" type="submit" disabled={enviando || semAcoes || semCorretoras}>
                            {enviando ? <LoaderCircle className="is-spinning" size={17} aria-hidden="true" /> : <Check size={17} aria-hidden="true" />}
                            {enviando ? 'Registrando…' : `Confirmar ${form.tipo === 'VENDA' ? 'venda' : 'compra'}`}
                        </button>
                    </footer>
                </form>
            </Card>
        </div>
    );
}
