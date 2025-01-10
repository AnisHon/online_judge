import {defineStore} from "pinia";
import {readonly, ref} from "vue";
import __ from "lodash";

export interface TabStoreType {
    title: string;
    name: string;
    closable: boolean;
}

export const useTabStore = defineStore("tabStore", () => {

    const tabs = ref<TabStoreType[]>([{
        title: '首页',
        name: 'backend-index',
        closable: false
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

        currentTab.value = activeName
        tabs.value = tabs_.filter((tab) => tab.name !== targetName);
    }


    const open = (name: string, title: string) => {
        const index = indexOf(name);
        if (index === -1) {
            tabs.value.push({
                name: name,
                title: title,
                closable: true
            })
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
        getTabs
    }
})