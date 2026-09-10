import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';
import { registrarOperacao } from '../services/carteira.js';
import { prepararOperacao } from './carteira.js';
import { rotuloOpcaoCorretora } from './corretoras.js';
import {
    criarFormularioLancamento,
    criarModeloLancamentos,
    descricaoSaldoVenda,
    limparLancamentoConcluido,
    obterQuantidadeDisponivel,
    rotuloOpcaoAcao,
    selecionarAcao,
    validarLancamento
} from './lancamentos.js';

const acao = { id: 7, ticker: 'WEGE3', nomeEmpresa: 'WEG', cotacaoAtual: 53.42, moeda: 'BRL' };
const corretora = { id: 12, nomeFantasia: 'XP Investimentos', cnpj: '02332886000104', email: 'atendimento@xpi.com.br' };
const posicoes = [{ acaoId: 7, quantidade: 8, ticker: 'WEGE3', moeda: 'BRL' }];

function formulario(overrides = {}) {
    return { ...criarFormularioLancamento(), acaoId: '7', corretoraId: '12', quantidade: '3', preco: '53.42', data: '2026-09-08', ...overrides };
}

test('representa o loading da página de lançamentos', () => {
    assert.deepEqual(criarModeloLancamentos({ loading: true }), { estado: 'loading' });
});

test('representa o erro de carregamento', () => {
    assert.deepEqual(criarModeloLancamentos({ error: true }), { estado: 'error' });
});

test('renderiza o formulário real com compra e venda', async () => {
    const fonte = await readFile(new URL('../components/HomeBroker.jsx', import.meta.url), 'utf8');
    assert.match(fonte, /Registrar lançamento/);
    assert.match(fonte, /> Comprar</);
    assert.match(fonte, /> Vender</);
});

test('inicia uma compra com data local e campos controlados', () => {
    const form = criarFormularioLancamento();
    assert.equal(form.tipo, 'COMPRA');
    assert.match(form.data, /^\d{4}-\d{2}-\d{2}$/);
    assert.equal(form.acaoId, '');
});

test('valida uma venda acima da posição disponível', () => {
    const erros = validarLancamento(formulario({ tipo: 'VENDA', quantidade: '9' }), posicoes);
    assert.match(erros.quantidade, /máxima disponível/);
});

test('seleciona ativo por id e preenche cotação e moeda', () => {
    const form = selecionarAcao(criarFormularioLancamento(), acao);
    assert.equal(form.acaoId, '7');
    assert.equal(form.preco, '53.42');
    assert.equal(form.moeda, 'BRL');
});

test('apresenta o ativo como ticker e nome da empresa', () => {
    assert.equal(rotuloOpcaoAcao(acao), 'WEGE3 — WEG');
});

test('apresenta a corretora pelo nome e CNPJ, sem e-mail', () => {
    const rotulo = rotuloOpcaoCorretora(corretora);
    assert.equal(rotulo, 'XP Investimentos — 02.332.886/0001-04');
    assert.doesNotMatch(rotulo, /atendimento@/);
});

test('mantém corretora.id como corretoraId numérico no payload', () => {
    assert.equal(prepararOperacao(formulario()).corretoraId, 12);
});

test('mantém acao.id como acaoId numérico no payload', () => {
    assert.equal(prepararOperacao(formulario()).acaoId, 7);
});

test('normaliza a quantidade inteira no payload', () => {
    assert.equal(prepararOperacao(formulario({ quantidade: '4' })).quantidade, 4);
});

test('normaliza valor unitário com vírgula no payload', () => {
    assert.equal(prepararOperacao(formulario({ preco: '53,42' })).valorUnitario, 53.42);
});

test('preserva a data YYYY-MM-DD no payload', () => {
    assert.equal(prepararOperacao(formulario({ data: '2026-09-07' })).data, '2026-09-07');
});

test('envia compra com Idempotency-Key sem chamar backend real', async () => {
    let chamada;
    await registrarOperacao('COMPRA', prepararOperacao(formulario()), 'chave-7a', async (path, options) => {
        chamada = { path, options };
        return { ok: true };
    });
    assert.equal(chamada.path, '/carteira/comprar');
    assert.equal(chamada.options.headers['Idempotency-Key'], 'chave-7a');
});

test('envia venda ao endpoint real do contrato', async () => {
    let pathRecebido;
    await registrarOperacao('VENDA', prepararOperacao(formulario({ tipo: 'VENDA' })), 'chave-venda', async path => {
        pathRecebido = path;
        return { ok: true };
    });
    assert.equal(pathRecebido, '/carteira/vender');
});

test('mantém os dados do formulário quando há erro e limpa só após sucesso', () => {
    const form = formulario({ tipo: 'VENDA' });
    assert.equal(form.preco, '53.42');
    const limpo = limparLancamentoConcluido(form);
    assert.equal(limpo.tipo, 'VENDA');
    assert.equal(limpo.corretoraId, '12');
    assert.equal(limpo.acaoId, '');
});

test('informa o saldo atual da venda e soma posições do mesmo ativo', () => {
    const repetidas = [...posicoes, { acaoId: 7, quantidade: 2 }];
    assert.equal(obterQuantidadeDisponivel(repetidas, '7'), 10);
    assert.match(descricaoSaldoVenda(formulario({ tipo: 'VENDA' }), repetidas), /10 unidades/);
});

test('bloqueia preventivamente venda de ativo sem posição', () => {
    const form = formulario({ tipo: 'VENDA', acaoId: '99' });
    assert.match(validarLancamento(form, posicoes).quantidade, /máxima disponível/);
    assert.match(descricaoSaldoVenda(form, posicoes), /não possui/);
});

test('protege contra duplo submit e expõe feedback seguro de sucesso e erro', async () => {
    const fonte = await readFile(new URL('../components/HomeBroker.jsx', import.meta.url), 'utf8');
    assert.match(fonte, /if \(enviandoRef\.current\) return/);
    assert.match(fonte, /registrada com sucesso/);
    assert.match(fonte, /feedback-\$\{feedback\.tone\}/);
});

test('possui estrutura responsiva própria sem tabela comprimida no mobile', async () => {
    const [fonte, css] = await Promise.all([
        readFile(new URL('../components/HomeBroker.jsx', import.meta.url), 'utf8'),
        readFile(new URL('../styles/lancamentos.css', import.meta.url), 'utf8')
    ]);
    assert.match(fonte, /launch-form-grid/);
    assert.match(css, /@media \(max-width: 640px\)/);
    assert.match(css, /grid-template-columns:\s*1fr/);
    assert.doesNotMatch(fonte, /<table/);
});
