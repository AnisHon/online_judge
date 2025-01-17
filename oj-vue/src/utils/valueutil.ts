export const isNullObj = (value: any) => {
    if (value === 0) {
        return false;
    }
    return value === null
        || value === undefined
        || typeof value === 'undefined'
        || isNaN(value)
}