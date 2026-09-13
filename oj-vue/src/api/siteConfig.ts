import {get} from '@/utils/http'

export interface SiteConfig {
    siteName: string;
    icpNumber: string;
    [key: string]: unknown;
}

export async function getSiteConfig(): Promise<SiteConfig> {
    const {data} = await get<SiteConfig>('/content-api/site/config')
    return data
}
