import {type LanguageView} from "@/api/language";
import {defineStore} from "pinia";
import {ref} from "vue";

// 编程语言 不是 国际化

defineStore("languages", () => {
    const languages = ref<LanguageView[]>([])

    const isEmpty = () => {
        return languages.value === undefined || languages.value.length === 0;
    }

    const getLanguages = () => {
        return languages.value
    }


    return {
        languages,
        getLanguages
    }

})

