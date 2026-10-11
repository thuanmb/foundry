import { configureStore } from "@reduxjs/toolkit";
import { render } from "@testing-library/react";
import { Provider } from "react-redux";
import { HelmetProvider } from "react-helmet-async";
import { MemoryRouter, Routes, Route } from "react-router-dom";
import tokenReducer from "../redux/tokenSlice";
import jobsReducer from "../redux/jobsSlice";
import { injectStore } from "../services/api";
import "../i18n/i18n";

export function makeStore(preloadedState) {
    const store = configureStore({
        reducer: { token: tokenReducer, jobs: jobsReducer },
        preloadedState,
    });
    injectStore(store);
    return store;
}

export function renderWithProviders(ui, { route = "/", path = "/", preloadedState, store = makeStore(preloadedState) } = {}) {
    const result = render(
        <HelmetProvider>
            <Provider store={store}>
                <MemoryRouter initialEntries={[route]}>
                    <Routes>
                        <Route path={path} element={ui} />
                    </Routes>
                </MemoryRouter>
            </Provider>
        </HelmetProvider>
    );
    return { store, ...result };
}
