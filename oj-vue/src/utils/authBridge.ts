const CHANNEL_NAME = 'oj-auth-state-v1'

export type AuthStateEvent = {
    type: 'LOGOUT' | 'SESSION_CHANGED'
    eventId: string
}

let channel: BroadcastChannel | null = null
let started = false

const createEventId = () => {
    if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') return crypto.randomUUID()
    return `${Date.now()}-${Math.random().toString(36).slice(2)}`
}

const getChannel = () => {
    if (typeof window === 'undefined' || !('BroadcastChannel' in window)) return null
    if (!channel) channel = new BroadcastChannel(CHANNEL_NAME)
    return channel
}

/** Only session state is broadcast. No credential is ever included in this message. */
export const broadcastAuthState = (type: AuthStateEvent['type']) => {
    getChannel()?.postMessage({type, eventId: createEventId()} satisfies AuthStateEvent)
}

export const startAuthStateBridge = (onLogout: () => void) => {
    const bridge = getChannel()
    if (started || !bridge) return
    started = true
    bridge.addEventListener('message', (event: MessageEvent<AuthStateEvent>) => {
        const message = event.data
        if (!message || (message.type !== 'LOGOUT' && message.type !== 'SESSION_CHANGED')) return
        if (message.type === 'LOGOUT') onLogout()
    })
}
