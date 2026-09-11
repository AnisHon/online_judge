import {ref} from "vue";
import mitt, {type Handler} from "mitt";
import {baseURL, del} from "@/utils/http.ts";
import {EventSourcePolyfill} from "event-source-polyfill";
import {useToken} from "@/stores/useToken.ts";

export enum SseEvent {
    UPDATE_POINT = "update-point",
    PING = "ping",
    UPDATE_JUDGE_STATE = "update-judge-state",
    SET_UUID = "set-uuid",
}

type SseConnection = EventSourcePolyfill & { _ojReconnect?: ReturnType<typeof setTimeout> };
const connections = new Map<string, SseConnection>();
const reconnectAttempts = new Map<string, number>();
const reconnectTimers = new Map<string, ReturnType<typeof setTimeout>>();
const emitter = ref(mitt());
const uuid = ref<string>();

export const SSE_URL = "/content-api/sse";
export const CLOSE_SSE_URL = "/sse/close";

export const initSSE = (url: string = SSE_URL) => {
    if (!useToken().token) return undefined;
    const existing = connections.get(url);
    if (existing) return existing;
    return connectSse(url);
};

export const getUuid = () => uuid.value;

const connectSse = (url: string): SseConnection => {
    const token = useToken().token;
    const query = uuid.value ? `?uuid=${encodeURIComponent(uuid.value)}` : "?uuid=";
    const eventSource = new EventSourcePolyfill(`${baseURL}/${url}${query}`, {
        headers: {token}
    }) as SseConnection;

    connections.set(url, eventSource);
    eventSource.onmessage = event => {
        try { emitter.value.emit(event.lastEventId, JSON.parse(event.data)); }
        catch (_) { /* 忽略无法解析的心跳数据 */ }
    };
    eventSource.onopen = () => reconnectAttempts.delete(url);
    eventSource.onerror = () => {
        if (connections.get(url) !== eventSource) return;
        eventSource.close();
        connections.delete(url);
        scheduleReconnect(url);
    };
    return eventSource;
};

const scheduleReconnect = (url: string) => {
    if (!useToken().token || connections.has(url) || reconnectTimers.has(url)) return;
    const attempt = (reconnectAttempts.get(url) || 0) + 1;
    reconnectAttempts.set(url, attempt);
    const delay = Math.min(30_000, 1_000 * 2 ** Math.min(attempt - 1, 5));
    const timer = setTimeout(() => {
        reconnectTimers.delete(url);
        if (useToken().token && !connections.has(url)) connectSse(url);
    }, delay);
    reconnectTimers.set(url, timer);
};

export const onSse = <T>(event: string, callback: Handler<T>) => emitter.value.on(event, callback as Handler<unknown>);
export const offSse = <T>(event: string, callback: Handler<T>) => emitter.value.off(event, callback as Handler<unknown>);

export const closeSse = (url: string = SSE_URL, notifyServer = true) => {
    const sse = connections.get(url);
    if (sse) {
        sse.close();
        connections.delete(url);
    }
    const timer = reconnectTimers.get(url);
    if (timer) clearTimeout(timer);
    reconnectTimers.delete(url);
    reconnectAttempts.delete(url);
    if (notifyServer && uuid.value && useToken().token) {
        void del<string>(CLOSE_SSE_URL, uuid.value).catch(() => undefined);
    }
};

export const closeAllSse = () => {
    for (const url of connections.keys()) closeSse(url, false);
};

window.addEventListener('beforeunload', () => closeAllSse());

onSse(SseEvent.SET_UUID, (str: string) => { uuid.value = str; });

export const isSseConnected = (url: string = SSE_URL) =>
    connections.get(url)?.readyState === EventSourcePolyfill.OPEN;
