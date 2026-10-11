import { configureStore, combineReducers } from '@reduxjs/toolkit';
import {
    persistStore,
    persistReducer,
    FLUSH,
    REHYDRATE,
    PAUSE,
    PERSIST,
    PURGE,
    REGISTER,
} from 'redux-persist';
import devToolsEnhancer from 'redux-devtools-expo-dev-plugin';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { secureStorage } from '@/services/secureStorage';
import authReducer from './slices/authSlice';
import anonymousReducer from './slices/anonymousSlice';
import configReducer from './slices/configSlice';
import notesReducer from './slices/notesSlice';
import jobsReducer from './slices/jobsSlice';

// Auth + Anonymous: individually persisted to secure storage (encrypted at OS level)
const authPersistConfig = { key: 'auth', storage: secureStorage };
const anonymousPersistConfig = { key: 'anonymous', storage: secureStorage };

const rootReducer = combineReducers({
    auth: persistReducer(authPersistConfig, authReducer),
    anonymous: persistReducer(anonymousPersistConfig, anonymousReducer),
    config: configReducer,
    notes: notesReducer,
    jobs: jobsReducer,
});

// Non-sensitive state that should survive restarts → AsyncStorage
const rootPersistConfig = {
    key: 'root',
    storage: AsyncStorage,
    whitelist: ['config'],
};

const persistedReducer = persistReducer(rootPersistConfig, rootReducer);

export const store = configureStore({
    reducer: persistedReducer,
    devTools: false,
    middleware: (getDefaultMiddleware) =>
        getDefaultMiddleware({
            serializableCheck: {
                ignoredActions: [FLUSH, REHYDRATE, PAUSE, PERSIST, PURGE, REGISTER],
            },
        }),
    enhancers: (getDefaultEnhancers) => getDefaultEnhancers().concat(devToolsEnhancer()),
});

export const persistor = persistStore(store);

// Start every dev session from a clean persisted state.
if (__DEV__) {
    persistor.purge();
}

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
