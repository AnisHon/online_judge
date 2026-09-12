<template>
  <ContestSubPageShell title="迟交设置" kicker="SUBMISSIONS / EXTENSIONS" description="为指定用户开放迟交窗口，并清晰查看每条延期记录。" :icon="Clock" tone="amber" :stats="stats">
    <template #actions><el-button :icon="Refresh" :loading="loading" @click="getList">刷新记录</el-button><el-button :icon="ArrowLeft" @click="router.back()">返回管理</el-button><el-button v-has="'problem:contest:edit'" type="primary" :icon="Plus" @click="handleAdd">添加迟交权限</el-button><el-button v-has="'problem:contest:remove'" type="danger" plain :disabled="!selectedIds.length" :icon="Delete" @click="handleDelete()">批量撤销</el-button></template>
    <section class="extension-panel">
      <div class="panel-heading"><div><span class="eyebrow">LATE SUBMISSION ACCESS</span><h2>延期记录</h2><p>竞赛 / 作业 #{{ contestId }} · 变更只影响指定用户。</p></div><el-input v-model="keyword" clearable class="keyword-input" :prefix-icon="Search" placeholder="搜索用户 ID 或昵称" /></div>
      <el-table v-loading="loading" class="extension-table" :data="filteredList" row-key="userId" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="52" align="center" /><el-table-column label="用户" min-width="260"><template #default="{ row }"><div class="user-cell"><span class="avatar">{{ String(row.nikeName || row.userId).slice(0, 1) }}</span><div><strong>{{ row.nikeName || '未设置昵称' }}</strong><span>ID {{ row.userId }}</span></div></div></template></el-table-column><el-table-column label="截止时间" min-width="220"><template #default="{ row }"><span class="deadline"><el-icon><Timer /></el-icon>{{ row.deadline }}</span></template></el-table-column><el-table-column label="状态" width="140"><template #default="{ row }"><span class="active-pill" :class="new Date(row.deadline).getTime() >= Date.now() ? 'is-active' : 'is-expired'"><i />{{ new Date(row.deadline).getTime() >= Date.now() ? '生效中' : '已过期' }}</span></template></el-table-column><el-table-column label="操作" width="130" fixed="right" align="right"><template #default="{ row }"><el-button v-has="'problem:contest:remove'" link type="danger" :icon="Delete" @click="handleDelete(row)">撤销</el-button></template></el-table-column>
        <template #empty><el-empty description="暂无迟交记录" /></template>
      </el-table>
    </section>

    <el-dialog v-model="open" title="添加迟交权限" width="min(520px, 92vw)" append-to-body destroy-on-close>
      <div class="dialog-intro"><span class="dialog-icon"><el-icon><Timer /></el-icon></span><div><strong>设置专属截止时间</strong><p>用户必须先加入当前竞赛或作业，截止时间不能早于当前时间。</p></div></div>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top"><el-form-item label="用户 ID" prop="userId"><el-input v-model="form.userId" placeholder="请输入用户 ID" /></el-form-item><el-form-item label="最迟提交时间" prop="deadline"><el-date-picker v-model="form.deadline" class="full-width" type="datetime" format="YYYY-MM-DD HH:mm:ss" :disabled-date="disabledDate" placeholder="选择截止时间" /></el-form-item></el-form>
      <template #footer><el-button @click="cancel">取消</el-button><el-button v-has="'problem:contest:edit'" type="primary" :loading="submitting" @click="submitForm">保存设置</el-button></template>
    </el-dialog>
  </ContestSubPageShell>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ArrowLeft, Clock, Delete, Plus, Refresh, Search, Timer } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import type { IdType } from '@/api/common'
import { addSupplement, getSupplements, removeSupplement, type Supplement, type SupplementForm } from '@/api/contest/supplement'
import ContestSubPageShell from '../component/ContestSubPageShell.vue'

const route = useRoute(); const router = useRouter(); const contestId = String(route.params.contestId || '')
const loading = ref(false); const submitting = ref(false); const open = ref(false); const keyword = ref(''); const list = ref<Supplement[]>([]); const selectedIds = ref<IdType[]>([]); const formRef = ref<FormInstance>()
const form = reactive<SupplementForm>({ contestId, userId: undefined, deadline: undefined })
const rules: FormRules<SupplementForm> = { userId: [{ required: true, message: '请输入用户 ID', trigger: 'blur' }], deadline: [{ required: true, message: '请选择截止时间', trigger: 'change' }] }
const filteredList = computed(() => { const value = keyword.value.trim().toLowerCase(); return value ? list.value.filter((item) => String(item.userId).toLowerCase().includes(value) || String(item.nikeName || '').toLowerCase().includes(value)) : list.value })
const stats = computed(() => [{ label: '延期用户', value: list.value.length, tone: 'amber' }, { label: '当前生效', value: list.value.filter((item) => new Date(item.deadline).getTime() >= Date.now()).length, tone: 'green' }, { label: '已选择', value: selectedIds.value.length, tone: 'violet' }, { label: '竞赛 / 作业', value: contestId, tone: 'blue' }])
const getList = async () => { loading.value = true; try { list.value = await getSupplements(contestId) || []; selectedIds.value = [] } finally { loading.value = false } }
const handleSelectionChange = (selection: Supplement[]) => { selectedIds.value = selection.map((item) => item.userId) }
const handleAdd = () => { form.userId = undefined; form.deadline = undefined; open.value = true }
const submitForm = async () => { if (!(await formRef.value?.validate().catch(() => false))) return; submitting.value = true; try { await addSupplement(form); ElMessage.success('迟交权限已添加'); open.value = false; await getList() } finally { submitting.value = false } }
const handleDelete = async (row?: Supplement) => { const ids = row ? [row.userId] : selectedIds.value; if (!ids.length) return; await ElMessageBox.confirm(`确定撤销 ${ids.length} 位用户的迟交权限吗？`, '撤销迟交权限', { confirmButtonText: '确认撤销', cancelButtonText: '取消', type: 'warning' }); await Promise.all(ids.map((id) => removeSupplement(contestId, id))); ElMessage.success('迟交权限已撤销'); await getList() }
const cancel = () => { open.value = false; formRef.value?.resetFields() }
const disabledDate = (date: Date) => date.getTime() < Date.now() - 86400000
onMounted(getList)
</script>

<style scoped>
.extension-panel { padding: 24px; border: 1px solid var(--el-border-color-lighter); border-radius: 20px; background: var(--el-bg-color); box-shadow: 0 14px 40px rgb(15 23 42 / 4%); }.panel-heading { display: flex; align-items: flex-end; justify-content: space-between; gap: 20px; margin-bottom: 18px; }.eyebrow { color: var(--el-color-warning); font-size: 10px; font-weight: 800; letter-spacing: .14em; }.panel-heading h2 { margin: 7px 0 5px; font-size: 21px; }.panel-heading p { margin: 0; color: var(--el-text-color-secondary); font-size: 12px; }.keyword-input { width: 260px; }.extension-table { border-radius: 14px; overflow: hidden; }.user-cell, .user-cell > div:last-child, .deadline { display: flex; align-items: center; }.user-cell { gap: 11px; }.user-cell > div:last-child { align-items: flex-start; flex-direction: column; gap: 4px; }.user-cell strong { color: var(--el-text-color-primary); }.user-cell span, .deadline { color: var(--el-text-color-secondary); font-size: 12px; }.avatar { display: grid; width: 36px; height: 36px; place-items: center; border-radius: 11px; background: color-mix(in srgb, var(--el-color-warning) 13%, var(--el-bg-color)); color: var(--el-color-warning) !important; font-weight: 800; }.deadline { gap: 7px; }.active-pill { display: inline-flex; align-items: center; gap: 6px; font-size: 12px; }.active-pill i { width: 6px; height: 6px; border-radius: 50%; background: currentColor; }.active-pill.is-active { color: var(--el-color-success); }.active-pill.is-expired { color: var(--el-text-color-placeholder); }.dialog-intro { display: flex; align-items: center; gap: 11px; margin-bottom: 20px; padding: 14px 16px; border-radius: 12px; background: var(--el-fill-color-light); }.dialog-icon { display: grid; width: 34px; height: 34px; flex: 0 0 auto; place-items: center; border-radius: 10px; background: color-mix(in srgb, var(--el-color-warning) 12%, var(--el-bg-color)); color: var(--el-color-warning); }.dialog-intro strong, .dialog-intro p { display: block; }.dialog-intro p { margin: 4px 0 0; color: var(--el-text-color-secondary); font-size: 12px; }.full-width { width: 100%; }.extension-table :deep(.el-table__header th) { color: var(--el-text-color-secondary); background: var(--el-fill-color-light); }
@media (max-width: 680px) { .extension-panel { padding: 16px 12px; }.panel-heading { align-items: stretch; flex-direction: column; }.keyword-input { width: 100%; } }
</style>
