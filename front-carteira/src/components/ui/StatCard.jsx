import { ArrowDownRight, ArrowUpRight, Minus } from 'lucide-react';
import Card from './Card.jsx';

export default function StatCard({ label, value, helper, tone = 'neutral', statusLabel, showIndicator = true }) {
    const TrendIcon = tone === 'positive' ? ArrowUpRight : tone === 'negative' ? ArrowDownRight : Minus;
    const trendLabel = statusLabel ?? (tone === 'positive' ? 'Resultado positivo' : tone === 'negative' ? 'Resultado negativo' : 'Valor estável');

    return (
        <Card className={`stat-card stat-${tone}`}>
            <div className="stat-label-row">
                <span className="stat-label">{label}</span>
                {showIndicator && <span className="stat-trend" aria-label={trendLabel} title={trendLabel}><TrendIcon size={16} aria-hidden="true" /></span>}
            </div>
            <div className="stat-value">{value}</div>
            {helper && <p className="stat-helper">{helper}</p>}
        </Card>
    );
}
