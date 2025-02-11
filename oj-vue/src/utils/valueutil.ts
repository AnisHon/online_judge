// 我到现在搞不明白JS弄这么多空类型干什么，我也不是专业写前端的，我只知道其他语言间接且没有冲突，JS可以考虑和《异形》联名，做它的吉祥物吗？
/**
 * IsNaN null undefined -> true
 * 0 "" -> false
 * @param value
 */
export const isNullObj = (value: any) => {
    if (value === 0) {
        return false;
    }
    return value === null
        || value === undefined
        || typeof value === 'undefined'
        || isNaN(value)
}