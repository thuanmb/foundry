import type { RootState } from './store';

// Auth
export const selectAuth = (state: RootState) => state.auth;
export const selectIsAuthenticated = (state: RootState) => state.auth.isAuthenticated;
export const selectAccessToken = (state: RootState) => state.auth.accessToken;
export const selectRefreshToken = (state: RootState) => state.auth.refreshToken;
export const selectUser = (state: RootState) => state.auth.user;
export const selectIsNewUser = (state: RootState) => state.auth.isNewUser;
export const selectSendOtpStatus = (state: RootState) => state.auth.sendOtpStatus;
export const selectVerifyOtpStatus = (state: RootState) => state.auth.verifyOtpStatus;
export const selectVerifyOtpError = (state: RootState) => state.auth.verifyOtpError;
export const selectFirebaseAuthStatus = (state: RootState) => state.auth.firebaseAuthStatus;
export const selectFirebaseAuthError = (state: RootState) => state.auth.firebaseAuthError;
export const selectProfileStatus = (state: RootState) => state.auth.profileStatus;
export const selectProfileError = (state: RootState) => state.auth.profileError;

// Anonymous
export const selectAnonymous = (state: RootState) => state.anonymous;
export const selectJweToken = (state: RootState) => state.anonymous.jweToken;

// Config
export const selectConfig = (state: RootState) => state.config;
export const selectConfigStatus = (state: RootState) => state.config.status;
export const selectFeatures = (state: RootState) => state.config.features;
export const selectPollingConfig = (state: RootState) => state.config.pollingConfig;

// Notes
export const selectNotes = (state: RootState) => state.notes.items;
export const selectNotesState = (state: RootState) => state.notes;
export const selectNoteById = (id: string | undefined) => (state: RootState) =>
    id ? state.notes.items.find((n) => n.id === id) : undefined;

// Jobs
export const selectJobsSubmitStatus = (state: RootState) => state.jobs.submitStatus;
export const selectJobsSubmitError = (state: RootState) => state.jobs.submitError;
export const selectJobById = (jobId: string | undefined) => (state: RootState) =>
    jobId ? state.jobs.byId[jobId] : undefined;
