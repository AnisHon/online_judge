<template>
  <section class="solution-interactions" aria-label="题解互动">
    <div class="solution-interactions__actions">
      <button
        class="interaction-button"
        :class="{'interaction-button--active': liked}"
        type="button"
        :aria-pressed="liked"
        :disabled="likePending"
        @click="toggleLike"
      >
        <span class="interaction-button__icon" aria-hidden="true">
          <el-icon v-if="likePending" class="is-loading"><Loading /></el-icon>
          <el-icon v-else><Star /></el-icon>
        </span>
        <span>{{ liked ? '已点赞' : '点赞' }}</span>
        <span class="interaction-button__count">{{ likeCount ?? '—' }}</span>
      </button>

      <button class="interaction-button" type="button" @click="$emit('focus-comments')">
        <span class="interaction-button__icon" aria-hidden="true"><el-icon><ChatDotRound /></el-icon></span>
        <span>评论</span>
        <span class="interaction-button__count">{{ solution.commentCount ?? '0' }}</span>
      </button>

      <el-button
        v-if="canManageComments && typeof solution.commentsOpen === 'boolean'"
        class="comments-state-button"
        :type="solution.commentsOpen ? 'default' : 'warning'"
        :loading="commentsPending"
        :disabled="commentsPending"
        @click="toggleComments"
      >
        <el-icon><component :is="solution.commentsOpen ? Lock : Unlock" /></el-icon>
        {{ solution.commentsOpen ? '关闭评论' : '开启评论' }}
      </el-button>

      <el-button v-if="isAuthor" text @click="toggleModeration">
        {{ showModeration ? '收起处理记录' : '处理记录' }}
      </el-button>
    </div>

    <p v-if="likeError || commentsError" class="interaction-error" role="status">
      {{ likeError || commentsError }}
    </p>

    <div v-if="showModeration" class="moderation-panel" aria-live="polite">
      <p v-if="moderationLoading" class="moderation-panel__hint">正在加载处理记录…</p>
      <p v-else-if="moderationError" class="moderation-panel__hint moderation-panel__hint--error">{{ moderationError }}</p>
      <p v-else-if="moderationActions.length === 0" class="moderation-panel__hint">暂无处理记录</p>
      <article v-for="(action, index) in moderationActions" :key="`${action.createdAt}-${index}`" class="moderation-entry">
        <div class="moderation-entry__meta">
          <strong>{{ moderationLabel(action.action) }}</strong>
          <time>{{ formatDate(action.createdAt) }}</time>
        </div>
        <p>{{ action.reason }}</p>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import {computed, onScopeDispose, ref} from 'vue'
import {ChatDotRound, Loading, Lock, Star, Unlock} from '@element-plus/icons-vue'
import type {Solution} from '@/api/solution'
import {getSolutionModeration, setSolutionCommentsState} from '@/api/solution'
import {useLikeAction} from '@/composables/social/useLikeAction'
import {commentUiError} from '@/utils/commentText'
import {hasPerm, isUserIdEqual} from '@/utils/authUtil'

const props = defineProps<{solution: Solution}>()
const emit = defineEmits<{
  (event: 'focus-comments'): void
  (event: 'comments-state-change', open: boolean): void
}>()

const solutionId = computed(() => props.solution.solutionId)
const isAuthor = computed(() => isUserIdEqual(props.solution.userId))
const canManageComments = computed(() => !hasPerm('policy:solution:deny')
  && (isAuthor.value || hasPerm('problem:solution:edit')))
const likeAction = useLikeAction(
  'solution', solutionId,
  computed(() => props.solution.likedByMe),
  computed(() => props.solution.likeCount ?? null),
)
const {liked, likeCount, pending: likePending, error: likeError, toggle: toggleLike} = likeAction
const commentsPending = ref(false)
const commentsError = ref('')
let commentsFlight: Promise<void> | null = null
let commentsGeneration = 0

const toggleComments = (): Promise<void> => {
  if (commentsFlight) return commentsFlight
  const id = solutionId.value
  const nextOpen = !props.solution.commentsOpen
  const generation = ++commentsGeneration
  commentsPending.value = true
  commentsError.value = ''
  const request = setSolutionCommentsState(id, nextOpen).then(result => {
    if (generation !== commentsGeneration || solutionId.value !== id) return
    emit('comments-state-change', result.commentsOpen)
  }).catch(error => {
    if (generation === commentsGeneration) commentsError.value = commentUiError(error)
  }).finally(() => {
    if (generation === commentsGeneration) {
      commentsPending.value = false
      commentsFlight = null
    }
  })
  commentsFlight = request
  return request
}

const showModeration = ref(false)
const moderationLoading = ref(false)
const moderationError = ref('')
const moderationActions = ref<{action: string; reason: string; createdAt: string}[]>([])
let moderationLoadedFor: string | null = null
let moderationController: AbortController | null = null
let moderationGeneration = 0

const loadModeration = async () => {
  const id = solutionId.value
  if (moderationLoadedFor === id || moderationLoading.value) return
  moderationController?.abort()
  const controller = new AbortController()
  moderationController = controller
  const generation = ++moderationGeneration
  moderationLoading.value = true
  moderationError.value = ''
  try {
    moderationActions.value = await getSolutionModeration(id, controller.signal)
    if (generation === moderationGeneration && solutionId.value === id) moderationLoadedFor = id
  } catch (error) {
    if (!controller.signal.aborted && generation === moderationGeneration) moderationError.value = commentUiError(error)
  } finally {
    if (generation === moderationGeneration) moderationLoading.value = false
  }
}

const toggleModeration = () => {
  showModeration.value = !showModeration.value
  if (showModeration.value) void loadModeration()
}

const moderationLabel = (action: string) => ({
  AUTHOR_ONLY: '设为仅作者可见', RESTORE: '恢复公开', DELETE: '删除题解',
}[action] || '题解处理')
const formatDate = (value: string) => {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '' : date.toLocaleString()
}

onScopeDispose(() => {
  commentsGeneration++
  moderationGeneration++
  moderationController?.abort()
})

</script>

<style scoped>
.solution-interactions { display: grid; gap: 10px; padding: 14px 0; border-bottom: 1px solid var(--el-border-color-lighter); }
.solution-interactions__actions { display: flex; flex-wrap: wrap; align-items: center; gap: 9px; }
.interaction-button { display: inline-flex; min-width: 124px; min-height: 38px; align-items: center; gap: 8px; padding: 0 12px; border: 1px solid var(--el-border-color); border-radius: 999px; background: var(--el-bg-color); color: var(--el-text-color-regular); font: inherit; cursor: pointer; transition: color .16s ease, border-color .16s ease, background-color .16s ease; }
.interaction-button:hover:not(:disabled), .interaction-button--active { border-color: var(--el-color-primary-light-5); background: var(--el-color-primary-light-9); color: var(--el-color-primary); }
.interaction-button:disabled { cursor: wait; }
.interaction-button__icon { display: inline-flex; width: 18px; height: 18px; flex: 0 0 18px; align-items: center; justify-content: center; font-size: 16px; }
.interaction-button__count { min-width: 1ch; margin-left: auto; color: var(--el-text-color-secondary); font-variant-numeric: tabular-nums; }
.comments-state-button { min-height: 38px; }
.interaction-error { margin: 0; color: var(--el-color-danger); font-size: 13px; }
.moderation-panel { display: grid; gap: 8px; padding: 12px 14px; border: 1px solid var(--el-border-color-lighter); border-radius: 12px; background: var(--el-fill-color-lighter); }
.moderation-panel__hint { margin: 0; color: var(--el-text-color-secondary); font-size: 13px; }
.moderation-panel__hint--error { color: var(--el-color-danger); }
.moderation-entry + .moderation-entry { padding-top: 9px; border-top: 1px solid var(--el-border-color-lighter); }
.moderation-entry__meta { display: flex; flex-wrap: wrap; justify-content: space-between; gap: 6px 14px; color: var(--el-text-color-primary); font-size: 13px; }
.moderation-entry__meta time { color: var(--el-text-color-secondary); font-size: 12px; }
.moderation-entry p { margin: 6px 0 0; color: var(--el-text-color-regular); line-height: 1.6; white-space: pre-wrap; overflow-wrap: anywhere; }
@media (max-width: 560px) { .solution-interactions__actions { gap: 7px; }.interaction-button { min-width: 112px; } }
@media (prefers-reduced-motion: reduce) { .interaction-button { transition: none; } }
</style>
