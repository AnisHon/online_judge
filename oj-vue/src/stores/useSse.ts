import {defineStore} from "pinia";
import {ref} from "vue";
import mitt, {type Handler} from "mitt";
import {baseURL} from "@/utils/http.ts";
import {EventSourcePolyfill} from "event-source-polyfill";
import {useToken} from "@/stores/useToken.ts";
import {getCookie} from "@/utils/cookie.ts";
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

        let url2 = `${baseURL}/${url}?uuid=`


        const eventSource = new EventSourcePolyfill(url2, {
            headers: {
                'token': token.token
            }
        });

        eventSource.onmessage = (event) => {
            emitter.value.emit(event.lastEventId, JSON.parse(event.data))
        };

        eventSource.onerror = (error) => {
            console.error("SSE 连接出错：", error);
            reconnect(eventSource.url)
        };
        sseMap.set(url2, eventSource);

        return eventSource;
    }

    const reconnect = (url: string) => {

        const uuid = getCookie("sse-uuid");
        const sse = sseMap.get(url);
        if (sse) {
            return sse;
        }

        const token = useToken();

        let url2: string;
        if (uuid) {
            url2 = `${url}?uuid=${uuid}`
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
            console.error("SSE 连接出错：", error);
            const url1 = eventSource.url;
            reconnect(url1)
        };
        sseMap.set(url2, eventSource);
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