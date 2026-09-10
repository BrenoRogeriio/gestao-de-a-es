const CONFIGURACAO_MOEDA = {
    BRL: { locale: 'pt-BR', currency: 'BRL' },
    USD: { locale: 'pt-BR', currency: 'USD' }
};

export function formatarMoeda(valor, moeda) {
    const configuracao = CONFIGURACAO_MOEDA[moeda];
    if (!configuracao) return `${moeda ?? ''} ${Number(valor ?? 0).toFixed(2)}`.trim();

    const formatado = new Intl.NumberFormat(configuracao.locale, {
        style: 'currency',
        currency: configuracao.currency
    }).format(Number(valor ?? 0));

    return moeda === 'USD' ? formatado.replace(/^(?:US\$|\$)\s*/, 'US$ ') : formatado;
}

export function simboloMoeda(moeda) {
    return moeda === 'USD' ? 'US$' : 'R$';
}

export function resumirCarteiraPorMoeda(posicoes, historico) {
    const moedas = [...new Set([
        ...posicoes.map(posicao => posicao.moeda),
        ...historico.map(transacao => transacao.moeda)
    ].filter(Boolean))].sort();

    return moedas.map(moeda => {
        const posicoesDaMoeda = posicoes.filter(posicao => posicao.moeda === moeda);
        const historicoDaMoeda = historico.filter(transacao => transacao.moeda === moeda);
        const patrimonioTotal = posicoesDaMoeda.reduce(
            (total, posicao) => total + Number(posicao.saldoTotalAtual ?? 0),
            0
        );
        const valorInvestido = posicoesDaMoeda.reduce(
            (total, posicao) => total + Number(posicao.precoMedio ?? 0) * Number(posicao.quantidade ?? 0),
            0
        );
        const totalGastoCompras = historicoDaMoeda
            .filter(transacao => transacao.tipo === 'COMPRA')
            .reduce((total, transacao) => total + Number(transacao.valorTotal ?? 0), 0);
        const totalRecebidoVendas = historicoDaMoeda
            .filter(transacao => transacao.tipo === 'VENDA')
            .reduce((total, transacao) => total + Number(transacao.valorTotal ?? 0), 0);
        const lucroTotal = patrimonioTotal + totalRecebidoVendas - totalGastoCompras;

        return {
            moeda,
            patrimonioTotal,
            valorInvestido,
            lucroTotal,
            rentabilidade: totalGastoCompras > 0 ? (lucroTotal / totalGastoCompras) * 100 : 0
        };
    });
}

export function dataLocalIso(data = new Date()) {
    const ano = data.getFullYear();
    const mes = String(data.getMonth() + 1).padStart(2, '0');
    const dia = String(data.getDate()).padStart(2, '0');
    return `${ano}-${mes}-${dia}`;
}

export function formatarDataOperacao(dataOperacao) {
    const partes = dataOperacao?.match(/^(\d{4})-(\d{2})-(\d{2})$/);
    return partes ? `${partes[3]}/${partes[2]}/${partes[1]}` : '-';
}

export function criarPayloadTransacao(form) {
    return {
        acaoId: Number.parseInt(form.acaoId, 10),
        corretoraId: Number.parseInt(form.corretoraId, 10),
        quantidade: Number.parseInt(form.quantidade, 10),
        valorUnitario: Number.parseFloat(form.preco.toString().replace(',', '.')),
        data: form.data || null
    };
}
