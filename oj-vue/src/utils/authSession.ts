import axios from 'axios'
import {useToken} from '@/stores/useToken'

interface AjaxResult<T> {
    code: number
    message?: string
    data?: T
}

interface AccessTokenPayload {
    accessToken?: string
    expiresIn?: number
}

export class SessionChangedError extends Error {
    constructor() {
        super('会话已切换')
        this.name = 'SessionChangedError'
    }
}

const refreshClient = axios.create({
    baseURL: '/api',
    timeout: 60000,
    withCredentials: true
})

let refreshing: {generation: number; promise: Promise<string>} | null = null
let initializing: {generation: number; promise: Promise<boolean>} | null = null

/**
 * One refresh request per in-memory session generation. The browser supplies
 * the HttpOnly cookie automatically; JavaScript never receives its value.
 */
export const refreshAccessToken = (generation = useToken().getSessionVersion()): Promise<string> => {
    const tokenStore = useToken()
    if (tokenStore.getSessionVersion() !== generation) {
        return Promise.reject(new SessionChangedError())
    }
    if (refreshing?.generation === generation) return refreshing.promise

    const promise = refreshClient.post<AjaxResult<AccessTokenPayload>>('/user-api/auth/refresh')
        .then(response => {
            const result = response.data
            const accessToken = result.data?.accessToken
            if (result.code !== 200 || !accessToken) {
                throw new Error(result.message || '刷新登录状态失败')
            }
            if (tokenStore.getSessionVersion() !== generation || tokenStore.authStatus === 'unauthenticated') {
                throw new SessionChangedError()
            }
            tokenStore.setAccessToken(accessToken)
            return accessToken
        })

    refreshing = {generation, promise}
    promise.finally(() => {
        if (refreshing?.promise === promise) refreshing = null
    }).catch(() => undefined)
    return promise
}

/** Restores the current Tab after a page reload without persisting Access Token. */
export const ensureAuthInitialized = (): Promise<boolean> => {
    const tokenStore = useToken()
    if (tokenStore.authStatus === 'authenticated' && tokenStore.hasToken()) return Promise.resolve(true)
    const currentGeneration = tokenStore.getSessionVersion()
    if (initializing?.generation === currentGeneration) return initializing.promise

    tokenStore.markInitializing()
    const generation = tokenStore.getSessionVersion()
    const promise = refreshAccessToken(generation)
        .then(() => true)
        .catch(() => {
            if (tokenStore.getSessionVersion() === generation) tokenStore.markUnauthenticated()
            return false
        })
        .finally(() => {
            if (initializing?.promise === promise) initializing = null
        })
    initializing = {generation, promise}
    return promise
}
