import {addResultNotify, binaryService, del, get, getFormData, removeResultNotify, service} from "@/utils/http.ts";
import type {OjCase} from "@/api/problem/index.ts";
import type {IdType} from "@/api/common.ts";
import {ElNotification} from "element-plus";
import {blobValidate} from "@/api/file";
import {saveAs} from "file-saver";

export interface OjCaseView {
    caseId: IdType,
    input: string,
    inputSize: number,
    output: string,
    outputSize: number,
    score: number
}

export const addCase = async (problemCase: OjCase) => {
    const formData = getFormData(problemCase);
    const {data} = await service<boolean>({
        method: "POST",
        url: "/problem-api/problem/case",
        data: formData
    });
    addResultNotify(data);
}


export const listCase = async (problemId: IdType) => {
    const {data} = await get<OjCaseView[]>("/problem-api/problem/case", problemId);
    return data;
}

export const removeCase = async (caseId: IdType | IdType[]) => {
    const {data} = await del<boolean>("/problem-api/problem/case", caseId);
    removeResultNotify(data);
}

export const downloadCase = async (path: string) => {
    try {
        const response = await binaryService.get('/problem-api/problem/download', {
            params: {path},
            responseType: 'blob'
        });
        if (blobValidate(response.data)) {
            saveAs(response.data, path)
        } else {
            ElNotification.error('测试用例下载失败，请稍后重试');
        }
    } catch (_) {
        ElNotification.error('测试用例下载失败，请稍后重试');
    }
}
