import type {ValidateFieldsError} from "async-validator";
import {ElNotification} from "element-plus";

export const notifyValidate = (validRecord?: ValidateFieldsError) => {
    if (validRecord) {
        const keys = Object.keys(validRecord);
        for (const key of keys) {
            const element = validRecord[key];
            if (element.length) {
                console.log(key, element);
                ElNotification.warning({title: "提交失败", message: element[0]?.message})
                return;
            }
        }
    }
}