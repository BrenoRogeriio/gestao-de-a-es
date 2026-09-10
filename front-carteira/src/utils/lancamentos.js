import { formatarQuantidade, validarFormularioOperacao } from './carteira.js';
import { dataLocalIso } from './financeiro.js';

export function criarFormularioLancamento({ tipo = 'COMPRA', corretoraId = '' } = {}) {
    return {
        tipo,
        acaoId: '',
        corretoraId,
        quantidade: '1',
        preco: '',
        data: dataLocalIso(),
        moeda: 'BRL'
    };
}

export function criarModeloLancamentos({ loading = false, error = false, acoes = [], corretoras = [], posicoes = [] } = {}) {
    if (loading) return { estado: 'loading' };
    if (error) return { estado: 'error' };
    return {
        estado: 'ready',
        acoes: Array.isArray(acoes) ? acoes : [],
        corretoras: Array.isArray(corretoras) ? corretoras : [],
        posicoes: Array.isArray(posicoes) ? posicoes : []
    };
}

export function rotuloOpcaoAcao(acao = {}) {
    const ticker = String(acao.ticker ?? '').trim();
    const nome = String(acao.nomeEmpresa ?? '').trim();
    if (ticker && nome && ticker.toLocaleUpperCase('pt-BR') !== nome.toLocaleUpperCase('pt-BR')) return `${ticker} — ${nome}`;
    return ticker || nome || 'Ativo sem identificação';
}

export function selecionarAcao(form, acao) {
    return {
        ...form,
        acaoId: acao?.id == null ? '' : String(acao.id),
        preco: acao?.cotacaoAtual == null ? '' : String(acao.cotacaoAtual),
        moeda: acao?.moeda || 'BRL'
    };
}

export function obterQuantidadeDisponivel(posicoes, acaoId) {
    if (!acaoId || !Array.isArray(posicoes)) return 0;
    return posicoes
        .filter(posicao => String(posicao?.acaoId) === String(acaoId))
        .reduce((total, posicao) => total + Number(posicao?.quantidade ?? 0), 0);
}

export function validarLancamento(form, posicoes) {
    const erros = {};
    if (!form.acaoId) erros.acaoId = 'Selecione um ativo.';
    return {
        ...erros,
        ...validarFormularioOperacao(form, obterQuantidadeDisponivel(posicoes, form.acaoId))
    };
}

export function descricaoSaldoVenda(form, posicoes) {
    if (form.tipo !== 'VENDA' || !form.acaoId) return '';
    const quantidade = obterQuantidadeDisponivel(posicoes, form.acaoId);
    return quantidade > 0
        ? `${formatarQuantidade(quantidade)} unidades disponíveis para venda.`
        : 'Você não possui uma posição disponível neste ativo.';
}

export function limparLancamentoConcluido(form) {
    return criarFormularioLancamento({ tipo: form.tipo, corretoraId: form.corretoraId });
}
