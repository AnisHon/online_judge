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
      <el-button type="primary" size="large" :icon="Promotion" :loading="isLoading" @click="submit">{{ isAdd ? '发布题解' : '保存修改' }}</el-button>
    </header>

    <section class="editor-card">
      <el-form ref="formRef" class="solution-form" size="large" label-position="top" :rules="rules" :model="form">
        <el-form-item class="title-item" prop="title">
          <el-input v-model="form.title" class="title-input" maxlength="100" show-word-limit placeholder="给这篇题解起一个清晰的标题" />
        </el-form-item>

        <div class="form-grid">
          <el-form-item label="关联题目" prop="problemId">
            <div class="problem-select-field">
              <el-input :model-value="selectedProblemText" readonly :title="selectedProblemFullText" placeholder="请选择题目">
                <template #prefix><el-icon><Collection /></el-icon></template>
                <template #append><el-button :icon="Search" @click="problemPickerOpen = true">选择题目</el-button></template>
              </el-input>
              <small>题目只能从可访问题库中选择，ID 过长时会自动截断，悬停可查看完整 ID。</small>
            </div>
          </el-form-item>

          <el-form-item label="展示设置">
            <div class="visibility-settings">
              <div class="setting-option"><span class="setting-option__icon"><el-icon><Lock /></el-icon></span><span><strong>仅自己可见</strong><small>暂不公开这篇题解</small></span><el-switch v-model="form.private_" /></div>
              <div v-if="hasSolutionEditPermission" class="setting-option"><span class="setting-option__icon setting-option__icon--amber"><el-icon><Top /></el-icon></span><span><strong>置顶题解</strong><small>在题解列表中优先展示</small></span><el-switch v-model="form.topUp" /></div>
            </div>
          </el-form-item>
        </div>
      </el-form>

      <div class="content-heading"><div><span class="section-index">01</span><div><strong>题解正文</strong><small>支持 Markdown、代码块与数学公式</small></div></div><span class="content-status"><i />Markdown 编辑模式</span></div>
      <MarkDownEditor v-model="form.content" class="editor" />
    </section>

    <el-dialog v-model="problemPickerOpen" title="选择关联题目" width="min(980px, 94vw)" append-to-body destroy-on-close>
      <ProblemPicker v-model="form.problemId" @select="handleProblemSelect" />
    </el-dialog>
  </main>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ArrowLeft, Collection, Lock, Promotion, Search, Top } from '@element-plus/icons-vue'
import { type FormInstance, type FormRules } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { debouncedAddSolution, debouncedAddSolutionAdmin, debouncedEditSolution, debouncedEditSolutionAdmin, getSolution, getSolutionAdmin, type SolutionForm } from '@/api/solution'
import { getProblems, getProblemsAdmin, type ProblemParam, type ProblemView, type TaggedProblemView } from '@/api/problem'
import type { IdType } from '@/api/common'
import MarkDownEditor from '@/components/MarkDownEditor/MarkDownEditor.vue'
import ProblemPicker from '@/components/ProblemPicker/ProblemPicker.vue'
import { hasPerm } from '@/utils/authUtil'
import useLoading from '@/hooks/useLoading'
import { notifyValidate } from '@/utils/validate'
import type { ValidateFieldsError } from 'async-validator'

interface SelectedProblem {
  problemId: IdType
  title: string
}

const route = useRoute()
const router = useRouter()
const hasSolutionEditPermission = hasPerm('problem:solution:edit')
const isAdmin = hasPerm('problem:solution:list')
const form = reactive<SolutionForm>({
  solutionId: undefined,
  problemId: undefined,
  topUp: false,
  title: '',
  private_: false,
  content: `# 思路

你选用何种方法解题？

# 解题过程

这些方法具体怎么运用？

# 复杂度

- 时间复杂度: $ O(∗) $

- 空间复杂度: $ O(∗) $

# Code

\`\`\`c++
/* 代码 */
\`\`\``,
})
const formRef = ref<FormInstance>()
const problemPickerOpen = ref(false)
const selectedProblem = ref<SelectedProblem>()
const { loading, isLoading, finish } = useLoading()
const isAdd = computed(() => !route.query.solutionId)
const selectedProblemText = computed(() => form.problemId ? `${selectedProblem.value?.title || '题目'} · #${shortId(form.problemId)}` : '')
const selectedProblemFullText = computed(() => form.problemId ? `${selectedProblem.value?.title || '题目'} · #${String(form.problemId)}` : '')

const rules: FormRules<typeof form> = {
  title: [{ required: true, message: '请填写题解标题', trigger: 'blur' }],
  problemId: [{ required: true, message: '请选择关联题目', trigger: 'change' }],
}

const shortId = (value: IdType) => {
  const text = String(value)
  return text.length > 18 ? `${text.slice(0, 8)}…${text.slice(-6)}` : text
}

const toSelectedProblem = (problem: ProblemView | TaggedProblemView): SelectedProblem => ({ problemId: String(problem.problemId), title: problem.title || '未命名题目' })
const handleProblemSelect = (problem: SelectedProblem) => { selectedProblem.value = problem; problemPickerOpen.value = false }

const addSolutionAdmin = debouncedAddSolutionAdmin((success: boolean) => { if (success) router.back() }, finish)
const addSolution = debouncedAddSolution((success: boolean) => { if (success) router.back() }, finish)
const editSolutionAdmin = debouncedEditSolutionAdmin((success: boolean) => { if (success) router.back() }, finish)
const editSolution = debouncedEditSolution((success: boolean) => { if (success) router.back() }, finish)

const submit = async () => {
  if (!formRef.value) return
  let valid = false
  await formRef.value.validate((result, invalidFields?: ValidateFieldsError) => { valid = result; notifyValidate(invalidFields) })
  if (!valid) return
  loading()
  if (isAdd.value) {
    if (isAdmin) addSolutionAdmin(form)
    else addSolution(form)
  } else if (isAdmin) editSolutionAdmin(form)
  else editSolution(form)
}

const fetchProblem = async (problemId: IdType) => {
  try {
    if (isAdmin) {
      const result = await getProblemsAdmin({ currentPage: 1, pageSize: 1, problemId })
      const problem = result.data.find(item => String(item.problemId) === String(problemId))
      if (problem) selectedProblem.value = toSelectedProblem(problem)
    } else {
      const result = await getProblems({ currentPage: 1, pageSize: 1, problemId: String(problemId) } as ProblemParam)
      const problem = result.data.find(item => String(item.problemId) === String(problemId))
      if (problem) selectedProblem.value = toSelectedProblem(problem)
    }
  } catch {
    // 题目名称补全失败时保留 ID，不能阻止用户编辑题解。
  }
}

const fillForm = async () => {
  if (route.query.problemId) form.problemId = String(route.query.problemId)
  if (route.query.solutionId) form.solutionId = String(route.query.solutionId)
  if (!isAdd.value) Object.assign(form, isAdmin ? await getSolutionAdmin(form.solutionId as IdType) : await getSolution(form.solutionId as IdType))
  if (form.problemId) await fetchProblem(form.problemId)
}

fillForm()
</script>

<style scoped lang="scss">
.solution-editor-page { display: flex; min-height: var(--in-main-content-height); flex-direction: column; margin: 0 auto; padding: 10px 0 30px; color: var(--el-text-color-primary); }
.editor-header { display: flex; align-items: center; justify-content: space-between; gap: 20px; margin-bottom: 18px; }
.editor-header__main { display: flex; align-items: center; min-width: 0; gap: 12px; }
.back-button { flex: 0 0 auto; width: 32px; height: 32px; padding: 0; color: var(--el-text-color-secondary); }
.editor-kicker { margin: 0 0 5px; color: var(--el-color-primary); font-size: 10px; font-weight: 800; letter-spacing: .16em; }
.editor-header h1 { margin: 0; font-size: 25px; letter-spacing: -.04em; }
.editor-subtitle { margin: 5px 0 0; color: var(--el-text-color-secondary); font-size: 12px; }
.editor-card { display: flex; min-height: 0; flex: 1; flex-direction: column; padding: 24px 28px 18px; border: 1px solid var(--el-border-color-light); border-radius: 20px; background: var(--el-bg-color); box-shadow: 0 14px 38px rgb(15 23 42 / 4%); }
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
.editor { min-height: 360px; flex: 1; }
.editor :deep(.md-editor) { height: 100%; border-color: var(--el-border-color-lighter); border-radius: 13px; }
@media (max-width: 820px) { .form-grid { grid-template-columns: 1fr; gap: 0; }.visibility-settings { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 600px) { .solution-editor-page { padding: 8px 12px 24px; }.editor-header { align-items: flex-start; flex-direction: column; }.editor-header > .el-button { align-self: stretch; }.editor-card { padding: 18px 14px 14px; }.visibility-settings { grid-template-columns: 1fr; }.content-heading { align-items: flex-start; flex-direction: column; }.content-status { align-self: flex-end; } }
</style>
