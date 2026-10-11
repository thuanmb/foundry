const sizeClasses = {
    sm: "h-5 w-5 border-2",
    md: "h-10 w-10 border-[3px]",
    lg: "h-16 w-16 border-4",
};

const LoadingSpinner = ({ size = "md", className = "", label }) => {
    return (
        <div className={`flex items-center justify-center ${className}`} role="status">
            <div
                className={`${sizeClasses[size] || sizeClasses.md} animate-spin rounded-full border-primary/20 border-t-primary`}
            />
            {label && <span className="sr-only">{label}</span>}
        </div>
    );
};

export default LoadingSpinner;
