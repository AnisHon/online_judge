import {defineStore} from "pinia";
import {ref} from "vue";
import mitt, {type Handler} from "mitt";
import {baseURL} from "@/utils/http.ts";
import {EventSourcePolyfill} from "event-source-polyfill";
import {useToken} from "@/stores/useToken.ts";
import Cookies from 'js-cookie';

export enum SseEvent {
    UPDATE_POINT = "update-point",
    PING = "ping",
    UPDATE_JUDGE_STATE= "update-judge-state",
    SER_UUID="set-uuid",

}
export const useSse = defineStore('sse', () => {


    const sseMap = new Map<String, EventSourcePolyfill>;
    const emitter = ref(mitt())
    const uuid = ref<string | undefined>(undefined);
    const initSSE = (url: string = "/user-api/sse") => {
        return connect(`${baseURL}/${url}`);
    }

    const connect = (url: string) => {

        const uuid = getUuid();
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
            connect(url1)
        };
        sseMap.set(url2, eventSource);
        return eventSource;
    }

    const on = <T> (event: string, callback: Handler<T>) => {
        //@ts-ignore
        emitter.value.on(event, callback)
    }

    const off = <T> (event: string, callback: Handler<T>) => {
        //@ts-ignore
        emitter.value.off(event, callback)
    }

    const close = (url: string) => {
        const sse = sseMap.get(url);
        if (sse) {
            sse.close();
            sseMap.delete(url);
        }
    }

    const getUuid = () => {
        return uuid.value;
    }

    on(SseEvent.SER_UUID, (str: string) => {
        uuid.value = str;
    })
    return {
        initSSE,
        on,
        close,
        getUuid,
        off
    }

})