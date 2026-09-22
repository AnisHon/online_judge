import {get, type successCallback} from "@/utils/http";
import type {ProblemInListView} from "@/api/list/index";
import useLoading from "@/hooks/useLoading";
import {debounce} from "lodash";
import type {IdType} from "@/api/common.ts";


const getContestProblems = async (listId: IdType) => {
    const {data} = await get<ProblemInListView[], IdType>("/problem-api/contest/problems", listId);
    return data;
}

const debouncedGetProblems = (success: successCallback<ProblemInListView[]>, failure?: (error: unknown) => void) => {
    const {loading, isLoading, finish} = useLoading()
    const get = debounce((x) => {
        getContestProblems(x)
            .then(success)
            .catch(error => failure?.(error))
            .finally(finish);
    }, 500);
    return {loading, isLoading, get};
}


export {
    getContestProblems,
    debouncedGetProblems,
}
