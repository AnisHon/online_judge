import {addResultNotify, baseURL, del, get, getFormData, removeResultNotify, service} from "@/utils/http.ts";
import type {OjCase} from "@/api/problem/index.ts";
import type {IdType} from "@/api/common.ts";
import axios from "axios";
import {useToken} from "@/stores/useToken.ts";
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
    axios.get('/problem-api/problem/download', {
        baseURL: baseURL,
        params: {
            path: path
        },
        headers: {
            token: useToken().token
        },
        responseType: 'blob'
    }).then((res) => {
        console.log(res)
        const isBlob = blobValidate(res.data);
        if (isBlob) {
            const blob = new Blob([res.data])
            saveAs(blob, path)
        } else {
            ElNotification.error(res.data.message)
        }
    })
}

