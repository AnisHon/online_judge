import {defineStore} from "pinia";
import {ref} from "vue";
import __ from "lodash";
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

    const set = (newTabs: TabStoreType[]) => {
        tabs.value = newTabs;
    }

    const setCurrentTab = (current: string) => {
        currentTab.value = current;
    }

    const indexOf = (name: string) => {
        return __.findIndex(tabs.value, tab => tab.name === name);
    }

    const removeTab = (targetName: string) => {
        const tabs_ = tabs.value;
        let activeName = currentTab.value
        if (activeName === targetName) {
            tabs_.forEach((tab, index) => {
                if (tab.name === targetName) {
                    const nextTab = tabs_[index + 1] || tabs_[index - 1]
                    if (nextTab) {
                        activeName = nextTab.name
                    }
                }
            })
        }

        // currentTab.value = activeName
        tabs.value = tabs_.filter((tab) => tab.name !== targetName);
        return activeName;
    }

    const getUrl = (name: string) => {
        const idx = indexOf(name);
        if (idx === -1) {
            return undefined;
        }

        return tabs.value[idx].url;

    }

    const open = (name: string, title: string, url: string) => {
        const find = router.getRoutes().find((route) => route.name === name);
        const index = indexOf(name);
        if (index === -1) {
            tabs.value.push({
                name: name,
                title: title,
                closable: true,
                component: <string>find?.meta?.component,
                url: url
            })
        } else {
            tabs.value[index].url = url;
        }

        currentTab.value = name;
    }

    const getCurrentTab = () => {
        return currentTab;
    }

    const getTabs = () => {
        return tabs;
    }

    return {
        removeTab,
        open,
        set,
        setCurrentTab,
        getCurrentTab,
        getTabs,
        getUrl
    }
})