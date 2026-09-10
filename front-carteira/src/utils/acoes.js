import { valorDisponivel } from './dashboard.js';

export const MERCADO_TODOS = 'TODOS';

export const MERCADOS = [
    { value: 'BRASIL', label: 'Brasil', moeda: 'BRL' },
    { value: 'ESTADOS_UNIDOS', label: 'Estados Unidos', moeda: 'USD' }
];

export const ORDENACOES_ACOES = [
    { value: 'ticker', label: 'Ticker (A–Z)' },
    { value: 'cotacao', label: 'Maior cotação' },
    { value: 'atualizacao', label: 'Atualização mais recente' }
];

export function rotuloMercadoAcao(mercado) {
    return MERCADOS.find(item => item.value === mercado)?.label ?? mercado ?? 'Mercado não informado';
}

export function normalizarTicker(ticker) {
    return String(ticker ?? '').trim().toUpperCase();
}

export function criarFormularioAcao() {
    return { ticker: '', mercado: 'BRASIL' };
}

export function validarFormularioAcao(form = {}) {
    const erros = {};
    const ticker = normalizarTicker(form.ticker);
    if (!ticker) erros.ticker = 'Informe o ticker do ativo.';
    else if (ticker.length > 20) erros.ticker = 'O ticker deve ter no máximo 20 caracteres.';
    if (!MERCADOS.some(item => item.value === form.mercado)) erros.mercado = 'Selecione um mercado válido.';
    return erros;
}

export function prepararCadastroAcao(form) {
    return { ticker: normalizarTicker(form.ticker), mercado: form.mercado };
}

export function formatarDataHoraCotacao(valor) {
    if (!valor) return '—';
    const data = new Date(valor);
    if (Number.isNaN(data.getTime())) return '—';
    const partes = new Intl.DateTimeFormat('pt-BR', {
        day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit'
    }).formatToParts(data);
    const parte = tipo => partes.find(item => item.type === tipo)?.value;
    return `${parte('day')}/${parte('month')}/${parte('year')} às ${parte('hour')}:${parte('minute')}`;
}

function instante(valor) {
    const numero = Date.parse(valor ?? '');
    return Number.isFinite(numero) ? numero : null;
}

export function ordenarAcoes(acoes = [], ordenacao = 'ticker') {
    return [...acoes].sort((a, b) => {
        if (ordenacao === 'ticker') return String(a?.ticker ?? '').localeCompare(String(b?.ticker ?? ''), 'pt-BR');
        const valorA = ordenacao === 'cotacao'
            ? (valorDisponivel(a?.cotacaoAtual) ? Number(a.cotacaoAtual) : null)
            : instante(a?.dataHoraCotacao);
        const valorB = ordenacao === 'cotacao'
            ? (valorDisponivel(b?.cotacaoAtual) ? Number(b.cotacaoAtual) : null)
            : instante(b?.dataHoraCotacao);
        if (valorA === null && valorB === null) return String(a?.ticker ?? '').localeCompare(String(b?.ticker ?? ''), 'pt-BR');
        if (valorA === null) return 1;
        if (valorB === null) return -1;
        return valorB - valorA || String(a?.ticker ?? '').localeCompare(String(b?.ticker ?? ''), 'pt-BR');
    });
}

export function criarModeloAcoes({
    loading = false,
    error = false,
    acoes = [],
    total = 0,
    busca = '',
    mercado = MERCADO_TODOS,
    ordenacao = 'ticker'
} = {}) {
    if (loading) return { estado: 'loading' };
    if (error) return { estado: 'error' };
    const lista = Array.isArray(acoes) ? acoes : [];
    if (lista.length === 0) return { estado: 'empty', acoes: [], total: 0 };

    const termo = busca.trim().toLocaleUpperCase('pt-BR');
    const filtradas = lista.filter(acao => (
        (mercado === MERCADO_TODOS || acao?.mercado === mercado)
        && (!termo || `${acao?.ticker ?? ''} ${acao?.nomeEmpresa ?? ''}`.toLocaleUpperCase('pt-BR').includes(termo))
    ));

    return {
        estado: filtradas.length ? 'ready' : 'filtered-empty',
        acoes: ordenarAcoes(filtradas, ordenacao),
        total: Number(total) || lista.length,
        encontrados: filtradas.length
    };
}

export function resumoAcoes(modelo, filtrosAtivos = false) {
    const quantidade = filtrosAtivos ? modelo.encontrados : modelo.total;
    return `${quantidade} ${quantidade === 1 ? (filtrosAtivos ? 'ativo encontrado' : 'ativo cadastrado') : (filtrosAtivos ? 'ativos encontrados' : 'ativos cadastrados')}`;
}
