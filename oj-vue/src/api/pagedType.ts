interface PagedType {
    pageSize: number;
    currentPage: number;
}

interface SortedPagedType extends PagedType {
    asc: boolean;
    sortColumn?: string;
}

interface PagedResponse<T> {
    pageSize: number;
    currentPage: number;
    totalRecords: number;
    data: T[];
}

const onlyPagedData = (data: SortedPagedType): boolean => {
    const keys = Object.getOwnPropertyNames(data);
    if (keys.length > 3) {
        return false;
    }

    const includeKeys = ['pageSize', 'currentPage', 'asc']
    for (const key of keys) {
        if (!includeKeys.includes(key)) {
            return false;
        }
    }

    return true;
}

const toPagedQueryData = (data: PagedType): PagedType => {
    return {pageSize: data.pageSize, currentPage: data.currentPage};
}

export type {
    PagedType,
    SortedPagedType,
    PagedResponse,
}
export {
    onlyPagedData,
    toPagedQueryData
}