

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