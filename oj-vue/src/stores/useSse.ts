import {defineStore} from "pinia";
import {ref} from "vue";
import mitt, {type Handler} from "mitt";
import {baseURL} from "@/utils/http.ts";
import {EventSourcePolyfill} from "event-source-polyfill";
import {useToken} from "@/stores/useToken.ts";
export enum SseEvent {
    UPDATE_POINT = "update-point"
}
export const useSse = defineStore('sse', () => {


    const sseMap = new Map<String, EventSourcePolyfill>;
    const emitter = ref(mitt())
    const initSSE = (url: string) => {
        const sse = sseMap.get(url);
        if (sse) {
            return sse;
        }

        const token = useToken();

        const eventSource = new EventSourcePolyfill(baseURL + "/" + url, {
            headers: {
                'token': token.token
            }
        });

        eventSource.onmessage = (event) => {
            emitter.value.emit(event.lastEventId, JSON.parse(event.data))
        };

        eventSource.onerror = (error) => {
            console.error("SSE 连接出错：", error);
            eventSource.close();
        };
        sseMap.set(url, eventSource);
        return eventSource;
    }


    const on = <T> (event: string, callback: Handler<T>) => {
        //@ts-ignore
        emitter.value.on(event, callback)
    }

    const close = (url: string) => {
        const sse = sseMap.get(url);
        if (sse) {
            sse.close();
            sseMap.delete(url);
        }
    }

    return {
        initSSE,
        on,
        close
    }

})