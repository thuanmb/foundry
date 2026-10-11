import React from "react";
import ReactDOM from "react-dom/client";
import { Provider } from "react-redux";
import { HelmetProvider } from "react-helmet-async";
import store from "./redux/store";
import App from "./App";
import ErrorBoundary from "./components/ErrorBoundary";
import "./i18n/i18n";
import "./index.css";

const root = document.getElementById("root");
if (!root) throw new Error("Root element not found");

ReactDOM.createRoot(root).render(
    <React.StrictMode>
        <ErrorBoundary>
            <HelmetProvider>
                <Provider store={store}>
                    <App />
                </Provider>
            </HelmetProvider>
        </ErrorBoundary>
    </React.StrictMode>
);
