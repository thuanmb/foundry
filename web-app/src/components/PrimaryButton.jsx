import { Link } from "react-router-dom";

const baseClasses =
    "inline-flex items-center justify-center gap-2 rounded-lg bg-primary font-medium text-on-primary shadow-sm transition-colors hover:bg-primary-hover focus:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 focus-visible:ring-offset-background";

const sizes = {
    sm: "px-3 py-1.5 text-sm",
    md: "px-4 py-2 text-sm",
    lg: "px-6 py-3 text-base",
};

const PrimaryButton = ({
    children,
    to,
    onClick,
    type = "button",
    className = "",
    size = "md",
    disabled = false,
    ...rest
}) => {
    const classes = `${baseClasses} ${sizes[size] || sizes.md} ${disabled ? "cursor-not-allowed opacity-50 pointer-events-none" : ""} ${className}`;

    if (to) {
        return (
            <Link to={to} className={classes} {...rest}>
                {children}
            </Link>
        );
    }

    return (
        <button type={type} onClick={onClick} disabled={disabled} className={classes} {...rest}>
            {children}
        </button>
    );
};

export default PrimaryButton;
