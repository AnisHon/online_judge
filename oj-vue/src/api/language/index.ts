import {get} from "@/utils/http";

interface LanguageView {
    gmtCreate: Date;
    languageId: number;
    languageName: string;
    seq: number;
    compileCommand: string;
}

async function getLanguages(): Promise<LanguageView[]> {
    const {data} = await get<LanguageView[], undefined>("/problem-api/language/list");
    return <LanguageView[]>data
}

export {
    type LanguageView,
    getLanguages
}