import { formatarMoeda } from './financeiro.js';

const ORDEM_MOEDAS = ['BRL', 'USD'];

export function valorDisponivel(valor) {
    return valor !== null && valor !== undefined && Number.isFinite(Number(valor));
}

export function tomFinanceiro(valor) {
    if (!valorDisponivel(valor)) return 'neutral';
    const numero = Number(valor);
    return numero > 0 ? 'positive' : numero < 0 ? 'negative' : 'neutral';
}

export function formatarValorFinanceiro(valor, moeda, comSinal = false) {
    if (!valorDisponivel(valor)) return '—';
    const numero = Number(valor);
    const formatado = formatarMoeda(comSinal ? Math.abs(numero) : numero, moeda);
    if (!comSinal || numero === 0) return formatado;
    return `${numero > 0 ? '+' : '−'} ${formatado}`;
}

export function formatarPercentualFinanceiro(valor, comSinal = true) {
    if (!valorDisponivel(valor)) return '—';
    const numero = Number(valor);
    const formatado = new Intl.NumberFormat('pt-BR', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    }).format(Math.abs(numero));
    if (!comSinal || numero === 0) return `${formatado}%`;
    return `${numero > 0 ? '+' : '−'} ${formatado}%`;
}

export function listarMoedasDisponiveis(resumos = [], posicoes = []) {
    const moedas = new Set([
        ...resumos.map(resumo => resumo?.moeda),
        ...posicoes.map(posicao => posicao?.moeda)
    ].filter(Boolean));

    return [...moedas].sort((a, b) => {
        const ordemA = ORDEM_MOEDAS.indexOf(a);
        const ordemB = ORDEM_MOEDAS.indexOf(b);
        if (ordemA === -1 && ordemB === -1) return a.localeCompare(b);
        if (ordemA === -1) return 1;
        if (ordemB === -1) return -1;
        return ordemA - ordemB;
    });
}

export function criarDistribuicao(posicoes = []) {
    if (posicoes.length === 0) return { estado: 'empty', itens: [] };
    if (posicoes.some(posicao => !valorDisponivel(posicao.valorAtual))) {
        return { estado: 'unavailable', itens: [] };
    }

    const porAtivo = new Map();
    posicoes.forEach(posicao => {
        const valorAtual = Number(posicao.valorAtual);
        if (valorAtual <= 0) return;
        const atual = porAtivo.get(posicao.ticker) ?? 0;
        porAtivo.set(posicao.ticker, atual + valorAtual);
    });

    const total = [...porAtivo.values()].reduce((soma, valor) => soma + valor, 0);
    if (total <= 0) return { estado: 'empty', itens: [] };

    const ordenados = [...porAtivo.entries()]
        .map(([ticker, valorAtual]) => ({ ticker, valorAtual, percentual: (valorAtual / total) * 100 }))
        .sort((a, b) => b.valorAtual - a.valorAtual);

    if (ordenados.length <= 5) return { estado: 'ready', itens: ordenados, total };

    const principais = ordenados.slice(0, 5);
    const valorOutros = ordenados.slice(5).reduce((soma, item) => soma + item.valorAtual, 0);
    return {
        estado: 'ready',
        total,
        itens: [
            ...principais,
            { ticker: 'Outros', valorAtual: valorOutros, percentual: (valorOutros / total) * 100 }
        ]
    };
}

export function criarModeloDashboard({
    loading = false,
    error = false,
    resumos = [],
    posicoes = [],
    moedaSelecionada
} = {}) {
    if (loading) return { estado: 'loading' };
    if (error) return { estado: 'error' };

    const listaResumos = Array.isArray(resumos) ? resumos : [];
    const listaPosicoes = Array.isArray(posicoes) ? posicoes : [];
    const moedas = listarMoedasDisponiveis(listaResumos, listaPosicoes);
    if (moedas.length === 0) return { estado: 'empty', moedas: [] };

    const moeda = moedas.includes(moedaSelecionada) ? moedaSelecionada : moedas[0];
    const resumo = listaResumos.find(item => item?.moeda === moeda) ?? null;
    const posicoesDaMoeda = listaPosicoes.filter(item => item?.moeda === moeda);

    return {
        estado: resumo || posicoesDaMoeda.length > 0 ? 'ready' : 'currency-empty',
        moeda,
        moedas,
        resumo,
        posicoes: posicoesDaMoeda,
        posicoesResumidas: posicoesDaMoeda.slice(0, 5),
        totalPosicoes: posicoesDaMoeda.length,
        semPosicoesComResultadoRealizado: posicoesDaMoeda.length === 0
            && valorDisponivel(resumo?.resultadoRealizadoTotal)
            && Number(resumo.resultadoRealizadoTotal) !== 0,
        distribuicao: criarDistribuicao(posicoesDaMoeda)
    };
}
