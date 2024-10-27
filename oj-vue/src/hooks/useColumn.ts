import {reactive} from "vue";

interface ColumnType {
    key: number,
    label: string,
    visible: boolean,
}

const useColumn = (labels: string[]) => {
    const columns= reactive<ColumnType[]>([]);

    for (let [index, item] of labels.entries()) {
        columns.push({key: index, label: item, visible: true});
    }

    return {columns}
}

export {
    useColumn
}

export type {
    ColumnType
}