<template>
  <ProblemModuleShell
    title="标签管理"
    kicker="PROBLEM / TAGS"
    description="用清晰的标签体系组织题库，帮助题目筛选与知识点维护。"
    :icon="CollectionTag"
    tone="violet"
  >
    <div class="module-toolbar">
      <div><span class="eyebrow">TAXONOMY</span><strong>题目标签</strong><small>共 {{ filteredList.length }} 个标签</small></div>
      <div class="toolbar-actions">
        <el-input v-model="keyword" clearable :prefix-icon="Search" placeholder="搜索标签名称" />
        <el-button :icon="Refresh" :loading="isLoading" @click="refreshList">刷新标签</el-button>
        <el-button v-has="'problem:tag:add'" type="primary" :icon="Plus" @click="handleAdd">新建标签</el-button>
        <el-button v-has="'problem:tag:remove'" type="danger" plain :disabled="!ids.length" :icon="Delete" @click="handleDelete()">删除</el-button>
      </div>
    </div>

    <el-table v-loading="isLoading" class="tag-table" :data="filteredList" row-key="tagId" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="54" align="center" />
      <el-table-column label="标签" min-width="280">
        <template #default="{ row }"><div class="tag-name-cell"><span class="tag-dot" :style="{ backgroundColor: row.tagColor || '#64748b' }" /><div><strong>{{ row.tagName }}</strong><span>#{{ row.tagId }}</span></div></div></template>
      </el-table-column>
      <el-table-column label="颜色" width="180">
        <template #default="{ row }"><span class="color-code"><i :style="{ backgroundColor: row.tagColor || '#64748b' }" />{{ row.tagColor || '未设置' }}</span></template>
      </el-table-column>
      <el-table-column label="预览" width="180">
        <template #default="{ row }"><el-tag :color="row.tagColor || '#64748b'" effect="dark" round>{{ row.tagName }}</el-tag></template>
      </el-table-column>
      <el-table-column label="创建时间" min-width="190" prop="createTime" show-overflow-tooltip />
      <el-table-column label="操作" width="180" fixed="right" align="right">
        <template #default="{ row }"><el-space><el-button v-has="'problem:tag:edit'" link type="primary" :icon="Edit" @click="handleUpdate(row)">编辑</el-button><el-button v-has="'problem:tag:remove'" link type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button></el-space></template>
      </el-table-column>
      <template #empty><el-empty description="还没有标签" /></template>
    </el-table>

    <el-dialog v-model="open" :title="dialogState === 1 ? '新建标签' : '编辑标签'" width="min(520px, 92vw)" append-to-body>
      <div class="dialog-intro"><span class="eyebrow">TAG SETTINGS</span><p>标签颜色会同步用于题目列表和筛选器。</p></div>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="标签名称" prop="tagName"><el-input v-model="form.tagName" maxlength="40" show-word-limit placeholder="例如：动态规划" /></el-form-item>
        <el-form-item label="标签颜色" prop="tagColor"><div class="color-control"><el-color-picker v-model="form.tagColor" :show-alpha="false" @change="normalizeTagColor" /><code>{{ form.tagColor || '请选择颜色' }}</code></div><small class="field-tip">仅保存十六进制颜色值，例如 #080942。</small></el-form-item>
      </el-form>
      <template #footer><el-button @click="cancel">取消</el-button><el-button type="primary" :loading="submitting" @click="submitForm">保存标签</el-button></template>
    </el-dialog>
  </ProblemModuleShell>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { CollectionTag, Delete, Edit, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { addTag, getTag, removeTag, type TagForm, type TagView, updateTag } from '@/api/tag'
import type { IdType } from '@/api/common'
import ProblemModuleShell from '@/views/backend/problem-module/component/ProblemModuleShell.vue'

const tableList = ref<TagView[]>([])
const keyword = ref('')
const ids = ref<IdType[]>([])
const open = ref(false)
const submitting = ref(false)
const loading = ref(false)
const isLoading = computed(() => loading.value)
const dialogState = ref(1)
const formRef = ref<FormInstance>()
const form = reactive<TagForm>({ tagId: undefined, tagName: '', tagColor: '#6366f1' })
const rules: FormRules<TagForm> = { tagName: [{ required: true, message: '请输入标签名称', trigger: 'blur' }], tagColor: [{ required: true, message: '请选择标签颜色', trigger: 'change' }] }

const filteredList = computed(() => {
  const value = keyword.value.trim().toLowerCase()
  return value ? tableList.value.filter((item) => item.tagName.toLowerCase().includes(value) || String(item.tagId).includes(value)) : tableList.value
})

const colorToHex = (value?: string) => {
  const text = String(value || '').trim()
  const hex = text.match(/^#([\da-f]{3}|[\da-f]{6})$/i)?.[1]
  if (hex) {
    const full = hex.length === 3 ? hex.split('').map(char => `${char}${char}`).join('') : hex
    return `#${full.toUpperCase()}`
  }
  const rgb = text.match(/^rgba?\(\s*(\d{1,3})\s*,\s*(\d{1,3})\s*,\s*(\d{1,3})(?:\s*,\s*[\d.]+)?\s*\)$/i)
  if (!rgb) return '#6366F1'
  return `#${[rgb[1], rgb[2], rgb[3]].map(channel => Math.min(255, Math.max(0, Number(channel))).toString(16).padStart(2, '0')).join('').toUpperCase()}`
}
const normalizeTagColor = (value?: string) => { form.tagColor = colorToHex(value || form.tagColor) }
const resetForm = () => Object.assign(form, { tagId: undefined, tagName: '', tagColor: '#6366F1' })
const getList = async () => { loading.value = true; try { tableList.value = (await getTag()) || [] } finally { loading.value = false } }
const refreshList = async () => { ids.value = []; await getList() }
const handleSelectionChange = (selection: TagView[]) => { ids.value = selection.map((item) => item.tagId) }
const handleAdd = () => { resetForm(); dialogState.value = 1; open.value = true }
const handleUpdate = (data?: TagView) => { const item = data || tableList.value.find((x) => x.tagId === ids.value[0]); if (!item) return; Object.assign(form, item, { tagColor: colorToHex(item.tagColor) }); dialogState.value = 2; open.value = true }
const submitForm = async () => { if (!(await formRef.value?.validate().catch(() => false))) return; form.tagColor = colorToHex(form.tagColor); submitting.value = true; try { if (dialogState.value === 1) await addTag(form); else await updateTag(form); ElMessage.success('标签已保存'); open.value = false; resetForm(); await refreshList() } finally { submitting.value = false } }
const handleDelete = async (row?: TagView) => { const target = row ? [row.tagId] : ids.value; if (!target.length) return; await ElMessageBox.confirm(`确定删除选中的 ${target.length} 个标签吗？`, '删除标签', { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' }); await removeTag(target); ElMessage.success('标签已删除'); await getList() }
const cancel = () => { open.value = false; resetForm() }

onMounted(getList)
</script>

<style lang="scss" scoped>
.module-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 20px; margin-bottom: 18px; }.module-toolbar > div:first-child { display: flex; align-items: baseline; gap: 10px; }.eyebrow { color: #8b5cf6; font-size: 10px; font-weight: 800; letter-spacing: .14em; }.module-toolbar strong { font-size: 18px; }.module-toolbar small { color: var(--el-text-color-secondary); font-size: 12px; }.toolbar-actions { display: flex; align-items: center; gap: 9px; }.toolbar-actions .el-input { width: 220px; }.tag-table { border-radius: 14px; overflow: hidden; }.tag-name-cell { display: flex; align-items: center; gap: 12px; }.tag-dot { width: 10px; height: 10px; flex: 0 0 auto; border-radius: 50%; box-shadow: 0 0 0 5px color-mix(in srgb, var(--el-text-color-primary) 5%, transparent); }.tag-name-cell div { display: flex; flex-direction: column; gap: 4px; }.tag-name-cell strong { color: var(--el-text-color-primary); }.tag-name-cell span { color: var(--el-text-color-secondary); font-size: 12px; }.color-code { display: inline-flex; align-items: center; gap: 8px; color: var(--el-text-color-secondary); font: 12px var(--code-font-family, monospace); }.color-code i { width: 16px; height: 16px; border-radius: 5px; }.dialog-intro { margin-bottom: 20px; padding: 14px 16px; border-radius: 12px; background: var(--el-fill-color-light); }.dialog-intro p { margin: 7px 0 0; color: var(--el-text-color-secondary); font-size: 13px; }.color-control { display: flex; align-items: center; gap: 12px; color: var(--el-text-color-secondary); font: 12px var(--code-font-family, monospace); }.color-control code { color: var(--el-text-color-primary); font: inherit; }.field-tip { display: block; margin-top: 6px; color: var(--el-text-color-secondary); font-size: 11px; }
@media (max-width: 720px) { .module-toolbar { align-items: flex-start; flex-direction: column; }.toolbar-actions { width: 100%; }.toolbar-actions .el-input { flex: 1; width: auto; } }
@media (max-width: 480px) { .module-content { padding: 14px 10px; }.module-toolbar > div:first-child { flex-wrap: wrap; gap: 5px 8px; }.toolbar-actions { flex-wrap: wrap; }.toolbar-actions .el-input { width: 100%; flex-basis: 100%; } }
</style>
