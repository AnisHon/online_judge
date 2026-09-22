import {getLanguages as getLanguagesApi, type LanguageView} from '@/api/language';
import {defineStore} from 'pinia';
import {ref} from 'vue';

export default defineStore('languages', () => {
  const languages = ref<LanguageView[]>([]);
  const loading = ref(false);
  const error = ref('');
  let request: Promise<LanguageView[]> | undefined;

  const getLanguages = async (force = false) => {
    if (!force && languages.value.length) return languages.value;
    if (!force && request) return request;
    loading.value = true;
    error.value = '';
    request = getLanguagesApi()
      .then(result => {
        languages.value = Array.isArray(result) ? [...result].sort((a, b) => a.seq - b.seq) : [];
        if (!languages.value.length) throw new Error('暂无可用编程语言');
        return languages.value;
      })
      .catch(reason => {
        error.value = reason instanceof Error ? reason.message : '编程语言加载失败';
        throw reason;
      })
      .finally(() => {
        loading.value = false;
        request = undefined;
      });
    return request;
  };

  return {languages, loading, error, getLanguages};
});
