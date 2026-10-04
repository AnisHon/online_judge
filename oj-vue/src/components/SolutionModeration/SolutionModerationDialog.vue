<template>
  <el-dialog :model-value="modelValue" :title="title" width="min(540px, 94vw)" append-to-body
             :close-on-click-modal="false" @update:model-value="$emit('update:modelValue', $event)">
    <el-form label-position="top" @submit.prevent="submit">
      <p class="target">{{ targetTitle || '所选内容' }} <span :title="targetId">#{{ targetId }}</span></p>
      <p class="hint">{{ mode === 'RESTORE' ? '解除审核限制后，仍按作者的可见性设置和关联题目权限展示。' : '处理原因会提供给作者，请填写清晰的说明。' }}</p>
      <el-form-item label="处理原因" required :error="error">
        <el-input v-model="reason" type="textarea" :rows="5" :disabled="pending" placeholder="说明本次处理的原因" />
        <small :class="{'over-limit': reasonLength > 500}">{{ reasonLength }} / 500</small>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="$emit('update:modelValue', false)">取消</el-button>
      <el-button :type="mode.includes('DELETE') ? 'danger' : 'primary'" :loading="pending"
                 :disabled="pending || !allowed" class="submit-button" @click="submit">确认处理</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import {computed, onBeforeUnmount, watch} from 'vue'
import {ElNotification} from 'element-plus'
import {deleteSolutionAdmin, moderateSolution} from '@/api/solution'
import {deleteCommentAdmin} from '@/api/comment/admin'
import {useUserStore} from '@/stores/useUserStore'
import {moderationPermissions, useModerationForm, type ModerationMode} from './moderationForm'

const props = defineProps<{modelValue: boolean; targetId: string; targetTitle?: string; mode: ModerationMode}>()
const emit = defineEmits<{(event: 'update:modelValue', value: boolean): void; (event: 'success'): void}>()
const store = useUserStore()
const permissions = computed(() => moderationPermissions(store.getAuths()))
const allowed = computed(() => props.mode === 'COMMENT_DELETE' ? permissions.value.removeComment
  : props.mode === 'SOLUTION_DELETE' ? permissions.value.remove : permissions.value.edit)
const title = computed(() => props.mode === 'AUTHOR_ONLY' ? '限制题解可见性'
  : props.mode === 'RESTORE' ? '解除审核限制' : props.mode === 'COMMENT_DELETE' ? '删除评论' : '删除题解')
const {reason, pending, error, reset, submit: send} = useModerationForm()
const reasonLength = computed(() => Array.from(reason.value).length)

const submit = async () => {
  if (!props.modelValue || !props.targetId) return
  const id = props.targetId
  const mode = props.mode
  const success = await send(allowed.value, (text, signal) => mode === 'COMMENT_DELETE'
    ? deleteCommentAdmin(id, text, signal) : mode === 'SOLUTION_DELETE'
      ? deleteSolutionAdmin(id, text, signal) : moderateSolution(id, mode, text, signal))
  if (!success) return
  ElNotification.success('处理成功')
  emit('success')
  emit('update:modelValue', false)
}
watch(() => [props.modelValue, props.targetId, props.mode, store.user?.userId], reset, {flush: 'sync'})
onBeforeUnmount(reset)
</script>

<style scoped>
.target { display: flex; flex-wrap: wrap; gap: 6px 12px; margin: 0; overflow-wrap: anywhere; font-weight: 600; }
.target span { max-width: 100%; overflow: hidden; text-overflow: ellipsis; color: var(--el-text-color-secondary); font-size: 12px; }
.hint { margin: 12px 0 20px; color: var(--el-text-color-secondary); line-height: 1.7; }
small { display: block; width: 100%; color: var(--el-text-color-secondary); text-align: right; }
.over-limit { color: var(--el-color-danger); }
.submit-button { min-width: 116px; }
</style>
