export default function PageHeader({ title, description, action }) {
    return (
        <div className="page-header">
            <div>
                <h2>{title}</h2>
                {description && <p>{description}</p>}
            </div>
            {action && <div className="page-header-action">{action}</div>}
        </div>
    );
}
