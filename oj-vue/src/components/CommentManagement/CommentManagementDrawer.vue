<template>
  <el-drawer :model-value="modelValue" title="评论管理" size="min(720px, 100vw)" append-to-body
             @update:model-value="$emit('update:modelValue', $event)">
    <el-empty v-if="!permissions.comments" description="评论管理需要题解查看、后台访问及评论查看权限" />
    <section v-else class="comment-management">
      <div class="filters">
        <el-select v-model="state" placeholder="全部状态" clearable aria-label="评论状态" @change="filter">
          <el-option label="可见" value="VISIBLE" /><el-option label="作者已删除" value="AUTHOR_DELETED" />
          <el-option label="管理员已删除" value="ADMIN_DELETED" />
        </el-select>
        <el-button :loading="pending" @click="load">刷新</el-button>
      </div>
      <p v-if="error" class="error" role="status">{{ error }}</p>
      <div v-loading="pending">
        <article v-for="row in rows" :key="row.commentId" class="comment-record">
          <header><strong>{{ row.rootId ? '回复' : '评论' }}</strong><el-tag size="small" effect="plain">{{ stateLabel(row.state) }}</el-tag></header>
          <small>评论 #{{ row.commentId }} · 作者 #{{ row.userId }} · {{ row.createdAt }}</small>
          <small v-if="row.parentId">回复评论 #{{ row.parentId }} · 根评论 #{{ row.rootId }}</small>
          <CommentText :text="row.content || ''" />
          <div v-if="row.reason" class="processing"><strong>处理说明</strong><CommentText :text="row.reason" /><small>操作者 #{{ row.operatorId }} · {{ row.actionAt }}</small></div>
          <div v-if="row.state === 'VISIBLE'" class="actions">
            <el-button v-if="permissions.removeComment" type="danger" plain size="small" @click="moderate(row)">管理删除</el-button>
            <el-button v-if="isUserIdEqual(row.userId)" size="small" :loading="ownPending === row.commentId"
                       :disabled="!!ownPending" @click="deleteOwn(row)">删除本人评论</el-button>
          </div>
        </article>
        <el-empty v-if="!pending && !rows.length && !error" description="暂无评论" :image-size="70" />
      </div>
      <el-pagination v-if="total > 20" v-model:current-page="page" :total="total" :page-size="20"
                     layout="prev, pager, next" @current-change="load" />
    </section>
    <SolutionModerationDialog v-model="dialogOpen" :target-id="targetId" mode="COMMENT_DELETE"
                              target-title="所选评论" @success="changed" />
  </el-drawer>
</template>

<script setup lang="ts">
import {computed, onBeforeUnmount, ref, watch} from 'vue'
import {ElMessageBox, ElNotification} from 'element-plus'
import {getAdminComments, type AdminComment} from '@/api/comment/admin'
import {deleteOwnComment, type CommentState} from '@/api/comment'
import CommentText from '@/components/CommentThread/CommentText.vue'
import SolutionModerationDialog from '@/components/SolutionModeration/SolutionModerationDialog.vue'
import {moderationError, moderationPermissions} from '@/components/SolutionModeration/moderationForm'
import {useUserStore} from '@/stores/useUserStore'
import {isUserIdEqual} from '@/utils/authUtil'

const props = defineProps<{modelValue: boolean; solutionId: string}>()
const emit = defineEmits<{(event: 'update:modelValue', value: boolean): void; (event: 'changed'): void}>()
const store = useUserStore()
const permissions = computed(() => moderationPermissions(store.getAuths()))
const state = ref<CommentState>()
const rows = ref<AdminComment[]>([])
const total = ref(0)
const page = ref(1)
const pending = ref(false)
const ownPending = ref('')
const error = ref('')
const targetId = ref('')
const dialogOpen = ref(false)
let generation = 0
let controller: AbortController | undefined
const cancel = () => { generation++; controller?.abort(); pending.value = false }
const stateLabel = (value: CommentState) => ({VISIBLE: '可见', AUTHOR_DELETED: '作者已删除', ADMIN_DELETED: '管理员已删除'}[value])
const load = async () => {
  cancel()
  if (!props.modelValue || !props.solutionId || !permissions.value.comments) return
  const version = generation
  const current = new AbortController()
  controller = current
  pending.value = true
  error.value = ''
  try {
    const result = await getAdminComments(props.solutionId, page.value, state.value || undefined, current.signal)
    if (version !== generation) return
    rows.value = result.data
    total.value = result.totalRecords
    if (page.value > 1 && !rows.value.length) {
      page.value = Math.max(1, Math.ceil(total.value / 20))
      void load()
    }
  } catch (failure) {
    if (version === generation && !current.signal.aborted) error.value = moderationError(failure)
  } finally { if (version === generation) pending.value = false }
}
const filter = () => { page.value = 1; void load() }
const moderate = (row: AdminComment) => {
  if (!permissions.value.removeComment) return
  targetId.value = row.commentId
  dialogOpen.value = true
}
const changed = () => { emit('changed'); void load() }
const deleteOwn = async (row: AdminComment) => {
  if (ownPending.value || !isUserIdEqual(row.userId)) return
  ownPending.value = row.commentId
  const version = generation
  try {
    await ElMessageBox.confirm('以作者身份删除本人评论，不记录管理处理原因。确认删除？', '删除本人评论', {type: 'warning'})
    if (version !== generation || !props.modelValue) return
    const deleted = await deleteOwnComment(row.commentId)
    if (version !== generation) return
    if (!deleted) { error.value = '评论未删除，请刷新后重试'; return }
    ElNotification.success('评论已删除')
    changed()
  } catch (failure) {
    if (version === generation && failure !== 'cancel' && failure !== 'close') error.value = moderationError(failure)
  } finally { if (ownPending.value === row.commentId) ownPending.value = '' }
}
watch(() => [props.modelValue, props.solutionId, permissions.value.comments, store.user?.userId], () => {
  cancel(); dialogOpen.value = false; rows.value = []; total.value = 0; page.value = 1; state.value = undefined; error.value = ''
  void load()
}, {immediate: true, flush: 'sync'})
onBeforeUnmount(cancel)
</script>

<style scoped>
.comment-management { min-width: 0; color: var(--el-text-color-primary); }
.filters { display: flex; align-items: center; gap: 10px; margin-bottom: 16px; }
.filters .el-select { min-width: 0; flex: 1; }
.comment-record { padding: 18px 0; border-bottom: 1px solid var(--el-border-color-lighter); }
.comment-record header { display: flex; align-items: center; gap: 10px; margin-bottom: 6px; }
small { display: block; color: var(--el-text-color-secondary); overflow-wrap: anywhere; line-height: 1.8; }
.processing { margin: 12px 0; padding: 12px; background: var(--el-fill-color-light); border-radius: 10px; }
.processing strong { font-size: 12px; }
.actions { display: flex; flex-wrap: wrap; gap: 8px; }
.actions .el-button { margin: 0; }
.error { color: var(--el-color-danger); }
.el-pagination { margin-top: 18px; justify-content: center; }
</style>
