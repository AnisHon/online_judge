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

const useStatuesColumn = (labels: string[], status: boolean[]) => {
    const columns= reactive<ColumnType[]>([]);

    for (let [index, item] of labels.entries()) {
        columns.push({
            key: index,
            label: item,
            visible: index < status.length ? status[index] : true
        });
    }

    return {columns}
}

export {
    useColumn,
    useStatuesColumn
}

export type {
    ColumnType,

}