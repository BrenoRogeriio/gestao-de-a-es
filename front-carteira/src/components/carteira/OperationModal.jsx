import { useEffect, useRef, useState } from 'react';
import { Check, LoaderCircle, ShoppingCart, TrendingDown, X } from 'lucide-react';
import { registrarOperacao } from '../../services/carteira.js';
import { consultarCorretoras } from '../../services/corretoras.js';
import {
    criarFormularioOperacao,
    formatarQuantidade,
    gerarChaveIdempotencia,
    obterTentativaIdempotente,
    prepararOperacao,
    validarFormularioOperacao
} from '../../utils/carteira.js';
import { formatarMoeda, simboloMoeda } from '../../utils/financeiro.js';
import { rotuloOpcaoCorretora } from '../../utils/corretoras.js';

export default function OperationModal({ posicao, tipo, onClose, onSuccess }) {
    const [form, setForm] = useState(() => criarFormularioOperacao(posicao, tipo));
    const [corretoras, setCorretoras] = useState([]);
    const [carregandoCorretoras, setCarregandoCorretoras] = useState(true);
    const [enviando, setEnviando] = useState(false);
    const [erros, setErros] = useState({});
    const [erroGeral, setErroGeral] = useState('');
    const dialogRef = useRef(null);
    const quantidadeRef = useRef(null);
    const tentativaRef = useRef(null);
    const titulo = tipo === 'VENDA' ? 'Vender ativo' : 'Comprar mais';
    const Icon = tipo === 'VENDA' ? TrendingDown : ShoppingCart;

    useEffect(() => {
        const controller = new AbortController();
        const elementoAnterior = document.activeElement;
        const overflowAnterior = document.body.style.overflow;
        document.body.style.overflow = 'hidden';

        consultarCorretoras(controller.signal)
            .then(dados => setCorretoras(dados.corretoras))
            .catch(falha => {
                if (falha.name !== 'AbortError') setErroGeral(falha.message);
            })
            .finally(() => {
                if (!controller.signal.aborted) setCarregandoCorretoras(false);
            });

        window.setTimeout(() => quantidadeRef.current?.focus(), 0);
        return () => {
            controller.abort();
            document.body.style.overflow = overflowAnterior;
            elementoAnterior?.focus?.();
        };
    }, []);

    useEffect(() => {
        const lidarComTecla = evento => {
            if (evento.key === 'Escape' && !enviando) onClose();
            if (evento.key !== 'Tab') return;
            const focaveis = dialogRef.current?.querySelectorAll(
                'button:not([disabled]), input:not([disabled]), select:not([disabled]), [tabindex="0"]'
            );
            if (!focaveis?.length) return;
            const primeiro = focaveis[0];
            const ultimo = focaveis[focaveis.length - 1];
            if (evento.shiftKey && document.activeElement === primeiro) {
                evento.preventDefault();
                ultimo.focus();
            } else if (!evento.shiftKey && document.activeElement === ultimo) {
                evento.preventDefault();
                primeiro.focus();
            }
        };
        document.addEventListener('keydown', lidarComTecla);
        return () => document.removeEventListener('keydown', lidarComTecla);
    }, [enviando, onClose]);

    const atualizarCampo = (campo, valor) => {
        setForm(atual => ({ ...atual, [campo]: valor }));
        setErros(atuais => ({ ...atuais, [campo]: undefined }));
        setErroGeral('');
    };

    const enviar = async evento => {
        evento.preventDefault();
        const validacao = validarFormularioOperacao(form, posicao.quantidade);
        setErros(validacao);
        if (Object.keys(validacao).length > 0) return;

        const payload = prepararOperacao(form);
        const tentativa = obterTentativaIdempotente(tentativaRef.current, payload, gerarChaveIdempotencia);
        tentativaRef.current = tentativa;
        setEnviando(true);
        setErroGeral('');
        try {
            await registrarOperacao(tipo, payload, tentativa.chave);
            await onSuccess(tipo, posicao.ticker);
        } catch (falha) {
            setErroGeral(falha.message || 'Não foi possível registrar a operação.');
            setEnviando(false);
        }
    };

    const valorTotal = Number(form.quantidade) * Number(String(form.preco).replace(',', '.'));

    return (
        <div className="operation-modal-backdrop" onMouseDown={evento => evento.target === evento.currentTarget && !enviando && onClose()}>
            <section
                className="operation-modal"
                ref={dialogRef}
                role="dialog"
                aria-modal="true"
                aria-labelledby="operation-modal-title"
                aria-describedby="operation-modal-description"
            >
                <header className="operation-modal-header">
                    <div className={`operation-modal-icon operation-modal-icon-${tipo.toLowerCase()}`}><Icon size={20} aria-hidden="true" /></div>
                    <div>
                        <h2 id="operation-modal-title">{titulo}: {posicao.ticker}</h2>
                        <p id="operation-modal-description">
                            {tipo === 'VENDA'
                                ? `${formatarQuantidade(posicao.quantidade)} unidades disponíveis para venda.`
                                : 'Registre uma nova compra deste ativo na carteira.'}
                        </p>
                    </div>
                    <button className="icon-button operation-modal-close" type="button" onClick={onClose} disabled={enviando} aria-label="Fechar formulário">
                        <X size={19} aria-hidden="true" />
                    </button>
                </header>

                <form className="operation-form" onSubmit={enviar} noValidate>
                    <div className="operation-asset-summary">
                        <span>Ativo</span>
                        <strong>{posicao.ticker}</strong>
                        <small>{posicao.mercado} · {posicao.moeda}</small>
                    </div>

                    {erroGeral && <div className="inline-feedback feedback-error" role="alert">{erroGeral}</div>}

                    <div className="operation-form-grid">
                        <div className="form-field-full">
                            <label className="lbl-form" htmlFor="operation-broker">Corretora</label>
                            <select
                                className="input-moderno"
                                id="operation-broker"
                                value={form.corretoraId}
                                disabled={carregandoCorretoras || enviando}
                                aria-invalid={Boolean(erros.corretoraId)}
                                aria-describedby={erros.corretoraId ? 'operation-broker-error' : undefined}
                                onChange={evento => atualizarCampo('corretoraId', evento.target.value)}
                            >
                                <option value="">{carregandoCorretoras ? 'Carregando corretoras…' : 'Selecione uma corretora'}</option>
                                {corretoras.map(corretora => <option key={corretora.id} value={corretora.id}>{rotuloOpcaoCorretora(corretora)}</option>)}
                            </select>
                            {erros.corretoraId && <span className="field-error" id="operation-broker-error">{erros.corretoraId}</span>}
                        </div>

                        <div>
                            <label className="lbl-form" htmlFor="operation-quantity">Quantidade</label>
                            <input
                                className="input-moderno"
                                id="operation-quantity"
                                ref={quantidadeRef}
                                type="number"
                                min="1"
                                max={tipo === 'VENDA' ? posicao.quantidade : undefined}
                                step="1"
                                value={form.quantidade}
                                disabled={enviando}
                                aria-invalid={Boolean(erros.quantidade)}
                                aria-describedby={erros.quantidade ? 'operation-quantity-error' : undefined}
                                onChange={evento => atualizarCampo('quantidade', evento.target.value)}
                            />
                            {erros.quantidade && <span className="field-error" id="operation-quantity-error">{erros.quantidade}</span>}
                        </div>

                        <div>
                            <label className="lbl-form" htmlFor="operation-date">Data da operação</label>
                            <input
                                className="input-moderno"
                                id="operation-date"
                                type="date"
                                value={form.data}
                                disabled={enviando}
                                aria-invalid={Boolean(erros.data)}
                                aria-describedby={erros.data ? 'operation-date-error' : undefined}
                                onChange={evento => atualizarCampo('data', evento.target.value)}
                            />
                            {erros.data && <span className="field-error" id="operation-date-error">{erros.data}</span>}
                        </div>

                        <div className="form-field-full">
                            <label className="lbl-form" htmlFor="operation-price">Valor unitário</label>
                            <div className={`input-with-prefix ${erros.preco ? 'input-invalid' : ''}`}>
                                <span className="input-prefix">{simboloMoeda(posicao.moeda)}</span>
                                <input
                                    id="operation-price"
                                    type="number"
                                    min="0.0001"
                                    step="0.0001"
                                    value={form.preco}
                                    disabled={enviando}
                                    aria-invalid={Boolean(erros.preco)}
                                    aria-describedby={erros.preco ? 'operation-price-error' : undefined}
                                    onChange={evento => atualizarCampo('preco', evento.target.value)}
                                />
                            </div>
                            {erros.preco && <span className="field-error" id="operation-price-error">{erros.preco}</span>}
                        </div>
                    </div>

                    <div className="operation-total">
                        <span>Valor total informado</span>
                        <strong>{Number.isFinite(valorTotal) ? formatarMoeda(valorTotal, posicao.moeda) : '—'}</strong>
                    </div>

                    <footer className="operation-modal-actions">
                        <button className="button button-secondary" type="button" onClick={onClose} disabled={enviando}>Cancelar</button>
                        <button className="button button-primary" type="submit" disabled={enviando || carregandoCorretoras}>
                            {enviando ? <LoaderCircle className="is-spinning" size={17} aria-hidden="true" /> : <Check size={17} aria-hidden="true" />}
                            {enviando ? 'Registrando…' : `Confirmar ${tipo === 'VENDA' ? 'venda' : 'compra'}`}
                        </button>
                    </footer>
                </form>
            </section>
        </div>
    );
}
