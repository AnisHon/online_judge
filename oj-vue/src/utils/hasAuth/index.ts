import {useMenuStore} from "@/stores/useMenuStore";
import type {DirectiveBinding} from "vue";

const has = {


    mounted(el: any, binding: DirectiveBinding<string | string[], string, string>) {



        const requiredPerms = binding.value
        const menu = useMenuStore()
        let result = true;
        if (requiredPerms instanceof Array) {
            requiredPerms.forEach((value) => {

                result = menu.getAuths().some((v) => v.perms === value) && result
            })
        } else {

            result = menu.getAuths().some((v) => v.perms === requiredPerms) || false

        }

        if (!result) {

            el.parentNode.removeChild(el);
        }
    }

}

const hasAny = {


    mounted(el: any, binding: DirectiveBinding<string | string[], string, string>) {



        const requiredPerms = binding.value
        const menu = useMenuStore()
        let result = false;
        if (requiredPerms instanceof Array) {
            requiredPerms.forEach((value) => {
                result = menu.getAuths().some((v) => v.perms === value) || result
            })
        } else {

            result = menu.getAuths().some((v) => v.perms === requiredPerms) || false

        }


        if (!result) {
            el.parentNode.removeChild(el);
        }
    }

}

export {
    has,
    hasAny
};