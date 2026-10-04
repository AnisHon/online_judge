<template>
  <form class="comment-composer" @submit.prevent="submit">
    <div v-if="replyTo" class="comment-composer__replying">
      <span>回复 <strong>@{{ replyTo }}</strong></span>
      <el-button text size="small" type="info" @click="$emit('cancel-reply')">取消</el-button>
    </div>
    <label class="sr-only" :for="textareaId">{{ replyTo ? `回复 ${replyTo}` : '发表评论' }}</label>
    <textarea
      :id="textareaId"
      ref="textarea"
      v-model="content"
      class="comment-composer__input"
      rows="3"
      :disabled="pending"
      :placeholder="replyTo ? `回复 @${replyTo}…` : '写下你的想法…（纯文本，最多 2000 字）'"
      @input="handleEdit"
      @select="rememberSelection"
      @keyup="rememberSelection"
      @click="rememberSelection"
    />
    <div class="comment-composer__footer">
      <div class="comment-composer__tools">
        <EmojiPicker @select="insertEmoji" />
        <span class="comment-composer__count" :class="{'comment-composer__count--over': characterCount > COMMENT_MAX_CODE_POINTS}">
          {{ characterCount }}/{{ COMMENT_MAX_CODE_POINTS }}
        </span>
        <span v-if="validationError" class="comment-composer__error" role="alert">{{ validationError }}</span>
        <span v-else-if="requestError" class="comment-composer__error" role="status">{{ requestError }}</span>
      </div>
      <el-button class="comment-composer__submit" type="primary" native-type="submit" :loading="pending" :disabled="pending || !normalizedContent">
        发送
      </el-button>
    </div>
  </form>
</template>

<script setup lang="ts">
import {computed, nextTick, onBeforeUnmount, ref, watch} from 'vue'
import {commentTextError, COMMENT_MAX_CODE_POINTS, createCommentRequestIdentity, commentUiError, normalizeCommentText} from '@/utils/commentText'
import type {CommentVo} from '@/api/comment'
import EmojiPicker from './EmojiPicker.vue'

const props = defineProps<{
  replyTo?: string
  submitComment: (content: string, clientRequestId: string) => Promise<CommentVo | null>
}>()
defineEmits<{(event: 'cancel-reply'): void}>()

const textareaId = `comment-input-${Math.random().toString(36).slice(2)}`
const textarea = ref<HTMLTextAreaElement | null>(null)
const content = ref('')
const pending = ref(false)
const requestError = ref('')
const validationError = ref('')
const requestIdentity = createCommentRequestIdentity()
const normalizedContent = computed(() => normalizeCommentText(content.value))
const characterCount = computed(() => Array.from(normalizedContent.value).length)
let selectionStart = 0
let selectionEnd = 0
let disposed = false

watch(content, value => {
  requestIdentity.edit(normalizeCommentText(value))
  requestError.value = ''
  validationError.value = ''
})

const handleEdit = () => rememberSelection()
const rememberSelection = () => {
  if (!textarea.value) return
  selectionStart = textarea.value.selectionStart
  selectionEnd = textarea.value.selectionEnd
}

const insertEmoji = async (emoji: string) => {
  if (pending.value) return
  const current = content.value
  content.value = `${current.slice(0, selectionStart)}${emoji}${current.slice(selectionEnd)}`
  const next = selectionStart + emoji.length
  selectionStart = next
  selectionEnd = next
  await nextTick()
  if (!textarea.value) return
  textarea.value.focus()
  textarea.value.setSelectionRange(next, next)
}

const submit = async () => {
  if (pending.value) return
  const normalized = normalizedContent.value
  validationError.value = commentTextError(content.value)
  requestError.value = ''
  if (validationError.value) return
  const clientRequestId = requestIdentity.begin(normalized)
  pending.value = true
  try {
    const created = await props.submitComment(normalized, clientRequestId)
    if (disposed || !created) return
    content.value = ''
    requestIdentity.complete()
  } catch (error) {
    if (!disposed) requestError.value = commentUiError(error)
  } finally {
    if (!disposed) pending.value = false
  }
}

onBeforeUnmount(() => { disposed = true })
</script>

<style scoped>
.comment-composer { display: grid; gap: 9px; min-width: 0; padding: 12px; border: 1px solid var(--el-border-color); border-radius: 13px; background: var(--el-bg-color); }
.comment-composer__replying { display: flex; align-items: center; justify-content: space-between; gap: 8px; color: var(--el-text-color-secondary); font-size: 13px; }
.comment-composer__replying strong { color: var(--el-color-primary); font-weight: 600; }
.comment-composer__input { display: block; box-sizing: border-box; width: 100%; min-height: 80px; max-height: 240px; resize: vertical; padding: 10px 11px; border: 1px solid var(--el-border-color-lighter); border-radius: 9px; outline: none; background: var(--el-fill-color-blank); color: var(--el-text-color-primary); font: inherit; line-height: 1.65; }
.comment-composer__input:focus { border-color: var(--el-color-primary); box-shadow: 0 0 0 2px var(--el-color-primary-light-8); }
.comment-composer__input::placeholder { color: var(--el-text-color-placeholder); }
.comment-composer__input:disabled { opacity: .72; cursor: wait; }
.comment-composer__footer, .comment-composer__tools { display: flex; align-items: center; }
.comment-composer__footer { justify-content: space-between; gap: 12px; }
.comment-composer__tools { min-width: 0; flex-wrap: wrap; gap: 8px; }
.comment-composer__count { color: var(--el-text-color-secondary); font-size: 12px; font-variant-numeric: tabular-nums; }
.comment-composer__count--over, .comment-composer__error { color: var(--el-color-danger); }
.comment-composer__error { font-size: 12px; }
.comment-composer__submit { box-sizing: border-box; width: 78px; min-width: 78px; height: 36px; flex: 0 0 78px; }
.sr-only { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0, 0, 0, 0); white-space: nowrap; clip-path: inset(50%); }
@media (max-width: 480px) { .comment-composer { padding: 10px; }.comment-composer__footer { align-items: flex-end; }.comment-composer__tools { gap: 4px; } }
</style>
