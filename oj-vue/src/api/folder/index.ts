import {get, type successCallback} from "@/utils/http";
import {debounce} from "lodash";
import useLoading from "@/hooks/useLoading";
import {add, remove, update} from "@/utils/simpleCRUD";
import type {IdType} from "@/api/common.ts";


enum FolderType {
    DIRECTORY = 'D',
    FILE = 'F',
    MENU = 'M'
}

interface FolderView {
    folderId: IdType;
    folderName: string;
    folderType: FolderType;
    listId: IdType;
    parentId: IdType;
}

interface FolderForm {
    folderId?: IdType;
    folderName?: string;
    folderType?: FolderType;
    listId?: IdType;
    parentId?: IdType;
}

interface TreedFolderView {
    id?: IdType;
    folder: FolderView;
    children: TreedFolderView[];
    file: boolean;
}

const getTreedFolderView = async () => {
    const {data} = await get<TreedFolderView[]>("/problem-api/folder/tree");
    return data;
}

const debouncedGetTreedFolder = (success: successCallback<TreedFolderView[]>) => {
    const {loading, isLoading, finish} = useLoading()
    const get = debounce(() => {
        getTreedFolderView()
            .then(success)
            .finally(finish);
    }, 500);
    return {loading, isLoading, get};
}


const dict = {
    folderType: [
        {label: "文件夹", value: FolderType.DIRECTORY},
        {label: "菜单栏", value: FolderType.MENU},
        {label: "文件", value: FolderType.FILE},
    ]

}

const removeFolder = async (id: IdType | IdType[]) => {
    await remove(id, "/problem-api/folder");
}

const addFolder = async (form: FolderForm) => {
    await add(form, "/problem-api/folder");
}

const debouncedAddFolder = (form: FolderForm, success: successCallback<void>) => {
    const {loading, isLoading, finish} = useLoading()
    const add = debounce(() => {
        addFolder(form)
            .then(success)
            .finally(finish);
    }, 1000);
    return {loading, isLoading, add};
}



const updateFolder = async (form: FolderForm) => {
    await update(form, "/problem-api/folder");
}

const debouncedUpdateFolder = (form: FolderForm, success: successCallback<void>) => {
    const {loading, isLoading, finish} = useLoading()
    const update = debounce(() => {
        updateFolder(form)
            .then(success)
            .finally(finish);
    }, 1000);
    return {loading, isLoading, update};
}

const getFolder = async (): Promise<FolderView[]> => {
    const {data} =  await get<FolderView[]>("/problem-api/folder");
    return data;
}

const debouncedGetFolder = (success: successCallback<FolderView[]>) => {
    const {loading, isLoading, finish} = useLoading()
    const get = debounce(() => {
        getFolder()
            .then(success)
            .finally(finish);
    }, 500
    );
    return {loading, isLoading, get};
}

export type {
    FolderForm,
    FolderView,
    TreedFolderView,

}


export {
    getFolder,
    debouncedGetFolder,
    removeFolder,
    addFolder,
    debouncedAddFolder,
    updateFolder,
    debouncedUpdateFolder,
    getTreedFolderView,
    debouncedGetTreedFolder,
    dict,
    FolderType
}



