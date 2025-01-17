import {defineStore} from "pinia";

export const useParamStore = defineStore('param',() =>{

    const map = new Map<string, any>();
    const set = (key: string, value: any) => {
        map.set(key, value);
    }

    const getAndRemove = (key: string) => {
        const value = map.get(key);
        map.delete(key);
        return value;
    }
    return {
        set,
        get: getAndRemove
    }
}, {
    persist: false
})