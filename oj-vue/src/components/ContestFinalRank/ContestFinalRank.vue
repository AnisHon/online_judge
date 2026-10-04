<template>
  <section class="final-rank" aria-label="最终成绩">
    <header class="final-rank__header">
      <div><strong>{{ rankStatusText(result, failed) }}</strong><small v-if="result?.ranking && result.state !== 'READY'">以下为上次结算成绩</small></div>
      <div class="final-rank__actions">
        <el-button v-if="canRebuild" class="rebuild-button" :loading="rebuilding" :disabled="rebuilding || loading" @click="rebuild(canRebuild)">重新结算</el-button>
        <el-button :disabled="loading" @click="refresh">刷新</el-button>
        <el-button @click="$emit('close')">关闭</el-button>
      </div>
    </header>
    <div class="final-rank__body" :aria-busy="loading">
      <el-table v-if="result?.ranking" :data="result.ranking.data" height="100%" row-key="userId">
        <el-table-column prop="rank" label="排名" width="75" />
        <el-table-column label="用户" min-width="230"><template #default="{row}"><div class="rank-user"><Avatar :user-id="rankAvatarId(row)" :size="32" /><span>{{ rankUserName(row) }}</span><el-tag v-for="role in row.user?.specialRoles || []" :key="role" size="small">{{ role }}</el-tag></div></template></el-table-column>
        <el-table-column prop="score" label="得分" width="110" />
        <el-table-column prop="correctCount" label="正确题数" width="100" />
        <el-table-column label="交卷" width="100"><template #default="{row}">{{ row.handedIn ? '已交卷' : '未交卷' }}</template></el-table-column>
        <template #empty><el-empty description="暂无参赛成绩" /></template>
      </el-table>
      <el-empty v-else :description="loading ? '正在加载成绩' : rankStatusText(result, failed)" />
    </div>
    <el-pagination v-if="result?.ranking" class="final-rank__pagination" small layout="prev, pager, next" :current-page="page" :page-size="20" :total="result.ranking.totalRecords" :disabled="loading" @current-change="setPage" />
  </section>
</template>
<script setup lang="ts">
import {computed, toRef} from 'vue'
import Avatar from '@/components/Avatar/Avatar.vue'
import {useFinalRank} from '@/composables/contest/useFinalRank'
import {rankAvatarId, rankStatusText, rankUserName} from '@/utils/contest/rankDisplay'
import {hasPerm} from '@/utils/authUtil'
const props = withDefaults(defineProps<{contestId: string; admin?: boolean}>(), {admin: false})
defineEmits<{close: []}>()
const canRebuild = computed(() => props.admin && hasPerm('problem:contest:rank'))
const {result, page, failed, loading, rebuilding, refresh, rebuild, setPage} = useFinalRank(toRef(props, 'contestId'), toRef(props, 'admin'))
</script>
<style scoped>
.final-rank { display:flex; flex-direction:column; gap:16px; height:100%; min-height:0; min-width:0; padding:20px; box-sizing:border-box; color:var(--el-text-color-primary); background:var(--el-bg-color); }
.final-rank__header { display:flex; align-items:center; justify-content:space-between; flex-wrap:wrap; gap:12px; flex:none; }
.final-rank__header small { display:block; margin-top:6px; color:var(--el-text-color-secondary); }
.final-rank__actions { display:flex; gap:8px; flex-wrap:wrap; }.final-rank__actions :deep(.el-button + .el-button) { margin-left:0; }
.rebuild-button { width:110px; }.final-rank__body { flex:1; min-height:0; min-width:0; overflow:hidden; }.final-rank__pagination { flex:none; justify-content:center; }
.rank-user { display:flex; align-items:center; gap:8px; flex-wrap:wrap; }.rank-user span { overflow-wrap:anywhere; }
@media(max-width:600px) { .final-rank { padding:12px; gap:10px; } }
</style>
