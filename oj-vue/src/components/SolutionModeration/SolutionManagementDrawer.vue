<template>
  <el-drawer :model-value="modelValue" title="题解详情" size="min(960px, 100vw)" append-to-body
             @update:model-value="$emit('update:modelValue', $event)">
    <el-empty v-if="!permissions.list" description="当前账号没有题解管理查看权限" />
    <section v-else class="solution-detail" v-loading="loading">
      <p v-if="error" class="error" role="status">{{ error }} <el-button link @click="load">重试</el-button></p>
      <template v-if="solution">
        <header>
          <h2>{{ solution.title }}</h2>
          <p class="metadata">题解 #{{ solution.solutionId }} · 作者 {{ solution.nikeName || `#${solution.userId}` }}</p>
          <p class="metadata">{{ solution.problemTitle || '关联题目' }} · #{{ solution.problemId }}</p>
          <div class="status"><el-tag effect="plain">{{ solutionVisibilityLabel(solution) }}</el-tag><el-tag effect="plain">{{ solution.commentsOpen === false ? '评论已关闭' : '评论开放' }}</el-tag></div>
        </header>
        <div class="actions">
          <el-button v-if="permissions.edit" @click="$emit('edit', solution)">编辑内容</el-button>
          <el-button v-if="permissions.edit && solution.moderationState !== 'AUTHOR_ONLY'" type="warning" plain @click="openAction('AUTHOR_ONLY')">限制可见性</el-button>
          <el-button v-if="permissions.edit && solution.moderationState === 'AUTHOR_ONLY'" type="primary" plain @click="openAction('RESTORE')">解除审核限制</el-button>
          <el-button v-if="permissions.comments" @click="commentsOpen = true">管理评论</el-button>
          <el-button v-if="permissions.remove" type="danger" plain @click="openAction('SOLUTION_DELETE')">删除题解</el-button>
        </div>
        <el-tabs v-model="tab">
          <el-tab-pane label="题解正文" name="body"><MarkdownPreview :text="solution.content" variant="compact" /></el-tab-pane>
          <el-tab-pane label="处理记录" name="history">
            <SolutionModerationHistory :solution-id="solutionId" :active="modelValue && tab === 'history'" :revision="revision" />
          </el-tab-pane>
        </el-tabs>
      </template>
    </section>
    <SolutionModerationDialog v-model="actionOpen" :target-id="solutionId" :target-title="solution?.title" :mode="mode" @success="changed" />
    <CommentManagementDrawer v-model="commentsOpen" :solution-id="solutionId" @changed="commentsChanged" />
  </el-drawer>
</template>

<script setup lang="ts">
import {computed, onBeforeUnmount, ref, watch} from 'vue'
import {getSolutionAdmin, type Solution} from '@/api/solution'
import {useUserStore} from '@/stores/useUserStore'
import {solutionVisibilityLabel} from '@/utils/solutionVisibility'
import MarkdownPreview from '@/components/MarkdownPreview.vue'
import CommentManagementDrawer from '@/components/CommentManagement/CommentManagementDrawer.vue'
import SolutionModerationDialog from './SolutionModerationDialog.vue'
import SolutionModerationHistory from './SolutionModerationHistory.vue'
import {moderationError, moderationPermissions, type ModerationMode} from './moderationForm'

const props = defineProps<{modelValue: boolean; solutionId: string}>()
const emit = defineEmits<{(event: 'update:modelValue', value: boolean): void;
  (event: 'changed'): void; (event: 'edit', solution: Solution): void}>()
const store = useUserStore()
const permissions = computed(() => moderationPermissions(store.getAuths()))
const solution = ref<Solution>()
const loading = ref(false)
const error = ref('')
const tab = ref('body')
const actionOpen = ref(false)
const commentsOpen = ref(false)
const mode = ref<ModerationMode>('AUTHOR_ONLY')
const revision = ref(0)
let generation = 0
let controller: AbortController | undefined
const cancel = () => { generation++; controller?.abort(); loading.value = false }
const load = async () => {
  cancel()
  if (!props.modelValue || !props.solutionId || !permissions.value.list) return
  const version = generation
  const current = new AbortController()
  controller = current
  loading.value = true
  error.value = ''
  try {
    const result = await getSolutionAdmin(props.solutionId, current.signal)
    if (version === generation) solution.value = result
  } catch (failure) {
    if (version === generation && !current.signal.aborted) error.value = moderationError(failure)
  } finally { if (version === generation) loading.value = false }
}
const openAction = (value: ModerationMode) => { mode.value = value; actionOpen.value = true }
const changed = () => {
  emit('changed'); revision.value++
  if (mode.value === 'SOLUTION_DELETE') emit('update:modelValue', false)
  else void load()
}
const commentsChanged = () => { revision.value++; emit('changed'); void load() }
watch(() => [props.modelValue, props.solutionId, permissions.value.list, store.user?.userId], () => {
  cancel(); solution.value = undefined; error.value = ''; tab.value = 'body'
  actionOpen.value = false; commentsOpen.value = false; revision.value++
  void load()
}, {immediate: true, flush: 'sync'})
onBeforeUnmount(cancel)
</script>

<style scoped>
.solution-detail { min-width: 0; width: 100%; color: var(--el-text-color-primary); }
h2 { margin: 0 0 10px; overflow-wrap: anywhere; }
.metadata { margin: 5px 0; color: var(--el-text-color-secondary); overflow-wrap: anywhere; }
.status, .actions { display: flex; flex-wrap: wrap; gap: 8px; }
.status { margin-top: 12px; }
.actions { margin: 20px 0; }
.actions .el-button { margin: 0; }
.error { color: var(--el-color-danger); }
.solution-detail :deep(.el-tabs__content), .solution-detail :deep(.el-tab-pane) { min-width: 0; max-width: 100%; }
</style>
