<template>
  <article class="comment-item" :class="{'comment-item--reply': !rootMode, 'comment-item--deleted': deleted}">
    <Avatar class="comment-item__avatar" :user-id="comment.author?.userId ?? null" :size="36" />
    <div class="comment-item__body">
      <div class="comment-item__heading">
        <div class="comment-item__identity">
          <strong>{{ comment.author?.nikeName || comment.author?.userName || '已注销用户' }}</strong>
          <span v-if="comment.replyTo" class="comment-item__reply-to">回复 <b>@{{ comment.replyTo.nikeName || comment.replyTo.userName || '已注销用户' }}</b></span>
          <time>{{ formatDate(comment.createdAt) }}</time>
        </div>
        <el-tag v-if="comment.author?.specialRoles?.length" size="small" effect="plain">
          {{ comment.author.specialRoles[0] }}
        </el-tag>
      </div>

      <CommentText v-if="comment.state === 'VISIBLE' && comment.content !== null" :text="comment.content" />
      <p v-else class="comment-item__tombstone">
        {{ comment.state === 'ADMIN_DELETED' ? '该评论已被管理员处理' : '该评论已由作者删除' }}
      </p>

      <div class="comment-item__actions">
        <button
          v-if="!deleted"
          class="comment-action comment-action--like"
          :class="{'comment-action--active': liked}"
          type="button"
          :aria-pressed="liked"
          :disabled="likePending"
          @click="toggleLike"
        >
          <span class="comment-action__icon" aria-hidden="true">
            <el-icon v-if="likePending" class="is-loading"><Loading /></el-icon>
            <el-icon v-else><Star /></el-icon>
          </span>
          <span>{{ likeCount ?? '—' }}</span>
        </button>
        <button v-if="comment.canDelete && !deleted" class="comment-action" type="button" @click="$emit('delete', comment)">
          删除
        </button>
        <button v-if="comment.state === 'VISIBLE'" class="comment-action" type="button" @click="$emit('reply', comment)">
          回复
        </button>
        <button v-if="rootMode" class="comment-action" type="button" @click="$emit('toggle-replies', comment.commentId)">
          {{ expanded ? '收起回复' : replyCount !== '0' ? `查看回复 ${replyCount}` : '查看回复' }}
        </button>
        <button v-if="canViewModeration" class="comment-action comment-action--moderation" type="button" @click="toggleModeration">
          {{ showModeration ? '收起处理说明' : '查看处理说明' }}
        </button>
      </div>
      <p v-if="likeError" class="comment-item__error" role="status">{{ likeError }}</p>

      <div v-if="showModeration" class="comment-moderation" aria-live="polite">
        <p v-if="moderationLoading">正在加载处理说明…</p>
        <p v-else-if="moderationError" class="comment-moderation__error">{{ moderationError }}</p>
        <template v-else-if="moderationActions.length">
          <article v-for="(action, index) in moderationActions" :key="`${action.createdAt}-${index}`" class="comment-moderation__entry">
            <time>{{ formatDate(action.createdAt) }}</time>
            <p>{{ action.reason }}</p>
          </article>
        </template>
        <p v-else>暂无处理说明</p>
      </div>
    </div>
  </article>
</template>

<script setup lang="ts">
import {computed, onBeforeUnmount, ref, watch} from 'vue'
import {Loading, Star} from '@element-plus/icons-vue'
import Avatar from '@/components/Avatar/Avatar.vue'
import CommentText from './CommentText.vue'
import {getCommentModeration, type CommentVo} from '@/api/comment'
import {useLikeAction} from '@/composables/social/useLikeAction'
import {commentUiError} from '@/utils/commentText'
import {isUserIdEqual} from '@/utils/authUtil'

const props = withDefaults(defineProps<{
  comment: CommentVo
  rootMode?: boolean
  expanded?: boolean
}>(), {rootMode: false, expanded: false})
defineEmits<{
  (event: 'reply', comment: CommentVo): void
  (event: 'toggle-replies', rootId: string): void
  (event: 'delete', comment: CommentVo): void
}>()

const deleted = computed(() => props.comment.state !== 'VISIBLE')
const replyCount = computed(() => props.comment.replyCount || '0')
const likeAction = useLikeAction(
  'comment', computed(() => props.comment.commentId),
  computed(() => props.comment.likedByMe), computed(() => props.comment.likeCount),
)
const {liked, likeCount, pending: likePending, error: likeError, toggle: toggleLike} = likeAction
const canViewModeration = computed(() => props.comment.state === 'ADMIN_DELETED'
  && isUserIdEqual(props.comment.author?.userId))
const showModeration = ref(false)
const moderationLoading = ref(false)
const moderationError = ref('')
const moderationActions = ref<{action: string; reason: string; createdAt: string}[]>([])
let loadedFor: string | null = null
let controller: AbortController | null = null
let generation = 0
let disposed = false

watch(() => props.comment.commentId, () => {
  generation++
  controller?.abort()
  controller = null
  loadedFor = null
  moderationActions.value = []
  moderationError.value = ''
  showModeration.value = false
})

const loadModeration = async () => {
  const id = props.comment.commentId
  if (!canViewModeration.value || loadedFor === id || moderationLoading.value) return
  controller?.abort()
  const requestController = new AbortController()
  controller = requestController
  const requestGeneration = ++generation
  moderationLoading.value = true
  moderationError.value = ''
  try {
    moderationActions.value = await getCommentModeration(id, requestController.signal)
    if (!disposed && generation === requestGeneration && props.comment.commentId === id) loadedFor = id
  } catch (error) {
    if (!disposed && !requestController.signal.aborted && generation === requestGeneration) {
      moderationError.value = commentUiError(error)
    }
  } finally {
    if (!disposed && generation === requestGeneration) moderationLoading.value = false
  }
}

const toggleModeration = () => {
  showModeration.value = !showModeration.value
  if (showModeration.value) void loadModeration()
}
const formatDate = (value: string) => {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '' : date.toLocaleString()
}

onBeforeUnmount(() => {
  disposed = true
  generation++
  controller?.abort()
})
</script>

<style scoped>
.comment-item { display: flex; min-width: 0; align-items: flex-start; gap: 11px; padding: 15px 0; }
.comment-item + .comment-item { border-top: 1px solid var(--el-border-color-lighter); }
.comment-item--reply { padding: 12px 0; }
.comment-item--deleted { opacity: .82; }
.comment-item__avatar { margin-top: 1px; }
.comment-item__body { min-width: 0; flex: 1; }
.comment-item__heading { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.comment-item__identity { display: flex; min-width: 0; flex-wrap: wrap; align-items: baseline; gap: 5px 9px; }
.comment-item__identity strong { color: var(--el-text-color-primary); font-size: 13px; }
.comment-item__identity time { color: var(--el-text-color-secondary); font-size: 12px; }
.comment-item__reply-to { color: var(--el-text-color-secondary); font-size: 12px; }
.comment-item__reply-to b { color: var(--el-color-primary); font-weight: 500; }
.comment-item__tombstone { margin: 7px 0 8px; color: var(--el-text-color-regular); font-size: 14px; line-height: 1.75; white-space: pre-wrap; overflow-wrap: anywhere; word-break: normal; }
.comment-item__tombstone { color: var(--el-text-color-secondary); font-style: italic; }
.comment-item__actions { display: flex; flex-wrap: wrap; align-items: center; gap: 2px 13px; }
.comment-action { display: inline-flex; min-height: 30px; align-items: center; gap: 5px; padding: 0 3px; border: 0; background: transparent; color: var(--el-text-color-secondary); font: inherit; font-size: 12px; cursor: pointer; }
.comment-action:hover:not(:disabled), .comment-action--active { color: var(--el-color-primary); }
.comment-action:disabled { cursor: wait; }
.comment-action__icon { display: inline-flex; width: 16px; height: 16px; flex: 0 0 16px; align-items: center; justify-content: center; font-size: 14px; }
.comment-action--moderation { color: var(--el-color-warning-dark-2); }
.comment-item__error { margin: 5px 0 0; color: var(--el-color-danger); font-size: 12px; }
.comment-moderation { display: grid; gap: 6px; margin-top: 8px; padding: 9px 11px; border: 1px solid var(--el-color-warning-light-7); border-radius: 9px; background: var(--el-color-warning-light-9); color: var(--el-text-color-regular); font-size: 12px; }
.comment-moderation > p { margin: 0; }
.comment-moderation__error { color: var(--el-color-danger); }
.comment-moderation__entry time { color: var(--el-text-color-secondary); }
.comment-moderation__entry p { margin: 3px 0 0; line-height: 1.6; white-space: pre-wrap; overflow-wrap: anywhere; }
@media (max-width: 480px) { .comment-item { gap: 8px; }.comment-item__heading { align-items: flex-start; }.comment-item__identity { gap: 4px 7px; } }
</style>
