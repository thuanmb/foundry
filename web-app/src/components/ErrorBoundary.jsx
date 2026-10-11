import { Component } from "react";
import { withTranslation } from "react-i18next";

class ErrorBoundary extends Component {
    constructor(props) {
        super(props);
        this.state = { hasError: false };
    }

    static getDerivedStateFromError() {
        return { hasError: true };
    }

    componentDidCatch(error, errorInfo) {
        console.error("ErrorBoundary caught:", error, errorInfo);
    }

    render() {
        const { t, fallback, children } = this.props;

        if (!this.state.hasError) return children;
        if (fallback) return fallback;

        return (
            <div role="alert" className="flex min-h-screen items-center justify-center bg-background px-4">
                <div className="max-w-md rounded-xl border border-border bg-surface p-8 text-center shadow-sm">
                    <h2 className="mb-3 text-xl font-semibold text-on-surface">{t("errors.boundaryTitle")}</h2>
                    <p className="mb-6 text-muted">{t("errors.boundaryMessage")}</p>
                    <button
                        type="button"
                        onClick={() => {
                            this.setState({ hasError: false });
                            window.location.href = "/";
                        }}
                        className="rounded-lg bg-primary px-4 py-2 font-medium text-on-primary hover:bg-primary-hover"
                    >
                        {t("errors.backHome")}
                    </button>
                </div>
            </div>
        );
    }
}

export default withTranslation()(ErrorBoundary);
