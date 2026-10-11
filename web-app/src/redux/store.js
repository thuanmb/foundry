import { configureStore } from "@reduxjs/toolkit";
import tokenReducer, { hydrateToken } from "./tokenSlice";
import jobsReducer from "./jobsSlice";
import { injectStore } from "../services/api";

const store = configureStore({
    reducer: {
        token: tokenReducer,
        jobs: jobsReducer,
    },
});

store.dispatch(hydrateToken());
injectStore(store);

export default store;
