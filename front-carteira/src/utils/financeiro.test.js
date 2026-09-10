import test from 'node:test';
import assert from 'node:assert/strict';
import {
    criarPayloadTransacao,
    dataLocalIso,
    formatarMoeda,
    resumirCarteiraPorMoeda
} from './financeiro.js';

test('formata valores em BRL e USD com a moeda correta', () => {
    assert.equal(formatarMoeda(1234.56, 'BRL').replace(/\u00a0/g, ' '), 'R$ 1.234,56');
    assert.equal(formatarMoeda(1234.56, 'USD'), 'US$ 1.234,56');
});

test('mantem totais de BRL e USD separados', () => {
    const resumos = resumirCarteiraPorMoeda([
        { moeda: 'BRL', saldoTotalAtual: 100, precoMedio: 10, quantidade: 10 },
        { moeda: 'USD', saldoTotalAtual: 100, precoMedio: 20, quantidade: 5 }
    ], []);

    assert.deepEqual(resumos.map(({ moeda, patrimonioTotal }) => ({ moeda, patrimonioTotal })), [
        { moeda: 'BRL', patrimonioTotal: 100 },
        { moeda: 'USD', patrimonioTotal: 100 }
    ]);
});

test('envia a data escolhida sem conversao de timezone', () => {
    const payload = criarPayloadTransacao({
        acaoId: '1', corretoraId: '2', quantidade: '3', preco: '12.50', data: '2026-09-05'
    });

    assert.equal(payload.data, '2026-09-05');
});

test('mantem a data opcional para clientes sem preenchimento', () => {
    const payload = criarPayloadTransacao({
        acaoId: '1', corretoraId: '2', quantidade: '3', preco: '12.50', data: ''
    });

    assert.equal(payload.data, null);
});

test('gera a data local sem converter para UTC', () => {
    assert.equal(dataLocalIso(new Date(2026, 8, 5, 0, 30)), '2026-09-05');
});
