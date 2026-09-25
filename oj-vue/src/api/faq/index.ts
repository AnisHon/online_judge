import {del, get, post, put} from "@/utils/http.ts";
import type {IdType} from "@/api/common.ts";

export interface Faq {
    faqId: IdType;
    question: string;
    answer: string;
}

export interface FaqDto {
    faqId?: IdType;
    question: string;
    answer: string;
}

export const listFaq = async (): Promise<Faq[]> => {
    const {data} = await get<Faq[]>("/content-api/faq/list");
    return data || [];
}

/** Backend read endpoint is permission-checked separately from public FAQ reading. */
export const listAdminFaq = async (): Promise<Faq[]> => {
    const {data} = await get<Faq[]>("/content-api/faq/admin/list");
    return data || [];
}

export const addFaq = async (faq: FaqDto): Promise<boolean> => {
    const {data} = await post<FaqDto, boolean>("/content-api/faq", faq);
    return data === true;
}

export const updateFaq = async (faq: Faq): Promise<boolean> => {
    const {data} = await put<Faq, boolean>("/content-api/faq", faq);
    return data === true;
}

export const removeFaq = async (ids: IdType | IdType[]): Promise<boolean> => {
    const {data} = await del<boolean>("/content-api/faq", ids);
    return data === true;
}
