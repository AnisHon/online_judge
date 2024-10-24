import {ref} from "vue";

const useLoading = (initStatus: boolean = false) => {
    const isLoading = ref(initStatus);
    const loading = () => {
        isLoading.value = true;
    };
    const finish = () => {
        isLoading.value = false;
    }
    return {
        isLoading,
        loading,
        finish
    }
}

export default useLoading;

