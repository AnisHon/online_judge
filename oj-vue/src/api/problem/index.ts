import {post} from "@/utils/http"


export interface ProblemParam {
    currentPage: number;
    pageSize: number;
    problemId: number | null;
    tagIds: number[] | null;
}



async function getProblems(problemParam: ProblemParam) {
    const {data} = await post("/problem-api/problem/list", problemParam)
    return data;
}


export {
    getProblems,
}