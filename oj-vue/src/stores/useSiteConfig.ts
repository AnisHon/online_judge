import {defineStore} from 'pinia'
import {getSiteConfig, type SiteConfig} from '@/api/siteConfig'

const defaultSiteConfig: SiteConfig = {
    siteName: '延拓Code',
    icpNumber: '津ICP备2025028121号-1',
}

/**
 * 站点配置只保存在当前页面生命周期内。
 * 不设置 persist，避免把站点配置写入 localStorage 或 cookie。
 */
export const useSiteConfig = defineStore('site-config', {
    state: () => ({
        config: {...defaultSiteConfig} as SiteConfig,
        loaded: false,
        loading: false,
    }),
    actions: {
        async load(force = false): Promise<SiteConfig> {
            if (this.loading) return this.config
            if (this.loaded && !force) return this.config

            this.loading = true
            try {
                const remoteConfig = await getSiteConfig()
                this.config = {
                    ...defaultSiteConfig,
                    ...remoteConfig,
                }
            } catch (_) {
                // 配置服务不可用时继续使用内置默认值，不阻塞应用启动。
            } finally {
                this.loaded = true
                this.loading = false
            }
            return this.config
        },
    },
})

export default useSiteConfig
