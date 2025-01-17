import {get} from "@/utils/http";
import type {IdType} from "@/api/common.ts";

interface LanguageView {
    gmtCreate: Date;
    languageId: IdType;
    languageName: string;
    seq: number;
    compileCommand: string;
}

async function getLanguages(): Promise<LanguageView[]> {
    const {data} = await get<LanguageView[], undefined>("/problem-api/language/list");
    return data
}

export {
    type LanguageView,
    getLanguages
}