<template>
  <main class="faq-manage">
    <header class="manage-heading">
      <div class="manage-title">
        <span class="manage-icon"><el-icon><QuestionFilled /></el-icon></span>
        <div>
          <p class="manage-kicker">SYSTEM / CONTENT</p>
          <h1>FAQ 管理</h1>
          <p>维护前台常见问题与对应说明。</p>
        </div>
      </div>
      <div class="manage-actions">
        <el-button v-has="'content:faq:list'" :icon="Refresh" :loading="loading" @click="loadFaqs">刷新</el-button>
        <el-button v-has="'content:faq:add'" type="primary" :icon="Plus" @click="openCreate">新增问答</el-button>
      </div>
    </header>

    <section v-if="canReadFaq" class="faq-table-panel">
      <div class="table-heading">
        <div><h2>问答列表</h2><p>共 {{ faqs.length }} 条，前台按此顺序展示。</p></div>
        <el-button
          v-if="selectedIds.length"
          v-has="'content:faq:remove'"
          text
          type="danger"
          :icon="Delete"
          @click="removeFaqs()"
        >删除所选（{{ selectedIds.length }}）</el-button>
      </div>

      <el-table
        ref="tableRef"
        v-loading="loading"
        :data="faqs"
        row-key="faqId"
        class="faq-table"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="48" align="center" />
        <el-table-column label="问题" min-width="260">
          <template #default="{ row }">
            <div class="question-cell"><span>Q</span><strong>{{ row.question }}</strong></div>
          </template>
        </el-table-column>
        <el-table-column label="回答" min-width="320" show-overflow-tooltip>
          <template #default="{ row }"><span class="answer-cell">{{ row.answer }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right" align="right">
          <template #default="{ row }">
            <el-space>
              <el-button v-has="'content:faq:edit'" link type="primary" :icon="Edit" @click="openEdit(row)">编辑</el-button>
              <el-button v-has="'content:faq:remove'" link type="danger" :icon="Delete" @click="removeFaqs(row)">删除</el-button>
            </el-space>
          </template>
        </el-table-column>
        <template #empty><el-empty description="还没有问答，点击右上角新增" /></template>
      </el-table>
    </section>
    <el-alert v-else type="error" :closable="false" show-icon title="当前账号没有 FAQ 管理权限" />

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="min(680px, 94vw)" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="saveFaq">
        <el-form-item label="问题" prop="question">
          <el-input v-model="form.question" maxlength="500" show-word-limit placeholder="例如：如何参加在线判题？" />
        </el-form-item>
        <el-form-item label="回答" prop="answer">
          <el-input v-model="form.answer" type="textarea" :rows="8" maxlength="20000" show-word-limit resize="vertical" placeholder="输入简明、清楚的回答；换行会保留。" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveFaq">保存</el-button>
      </template>
    </el-dialog>
  </main>
</template>

<script setup lang="ts">
import {computed, onMounted, reactive, ref} from 'vue'
import {Delete, Edit, Plus, QuestionFilled, Refresh} from '@element-plus/icons-vue'
import {ElMessage, ElMessageBox, type FormInstance, type FormRules, type TableInstance} from 'element-plus'
import {addFaq, listAdminFaq, removeFaq, updateFaq, type Faq, type FaqDto} from '@/api/faq'
import type {IdType} from '@/api/common'
import {hasPerm} from '@/utils/authUtil'

const canReadFaq = computed(() => hasPerm('content:faq:list'))
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editing = ref(false)
const faqs = ref<Faq[]>([])
const selectedIds = ref<IdType[]>([])
const tableRef = ref<TableInstance>()
const formRef = ref<FormInstance>()
const form = reactive<FaqDto>({question: '', answer: ''})
const dialogTitle = computed(() => editing.value ? '编辑问答' : '新增问答')
const rules: FormRules<FaqDto> = {
  question: [{required: true, whitespace: true, message: '请填写问题', trigger: 'blur'}],
  answer: [{required: true, whitespace: true, message: '请填写回答', trigger: 'blur'}],
}

const loadFaqs = async () => {
  if (!canReadFaq.value) return
  loading.value = true
  try {
    faqs.value = await listAdminFaq()
    selectedIds.value = []
    tableRef.value?.clearSelection()
  } catch {
    ElMessage.error('FAQ 列表加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const handleSelectionChange = (rows: Faq[]) => {
  selectedIds.value = rows.map(row => row.faqId)
}

const resetForm = () => {
  Object.assign(form, {faqId: undefined, question: '', answer: ''})
  formRef.value?.clearValidate()
}

const openCreate = () => {
  editing.value = false
  resetForm()
  dialogVisible.value = true
}

const openEdit = (faq: Faq) => {
  editing.value = true
  resetForm()
  Object.assign(form, faq)
  dialogVisible.value = true
}

const saveFaq = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const saved = editing.value
      ? await updateFaq({...form, faqId: form.faqId!} as Faq)
      : await addFaq({...form, faqId: undefined})
    if (!saved) {
      ElMessage.error('问答保存失败')
      return
    }
    ElMessage.success(editing.value ? '问答已更新' : '问答已新增')
    dialogVisible.value = false
    await loadFaqs()
  } catch {
    ElMessage.error('问答保存失败，请稍后重试')
  } finally {
    saving.value = false
  }
}

const removeFaqs = async (faq?: Faq) => {
  const ids = faq ? [faq.faqId] : [...selectedIds.value]
  if (!ids.length) return
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${ids.length} 条问答吗？`, '删除问答', {
      confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning',
    })
    const removed = await removeFaq(ids)
    if (!removed) {
      ElMessage.error('问答删除失败')
      return
    }
    ElMessage.success('问答已删除')
    await loadFaqs()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error('问答删除失败，请稍后重试')
  }
}

onMounted(loadFaqs)
</script>

<style scoped>
.faq-manage { width: 100%; max-width: 1440px; margin: 0 auto; color: var(--el-text-color-primary); }
.manage-heading { display: flex; align-items: center; justify-content: space-between; gap: 18px; margin-bottom: 18px; }
.manage-title { display: flex; min-width: 0; align-items: center; gap: 13px; }
.manage-icon { display: grid; width: 42px; height: 42px; flex: 0 0 auto; place-items: center; border-radius: 12px; color: #7c3aed; background: rgb(124 58 237 / 12%); font-size: 20px; }
.manage-kicker { margin: 0 0 3px; color: var(--el-text-color-secondary); font-size: 10px; font-weight: 800; letter-spacing: .14em; }
.manage-heading h1 { margin: 0; font-size: 24px; letter-spacing: -.035em; }
.manage-heading p:last-child { margin: 4px 0 0; color: var(--el-text-color-secondary); font-size: 12px; }
.manage-actions { display: flex; flex: 0 0 auto; gap: 8px; }
.faq-table-panel { min-width: 0; padding: 18px 20px 12px; overflow: hidden; border: 1px solid var(--el-border-color-lighter); border-radius: 12px; background: var(--el-bg-color); box-shadow: 0 4px 18px rgb(15 23 42 / 4%); }
.table-heading { display: flex; align-items: center; justify-content: space-between; gap: 14px; margin-bottom: 13px; }
.table-heading h2 { margin: 0; font-size: 16px; }.table-heading p { margin: 4px 0 0; color: var(--el-text-color-secondary); font-size: 12px; }
.faq-table :deep(.el-table__cell) { padding: 11px 0; }.faq-table :deep(.el-table__header-wrapper th) { color: var(--el-text-color-secondary); font-size: 12px; background: var(--el-fill-color-light); }
.question-cell { display: flex; min-width: 0; align-items: center; gap: 10px; }.question-cell > span { display: grid; width: 25px; height: 25px; flex: 0 0 auto; place-items: center; border-radius: 8px; color: var(--el-color-primary); background: var(--el-color-primary-light-9); font: 700 11px var(--code-font-family, monospace); }.question-cell strong { overflow: hidden; font-size: 13px; font-weight: 600; text-overflow: ellipsis; white-space: nowrap; }.answer-cell { display: block; max-width: 100%; overflow: hidden; color: var(--el-text-color-secondary); text-overflow: ellipsis; white-space: nowrap; }
@media (max-width: 680px) { .manage-heading { align-items: flex-start; flex-direction: column; }.manage-actions { width: 100%; }.faq-table-panel { padding: 14px 12px 8px; }.table-heading { align-items: flex-start; flex-direction: column; } }
</style>
