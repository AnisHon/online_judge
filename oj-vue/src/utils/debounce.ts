function funcForwarding<T extends (...args: any[]) => any>(fn: T, ...args: Parameters<T>): ReturnType<T> {
    return fn(...args);
}

function debounce(func: Function, wait: number) {
    let timeout: number = wait;
    return function() {
        clearTimeout(timeout);
        timeout = setTimeout(() => func(), wait)
    };
}

export  {
    debounce,
}