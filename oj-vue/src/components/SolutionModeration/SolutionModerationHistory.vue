<template>
  <section class="moderation-history" aria-label="处理记录">
    <p v-if="error" class="error" role="status">{{ error }} <el-button link @click="load">重试</el-button></p>
    <div v-loading="pending">
      <article v-for="item in rows" :key="item.actionId" class="history-entry">
        <header><strong>{{ moderationActionLabel(item.action, item.targetType) }}</strong><el-tag size="small" effect="plain">{{ item.targetType === 'COMMENT' ? '评论' : '题解' }}</el-tag></header>
        <p class="reason">{{ item.reason }}</p>
        <small>{{ item.createdAt }} · 操作者 #{{ item.operatorId }} · 目标 #{{ item.targetId }}</small>
      </article>
      <el-empty v-if="!pending && !rows.length && !error" description="暂无处理记录" :image-size="70" />
    </div>
    <el-pagination v-if="total > 20" v-model:current-page="page" :total="total" :page-size="20"
                   layout="prev, pager, next" @current-change="load" />
  </section>
</template>

<script setup lang="ts">
import {onBeforeUnmount, ref, watch} from 'vue'
import {getSolutionModerationHistory, type ModerationAction} from '@/api/solution'
import {hasPerm} from '@/utils/authUtil'
import {useUserStore} from '@/stores/useUserStore'
import {moderationActionLabel, moderationError} from './moderationForm'

const props = withDefaults(defineProps<{solutionId: string; active?: boolean; revision?: number}>(), {active: true, revision: 0})
const store = useUserStore()
const rows = ref<ModerationAction[]>([])
const total = ref(0)
const page = ref(1)
const pending = ref(false)
const error = ref('')
let generation = 0
let controller: AbortController | undefined
const cancel = () => { generation++; controller?.abort(); pending.value = false }
const load = async () => {
  cancel()
  if (!props.active || !props.solutionId || !hasPerm(['system:backend:access', 'problem:solution:list'])) return
  const version = generation
  const current = new AbortController()
  controller = current
  pending.value = true
  error.value = ''
  try {
    const data = await getSolutionModerationHistory(props.solutionId, page.value, current.signal)
    if (version !== generation) return
    rows.value = data.data
    total.value = data.totalRecords
  } catch (failure) {
    if (version === generation && !current.signal.aborted) error.value = moderationError(failure)
  } finally { if (version === generation) pending.value = false }
}
watch(() => [props.solutionId, props.active, props.revision, store.user?.userId, store.getAuths().includes('problem:solution:list')], () => {
  cancel(); rows.value = []; total.value = 0; page.value = 1
  void load()
}, {immediate: true, flush: 'sync'})
onBeforeUnmount(cancel)
</script>

<style scoped>
.moderation-history { min-width: 0; }
.history-entry { padding: 16px 0; border-bottom: 1px solid var(--el-border-color-lighter); }
.history-entry header { display: flex; flex-wrap: wrap; align-items: center; gap: 10px; }
.reason { white-space: pre-wrap; overflow-wrap: anywhere; color: var(--el-text-color-regular); line-height: 1.7; }
small { display: block; color: var(--el-text-color-secondary); overflow-wrap: anywhere; }
.error { color: var(--el-color-danger); }
.el-pagination { margin-top: 18px; justify-content: center; }
</style>
