import api from './api';
import type { MobileConfigResponse } from '@/types/config';

export async function fetchMobileConfig(): Promise<MobileConfigResponse> {
    const { data } = await api.get<MobileConfigResponse>('/config/mobile');
    return data;
}
