<template>
  <div class="enhanced-editor">
    <div ref="toolbarRef" class="editor-toolbar">
      <div class="editor-toolbar__left">
        <el-popover v-model:visible="languagePickerOpen" placement="bottom-start" :width="360" trigger="click">
          <template #reference>
            <button class="language-trigger" type="button" aria-label="选择编程语言">
              <span class="language-trigger__mark">{{ languageBadge }}</span>
              <span class="language-trigger__copy"><small>编程语言</small><strong>{{ currLang || '选择语言' }}</strong></span>
              <el-icon class="language-trigger__arrow"><ArrowDown /></el-icon>
            </button>
          </template>
          <div class="language-menu">
            <div class="language-menu__heading"><strong>选择编程语言</strong><span>{{ languages.length }} 种可用</span></div>
            <div v-if="languageError" class="language-state" role="alert">
              <span>{{ languageError }}</span>
              <el-button link type="primary" @click="initLanguages(true)">重试</el-button>
            </div>
            <div v-else-if="languages.length" class="language-grid" role="listbox" aria-label="编程语言列表">
              <button
                  v-for="item in languages"
                  :key="String(item.languageId)"
                  class="language-card"
                  :class="{ 'language-card--active': sameLanguage(item.languageId, codeForm.languageId) }"
                  type="button"
                  role="option"
                  :aria-selected="sameLanguage(item.languageId, codeForm.languageId)"
                  @click="selectLanguage(item.languageId)"
              >
                <span class="language-card__badge">{{ languageBadgeFor(item.languageName) }}</span>
                <span class="language-card__copy"><strong>{{ item.languageName }}</strong><small>ID {{ item.languageId }}</small></span>
                <el-icon v-if="sameLanguage(item.languageId, codeForm.languageId)" class="language-card__check"><Check /></el-icon>
              </button>
            </div>
            <el-empty v-else :image-size="42" description="正在加载语言列表" />
          </div>
        </el-popover>
      </div>
      <div class="editor-toolbar__actions">
        <el-button class="action-button action-button--test" text aria-label="运行自定义测试" :disabled="disableSubmit || !canSubmit" @click="emit('test')" :loading="testBusy">
          <el-icon><VideoPlay /></el-icon><span>运行测试</span>
        </el-button>
        <el-button class="action-button action-button--submit" type="primary" aria-label="提交代码" :disabled="disableSubmit || !canSubmit" @click="emit('submit')" :loading="submitBusy">
          <el-icon><Promotion /></el-icon><span>提交代码</span>
        </el-button>
        <el-button class="action-button action-button--log" text aria-label="查看提交记录" @click="emit('open-log')">
          <el-icon><List /></el-icon><span>记录</span>
        </el-button>
      </div>
    </div>
    <code-editor v-model="codeForm.code" :language="currLang" :height="codeEditHeight" ref="codeEditorRef" />


  </div>
</template>

<script setup lang="ts">
import CodeEditor from "@/components/EnhancedCodeEdior/CodeEditor/CodeEditor.vue";
import {computed, onMounted, ref} from "vue";
import type {JudgeForm} from "@/api/problem/judge";
import useLanguage from "@/stores/useLanguage";
import type {LanguageView} from "@/api/language";
import {ArrowDown, Check, List, Promotion, VideoPlay} from "@element-plus/icons-vue";

const languageStore = useLanguage();

const codeEditorRef = ref<InstanceType<typeof CodeEditor> | null>(null);

const toolbarRef = ref<HTMLElement>();

const codeForm = defineModel<JudgeForm>({required: true})

const languages = ref<LanguageView[]>([]);
const languagePickerOpen = ref(false);

const {heightProp, submitLoading, testLoading, loading = false, disableSubmit = false} = defineProps<{
  heightProp: number,
  submitLoading?: boolean,
  testLoading?: boolean,
  /** 兼容后台答题详情等旧调用方，新的工作台使用 submitLoading/testLoading。 */
  loading?: boolean,
  disableSubmit?: boolean
}>()

const submitBusy = computed(() => submitLoading ?? loading);
const testBusy = computed(() => testLoading ?? false);

const emit = defineEmits<{
  (e: 'open-log'): void,
  (e: 'submit'): void;
  (e: 'test'): void;
  (e: 'full-screen'): void;
  (e: 'onReady'): void;
}>()

const codeEditHeight = computed(() => {
  return Math.max(240, heightProp - (toolbarRef.value?.offsetHeight || 56) - 10);
})


const sameLanguage = (left: unknown, right: unknown) => String(left) === String(right);
const selectedLanguage = computed(() => languages.value.find(item => sameLanguage(item.languageId, codeForm.value.languageId)));
const currLang = computed(() => selectedLanguage.value?.languageName || "");
const languageError = ref('');
const canSubmit = computed(() => languages.value.length > 0 && !!selectedLanguage.value && !languageError.value);
const languageBadgeFor = (name?: string) => {
  const value = (name || "?").toLowerCase();
  if (value.includes("python")) return "Py";
  if (value.includes("java")) return "Jv";
  if (value.includes("golang") || value === "go") return "Go";
  if (value === "c") return "C";
  if (value.includes("c++")) return "C++";
  return (name || "?").slice(0, 3);
};
const languageBadge = computed(() => languageBadgeFor(currLang.value));
const selectLanguage = (languageId: LanguageView["languageId"]) => {
  codeForm.value.languageId = languageId;
  languagePickerOpen.value = false;
};

const initLanguages = async (force = false) => {
  languageError.value = '';
  try {
    const languageArray = await languageStore.getLanguages(force);
    languages.value = [...languageArray].sort((a, b) => a.seq - b.seq);
  } catch (error) {
    languageError.value = error instanceof Error ? error.message : '编程语言加载失败';
  }
}

onMounted(() => {
  emit("onReady")
})

// created
void initLanguages();




</script>

<style scoped>
.enhanced-editor { display: flex; min-height: 0; flex: 1; flex-direction: column; }
.editor-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 14px; min-height: 54px; padding: 0 4px 10px; }
.editor-toolbar__left, .editor-toolbar__actions { display: flex; align-items: center; }
.editor-toolbar__left { min-width: 0; gap: 14px; }
.language-trigger { display: flex; min-width: 148px; align-items: center; gap: 9px; padding: 6px 9px; border: 1px solid var(--el-border-color-light); border-radius: 11px; background: var(--el-bg-color); color: var(--el-text-color-primary); cursor: pointer; text-align: left; transition: border-color .18s ease, background-color .18s ease, transform .18s ease; }
.language-trigger:hover { border-color: var(--el-color-primary-light-5); background: var(--el-fill-color-light); transform: translateY(-1px); }
.language-trigger__mark, .language-card__badge { display: grid; place-items: center; border-radius: 8px; background: var(--el-color-primary-light-9); color: var(--el-color-primary); font: 800 11px/1 var(--code-font-family, monospace); }
.language-trigger__mark { width: 31px; height: 31px; flex: 0 0 auto; }
.language-trigger__copy, .language-card__copy { display: flex; min-width: 0; flex-direction: column; gap: 2px; }
.language-trigger__copy small { color: var(--el-text-color-secondary); font-size: 9px; }
.language-trigger__copy strong { overflow: hidden; max-width: 112px; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.language-trigger__arrow { margin-left: auto; color: var(--el-text-color-placeholder); font-size: 12px; }
.language-menu { max-width: calc(100vw - 24px); color: var(--el-text-color-primary); }
.language-state { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 18px 8px; color: var(--el-text-color-secondary); font-size: 12px; }
.language-menu__heading { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 10px; }.language-menu__heading strong { font-size: 13px; }.language-menu__heading span { color: var(--el-text-color-secondary); font-size: 11px; }
.language-grid { display: grid; max-height: 310px; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 7px; overflow-y: auto; padding: 2px; }
.language-card { display: flex; min-width: 0; align-items: center; gap: 8px; padding: 8px; border: 1px solid var(--el-border-color-lighter); border-radius: 10px; background: var(--el-bg-color); color: var(--el-text-color-primary); cursor: pointer; text-align: left; transition: border-color .18s ease, background-color .18s ease; }.language-card:hover, .language-card--active { border-color: var(--el-color-primary-light-5); background: var(--el-color-primary-light-9); }
.language-card__badge { width: 29px; height: 29px; flex: 0 0 auto; font-size: 10px; }.language-card__copy { overflow: hidden; }.language-card__copy strong, .language-card__copy small { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.language-card__copy strong { font-size: 11px; }.language-card__copy small { color: var(--el-text-color-secondary); font: 10px var(--code-font-family, monospace); }.language-card__check { margin-left: auto; flex: 0 0 auto; color: var(--el-color-primary); }
.editor-toolbar__actions { flex: 0 0 auto; gap: 2px; }
.editor-toolbar__actions .el-button { min-height: 34px; margin: 0; justify-content: center; }
.action-button--test { min-width: 98px; }
.action-button--submit { min-width: 108px; }
.action-button--log { min-width: 68px; }
.editor-toolbar__actions .el-button + .el-button { margin-left: 0; }
@media (max-width: 900px) { .editor-toolbar { align-items: flex-start; flex-direction: column; }.editor-toolbar__left { width: 100%; }.language-trigger { width: 100%; }.editor-toolbar__actions { width: 100%; justify-content: flex-end; } }

</style>
