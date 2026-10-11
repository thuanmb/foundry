const Card = ({ children, className = "", as: Tag = "div", ...rest }) => {
    return (
        <Tag
            className={`rounded-xl border border-border bg-surface p-6 shadow-sm md:p-8 ${className}`}
            {...rest}
        >
            {children}
        </Tag>
    );
};

export default Card;
