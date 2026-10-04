<template>
  <el-dialog :model-value="modelValue" :title="type === 'following' ? '关注列表' : '粉丝列表'"
             width="min(520px, calc(100vw - 32px))" align-center append-to-body
             @update:model-value="emit('update:modelValue', $event)">
    <div class="follow-list" :aria-busy="loading">
      <p v-if="error" class="follow-list__error" role="alert">{{ error }}
        <el-button text type="primary" :disabled="loading" @click="void load(requestedPage)">重试</el-button>
      </p>
      <p v-if="loading" class="follow-list__status" role="status">正在加载…</p>
      <el-scrollbar max-height="55vh">
        <ul v-if="rows.length" class="follow-list__rows">
          <li v-for="user in rows" :key="user.userId">
            <RouterLink class="follow-list__user" :to="{name: 'profile', params: {id: user.userId}}"
                        @click="emit('update:modelValue', false)">
              <Avatar :user-id="user.userId" :size="40"/>
              <div class="follow-list__identity">
                <strong :title="user.nikeName || user.userName">{{ user.nikeName || user.userName || '用户' }}</strong>
                <span :title="user.userId">@{{ user.userName }} · {{ shortProfileId(user.userId) }}</span>
                <div v-if="user.specialRoles.length" class="follow-list__roles">
                  <el-tag v-for="role in user.specialRoles" :key="role" size="small" effect="plain">{{ role }}</el-tag>
                </div>
              </div>
            </RouterLink>
          </li>
        </ul>
        <el-empty v-else-if="!loading && !error" :image-size="64" description="暂无用户"/>
      </el-scrollbar>
      <el-pagination v-if="total > 20" class="follow-list__pagination" size="small" background
                     layout="prev, pager, next" :pager-count="5" :current-page="page" :page-size="20"
                     :total="total" :disabled="loading" @current-change="void load($event)"/>
    </div>
  </el-dialog>
</template>
<script setup lang="ts">
import {onScopeDispose, ref, watch} from 'vue'
import {RouterLink} from 'vue-router'
import Avatar from '@/components/Avatar/Avatar.vue'
import {getFollowUsersPage, isFollowUserId, type FollowListType, type FollowUserSummary} from '@/api/follow'
import {shortProfileId} from '@/utils/profile'
import {useToken} from '@/stores/useToken'
import {useUserStore} from '@/stores/useUserStore'

const props = defineProps<{modelValue: boolean; userId: string | null; type: FollowListType}>()
const emit = defineEmits<{'update:modelValue': [value: boolean]}>()
const token = useToken()
const userStore = useUserStore()
const rows = ref<FollowUserSummary[]>([])
const total = ref(0)
const page = ref(1)
const requestedPage = ref(1)
const loading = ref(false)
const error = ref('')
let generation = 0
let controller: AbortController | undefined
const load = async (currentPage: number) => {
  if (!props.modelValue || !isFollowUserId(props.userId) || token.authStatus !== 'authenticated') return
  const request = ++generation
  controller?.abort()
  controller = new AbortController()
  loading.value = true
  requestedPage.value = currentPage
  error.value = ''
  try {
    const result = await getFollowUsersPage(props.userId, props.type, currentPage, controller.signal)
    if (request !== generation) return
    rows.value = result.data
    total.value = result.totalRecords
    page.value = currentPage
  } catch {
    if (request === generation) error.value = '用户列表加载失败，请重试'
  } finally {
    if (request === generation) loading.value = false
  }
}
watch([() => props.modelValue, () => props.userId, () => props.type,
  () => token.sessionVersion, () => token.authStatus, () => userStore.user?.userId], () => {
  generation++
  controller?.abort()
  rows.value = []
  total.value = 0
  page.value = 1
  requestedPage.value = 1
  error.value = ''
  loading.value = false
  void load(1)
}, {immediate: true, flush: 'sync'})
onScopeDispose(() => { generation++; controller?.abort() })
</script>
<style scoped>
.follow-list { min-width: 0; }
.follow-list__rows { list-style: none; margin: 0; padding: 0; }
.follow-list__rows li + li { border-top: 1px solid var(--el-border-color-lighter); }
.follow-list__user { display: flex; align-items: center; gap: 12px; padding: 12px 4px; color: var(--el-text-color-primary); text-decoration: none; border-radius: 6px; }
.follow-list__user:hover { background: var(--el-fill-color-light); }
.follow-list__user:focus-visible { outline: 2px solid var(--el-color-primary); outline-offset: -2px; }
.follow-list__identity { min-width: 0; flex: 1; }
.follow-list__identity > strong, .follow-list__identity > span { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.follow-list__identity > span { margin-top: 4px; font-size: 12px; color: var(--el-text-color-secondary); }
.follow-list__roles { display: flex; flex-wrap: wrap; gap: 4px; margin-top: 6px; }
.follow-list__roles :deep(.el-tag) { max-width: 100%; white-space: normal; height: auto; overflow-wrap: anywhere; }
.follow-list__error { color: var(--el-color-danger); margin: 0 0 8px; }
.follow-list__status { color: var(--el-text-color-secondary); margin: 0 0 8px; }
.follow-list__pagination { justify-content: center; margin-top: 16px; }
</style>
