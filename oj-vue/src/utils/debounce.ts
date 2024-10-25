import NProgress from 'nprogress'
import 'nprogress/nprogress.css'

NProgress.configure({
    parent: '#main-box'
})

function funcForwarding<T extends (...args: any[]) => any>(fn: T, ...args: Parameters<T>): ReturnType<T> {
    return fn(...args);
}

function debounce(func: Function, wait: number, loadingStatue: boolean = true) {
    let timeout: number = wait;
    return function() {
        if (loadingStatue) {
            NProgress.start();
        }

        clearTimeout(timeout);
        timeout = setTimeout(() => {
            if (loadingStatue) {
                NProgress.done();
            }
            func();
        }, wait)
    };
}

export  {
    debounce,
}