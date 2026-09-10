import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';
import { consultarHistorico } from '../services/carteira.js';
import { formatarPreco } from './carteira.js';
import { formatarPercentualFinanceiro, formatarValorFinanceiro } from './dashboard.js';
import {
    criarModeloHistorico,
    dataDaOperacao,
    FILTRO_TODOS,
    formatarCorretora,
    resumoOperacoes
} from './historico.js';

const compraBRL = {
    acaoId: 1,
    tipo: 'COMPRA',
    ticker: 'WEGE3',
    mercado: 'BRASIL',
    moeda: 'BRL',
    corretoraCnpj: '12345678000190',
    quantidade: 10,
    valorUnitario: 100,
    valorTotal: 1000,
    precoMedioOperacao: null,
    resultadoRealizado: null,
    rentabilidadeRealizada: null,
    data: '2026-09-01T10:30:00-03:00',
    dataOperacao: '2026-09-01'
};

const vendaLucroBRL = {
    ...compraBRL,
    tipo: 'VENDA',
    quantidade: 4,
    valorUnitario: 130,
    valorTotal: 520,
    precoMedioOperacao: 106.6675,
    resultadoRealizado: 93.33,
    rentabilidadeRealizada: 21.88,
    data: '2026-09-04T18:00:00-03:00',
    dataOperacao: '2026-09-04'
};

const vendaPrejuizoUSD = {
    ...vendaLucroBRL,
    acaoId: 2,
    ticker: 'MSFT',
    mercado: 'EUA',
    moeda: 'USD',
    valorUnitario: 450,
    precoMedioOperacao: 500,
    resultadoRealizado: -100,
    rentabilidadeRealizada: -10,
    data: '2026-09-03T09:00:00-03:00',
    dataOperacao: '2026-09-03'
};

const historico = [compraBRL, vendaLucroBRL, vendaPrejuizoUSD];

test('representa o estado de loading', () => {
    assert.deepEqual(criarModeloHistorico({ loading: true }), { estado: 'loading' });
});

test('representa o estado de erro', () => {
    assert.deepEqual(criarModeloHistorico({ error: true }), { estado: 'error' });
});

test('representa histórico vazio', () => {
    assert.deepEqual(criarModeloHistorico(), { estado: 'empty', operacoes: [], moedas: [] });
});

test('preserva os campos reais de uma compra', () => {
    const [operacao] = criarModeloHistorico({ historico: [compraBRL] }).operacoes;
    assert.equal(operacao.tipo, 'COMPRA');
    assert.equal(operacao.ticker, 'WEGE3');
    assert.equal(operacao.resultadoRealizado, null);
});

test('preserva preço médio, resultado e rentabilidade de uma venda', () => {
    const [operacao] = criarModeloHistorico({ historico: [vendaLucroBRL] }).operacoes;
    assert.equal(operacao.tipo, 'VENDA');
    assert.equal(operacao.precoMedioOperacao, 106.6675);
    assert.equal(operacao.rentabilidadeRealizada, 21.88);
});

test('formata resultado realizado positivo com sinal', () => {
    assert.match(formatarValorFinanceiro(vendaLucroBRL.resultadoRealizado, 'BRL', true), /^\+/);
});

test('formata resultado realizado negativo com sinal', () => {
    assert.match(formatarValorFinanceiro(vendaPrejuizoUSD.resultadoRealizado, 'USD', true), /^−/);
});

test('mantém resultado null semanticamente indisponível', () => {
    assert.equal(formatarValorFinanceiro(null, 'BRL', true), '—');
});

test('formata preço médio da operação com até quatro casas', () => {
    assert.match(formatarPreco(vendaLucroBRL.precoMedioOperacao, 'BRL').replace(/\u00a0/g, ' '), /106,6675/);
});

test('formata rentabilidade realizada com sinal', () => {
    assert.equal(formatarPercentualFinanceiro(21.88), '+ 21,88%');
});

test('formata valores do histórico em BRL', () => {
    assert.match(formatarValorFinanceiro(93.33, 'BRL', true).replace(/\u00a0/g, ' '), /R\$ 93,33/);
});

test('formata valores do histórico em USD', () => {
    assert.match(formatarValorFinanceiro(-100, 'USD', true), /US\$ 100,00/);
});

test('filtra somente compras', () => {
    const modelo = criarModeloHistorico({ historico, tipo: 'COMPRA' });
    assert.deepEqual(modelo.operacoes.map(item => item.tipo), ['COMPRA']);
    assert.equal(resumoOperacoes(modelo.total, 'COMPRA'), '1 compra encontrada');
});

test('filtra somente vendas', () => {
    const modelo = criarModeloHistorico({ historico, tipo: 'VENDA' });
    assert.ok(modelo.operacoes.every(item => item.tipo === 'VENDA'));
    assert.equal(resumoOperacoes(modelo.total, 'VENDA'), '2 vendas encontradas');
});

test('filtra BRL e USD sem misturar moedas', () => {
    assert.ok(criarModeloHistorico({ historico, moeda: 'BRL' }).operacoes.every(item => item.moeda === 'BRL'));
    assert.deepEqual(criarModeloHistorico({ historico, moeda: 'USD' }).operacoes.map(item => item.ticker), ['MSFT']);
});

test('busca ticker ignorando maiúsculas, minúsculas e espaços externos', () => {
    const modelo = criarModeloHistorico({ historico, busca: '  msFt ' });
    assert.deepEqual(modelo.operacoes.map(item => item.ticker), ['MSFT']);
});

test('filtra período inclusivo usando YYYY-MM-DD sem converter timezone', () => {
    const modelo = criarModeloHistorico({ historico, dataInicial: '2026-09-02', dataFinal: '2026-09-03' });
    assert.deepEqual(modelo.operacoes.map(item => item.ticker), ['MSFT']);
    assert.equal(dataDaOperacao(vendaPrejuizoUSD), '2026-09-03');
});

test('usa o prefixo local de data como fallback sem mudar o dia', () => {
    assert.equal(dataDaOperacao({ data: '2026-09-05T00:30:00+14:00' }), '2026-09-05');
});

test('representa filtros sem resultado separadamente do histórico vazio', () => {
    const modelo = criarModeloHistorico({ historico, busca: 'PETR4' });
    assert.equal(modelo.estado, 'filtered-empty');
    assert.equal(modelo.totalOriginal, 3);
});

test('ordena do dia e horário mais recente para o mais antigo', () => {
    const mesmoDiaMaisTarde = { ...compraBRL, ticker: 'VALE3', data: '2026-09-04T20:00:00-03:00', dataOperacao: '2026-09-04' };
    const modelo = criarModeloHistorico({ historico: [compraBRL, vendaLucroBRL, mesmoDiaMaisTarde] });
    assert.deepEqual(modelo.operacoes.map(item => item.ticker), ['VALE3', 'WEGE3', 'WEGE3']);
});

test('consulta apenas o endpoint real sem backend nos testes', async () => {
    let chamada;
    const dados = await consultarHistorico(undefined, async (path, options) => {
        chamada = { path, options };
        return { ok: true, json: async () => historico };
    });
    assert.equal(chamada.path, '/carteira/historico');
    assert.equal(chamada.options.cache, 'no-store');
    assert.equal(dados.length, 3);
});

test('mantém CNPJ da corretora como detalhe secundário formatado', () => {
    assert.equal(formatarCorretora(compraBRL.corretoraCnpj), '12.345.678/0001-90');
});

test('mantém estruturas distintas para tabela desktop e cards mobile', async () => {
    const componente = await readFile(new URL('../components/historico/HistoricoLista.jsx', import.meta.url), 'utf8');
    const css = await readFile(new URL('../styles/historico.css', import.meta.url), 'utf8');
    assert.match(componente, /history-table-wrap/);
    assert.match(componente, /history-mobile-list/);
    assert.match(componente, /<th scope="col">/);
    assert.match(css, /@media \(max-width: 820px\)/);
    assert.match(css, /\.history-table-wrap \{\s*display: none;/);
});

test('filtros usam valor neutro explícito', () => {
    assert.equal(FILTRO_TODOS, 'TODOS');
});
