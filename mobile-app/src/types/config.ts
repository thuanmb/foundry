export interface MobileConfigPollingConfig {
    initialDelaySec: number;
    maxDelaySec: number;
    backoffMultiplier: number;
}

export interface MobileConfigResponse {
    features: Record<string, boolean>;
    pollingConfig?: MobileConfigPollingConfig;
    /** Alternate key name accepted for forward compatibility. */
    polling?: MobileConfigPollingConfig;
    minAppVersion?: string;
}
