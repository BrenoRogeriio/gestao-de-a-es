export default function Card({ as: Component = 'section', className = '', children, ...props }) {
    return <Component className={`card ${className}`.trim()} {...props}>{children}</Component>;
}
