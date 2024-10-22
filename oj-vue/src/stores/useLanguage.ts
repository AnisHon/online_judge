import {getLanguages as getLanguagesApi, type LanguageView} from "@/api/language";
import {defineStore} from "pinia";
import {ref} from "vue";

// 编程语言 不是 国际化

export default defineStore("languages", () => {
    const languages = ref<LanguageView[]>([])

    const isEmpty = () => {
        return languages.value === undefined || languages.value.length === 0;
    }

    const getLanguages = async () => {
        if (isEmpty()) {
            languages.value = await getLanguagesApi()
            languages.value.sort((a, b) => a.seq - b.seq)
        }
        return languages.value
    }


    return {
        languages,
        getLanguages
    }

})

