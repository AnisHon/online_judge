import {get} from '@/utils/http'

export interface SiteConfig {
    siteName: string;
    icpNumber: string;
}

export async function getSiteConfig(): Promise<SiteConfig> {
    const {data} = await get<SiteConfig>('/content-api/info/config')
    return data
}
