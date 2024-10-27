import {useMenuStore} from "@/stores/useMenuStore";
import type {DirectiveBinding} from "vue";

const has = {


    mounted(el: any, binding: DirectiveBinding<string, string, string>) {
        const requiredPerms = binding.value
        const menu = useMenuStore()
        const result = menu.getAuths().some((v) => v.perms === requiredPerms) || false
        if (!result) {
            el.parentNode.removeChild(el);
        }

    }

}

export default has;