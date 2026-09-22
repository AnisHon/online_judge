<template>
  <main class="solution-editor-page common-max-width-page">
    <header class="editor-header">
      <div class="editor-header__main">
        <el-button class="back-button" text :icon="ArrowLeft" aria-label="返回" @click="router.back()" />
        <div>
          <p class="editor-kicker">KNOWLEDGE BASE / SOLUTION</p>
          <h1>{{ isAdd ? '写一篇题解' : '编辑题解' }}</h1>
          <p class="editor-subtitle">把解题思路整理成可复用的知识，让每一次提交都留下价值。</p>
        </div>
      </div>
      <el-button
          type="primary"
          size="large"
          :icon="Promotion"
          :loading="isSubmitting"
          :disabled="initState !== 'ready' || isSubmitting"
          @click="submit"
      >
        {{ isAdd ? '发布题解' : '保存修改' }}
      </el-button>
    </header>

    <section class="editor-card" :class="`editor-card--${initState}`">
      <div v-if="initState === 'loading'" class="editor-state editor-state--loading">
        <el-skeleton :rows="5" animated />
        <div class="editor-state__hint">正在加载题解内容…</div>
      </div>

      <div v-else-if="initState === 'error'" class="editor-state editor-state--error">
        <el-icon><WarningFilled /></el-icon>
        <strong>{{ initError || '题解加载失败' }}</strong>
        <el-button type="primary" plain @click="initialize">重新加载</el-button>
      </div>

      <template v-else>
        <el-form
            ref="formRef"
            class="solution-form"
            size="large"
            label-position="top"
            :rules="rules"
            :model="form"
        >
          <el-form-item class="title-item" prop="title">
            <el-input
                v-model="form.title"
                class="title-input"
                maxlength="50"
                show-word-limit
                placeholder="给这篇题解起一个清晰的标题"
            />
          </el-form-item>

          <div class="form-grid">
            <el-form-item label="关联题目" prop="problemId">
              <div class="problem-select-field">
                <el-input
                    :model-value="selectedProblemText"
                    readonly
                    :title="selectedProblemFullText"
                    placeholder="请选择题目"
                >
                  <template #prefix><el-icon><Collection /></el-icon></template>
                  <template #append>
                    <el-button :icon="Search" @click="problemPickerOpen = true">选择题目</el-button>
                  </template>
                </el-input>
                <small>题目只能从可访问题库中选择，悬停可查看完整 ID。</small>
              </div>
            </el-form-item>

            <el-form-item label="展示设置">
              <div class="visibility-settings">
                <div class="setting-option">
                  <span class="setting-option__icon"><el-icon><Lock /></el-icon></span>
                  <span><strong>仅自己可见</strong><small>暂不公开这篇题解</small></span>
                  <el-switch v-model="form.private_" />
                </div>
                <div v-if="canSolutionEdit" class="setting-option">
                  <span class="setting-option__icon setting-option__icon--amber"><el-icon><Top /></el-icon></span>
                  <span><strong>置顶题解</strong><small>在题解列表中优先展示</small></span>
                  <el-switch v-model="form.topUp" />
                </div>
              </div>
            </el-form-item>
          </div>
        </el-form>

        <div class="content-heading">
          <div>
            <span class="section-index">01</span>
            <div><strong>题解正文</strong><small>支持 Markdown、代码块与数学公式</small></div>
          </div>
          <span class="content-status"><i />Markdown 编辑模式</span>
        </div>

        <div class="content-field" :class="{'content-field--error': contentError}">
          <MarkDownEditor
              v-model="form.content"
              class="editor"
              mode="flex"
              @update:model-value="clearContentError"
          />
          <p v-if="contentError" class="content-error">{{ contentError }}</p>
        </div>
      </template>
    </section>

    <el-dialog
        v-model="problemPickerOpen"
        title="选择关联题目"
        width="min(980px, 94vw)"
        append-to-body
        destroy-on-close
    >
      <ProblemPicker v-model="form.problemId" @select="handleProblemSelect" />
    </el-dialog>
  </main>
</template>

<script setup lang="ts">
import {computed, onBeforeUnmount, reactive, ref, watch} from 'vue'
import {ArrowLeft, Collection, Lock, Promotion, Search, Top, WarningFilled} from '@element-plus/icons-vue'
import {ElNotification, type FormInstance, type FormRules} from 'element-plus'
import {useRoute, useRouter} from 'vue-router'
import {addSolution, editSolution, getSolution, getSolutionAdmin, type SolutionForm} from '@/api/solution'
import {getProblems, getProblemsAdmin, type ProblemParam, type ProblemView, type TaggedProblemView} from '@/api/problem'
import type {IdType} from '@/api/common'
import MarkDownEditor from '@/components/MarkDownEditor/MarkDownEditor.vue'
import ProblemPicker from '@/components/ProblemPicker/ProblemPicker.vue'
import {hasAnyPerm, hasPerm} from '@/utils/authUtil'
import {ApiError} from '@/utils/http'
import {notifyValidate} from '@/utils/validate'

interface SelectedProblem {
  problemId: IdType
  title: string
}

const DEFAULT_CONTENT = `# 思路

你选用何种方法解题？

# 解题过程

这些方法具体怎么运用？

# 复杂度

- 时间复杂度: $ O(∗) $

- 空间复杂度: $ O(∗) $

# Code

\`\`\`c++
/* 代码 */
\`\`\``

const route = useRoute()
const router = useRouter()
const createEmptyForm = (): SolutionForm => ({
  solutionId: undefined,
  problemId: undefined,
  topUp: false,
  title: '',
  private_: false,
  content: DEFAULT_CONTENT,
})

const form = reactive<SolutionForm>(createEmptyForm())
const formRef = ref<FormInstance>()
const problemPickerOpen = ref(false)
const selectedProblem = ref<SelectedProblem>()
const initState = ref<'loading' | 'ready' | 'error'>('loading')
const initError = ref('')
const contentError = ref('')
const isSubmitting = ref(false)
const isAdd = computed(() => !String(route.query.solutionId ?? '').trim())
const canSolutionEdit = computed(() => hasPerm('problem:solution:edit'))
const canSolutionAdd = computed(() => hasPerm('problem:solution:add'))
const canSolutionList = computed(() => hasPerm('problem:solution:list'))
const canAdminProblemQuery = computed(() => hasAnyPerm(['problem:problem:list', 'problem:solution:list']))
const useAdminSolutionMutation = computed(() => isAdd.value ? canSolutionAdd.value : canSolutionEdit.value)

const shortId = (value: IdType) => {
  const text = String(value)
  return text.length > 18 ? `${text.slice(0, 8)}…${text.slice(-6)}` : text
}

const selectedProblemText = computed(() => form.problemId
    ? `${selectedProblem.value?.title || '题目'} · #${shortId(form.problemId)}`
    : '')
const selectedProblemFullText = computed(() => form.problemId
    ? `${selectedProblem.value?.title || '题目'} · #${String(form.problemId)}`
    : '')

const rules: FormRules<typeof form> = {
  title: [{required: true, message: '请填写题解标题', trigger: 'blur'}],
  problemId: [{required: true, message: '请选择关联题目', trigger: 'change'}],
}

const toSelectedProblem = (problem: ProblemView | TaggedProblemView): SelectedProblem => ({
  problemId: String(problem.problemId),
  title: problem.title || '未命名题目'
})

const handleProblemSelect = (problem: SelectedProblem) => {
  selectedProblem.value = problem
  problemPickerOpen.value = false
}

const validateContent = (): boolean => {
  if (form.content?.trim()) {
    contentError.value = ''
    return true
  }
  contentError.value = '请填写题解正文'
  return false
}

const clearContentError = () => {
  if (form.content?.trim()) contentError.value = ''
}

const submit = async () => {
  if (!formRef.value || initState.value !== 'ready' || isSubmitting.value) return

  const valid = await new Promise<boolean>(resolve => {
    formRef.value!.validate((result, invalidFields) => {
      notifyValidate(invalidFields)
      resolve(result)
    })
  })
  if (!valid || !validateContent()) return

  isSubmitting.value = true
  const snapshot: SolutionForm = {...form}
  try {
    if (isAdd.value) {
      await addSolution(snapshot, useAdminSolutionMutation.value)
    } else {
      await editSolution(snapshot, useAdminSolutionMutation.value)
    }
    ElNotification.success(isAdd.value ? '题解发布成功' : '题解保存成功')
    await router.back()
  } catch (error) {
    if (!(error instanceof ApiError && error.notified)) {
      ElNotification.error(error instanceof Error ? error.message : '题解提交失败，请稍后重试')
    }
  } finally {
    isSubmitting.value = false
  }
}

let initSequence = 0

const fetchProblem = async (problemId: IdType, sequence: number) => {
  try {
    if (canAdminProblemQuery.value) {
      const result = await getProblemsAdmin({currentPage: 1, pageSize: 1, problemId})
      const problem = result.data.find(item => String(item.problemId) === String(problemId))
      if (problem && sequence === initSequence) selectedProblem.value = toSelectedProblem(problem)
    } else {
      const result = await getProblems({
        currentPage: 1,
        pageSize: 1,
        problemId: String(problemId)
      } as ProblemParam)
      const problem = result.data.find(item => String(item.problemId) === String(problemId))
      if (problem && sequence === initSequence) selectedProblem.value = toSelectedProblem(problem)
    }
  } catch {
    // 名称补全失败时保留 ID，不能阻止用户编辑题解。
  }
}

const initialize = async () => {
  const sequence = ++initSequence
  initState.value = 'loading'
  initError.value = ''
  contentError.value = ''
  selectedProblem.value = undefined
  Object.assign(form, createEmptyForm())

  const problemId = String(route.query.problemId ?? '').trim()
  const solutionId = String(route.query.solutionId ?? '').trim()
  if (problemId) form.problemId = problemId

  try {
    if (solutionId) {
      form.solutionId = solutionId
      const solution = canSolutionList.value
          ? await getSolutionAdmin(solutionId)
          : await getSolution(solutionId)
      if (sequence !== initSequence) return
      Object.assign(form, solution)
    }
    if (form.problemId) await fetchProblem(form.problemId, sequence)
    if (sequence === initSequence) initState.value = 'ready'
  } catch (error) {
    if (sequence !== initSequence) return
    initState.value = 'error'
    initError.value = error instanceof ApiError ? error.message : '题解加载失败，请检查网络后重试'
  }
}

watch(() => [route.query.solutionId, route.query.problemId], initialize, {immediate: true})
onBeforeUnmount(() => {
  initSequence++
})
</script>

<style scoped lang="scss">
.solution-editor-page {
  display: flex;
  min-height: var(--in-main-content-height);
  flex-direction: column;
  margin: 0 auto;
  padding: 10px 0 30px;
  color: var(--el-text-color-primary);
}

.editor-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 18px;
}

.editor-header__main { display: flex; align-items: center; min-width: 0; gap: 12px; }
.back-button { flex: 0 0 auto; width: 32px; height: 32px; padding: 0; color: var(--el-text-color-secondary); }
.editor-kicker { margin: 0 0 5px; color: var(--el-color-primary); font-size: 10px; font-weight: 800; letter-spacing: .16em; }
.editor-header h1 { margin: 0; font-size: 25px; letter-spacing: -.04em; }
.editor-subtitle { margin: 5px 0 0; color: var(--el-text-color-secondary); font-size: 12px; }

.editor-card {
  display: flex;
  min-height: 0;
  flex: 1;
  flex-direction: column;
  overflow: hidden;
  padding: 24px 28px 18px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 20px;
  background: var(--el-bg-color);
  box-shadow: 0 14px 38px rgb(15 23 42 / 4%);
}

.editor-card--loading, .editor-card--error { min-height: 360px; }
.editor-state { display: flex; min-height: 300px; align-items: center; justify-content: center; flex-direction: column; gap: 18px; }
.editor-state--loading { align-items: stretch; justify-content: flex-start; padding: 24px 10px; }
.editor-state__hint { color: var(--el-text-color-secondary); font-size: 12px; text-align: center; }
.editor-state--error { color: var(--el-text-color-secondary); }
.editor-state--error > .el-icon { color: var(--el-color-danger); font-size: 34px; }
.editor-state--error strong { color: var(--el-text-color-primary); font-size: 14px; }

.solution-form { flex: 0 0 auto; }
.title-item { margin-bottom: 22px; }
.title-input :deep(.el-input__wrapper) { padding: 8px 0; border-radius: 0; box-shadow: 0 1px 0 var(--el-border-color) !important; background: transparent; }
.title-input :deep(.el-input__inner) { color: var(--el-text-color-primary); font-size: clamp(22px, 3vw, 32px); font-weight: 750; letter-spacing: -.04em; }
.title-input :deep(.el-input__count) { color: var(--el-text-color-placeholder); font-size: 11px; }
.form-grid { display: grid; grid-template-columns: minmax(0, 1.15fr) minmax(300px, .85fr); gap: 22px; }
.solution-form :deep(.el-form-item__label) { height: auto; margin-bottom: 7px; color: var(--el-text-color-regular); font-size: 12px; font-weight: 650; line-height: 1.2; }
.problem-select-field { min-width: 0; }
.problem-select-field :deep(.el-input__inner) { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.problem-select-field small { display: block; margin: 6px 0 0 2px; color: var(--el-text-color-secondary); font-size: 11px; line-height: 1.5; }

.visibility-settings { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }
.setting-option { display: flex; align-items: center; gap: 9px; min-width: 0; padding: 10px 11px; border: 1px solid var(--el-border-color-lighter); border-radius: 11px; background: var(--el-bg-color-page); }
.setting-option > span:nth-child(2) { display: flex; min-width: 0; flex: 1; flex-direction: column; gap: 3px; }
.setting-option strong { overflow: hidden; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.setting-option small { color: var(--el-text-color-secondary); font-size: 10px; white-space: nowrap; }
.setting-option__icon { display: grid; width: 27px; height: 27px; flex: 0 0 auto; place-items: center; border-radius: 8px; background: color-mix(in srgb, var(--el-color-primary) 12%, var(--el-bg-color)); color: var(--el-color-primary); font-size: 13px; }
.setting-option__icon--amber { background: color-mix(in srgb, var(--el-color-warning) 13%, var(--el-bg-color)); color: var(--el-color-warning); }

.content-heading { display: flex; align-items: center; justify-content: space-between; gap: 14px; margin: 8px 0 13px; padding-top: 18px; border-top: 1px solid var(--el-border-color-lighter); }
.content-heading > div { display: flex; align-items: center; gap: 10px; }
.section-index { color: var(--el-color-primary); font: 700 11px var(--code-font-family, monospace); letter-spacing: .08em; }
.content-heading strong, .content-heading small { display: block; }
.content-heading strong { font-size: 14px; }
.content-heading small { margin-top: 3px; color: var(--el-text-color-secondary); font-size: 11px; }
.content-status { display: inline-flex; align-items: center; gap: 6px; color: var(--el-text-color-secondary); font-size: 11px; }
.content-status i { width: 6px; height: 6px; border-radius: 50%; background: var(--el-color-success); }

.content-field { display: flex; min-height: 360px; flex: 1; flex-direction: column; min-width: 0; }
.content-field--error { padding-bottom: 24px; }
.editor { min-height: 0; flex: 1; }
.editor :deep(.md-editor) { border-color: var(--el-border-color-lighter); border-radius: 13px; }
.content-error { margin: 7px 2px 0; color: var(--el-color-danger); font-size: 12px; }

@media (max-width: 820px) {
  .form-grid { grid-template-columns: 1fr; gap: 0; }
  .visibility-settings { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}

@media (max-width: 600px) {
  .solution-editor-page { padding: 8px 12px 24px; }
  .editor-header { align-items: flex-start; flex-direction: column; }
  .editor-header > .el-button { align-self: stretch; }
  .editor-card { padding: 18px 14px 14px; }
  .visibility-settings { grid-template-columns: 1fr; }
  .content-heading { align-items: flex-start; flex-direction: column; }
  .content-status { align-self: flex-end; }
}
</style>
