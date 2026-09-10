import { listarMoedasDisponiveis, valorDisponivel } from './dashboard.js';
import { criarPayloadTransacao, dataLocalIso } from './financeiro.js';

export const OPCOES_ORDENACAO = [
    { value: 'ticker', label: 'Ativo (A-Z)' },
    { value: 'valorAtual', label: 'Maior valor atual' },
    { value: 'resultadoNaoRealizado', label: 'Maior resultado' },
    { value: 'rentabilidadePercentual', label: 'Maior rentabilidade' }
];

export function rotuloMercado(mercado) {
    const rotulos = { BRASIL: 'Brasil', EUA: 'EUA' };
    return rotulos[mercado] ?? mercado ?? 'Mercado não informado';
}

export function formatarQuantidade(quantidade) {
    if (!valorDisponivel(quantidade)) return '—';
    return new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 4 }).format(Number(quantidade));
}

export function formatarPreco(valor, moeda) {
    if (!valorDisponivel(valor)) return '—';
    const currency = moeda === 'USD' ? 'USD' : 'BRL';
    const locale = 'pt-BR';
    const formatado = new Intl.NumberFormat(locale, {
        style: 'currency',
        currency,
        minimumFractionDigits: 2,
        maximumFractionDigits: 4
    }).format(Number(valor));
    return currency === 'USD' ? formatado.replace(/^(?:US\$|\$)\s*/, 'US$ ') : formatado;
}

export function ordenarPosicoes(posicoes, ordenacao = 'ticker') {
    const lista = [...posicoes];
    if (ordenacao === 'ticker') {
        return lista.sort((a, b) => String(a?.ticker ?? '').localeCompare(String(b?.ticker ?? '')));
    }

    return lista.sort((a, b) => {
        const aDisponivel = valorDisponivel(a?.[ordenacao]);
        const bDisponivel = valorDisponivel(b?.[ordenacao]);
        if (!aDisponivel && !bDisponivel) return String(a?.ticker ?? '').localeCompare(String(b?.ticker ?? ''));
        if (!aDisponivel) return 1;
        if (!bDisponivel) return -1;
        return Number(b[ordenacao]) - Number(a[ordenacao]);
    });
}

export function criarModeloCarteira({
    loading = false,
    error = false,
    resumos = [],
    posicoes = [],
    moedaSelecionada,
    busca = '',
    ordenacao = 'ticker'
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
    const termo = busca.trim().toLocaleUpperCase('pt-BR');
    const filtradas = termo
        ? posicoesDaMoeda.filter(item => `${item?.ticker ?? ''} ${item?.nomeEmpresa ?? ''}`.toLocaleUpperCase('pt-BR').includes(termo))
        : posicoesDaMoeda;

    return {
        estado: resumo || posicoesDaMoeda.length > 0 ? 'ready' : 'currency-empty',
        moeda,
        moedas,
        resumo,
        posicoes: ordenarPosicoes(filtradas, ordenacao),
        totalPosicoes: posicoesDaMoeda.length,
        buscaSemResultado: posicoesDaMoeda.length > 0 && filtradas.length === 0,
        semPosicoesComResultadoRealizado: posicoesDaMoeda.length === 0
            && valorDisponivel(resumo?.resultadoRealizadoTotal)
            && Number(resumo.resultadoRealizadoTotal) !== 0
    };
}

export function criarFormularioOperacao(posicao, tipo) {
    return {
        tipo,
        acaoId: String(posicao.acaoId),
        corretoraId: '',
        quantidade: '1',
        preco: valorDisponivel(posicao.cotacaoAtual) ? String(posicao.cotacaoAtual) : '',
        data: dataLocalIso(),
        moeda: posicao.moeda
    };
}

export function validarFormularioOperacao(form, quantidadeDisponivel) {
    const erros = {};
    const quantidade = Number(form.quantidade);
    const preco = Number(String(form.preco).replace(',', '.'));

    if (!form.corretoraId) erros.corretoraId = 'Selecione uma corretora.';
    if (!Number.isInteger(quantidade) || quantidade <= 0) erros.quantidade = 'Informe uma quantidade inteira maior que zero.';
    if (form.tipo === 'VENDA' && Number.isFinite(quantidade) && quantidade > Number(quantidadeDisponivel)) {
        erros.quantidade = `A quantidade máxima disponível é ${formatarQuantidade(quantidadeDisponivel)}.`;
    }
    if (!Number.isFinite(preco) || preco <= 0) erros.preco = 'Informe um valor unitário maior que zero.';
    if (form.data && !/^\d{4}-\d{2}-\d{2}$/.test(form.data)) erros.data = 'Informe uma data válida.';
    return erros;
}

export function prepararOperacao(form) {
    return criarPayloadTransacao(form);
}

export function obterTentativaIdempotente(tentativaAtual, payload, gerarChave) {
    const assinatura = JSON.stringify(payload);
    if (tentativaAtual?.assinatura === assinatura && tentativaAtual?.chave) return tentativaAtual;
    return { chave: gerarChave(), assinatura };
}

export function gerarChaveIdempotencia() {
    if (globalThis.crypto?.randomUUID) return globalThis.crypto.randomUUID();
    const aleatorio = globalThis.crypto?.getRandomValues
        ? globalThis.crypto.getRandomValues(new Uint32Array(4)).join('-')
        : `${Date.now()}-${Math.random().toString(16).slice(2)}`;
    return `front-${aleatorio}`;
}
