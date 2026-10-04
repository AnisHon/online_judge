<template>
  <section ref="threadElement" class="comment-thread" aria-labelledby="comment-thread-title">
    <header class="comment-thread__header">
      <div>
        <h2 id="comment-thread-title">评论</h2>
        <p>交流思路，保持友善</p>
      </div>
      <span v-if="rootsLoaded" class="comment-thread__total">{{ commentCount ?? totalRecords }} 条</span>
    </header>

    <div v-if="canComment" v-show="commentsOpen" class="comment-thread__composer">
      <CommentComposer :submit-comment="submitRoot" />
    </div>
    <p v-if="!commentsOpen" class="comment-thread__closed">作者暂时关闭了评论，已有评论仍可查看和点赞。</p>
    <p v-else-if="!canComment" class="comment-thread__closed">当前账号暂不能发布评论。</p>

    <div v-if="rootsError" class="comment-thread__state comment-thread__state--error" role="alert">
      <span>{{ rootsError }}</span>
      <el-button text type="primary" @click="loadRoots()">重试</el-button>
    </div>
    <div v-else-if="rootsLoading && !rootsLoaded" class="comment-thread__loading" aria-label="正在加载评论">
      <el-skeleton :count="3" animated><template #template><div class="comment-skeleton"><el-skeleton-item variant="circle" /><div><el-skeleton-item variant="text" /><el-skeleton-item variant="p" /></div></div></template></el-skeleton>
    </div>
    <el-empty v-else-if="rootsLoaded && roots.length === 0" description="还没有评论，来留下第一条吧" :image-size="72" />

    <div v-if="roots.length" class="comment-thread__list" aria-live="polite">
      <div v-for="root in roots" :key="root.commentId" class="comment-thread__root">
        <CommentItem
          :comment="root"
          root-mode
          :expanded="expandedRoots.has(root.commentId)"
          @reply="beginReply(root, $event)"
          @toggle-replies="toggleReplies"
          @delete="deleteComment"
        />
        <div v-if="expandedRoots.has(root.commentId)" class="comment-thread__replies">
          <CommentComposer
            v-if="canComment && replyTarget?.rootId === root.commentId"
            v-show="commentsOpen"
            :key="`${root.commentId}-${replyTarget.parentId}`"
            :reply-to="replyTarget.name"
            :submit-comment="submitReply"
            @cancel-reply="replyTarget = null"
          />
          <div v-if="replyState(root.commentId).error" class="comment-thread__state comment-thread__state--error" role="alert">
            <span>{{ replyState(root.commentId).error }}</span>
            <el-button text type="primary" @click="loadReplies(root.commentId, false, true)">重试</el-button>
          </div>
          <div v-else-if="replyState(root.commentId).loading && !replyState(root.commentId).loaded" class="comment-thread__reply-loading">
            <el-skeleton :count="2" animated><el-skeleton-item variant="text" /><el-skeleton-item variant="p" /></el-skeleton>
          </div>
          <div v-if="replyState(root.commentId).data.length" class="comment-thread__reply-list">
            <CommentItem
              v-for="reply in replyState(root.commentId).data"
              :key="reply.commentId"
              :comment="reply"
              @reply="beginReply(root, $event)"
              @delete="deleteComment"
            />
          </div>
          <p v-else-if="replyState(root.commentId).loaded" class="comment-thread__no-replies">还没有回复</p>
          <el-button
            v-if="hasMoreReplies(root.commentId)"
            class="comment-thread__more comment-thread__more--reply"
            text
            :loading="replyState(root.commentId).loading"
            @click="loadReplies(root.commentId, true)"
          >加载更多回复</el-button>
        </div>
      </div>
      <el-button
        v-if="hasMoreRoots"
        class="comment-thread__more"
        :loading="rootsLoading"
        :disabled="rootsLoading"
        @click="loadRoots(true)"
      >加载更多评论</el-button>
    </div>

    <p v-if="deleteError" class="comment-thread__delete-error" role="status">{{ deleteError }}</p>
  </section>
</template>

<script setup lang="ts">
import {computed, ref, watch} from 'vue'
import type {IdType} from '@/api/common'
import type {CommentVo} from '@/api/comment'
import CommentComposer from './CommentComposer.vue'
import CommentItem from './CommentItem.vue'
import {useCommentThread} from '@/composables/social/useCommentThread'
import {commentUiError} from '@/utils/commentText'
import {hasPerm} from '@/utils/authUtil'

const props = withDefaults(defineProps<{
  solutionId: IdType
  commentCount?: string | number | null
  commentsOpen?: boolean
}>(), {commentsOpen: true})
const emit = defineEmits<{(event: 'comments-changed', delta: 1 | -1): void}>()

const threadElement = ref<HTMLElement | null>(null)
const solutionId = computed(() => props.solutionId)
const thread = useCommentThread(solutionId)
const {roots, totalRecords, rootsLoaded, rootsLoading, rootsError,
  changeRevision, lastChangeDelta, loadRoots, loadReplies, replyState, createRoot, createReply, deleteComment: deleteOwn} = thread
const commentsOpen = computed(() => props.commentsOpen)
const canComment = computed(() => !hasPerm('policy:comment:deny'))
const hasMoreRoots = computed(() => BigInt(roots.value.length) < BigInt(totalRecords.value))
const expandedRoots = ref(new Set<IdType>())
const replyTarget = ref<{rootId: IdType; parentId: IdType; name: string} | null>(null)
const deleteError = ref('')
let lastRevision = 0

const hasMoreReplies = (rootId: IdType) => {
  const state = replyState(rootId)
  return state.loaded && BigInt(state.data.length) < BigInt(state.totalRecords)
}

watch(changeRevision, value => {
  if (value !== lastRevision) {
    lastRevision = value
    emit('comments-changed', lastChangeDelta.value)
  }
})

watch(solutionId, () => {
  expandedRoots.value = new Set()
  replyTarget.value = null
})

const toggleReplies = (rootId: IdType) => {
  const next = new Set(expandedRoots.value)
  if (next.has(rootId)) {
    next.delete(rootId)
    expandedRoots.value = next
  }
  else {
    next.add(rootId)
    expandedRoots.value = next
    void loadReplies(rootId)
  }
}

const beginReply = (root: CommentVo, target: CommentVo) => {
  if (!commentsOpen.value || target.state !== 'VISIBLE') return
  expandedRoots.value = new Set(expandedRoots.value).add(root.commentId)
  replyTarget.value = {
    rootId: root.commentId,
    parentId: target.commentId,
    name: target.author?.nikeName || target.author?.userName || '已注销用户',
  }
  void loadReplies(root.commentId)
}

const submitRoot = (content: string, requestId: string) => createRoot(content, requestId)
const submitReply = (content: string, requestId: string) => {
  const target = replyTarget.value
  if (!target || !commentsOpen.value) return Promise.resolve(null)
  return createReply(target.rootId, target.parentId, content, requestId).then(comment => {
    if (comment && replyTarget.value?.parentId === target.parentId) replyTarget.value = null
    return comment
  })
}

const deleteComment = async (comment: CommentVo) => {
  deleteError.value = ''
  try {
    await deleteOwn(comment)
  } catch (error) {
    // The item remains unchanged; the request's safe status is shown locally.
    deleteError.value = commentUiError(error)
  }
}

const focus = () => threadElement.value?.scrollIntoView({behavior: 'smooth', block: 'start'})
defineExpose({focus})

</script>

<style scoped>
.comment-thread { min-width: 0; margin-top: 28px; padding: clamp(16px, 3vw, 24px); border: 1px solid var(--el-border-color-lighter); border-radius: 16px; background: var(--el-bg-color); scroll-margin-top: 24px; }
.comment-thread__header { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 16px; }
.comment-thread__header h2 { margin: 0; color: var(--el-text-color-primary); font-size: 19px; line-height: 1.35; }
.comment-thread__header p { margin: 4px 0 0; color: var(--el-text-color-secondary); font-size: 12px; }
.comment-thread__total { flex: 0 0 auto; color: var(--el-text-color-secondary); font-size: 13px; font-variant-numeric: tabular-nums; }
.comment-thread__composer { margin-bottom: 18px; }
.comment-thread__closed { margin: 0 0 16px; padding: 10px 12px; border-radius: 9px; background: var(--el-fill-color-lighter); color: var(--el-text-color-secondary); font-size: 13px; }
.comment-thread__root + .comment-thread__root { border-top: 1px solid var(--el-border-color-lighter); }
.comment-thread__replies { display: grid; gap: 5px; margin: 0 0 12px 26px; padding: 0 0 0 16px; border-left: 2px solid var(--el-border-color-lighter); }
.comment-thread__reply-list { min-width: 0; }
.comment-thread__no-replies { margin: 4px 0; color: var(--el-text-color-secondary); font-size: 12px; }
.comment-thread__state { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; padding: 10px 12px; color: var(--el-text-color-secondary); font-size: 13px; }
.comment-thread__state--error { border-radius: 9px; background: var(--el-color-danger-light-9); color: var(--el-color-danger); }
.comment-thread__loading { display: grid; gap: 12px; padding: 12px 0; }
.comment-skeleton { display: flex; gap: 12px; }.comment-skeleton > div { display: grid; flex: 1; gap: 8px; }
.comment-thread__reply-loading { padding: 8px 0; }
.comment-thread__more { display: flex; width: 100%; min-height: 38px; justify-content: center; margin: 4px auto 0; }
.comment-thread__more--reply { min-height: 32px; margin: 0; }
.comment-thread__delete-error { margin: 12px 0 0; color: var(--el-color-danger); font-size: 13px; }
@media (max-width: 560px) { .comment-thread { margin-top: 20px; padding: 15px 12px; border-radius: 12px; }.comment-thread__replies { margin-left: 12px; padding-left: 10px; } }
</style>
