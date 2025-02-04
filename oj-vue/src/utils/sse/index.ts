import {ref} from "vue";
import mitt, {type Handler} from "mitt";
import {baseURL, del} from "@/utils/http.ts";
import {EventSourcePolyfill} from "event-source-polyfill";
import {useToken} from "@/stores/useToken.ts";

export enum SseEvent {
    UPDATE_POINT = "update-point",
    PING = "ping",
    UPDATE_JUDGE_STATE= "update-judge-state",
    SET_UUID="set-uuid",

}

const sseMap = new Map<String, EventSourcePolyfill>;
const emitter = ref(mitt())
const uuid = ref<string>();

export const SSE_URL = "/content-api/sse";
export const CLOSE_SSE_URL = "/sse/close";

export const initSSE = (url: string = SSE_URL) => {
    if (!useToken().token) {
        return undefined;
    }
    if (!sseMap.has(url)) {
        sseMap.set(url, connectSse(`${baseURL}/${url}`));
    }
    return sseMap.get(url)
}

export const getUuid = () => {
    return uuid.value;
}

const connectSse = (url: string) => {

    const id = getUuid();
    const sse = sseMap.get(url);
    if (sse) {
        return sse;
    }

    const token = useToken();

    let url2: string;
    if (id) {
        url2 = `${url}?uuid=${id}`
    } else {
        url2 = `${url}?uuid=`
    }

    const eventSource = new EventSourcePolyfill(url2, {
        headers: {
            'token': token.token
        }
    });

    eventSource.onmessage = (event) => {
        emitter.value.emit(event.lastEventId, JSON.parse(event.data))
    };

    eventSource.onerror = (error) => {
        eventSource.close();
        console.error("SSE 连接断开：", error);
        const url1 = eventSource.url;
        connectSse(url1)
    };
    eventSource.onopen = () => {
        console.log("SSE 连接成功")
    }
    sseMap.set(url2, eventSource);
    return eventSource;
}

export const onSse = <T> (event: string, callback: Handler<T>) => {
    //@ts-ignore
    emitter.value.on(event, callback)
}

export const offSse = <T> (event: string, callback: Handler<T>) => {
    //@ts-ignore
    emitter.value.off(event, callback)
}

window.addEventListener('beforeunload', () => closeSse(SSE_URL));
export const closeSse = (url: string | undefined) => {
    if (!url || !useToken().token) {
        return;
    }
    const sse = sseMap.get(url);
    if (sse) {
        sse.close();
        sseMap.delete(url);

        del<string>(CLOSE_SSE_URL, uuid.value)
    }
}

onSse(SseEvent.SET_UUID, (str: string) => {
    uuid.value = str;
})

export const isSseConnected = (url: string = SSE_URL) => {
    const readyState = sseMap.get(url)?.readyState;
    return readyState === EventSourcePolyfill.OPEN;
}