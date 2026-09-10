import { formatarMoeda } from '../../utils/financeiro.js';

export default function CurrencyValue({ value, currency = 'BRL', tone = 'neutral', className = '' }) {
    const numericValue = Number(value ?? 0);
    const signal = numericValue > 0 ? 'positivo' : numericValue < 0 ? 'negativo' : 'neutro';
    const formattedValue = formatarMoeda(numericValue, currency);

    return (
        <span className={`currency-value tone-${tone} ${className}`.trim()} aria-label={`${formattedValue}, valor ${signal}`}>
            {formattedValue}
        </span>
    );
}
