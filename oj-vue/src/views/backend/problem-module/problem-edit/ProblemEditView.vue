<template>
  <div v-loading="isGetLoading" class="problem-editor-page">
    <el-scrollbar ref="scrollbarRef" class="problem-editor-scroll">
      <header class="editor-header">
        <div class="editor-header__identity">
          <el-button text :icon="ArrowLeft" aria-label="返回题目管理" @click="back" />
          <span class="editor-header__icon"><el-icon><EditPen /></el-icon></span>
          <div>
            <p class="eyebrow">PROBLEM / {{ isAdd ? 'CREATE' : 'EDIT' }}</p>
            <h1>{{ isAdd ? '新增题目' : '修改题目' }}</h1>
            <p>{{ isAdd ? '把题面、判题数据和答案组织成一份清晰的题目资产。' : `正在编辑「${problem.problem.title || '未命名题目'}」` }}</p>
          </div>
        </div>
        <div class="editor-header__actions">
          <span class="save-state"><i :class="{ 'is-ready': !isGetLoading && !isDirty }" />{{ isGetLoading ? '正在读取' : isDirty ? '有未保存修改' : isAdd ? '新建模式' : '已加载原题' }}</span>
          <el-button :icon="View" @click="togglePreview">{{ isShowPreview ? '关闭预览' : '预览题面' }}</el-button>
          <el-button type="primary" :icon="Check" :loading="isUpdateLoading || isAddLoading" @click="submit">保存题目</el-button>
        </div>
      </header>

      <div class="editor-layout">
        <aside class="editor-sidebar">
          <div class="side-card side-card--summary">
            <span class="side-card__eyebrow">EDITOR MAP</span>
            <strong>{{ completionText }}</strong>
            <div class="completion-bar"><i :style="{ width: `${completion}%` }" /></div>
            <span class="side-card__muted">完成基础信息后，再配置题面内容。</span>
          </div>
          <nav class="editor-nav" aria-label="题目编辑区块">
            <button v-for="item in sections" :key="item.id" type="button" :class="{ 'is-active': activeSection === item.id }" @click="scrollToSection(item.id)">
              <span class="editor-nav__number">{{ item.number }}</span><span><strong>{{ item.label }}</strong><small>{{ item.description }}</small></span>
            </button>
          </nav>
          <div class="side-card side-card--tip"><el-icon><InfoFilled /></el-icon><span>题型确定后，答案编辑器会自动切换对应模式。</span></div>
        </aside>

        <main class="editor-main">
          <el-form ref="formRef" :model="problem" :rules="rules" label-position="top" class="problem-form" @submit.prevent="submit">
            <section id="basic" class="editor-section" :class="sectionClass('basic')">
              <div class="section-heading"><div><span>01 / FOUNDATION</span><h2>基础信息</h2><p>先定义题目的身份和可见范围。</p></div><el-icon><Collection /></el-icon></div>
              <div class="field-grid field-grid--basic">
                <el-form-item class="field-grid__wide" label="题目名称" prop="problem.title"><el-input v-model="problem.problem.title" size="large" :prefix-icon="EditPen" maxlength="120" show-word-limit placeholder="例如：两数之和" /></el-form-item>
                <el-form-item label="题目类型" prop="problem.type"><el-select v-model="problem.problem.type" class="full-width" :disabled="!isAdd" placeholder="选择题目类型" @change="handleTypeChange"><el-option v-for="item in dict.problemType" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
                <el-form-item label="可见范围" prop="problem.auth"><el-select v-model="problem.problem.auth" class="full-width" placeholder="选择可见范围"><el-option v-for="item in dict.problemAuth" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
                <el-form-item label="题目来源" prop="problem.source"><el-input v-model="problem.problem.source" :prefix-icon="Link" maxlength="120" placeholder="例如：校内题库、Codeforces" /></el-form-item>
              </div>
              <div class="type-preview" :class="`type-preview--${typeTone}`"><span class="type-preview__icon"><el-icon><component :is="typeIcon" /></el-icon></span><div><strong>{{ typeLabel }}</strong><span>{{ typeDescription }}</span></div></div>
            </section>

            <section id="statement" class="editor-section" :class="sectionClass('statement')">
              <div class="section-heading"><div><span>02 / STATEMENT</span><h2>题面内容</h2><p>题面和样例分开组织，编辑时更容易保持结构。</p></div><el-icon><Document /></el-icon></div>
              <div class="editor-field"><div class="field-label"><strong>题目描述</strong><span>支持 Markdown、公式和图片</span></div><MarkDownEditor v-model="problem.problem.description" /></div>
              <div v-if="isOjProblem" class="statement-grid">
                <div class="editor-field"><div class="field-label"><strong>输入格式</strong><span>说明输入数据的结构</span></div><MarkDownEditor v-model="problem.ojProblem.input" /></div>
                <div class="editor-field"><div class="field-label"><strong>输出格式</strong><span>说明需要输出的内容</span></div><MarkDownEditor v-model="problem.ojProblem.output" /></div>
                <div class="example-field"><div class="field-label"><strong>输入样例</strong><span>给出一组典型输入</span></div><el-input v-model="problem.ojProblem.inputExample" type="textarea" :rows="6" resize="vertical" placeholder="输入样例" /></div>
                <div class="example-field"><div class="field-label"><strong>输出样例</strong><span>对应的预期输出</span></div><el-input v-model="problem.ojProblem.outputExample" type="textarea" :rows="6" resize="vertical" placeholder="输出样例" /></div>
              </div>
              <div class="editor-field editor-field--hint"><div class="field-label"><strong>提示</strong><span>可选，用于补充解题方向或注意事项</span></div><MarkDownEditor :model-value="problem.problem.hint || undefined" @update:model-value="problem.problem.hint = $event || ''" /></div>
            </section>

            <section v-if="isOjProblem" id="limits" class="editor-section" :class="sectionClass('limits')">
              <div class="section-heading"><div><span>03 / RUNTIME</span><h2>判题配置</h2><p>统一设置运行资源限制，避免单位混用。</p></div><el-icon><Timer /></el-icon></div>
              <div class="field-grid field-grid--limits">
                <el-form-item label="时间限制" prop="ojProblem.timeLimit"><el-input-number v-model="problem.ojProblem.timeLimit" class="full-width" :min="1" :max="600000" controls-position="right" /><small class="unit-hint">毫秒（ms）</small></el-form-item>
                <el-form-item label="内存限制" prop="ojProblem.memoryLimit"><el-input-number v-model="problem.ojProblem.memoryLimit" class="full-width" :min="1" :max="1048576" controls-position="right" /><small class="unit-hint">KiB</small></el-form-item>
                <el-form-item label="栈空间限制" prop="ojProblem.stackLimit"><el-input-number v-model="problem.ojProblem.stackLimit" class="full-width" :min="1" :max="1048576" controls-position="right" /><small class="unit-hint">MiB</small></el-form-item>
                <el-form-item label="难度" prop="ojProblem.difficulty"><el-select v-model="problem.ojProblem.difficulty" class="full-width" placeholder="选择难度"><el-option v-for="item in dict.difficulty" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
              </div>
            </section>

            <section v-if="isChoiceProblem || isFillProblem" id="answers" class="editor-section" :class="sectionClass('answers')">
              <div class="section-heading"><div><span>03 / ANSWERS</span><h2>答案设计</h2><p>{{ isFillProblem ? '按填空顺序管理答案和分值。' : '把选项内容、正确答案和分值放在同一张卡片里。' }}</p></div><el-icon><Edit /></el-icon></div>
              <AnswerEditor v-model:answers="problem.choices" :type="problem.problem.type" />
            </section>

            <section v-if="isOjProblem" id="cases" class="editor-section" :class="sectionClass('cases')">
              <div class="section-heading"><div><span>04 / TEST DATA</span><h2>测试数据</h2><p>{{ isAdd ? '为新题目添加基础测试点，保存后还可以独立维护文件。' : '已有题目的测试点保持独立管理，避免编辑题面时误改判题数据。' }}</p></div><el-icon><Files /></el-icon></div>
              <CaseEditor v-if="isAdd" v-model:cases="problem.cases" />
              <div v-else class="managed-cases"><span class="managed-cases__icon"><el-icon><Files /></el-icon></span><div><strong>测试点已独立管理</strong><p>修改题目时不会覆盖现有测试数据，你可以进入测试用例页面继续添加、删除或下载文件。</p></div><el-button type="primary" plain :icon="ArrowRight" @click="openCaseManager">管理测试用例</el-button></div>
            </section>

            <section v-if="!problem.problem.type" class="editor-section editor-section--empty"><el-empty description="请先在基础信息中选择题目类型" /></section>
          </el-form>

          <footer class="editor-footer"><el-button :icon="ArrowLeft" @click="back">返回题目管理</el-button><div><el-button :icon="View" @click="togglePreview">{{ isShowPreview ? '关闭预览' : '查看预览' }}</el-button><el-button type="primary" :icon="Check" :loading="isUpdateLoading || isAddLoading" @click="submit">保存题目</el-button></div></footer>
        </main>

        <aside v-if="isShowPreview" class="preview-pane">
          <div class="preview-pane__header"><div><span>LIVE PREVIEW</span><strong>题面预览</strong></div><el-button text circle :icon="Close" aria-label="关闭预览" @click="isShowPreview = false" /></div>
          <el-scrollbar><ProblemReviewer :problem="problem" /></el-scrollbar>
        </aside>
      </div>
    </el-scrollbar>
  </div>
</template>

<script setup lang="ts">
import {computed, reactive, ref, watch} from 'vue';
import {onBeforeRouteLeave, useRoute, useRouter} from 'vue-router';
import {ArrowLeft, ArrowRight, Check, Close, Collection, Document, Edit, EditPen, Files, InfoFilled, Link, Timer, View} from '@element-plus/icons-vue';
import {ElMessage, ElMessageBox, ElNotification, type FormInstance, type FormRules} from 'element-plus';
import {addProblems, dict, getAdminDetailProblem, ProblemAuth, type ProblemForm, ProblemType, updateProblems} from '@/api/problem';
import type {IdType} from '@/api/common';
import {useTabStore} from '@/stores/useTabStore';
import MarkDownEditor from '@/components/MarkDownEditor/MarkDownEditor.vue';
import ProblemReviewer from '@/views/backend/problem-module/problem-edit/problem-reviewer/ProblemReviewer.vue';
import AnswerEditor from './AnswerEditor.vue';
import CaseEditor from './CaseEditor.vue';

const route = useRoute();
const router = useRouter();
const tabStore = useTabStore();
const PROBLEM_MANAGEMENT_ROUTE = 'problem-edit';
const problemId = computed<IdType | undefined>(() => route.query.id ? String(route.query.id) : undefined);
const isAdd = computed(() => !problemId.value);
const isShowPreview = ref(false);
const activeSection = ref('basic');
const formRef = ref<FormInstance>();
const problem = reactive<ProblemForm>({problem: {problemId: undefined, title: '', description: '', source: '', type: undefined, auth: ProblemAuth.PUBLIC, hint: ''}, ojProblem: {problemId: undefined, difficulty: undefined, memoryLimit: 131072, stackLimit: 128, timeLimit: 1000, input: '', output: '', inputExample: '', outputExample: '', createTime: undefined}, choices: [], cases: []});
const rules: FormRules = {'problem.title': [{required: true, message: '请输入题目名称', trigger: 'blur'}], 'problem.type': [{required: true, message: '请选择题目类型', trigger: 'change'}], 'problem.auth': [{required: true, message: '请选择可见范围', trigger: 'change'}], 'problem.description': [{required: true, message: '请填写题目描述', trigger: 'blur'}], 'ojProblem.timeLimit': [{required: true, message: '请设置时间限制', trigger: 'change'}], 'ojProblem.memoryLimit': [{required: true, message: '请设置内存限制', trigger: 'change'}]};
const isOjProblem = computed(() => problem.problem.type === ProblemType.OJ);
const isChoiceProblem = computed(() => problem.problem.type === ProblemType.CHOICE || problem.problem.type === ProblemType.MULTI_CHOICE);
const isFillProblem = computed(() => problem.problem.type === ProblemType.FILL);
const sections = computed(() => [
  {id: 'basic', number: '01', label: '基础信息', description: '名称与类型'},
  {id: 'statement', number: '02', label: '题面内容', description: '描述与样例'},
  ...(isOjProblem.value
      ? [{id: 'limits', number: '03', label: '判题配置', description: '资源与难度'}, {id: 'cases', number: '04', label: '测试数据', description: '输入与输出'}]
      : [{id: 'answers', number: '03', label: '答案设计', description: '选项与分值'}]),
]);
const typeLabel = computed(() => dict.problemType.find(item => item.value === problem.problem.type)?.label || '未选择题型');
const typeDescription = computed(() => isOjProblem.value ? '编程题：需要配置运行限制和测试数据。' : isFillProblem.value ? '填空题：按空管理可接受答案。' : isChoiceProblem.value ? problem.problem.type === ProblemType.MULTI_CHOICE ? '多选题：可以设置多个正确选项。' : '单选题：设置一个正确选项。' : '选择题型后会显示对应编辑器。');
const typeTone = computed(() => isOjProblem.value ? 'blue' : isFillProblem.value ? 'amber' : isChoiceProblem.value ? 'violet' : 'muted');
const typeIcon = computed(() => isOjProblem.value ? Files : isFillProblem.value ? EditPen : Collection);
const completion = computed(() => { let value = 0; if (problem.problem.title?.trim()) value += 25; if (problem.problem.type) value += 25; if (problem.problem.description?.trim()) value += 25; if (isOjProblem.value ? problem.cases.length > 0 : problem.choices.length > 0) value += 25; return value; });
const completionText = computed(() => `${completion.value}% ready`);
const sectionClass = (id: string) => ({'is-active': activeSection.value === id});
const scrollToSection = (id: string) => { activeSection.value = id; document.getElementById(id)?.scrollIntoView({behavior: 'smooth', block: 'start'}); };
const togglePreview = () => { isShowPreview.value = !isShowPreview.value; };
const lastType = ref<ProblemType | undefined>(problem.problem.type);
const typeDrafts = new Map<ProblemType, {choices: ProblemForm['choices']; cases: ProblemForm['cases']}>();
let revertingType = false;
const cloneItems = <T extends object>(items: T[]) => items.map(item => ({...item}));
const handleTypeChange = async (nextType: ProblemType) => {
  if (revertingType) {
    revertingType = false;
    return;
  }
  const previousType = lastType.value;
  if (!isAdd.value || previousType === undefined || previousType === nextType) {
    lastType.value = nextType;
    return;
  }
  const hasCurrentDraft = problem.choices.length > 0 || problem.cases.length > 0;
  if (hasCurrentDraft) {
    try {
      await ElMessageBox.confirm(
          '当前题型已经填写了答案或测试数据。切换后会把当前内容保留在草稿中，是否继续？',
          '切换题型',
          {confirmButtonText: '保留并切换', cancelButtonText: '留在当前题型', type: 'warning'}
      );
    } catch {
      revertingType = true;
      problem.problem.type = previousType;
      return;
    }
    typeDrafts.set(previousType, {choices: cloneItems(problem.choices), cases: cloneItems(problem.cases)});
  }
  const nextDraft = typeDrafts.get(nextType);
  problem.choices.splice(0, problem.choices.length, ...(nextDraft?.choices ? cloneItems(nextDraft.choices) : []));
  problem.cases.splice(0, problem.cases.length, ...(nextDraft?.cases ? cloneItems(nextDraft.cases) : []));
  lastType.value = nextType;
};
const openCaseManager = () => router.push({name: 'case-edit', params: {problemId: problem.problem.problemId}});
const isGetLoading = ref(false);
const isUpdateLoading = ref(false);
const isAddLoading = ref(false);
const isDirty = ref(false);
const savedSnapshot = ref('');
const snapshot = () => JSON.stringify(problem, (_key, value) => value instanceof File ? value.name : value);
const markClean = () => {
  savedSnapshot.value = snapshot();
  isDirty.value = false;
};
const createPayload = (): ProblemForm => ({
  problem: {...problem.problem},
  ojProblem: {...problem.ojProblem},
  choices: cloneItems(problem.choices),
  cases: cloneItems(problem.cases),
});
const confirmDiscard = async () => {
  if (!isDirty.value || isUpdateLoading.value || isAddLoading.value) return true;
  try {
    await ElMessageBox.confirm('当前页面有未保存的修改，离开后这些内容会丢失。', '确认离开', {confirmButtonText: '离开', cancelButtonText: '继续编辑', type: 'warning'});
    return true;
  } catch {
    return false;
  }
};
const back = async () => {
  if (!await confirmDiscard()) return;
  const name = String(route.name || 'edit-problem');
  await router.push({name: PROBLEM_MANAGEMENT_ROUTE});
  tabStore.removeTab(name);
};
const submit = async () => {
  if (isGetLoading.value || isUpdateLoading.value || isAddLoading.value) return;
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) { ElNotification.warning({title: '信息还不完整', message: '请先补齐标记为必填的内容。'}); return; }
  if (isChoiceProblem.value && !problem.choices.length) { ElNotification.warning({title: '还没有选项', message: '请至少添加一个选项。'}); scrollToSection('answers'); return; }
  if (isChoiceProblem.value && !problem.choices.some(item => item.isCorrect)) { ElNotification.warning({title: '还没有正确答案', message: '请至少标记一个正确选项。'}); scrollToSection('answers'); return; }
  if (isFillProblem.value && problem.choices.some(item => !item.answerText?.trim())) { ElNotification.warning({title: '填空答案不完整', message: '请填写每个空的可接受答案。'}); scrollToSection('answers'); return; }
  if (isOjProblem.value && isAdd.value && !problem.cases.length) { ElNotification.warning({title: '还没有测试数据', message: '请至少添加一个测试点。'}); scrollToSection('cases'); return; }
  const payload = createPayload();
  try {
    if (isAdd.value) {
      isAddLoading.value = true;
      const id = await addProblems(payload);
      if (!id || id === 'null') throw new Error('题目创建未返回题目 ID');
      ElMessage.success('题目已创建');
      markClean();
      await router.back();
    } else {
      isUpdateLoading.value = true;
      await updateProblems(payload);
      ElMessage.success('题目已保存');
      markClean();
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '题目保存失败，请稍后重试');
  } finally {
    isAddLoading.value = false;
    isUpdateLoading.value = false;
  }
};
let loadRequestId = 0;
let revertingRoute = false;
const resetProblem = () => {
  Object.assign(problem.problem, {problemId: undefined, title: '', description: '', source: '', type: undefined, auth: ProblemAuth.PUBLIC, hint: ''});
  Object.assign(problem.ojProblem, {problemId: undefined, difficulty: undefined, memoryLimit: 131072, stackLimit: 128, timeLimit: 1000, input: '', output: '', inputExample: '', outputExample: '', createTime: undefined});
  problem.cases.splice(0);
  problem.choices.splice(0);
  lastType.value = undefined;
};
const loadProblem = async () => {
  const requestId = ++loadRequestId;
  isGetLoading.value = true;
  if (isAdd.value) {
    resetProblem();
    markClean();
    isGetLoading.value = false;
    return;
  }
  try {
    const data = await getAdminDetailProblem(problemId.value);
    if (requestId !== loadRequestId) return;
    Object.assign(problem.problem, data.problem);
    Object.assign(problem.ojProblem, data.ojProblem || {});
    problem.cases.splice(0, problem.cases.length, ...(data.cases || []));
    problem.choices.splice(0, problem.choices.length, ...(data.choices || []));
    lastType.value = problem.problem.type;
    markClean();
  } catch (error) {
    if (requestId === loadRequestId) ElMessage.error(error instanceof Error ? error.message : '题目加载失败，请稍后重试');
  } finally {
    if (requestId === loadRequestId) isGetLoading.value = false;
  }
};
watch(problemId, async (nextId, previousId) => {
  if (revertingRoute) {
    revertingRoute = false;
    return;
  }
  if (previousId !== undefined && nextId !== previousId && !await confirmDiscard()) {
    revertingRoute = true;
    await router.replace({name: 'edit-problem', query: previousId ? {id: previousId} : undefined});
    return;
  }
  await loadProblem();
}, {immediate: true});
watch(problem, () => {
  if (savedSnapshot.value) isDirty.value = snapshot() !== savedSnapshot.value;
}, {deep: true});
onBeforeRouteLeave(async (_to, _from, next) => {
  next(await confirmDiscard());
});
watch(() => [problem.problem.title, problem.problem.type, problem.problem.description, problem.cases.length, problem.choices.length], () => { if (activeSection.value === '') activeSection.value = 'basic'; });
</script>

<style lang="scss" scoped>
.problem-editor-page { width: 100%; min-width: 0; min-height: 0; overflow: hidden; color: var(--el-text-color-primary); }.problem-editor-scroll { width: 100%; height: calc(100vh - var(--menu-height) - 24px); min-width: 0; }.problem-editor-scroll :deep(.el-scrollbar__wrap) { overflow-x: hidden; }.problem-editor-scroll :deep(.el-scrollbar__view) { min-width: 0; min-height: 100%; }.editor-header { display: flex; align-items: center; justify-content: space-between; gap: 24px; max-width: 1480px; margin: 0 auto 18px; padding: 4px; }.editor-header__identity, .editor-header__actions, .editor-header__actions > * { display: flex; align-items: center; }.editor-header__identity { min-width: 0; gap: 12px; }.editor-header__identity > .el-button { flex: 0 0 auto; }.editor-header__icon { display: grid; width: 42px; height: 42px; flex: 0 0 auto; place-items: center; border-radius: 12px; background: rgb(37 99 235 / 12%); color: #2563eb; font-size: 21px; }.eyebrow, .section-heading > div > span, .preview-pane__header span, .side-card__eyebrow { margin: 0 0 4px; color: var(--el-text-color-secondary); font-size: 10px; font-weight: 800; letter-spacing: .15em; }.editor-header h1 { margin: 0; font-size: 25px; letter-spacing: -.04em; }.editor-header__identity p:last-child { margin: 4px 0 0; color: var(--el-text-color-secondary); font-size: 12px; }.editor-header__actions { justify-content: flex-end; gap: 9px; flex-wrap: wrap; }.save-state { gap: 6px; margin-right: 5px; color: var(--el-text-color-secondary); font-size: 11px; white-space: nowrap; }.save-state i { width: 6px; height: 6px; border-radius: 50%; background: var(--el-color-warning); }.save-state i.is-ready { background: var(--el-color-success); }.editor-layout { display: grid; grid-template-columns: 208px minmax(0, 1fr) minmax(360px, .72fr); gap: 16px; max-width: 1480px; min-width: 0; margin: 0 auto; padding-bottom: 34px; }.editor-sidebar { position: sticky; top: 12px; align-self: start; }.side-card { padding: 15px; border: 1px solid var(--el-border-color-lighter); border-radius: 14px; background: var(--el-bg-color); }.side-card--summary strong { display: block; margin: 7px 0 10px; color: var(--el-color-primary); font: 700 21px var(--code-font-family, monospace); }.side-card__muted { display: block; margin-top: 9px; color: var(--el-text-color-secondary); font-size: 11px; line-height: 1.5; }.completion-bar { height: 5px; overflow: hidden; border-radius: 99px; background: var(--el-fill-color); }.completion-bar i { display: block; height: 100%; border-radius: inherit; background: var(--el-color-primary); transition: width .25s ease; }.editor-nav { display: flex; flex-direction: column; gap: 3px; margin: 14px 0; }.editor-nav button { display: flex; width: 100%; align-items: center; gap: 10px; padding: 10px 8px; border: 1px solid transparent; border-radius: 10px; color: var(--el-text-color-secondary); text-align: left; background: transparent; cursor: pointer; transition: all .18s ease; }.editor-nav button:hover, .editor-nav button.is-active { border-color: var(--el-border-color-lighter); background: var(--el-fill-color-light); color: var(--el-text-color-primary); }.editor-nav button.is-active .editor-nav__number { background: var(--el-color-primary); color: #fff; }.editor-nav__number { display: grid; width: 25px; height: 25px; flex: 0 0 auto; place-items: center; border-radius: 8px; background: var(--el-fill-color); color: var(--el-text-color-secondary); font: 700 10px var(--code-font-family, monospace); }.editor-nav button > span:last-child { display: flex; min-width: 0; flex-direction: column; gap: 2px; }.editor-nav strong { font-size: 12px; }.editor-nav small { color: var(--el-text-color-secondary); font-size: 10px; }.side-card--tip { display: flex; align-items: flex-start; gap: 8px; color: var(--el-text-color-secondary); font-size: 11px; line-height: 1.5; }.side-card--tip .el-icon { flex: 0 0 auto; color: var(--el-color-primary); font-size: 15px; }.editor-main { min-width: 0; }.problem-form { display: flex; flex-direction: column; gap: 14px; }.editor-section { scroll-margin-top: 18px; min-width: 0; padding: 23px 24px 25px; border: 1px solid var(--el-border-color-lighter); border-radius: 17px; background: var(--el-bg-color); box-shadow: 0 9px 28px rgb(15 23 42 / 3%); transition: border-color .2s ease, box-shadow .2s ease; }.editor-section.is-active { border-color: color-mix(in srgb, var(--el-color-primary) 26%, var(--el-border-color-lighter)); box-shadow: 0 10px 32px color-mix(in srgb, var(--el-color-primary) 7%, transparent); }.section-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 18px; margin-bottom: 19px; }.section-heading h2 { margin: 5px 0 4px; font-size: 18px; letter-spacing: -.025em; }.section-heading p { margin: 0; color: var(--el-text-color-secondary); font-size: 11px; }.section-heading > .el-icon { color: var(--el-color-primary); font-size: 22px; }.field-grid { display: grid; gap: 0 16px; }.field-grid--basic { grid-template-columns: repeat(3, minmax(0, 1fr)); }.field-grid__wide { grid-column: 1 / -1; }.field-grid--limits { grid-template-columns: repeat(4, minmax(0, 1fr)); }.full-width { width: 100%; }.problem-form :deep(.el-form-item) { min-width: 0; margin-bottom: 15px; }.problem-form :deep(.el-form-item__label) { height: auto; margin-bottom: 6px; color: var(--el-text-color-primary); font-size: 12px; line-height: 1.4; }.unit-hint { display: block; margin-top: 4px; color: var(--el-text-color-secondary); font-size: 10px; }.type-preview { display: flex; align-items: center; gap: 10px; padding: 11px 13px; border: 1px solid var(--el-border-color-lighter); border-radius: 12px; background: var(--type-color-bg); }.type-preview__icon { display: grid; width: 32px; height: 32px; flex: 0 0 auto; place-items: center; border-radius: 10px; background: var(--type-color-bg); color: var(--type-color); }.type-preview strong, .type-preview span { display: block; }.type-preview strong { font-size: 12px; }.type-preview > div > span { margin-top: 3px; color: var(--el-text-color-secondary); font-size: 11px; }.type-preview--blue { --type-color: #2563eb; --type-color-bg: rgb(37 99 235 / 12%); }.type-preview--amber { --type-color: #d97706; --type-color-bg: rgb(217 119 6 / 12%); }.type-preview--violet { --type-color: #7c3aed; --type-color-bg: rgb(124 58 237 / 12%); }.type-preview--muted { --type-color: var(--el-text-color-secondary); --type-color-bg: var(--el-fill-color); }.statement-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }.editor-field, .example-field { min-width: 0; margin-bottom: 17px; }.field-label { display: flex; align-items: baseline; justify-content: space-between; gap: 10px; margin-bottom: 7px; }.field-label strong { font-size: 12px; }.field-label span { color: var(--el-text-color-secondary); font-size: 10px; }.editor-field--hint { margin-top: 3px; margin-bottom: 0; }.example-field textarea { font: 13px/1.65 var(--code-font-family, 'JetBrains Mono', Consolas, monospace); }.managed-cases { display: flex; align-items: center; gap: 13px; padding: 17px; border: 1px dashed var(--el-border-color); border-radius: 14px; background: var(--el-fill-color-light); }.managed-cases__icon { display: grid; width: 38px; height: 38px; flex: 0 0 auto; place-items: center; border-radius: 11px; background: var(--el-color-primary-light-9); color: var(--el-color-primary); }.managed-cases > div { min-width: 0; flex: 1; }.managed-cases strong { font-size: 13px; }.managed-cases p { margin: 4px 0 0; color: var(--el-text-color-secondary); font-size: 11px; line-height: 1.55; }.editor-section--empty { display: grid; place-items: center; min-height: 220px; }.preview-pane { position: sticky; top: 12px; display: flex; height: calc(100vh - var(--menu-height) - 70px); min-width: 0; align-self: start; flex-direction: column; overflow: hidden; border: 1px solid var(--el-border-color-lighter); border-radius: 17px; background: var(--el-bg-color); }.preview-pane__header { display: flex; align-items: center; justify-content: space-between; padding: 16px 17px 13px; border-bottom: 1px solid var(--el-border-color-lighter); }.preview-pane__header div { display: flex; flex-direction: column; gap: 4px; }.preview-pane__header span { margin: 0; color: var(--el-color-primary); }.preview-pane__header strong { font-size: 14px; }.preview-pane > .el-scrollbar { min-height: 0; flex: 1; padding: 16px; }.preview-pane :deep(.header h1) { font-size: 21px; }.preview-pane :deep(.content) { padding-bottom: 25px; }.editor-footer { display: flex; align-items: center; justify-content: space-between; gap: 14px; padding: 18px 3px 0; }.editor-footer > div { display: flex; gap: 8px; }
.type-preview .type-preview__icon { display: grid; align-items: center; justify-items: center; }
@media (max-width: 1180px) { .editor-layout { grid-template-columns: 180px minmax(0, 1fr); }.preview-pane { position: fixed; z-index: 20; inset: calc(var(--menu-height) + 12px) 14px 14px; height: auto; }.field-grid--limits { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 760px) { .problem-editor-scroll { height: auto; }.editor-header { align-items: flex-start; flex-direction: column; gap: 14px; }.editor-header__actions { width: 100%; justify-content: flex-start; }.editor-layout { display: block; }.editor-sidebar { position: static; margin-bottom: 14px; }.editor-nav { display: grid; grid-template-columns: repeat(3, 1fr); }.editor-nav button { align-items: flex-start; flex-direction: column; gap: 5px; }.side-card--tip { display: none; }.field-grid--basic, .field-grid--limits, .statement-grid { grid-template-columns: 1fr; }.field-grid__wide { grid-column: auto; }.editor-section { padding: 18px 15px 20px; }.section-heading h2 { font-size: 17px; }.editor-footer { align-items: stretch; flex-direction: column-reverse; }.editor-footer > div { justify-content: flex-end; } }
@media (max-width: 460px) { .editor-header__actions .save-state { display: none; }.editor-nav { grid-template-columns: 1fr 1fr; }.editor-nav button { padding: 8px 5px; }.managed-cases { align-items: flex-start; flex-wrap: wrap; }.managed-cases .el-button { margin-left: 51px; } }
</style>
