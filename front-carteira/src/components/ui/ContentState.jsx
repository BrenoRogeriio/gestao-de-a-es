import { Inbox } from 'lucide-react';
import Card from './Card.jsx';

export default function ContentState({
    title,
    description,
    action,
    icon: Icon = Inbox,
    className = '',
    role,
    ariaLive
}) {
    return (
        <Card className={`content-state ${className}`.trim()} role={role} aria-live={ariaLive}>
            <span className="content-state-icon" aria-hidden="true"><Icon size={24} /></span>
            <h2>{title}</h2>
            {description && <p>{description}</p>}
            {action && <div className="content-state-action">{action}</div>}
        </Card>
    );
}
