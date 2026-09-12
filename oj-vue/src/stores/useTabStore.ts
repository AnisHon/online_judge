import {defineStore} from "pinia";
import {ref} from "vue";
import router from "@/router";

export interface TabStoreType {
    title: string;
    name: string;
    closable: boolean;
    component: string;
    url: string;
}

export const useTabStore = defineStore("tabStore", () => {

    const tabs = ref<TabStoreType[]>([{
        title: '首页',
        name: 'backend-index',
        closable: false,
        component: 'Index',
        url: "/backend"
    }]);

    const currentTab = ref("backend-index");

    const indexOf = (name: string) => tabs.value.findIndex(tab => tab.name === name);

    const removeTab = (targetName: string) => {
        const targetIndex = indexOf(targetName);
        if (targetIndex === -1) return currentTab.value;

        let activeName = currentTab.value;
        if (activeName === targetName) {
            const nextTab = tabs.value[targetIndex + 1] || tabs.value[targetIndex - 1];
            if (nextTab) activeName = nextTab.name;
        }

        tabs.value.splice(targetIndex, 1);
        currentTab.value = activeName;
        return activeName;
    }

    const getUrl = (name: string) => {
        const idx = indexOf(name);
        if (idx === -1) {
            return undefined;
        }

        return tabs.value[idx]?.url;

    }

    const open = (name: string, title: string, url: string) => {
        const matchedRoute = router.getRoutes().find(route => route.name === name);
        const index = indexOf(name);
        if (index === -1) {
            tabs.value.push({
                name: name,
                title: title,
                closable: true,
                component: String(matchedRoute?.meta?.component || ''),
                url: url
            })
        } else {
            tabs.value[index].url = url;
        }

        currentTab.value = name;
    }

    return {
        tabs,
        currentTab,
        removeTab,
        open,
        getUrl
    }
})
