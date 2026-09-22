<template>
  <section class="profile-activity">
    <template v-if="section === 'practice'">
      <div class="difficulty-grid">
        <section
          v-for="group in difficultyGroups"
          :key="group.key"
          class="difficulty-card"
          :class="`difficulty-card--${group.key}`"
        >
          <header class="difficulty-card__header">
            <span class="difficulty-dot" aria-hidden="true"></span>
            <strong>{{ group.label }}</strong>
            <small>{{ group.items.length }}</small>
          </header>
          <div v-if="group.items.length" class="problem-cloud">
            <button
              v-for="id in group.items"
              :key="id"
              class="problem-chip"
              type="button"
              :title="`打开题目 ${id}`"
              :aria-label="`打开题目 ${id}`"
              @click="emit('open-problem', id)"
            >#{{ shortProfileId(id) }}</button>
          </div>
          <el-empty v-else :image-size="34" description="暂无记录" />
        </section>
      </div>
      <p v-if="activity.solvedProblemsTruncated" class="activity-limit">
        仅展示最近 {{ PROFILE_ACTIVITY_LIMITS.solvedProblems }} 道已通过题目
      </p>
    </template>

    <template v-else-if="section === 'contests'">
      <div v-if="activity.contests.length" class="activity-list">
        <button
          v-for="contest in activity.contests"
          :key="contest.contestId"
          type="button"
          class="activity-item"
          :aria-label="`打开${contestTypeLabel(contest.type)} ${contest.title}，ID ${contest.contestId}`"
          @click="emit('open-contest', contest.contestId)"
        >
          <span class="activity-item__icon" :class="`activity-item__icon--${contestTone(contest.type)}`">
            <el-icon><component :is="contestIcon(contest.type)" /></el-icon>
          </span>
          <span class="activity-item__main">
            <strong>{{ contest.title }}</strong>
            <small>ID {{ shortProfileId(contest.contestId) }} · {{ contestTypeLabel(contest.type) }}</small>
          </span>
          <span class="activity-item__date">{{ formatProfileDate(contest.startTime) }}</span>
          <el-icon class="activity-item__arrow"><ArrowRight /></el-icon>
        </button>
      </div>
      <el-empty v-else description="还没有参加过活动" />
      <p v-if="activity.contestsTruncated" class="activity-limit">
        仅展示最近 {{ PROFILE_ACTIVITY_LIMITS.contests }} 项活动
      </p>
    </template>

    <template v-else>
      <div v-if="visibleSolutions.length" class="activity-list">
        <button
          v-for="solution in visibleSolutions"
          :key="solution.solutionId"
          type="button"
          class="activity-item"
          :aria-label="`打开题解 ${solution.title}，题目 ID ${solution.problemId}`"
          @click="emit('open-solution', solution.solutionId)"
        >
          <span class="activity-item__icon activity-item__icon--solution">
            <el-icon><EditPen /></el-icon>
          </span>
          <span class="activity-item__main">
            <strong>{{ solution.title }}</strong>
            <small>{{ solution.problemTitle || '题目' }} · ID {{ shortProfileId(solution.problemId) }}</small>
          </span>
          <el-tag v-if="solution.private_ && activity.owner" size="small" type="info" effect="plain">仅自己可见</el-tag>
          <span class="activity-item__date">{{ formatProfileDate(solution.createTime) }}</span>
          <el-icon class="activity-item__arrow"><ArrowRight /></el-icon>
        </button>
      </div>
      <el-empty v-else :description="activity.owner ? '还没有写过题解' : '还没有公开题解'" />
      <p v-if="activity.solutionsTruncated" class="activity-limit">
        仅展示最近 {{ PROFILE_ACTIVITY_LIMITS.solutions }} 篇题解
      </p>
    </template>
  </section>
</template>

<script setup lang="ts">
import {computed} from 'vue'
import {ArrowRight, EditPen, Notebook, Trophy} from '@element-plus/icons-vue'
import {ContestType} from '@/api/contest'
import {PROFILE_ACTIVITY_LIMITS, type ProfileActivity} from '@/api/profile'
import type {IdType} from '@/api/common'
import {formatProfileDate, shortProfileId} from '@/utils/profile'

const props = defineProps<{activity: ProfileActivity; section: 'practice' | 'contests' | 'solutions'}>()
const emit = defineEmits<{
  (event: 'open-problem', id: IdType): void
  (event: 'open-contest', id: IdType): void
  (event: 'open-solution', id: IdType): void
}>()

const difficultyGroups = computed(() => [
  {key: 'easy', label: '简单', items: props.activity.solvedProblems.easy},
  {key: 'medium', label: '中等', items: props.activity.solvedProblems.medium},
  {key: 'hard', label: '困难', items: props.activity.solvedProblems.hard},
  {key: 'unknown', label: '未分类', items: props.activity.solvedProblems.unknown},
])

const visibleSolutions = computed(() => props.activity.owner
  ? props.activity.solutions
  : props.activity.solutions.filter(solution => !solution.private_))

const contestTypeLabel = (type: ContestType | null) => {
  if (type === ContestType.HOMEWORK) return '作业'
  if (type === ContestType.CONTEST) return '比赛'
  return '其他活动'
}

const contestTone = (type: ContestType | null) => type === ContestType.HOMEWORK ? 'homework' : type === ContestType.CONTEST ? 'contest' : 'other'
const contestIcon = (type: ContestType | null) => type === ContestType.HOMEWORK ? Notebook : type === ContestType.CONTEST ? Trophy : ArrowRight
</script>

<style scoped>
.profile-activity {
  min-width: 0;
  color: var(--el-text-color-primary);
}

.difficulty-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.difficulty-card {
  min-width: 0;
  padding: 15px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 13px;
  background: var(--el-fill-color-lighter);
}

.difficulty-card__header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.difficulty-card__header small {
  margin-left: auto;
  color: var(--el-text-color-placeholder);
}

.difficulty-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--el-color-info);
}

.difficulty-card--easy .difficulty-dot { background: var(--el-color-success); }
.difficulty-card--medium .difficulty-dot { background: var(--el-color-warning); }
.difficulty-card--hard .difficulty-dot { background: var(--el-color-danger); }

.problem-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
}

.problem-chip {
  max-width: 100%;
  padding: 7px 9px;
  overflow: hidden;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  color: var(--el-text-color-regular);
  background: var(--el-bg-color);
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 11px;
  cursor: pointer;
  text-overflow: ellipsis;
  transition: color .16s ease, border-color .16s ease, background-color .16s ease, transform .16s ease;
}

.problem-chip:hover,
.problem-chip:focus-visible {
  border-color: var(--el-color-primary-light-5);
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  transform: translateY(-1px);
}

.problem-chip:focus-visible,
.activity-item:focus-visible {
  outline: 2px solid var(--el-color-primary);
  outline-offset: 2px;
}

.difficulty-card :deep(.el-empty) {
  padding: 10px 0 3px;
}

.difficulty-card :deep(.el-empty__description p) {
  color: var(--el-text-color-placeholder);
  font-size: 11px;
}

.activity-list {
  display: grid;
  gap: 8px;
}

.activity-item {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  min-width: 0;
  padding: 12px;
  border: 1px solid transparent;
  border-radius: 12px;
  color: inherit;
  background: var(--el-fill-color-lighter);
  text-align: left;
  cursor: pointer;
  transition: color .18s ease, border-color .18s ease, background-color .18s ease, transform .18s ease;
}

.activity-item:hover {
  border-color: var(--el-color-primary-light-7);
  background: var(--el-color-primary-light-9);
  transform: translateY(-1px);
}

.activity-item__icon {
  display: grid;
  width: 34px;
  height: 34px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 10px;
  color: var(--el-color-primary);
  background: color-mix(in srgb, var(--el-color-primary) 12%, var(--el-bg-color));
}

.activity-item__icon--homework { color: var(--el-color-warning); background: var(--el-color-warning-light-9); }
.activity-item__icon--contest { color: var(--el-color-success); background: var(--el-color-success-light-9); }
.activity-item__icon--other { color: var(--el-color-info); background: var(--el-fill-color); }
.activity-item__icon--solution { color: var(--el-color-primary); }

.activity-item__main {
  display: block;
  min-width: 0;
  flex: 1;
}

.activity-item__main strong,
.activity-item__main small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.activity-item__main strong { font-size: 13px; }

.activity-item__main small {
  margin-top: 4px;
  color: var(--el-text-color-secondary);
  font-size: 11px;
}

.activity-item__date {
  flex: 0 0 auto;
  color: var(--el-text-color-placeholder);
  font-size: 11px;
}

.activity-item__arrow {
  flex: 0 0 auto;
  color: var(--el-text-color-placeholder);
}

.activity-limit {
  margin: 12px 2px 0;
  color: var(--el-text-color-placeholder);
  font-size: 12px;
}

@media (max-width: 700px) {
  .difficulty-grid { grid-template-columns: 1fr; }
}

@media (max-width: 480px) {
  .activity-item__date { display: none; }
}
</style>
