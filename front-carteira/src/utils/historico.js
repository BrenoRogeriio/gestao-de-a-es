import { listarMoedasDisponiveis } from './dashboard.js';

export const FILTRO_TODOS = 'TODOS';

export function dataDaOperacao(operacao) {
    if (/^\d{4}-\d{2}-\d{2}$/.test(operacao?.dataOperacao ?? '')) return operacao.dataOperacao;
    const dataIso = String(operacao?.data ?? '').match(/^(\d{4}-\d{2}-\d{2})/);
    return dataIso?.[1] ?? '';
}

function instanteDaOperacao(operacao) {
    const instante = Date.parse(operacao?.data ?? '');
    return Number.isFinite(instante) ? instante : 0;
}

export function ordenarHistorico(historico = []) {
    return [...historico]
        .map((operacao, indice) => ({ operacao, indice }))
        .sort((a, b) => {
            const porDia = dataDaOperacao(b.operacao).localeCompare(dataDaOperacao(a.operacao));
            if (porDia !== 0) return porDia;
            const porInstante = instanteDaOperacao(b.operacao) - instanteDaOperacao(a.operacao);
            return porInstante !== 0 ? porInstante : a.indice - b.indice;
        })
        .map(item => item.operacao);
}

export function formatarCorretora(cnpj) {
    const digitos = String(cnpj ?? '').replace(/\D/g, '');
    if (digitos.length !== 14) return cnpj || 'Não informada';
    return digitos.replace(/^(\d{2})(\d{3})(\d{3})(\d{4})(\d{2})$/, '$1.$2.$3/$4-$5');
}

export function criarModeloHistorico({
    loading = false,
    error = false,
    historico = [],
    tipo = FILTRO_TODOS,
    moeda = FILTRO_TODOS,
    busca = '',
    dataInicial = '',
    dataFinal = ''
} = {}) {
    if (loading) return { estado: 'loading' };
    if (error) return { estado: 'error' };

    const lista = Array.isArray(historico) ? historico : [];
    if (lista.length === 0) return { estado: 'empty', operacoes: [], moedas: [] };

    const termo = busca.trim().toLocaleUpperCase('pt-BR');
    const moedas = listarMoedasDisponiveis([], lista);
    const operacoes = ordenarHistorico(lista).filter(operacao => {
        const data = dataDaOperacao(operacao);
        return (tipo === FILTRO_TODOS || operacao?.tipo === tipo)
            && (moeda === FILTRO_TODOS || operacao?.moeda === moeda)
            && (!termo || String(operacao?.ticker ?? '').toLocaleUpperCase('pt-BR').includes(termo))
            && (!dataInicial || (data && data >= dataInicial))
            && (!dataFinal || (data && data <= dataFinal));
    });

    return {
        estado: operacoes.length === 0 ? 'filtered-empty' : 'ready',
        operacoes,
        moedas,
        total: operacoes.length,
        totalOriginal: lista.length
    };
}

export function resumoOperacoes(total, tipo = FILTRO_TODOS) {
    if (tipo === 'COMPRA') return `${total} ${total === 1 ? 'compra encontrada' : 'compras encontradas'}`;
    if (tipo === 'VENDA') return `${total} ${total === 1 ? 'venda encontrada' : 'vendas encontradas'}`;
    return `${total} ${total === 1 ? 'operação encontrada' : 'operações encontradas'}`;
}
