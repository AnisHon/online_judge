<template>
  <div class="enhanced-editor">
    <div ref="toolbarRef" class="editor-toolbar">
      <div class="editor-toolbar__left">
        <div class="editor-toolbar__label"><span class="editor-toolbar__eyebrow">LANGUAGE</span><strong>{{ currLang || '选择语言' }}</strong></div>
        <div class="language-picker" role="tablist" aria-label="选择编程语言">
          <button
              v-for="item in languages || []"
              :key="item.languageId"
              class="language-pill"
              :class="{ 'language-pill--active': item.languageId === codeForm.languageId }"
              type="button"
              @click="codeForm.languageId = item.languageId"
          >
            {{ item.languageName }}
          </button>
        </div>
      </div>
      <div class="editor-toolbar__actions">
        <el-button text :disabled="disableSubmit" @click="emit('test')" :loading="loading">
          <el-icon><VideoPlay /></el-icon><span>运行测试</span>
        </el-button>
        <el-button type="primary" :disabled="disableSubmit" @click="emit('submit')" :loading="loading">
          <el-icon><Promotion /></el-icon><span>提交代码</span>
        </el-button>
        <el-button text @click="emit('open-log')">
          <el-icon><List /></el-icon><span>记录</span>
        </el-button>
      </div>
    </div>
    <code-editor v-model="codeForm.code" :language="currLang" :theme="theme" :height="codeEditHeight" ref="codeEditorRef" />


  </div>
</template>

<script setup lang="ts">
import CodeEditor from "@/components/EnhancedCodeEdior/CodeEditor/CodeEditor.vue";
import {computed, onMounted, ref, watch} from "vue";
import type {JudgeForm} from "@/api/problem/judge";
import useLanguage from "@/stores/useLanguage";
import type {LanguageView} from "@/api/language";
import {List, Promotion, VideoPlay} from "@element-plus/icons-vue";

const languageStore = useLanguage();

const codeEditorRef = ref<InstanceType<typeof CodeEditor> | null>(null);

const toolbarRef = ref<HTMLElement>();

const codeForm = defineModel<JudgeForm>({required: true})

const theme = ref("eclipse");

const currLang = ref<string>("");

const languages = ref<LanguageView[] | null>(null);

const {heightProp, loading, disableSubmit = false} = defineProps<{heightProp: number, loading: boolean, disableSubmit?: boolean}>()

const emit = defineEmits<{
  (e: 'open-log'): void,
  (e: 'submit'): void;
  (e: 'test'): void;
  (e: 'fullScreen'): void;
  (e: 'onReady'): void;
}>()

const codeEditHeight = computed(() => {
  return Math.max(240, heightProp - (toolbarRef.value?.offsetHeight || 56) - 10);
})


const onHandleFullScreen = () => {
  emit('fullScreen');
};

const setLanguage = () => {
  languages.value?.forEach((item) => {
    if (item.languageId === codeForm.value.languageId) {
      currLang.value = item.languageName;
    }
  })
}

const initLanguages = () => {
  languageStore.getLanguages()
      .then((languageArray) => {
        languages.value = languageArray;
        setLanguage();
      })
}

watch(() => codeForm.value.languageId, () => {
  setLanguage();
})

onMounted(() => {
  emit("onReady")
})

// created
initLanguages();




</script>

<style scoped>
.enhanced-editor { display: flex; min-height: 0; flex: 1; flex-direction: column; }
.editor-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 14px; min-height: 54px; padding: 0 4px 10px; }
.editor-toolbar__left, .editor-toolbar__actions, .language-picker { display: flex; align-items: center; }
.editor-toolbar__left { min-width: 0; gap: 14px; }
.editor-toolbar__label { min-width: 72px; }
.editor-toolbar__label span, .editor-toolbar__label strong { display: block; }
.editor-toolbar__eyebrow { color: var(--el-text-color-secondary); font-size: 9px; font-weight: 800; letter-spacing: .12em; }
.editor-toolbar__label strong { margin-top: 3px; color: var(--el-text-color-primary); font-size: 12px; }
.language-picker { max-width: min(46vw, 480px); gap: 5px; overflow-x: auto; padding-bottom: 2px; }
.language-pill { flex: 0 0 auto; padding: 6px 10px; border: 1px solid var(--el-border-color-light); border-radius: 8px; background: var(--el-fill-color-lighter); color: var(--el-text-color-secondary); cursor: pointer; font: inherit; font-size: 11px; transition: .2s ease; }
.language-pill:hover { border-color: var(--el-color-primary-light-5); color: var(--el-color-primary); }
.language-pill--active { border-color: var(--el-color-primary); background: var(--el-color-primary-light-9); color: var(--el-color-primary); font-weight: 700; }
.editor-toolbar__actions { flex: 0 0 auto; gap: 2px; }
.editor-toolbar__actions .el-button { margin: 0; }
.editor-toolbar__actions .el-button + .el-button { margin-left: 0; }
@media (max-width: 900px) { .editor-toolbar { align-items: flex-start; flex-direction: column; }.editor-toolbar__left, .language-picker { width: 100%; max-width: none; }.editor-toolbar__actions { width: 100%; justify-content: flex-end; } }

</style>
