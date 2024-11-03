import {fetch} from "@/utils/simpleCRUD";
import {get, type successCallback} from "@/utils/http";
import type {ProblemInListView} from "@/api/list/index";
import useLoading from "@/hooks/useLoading";
import {debounce} from "lodash";


const contestProblems = async (listId: number) => {
    const {data} = await get<ProblemInListView[], number>("problem-api/contest/problems", listId);
    return data;
}

const debouncedGetProblems = (success: successCallback<ProblemInListView[]>) => {
    const {loading, isLoading, finish} = useLoading()
    const get = debounce((x) => {
        contestProblems(x)
            .then(success)
            .finally(finish);
    }, 500);
    return {loading, isLoading, get};
}

export {
    debouncedGetProblems,
}
