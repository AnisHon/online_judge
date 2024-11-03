import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import {numberToLetter} from "@/utils/stringUtils";

NProgress.configure({
    parent: '#main-box'
})

function funcForwarding<T extends (...args: any[]) => any>(fn: T, ...args: Parameters<T>): ReturnType<T> {
    return fn(...args);
}

function debounce(func: Function, wait: number, loadingStatue: boolean = true) {
    let timeout: NodeJS.Timeout;
    return function() {
        if (loadingStatue) {
            try {
                NProgress.start();
            } catch (e) {
                console.log(e)
            }

        }

        clearTimeout(timeout);
        timeout = setTimeout(() => {
            func();
            if (loadingStatue) {
                try {
                    NProgress.done();
                } catch (e) {
                    console.log(e)
                }

            }

        }, wait)
    };
}

export  {
    debounce,
}