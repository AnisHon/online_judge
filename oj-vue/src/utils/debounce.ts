



function funcForwarding<T extends (...args: any[]) => any>(fn: T, ...args: Parameters<T>): ReturnType<T> {
    return fn(...args);
}

function debounce(func: Function, wait: number, loadingStatue: boolean = true) {

    let timeout: NodeJS.Timeout;
    return function() {


        clearTimeout(timeout);
        timeout = setTimeout(() => func(), wait)
    };
}

export  {
    debounce,
}