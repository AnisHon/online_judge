import {defineStore} from "pinia";
import {ref} from "vue";
import mitt from "mitt";

export const useMitt = defineStore('mitt',() =>{

    const emitter = ref(mitt())
    const get = () => {
        return emitter.value;
    }
    return {
        get
    }
}, {
    persist: false
})