<template>
  <main class="list-container common-max-width-page">
    <header class="list-header"><div><p class="eyebrow">CURATED PRACTICE</p><h1>题单</h1><p>按主题和学习路径组织题目，循序渐进地完成一组练习。</p></div><div class="list-header__mark"><el-icon><List /></el-icon></div></header>
    <el-row class="list-layout" :gutter="18">
      <el-col :span="8" class="directory-panel">
        <div class="panel-heading"><div><strong>学习路径</strong><small>选择一个题单开始练习</small></div><el-icon><Collection /></el-icon></div>
        <el-tree
            class="directory-tree"
            :data="treedViews"
            :props="defaultProps"
            @node-click="handleNodeClick"
        >
          <template #default="{node, data}">
            <span class="tree-node"><span class="tree-node__icon"><el-icon><component :is="isFile(data.folder.folderType) ? Reading : Collection" /></el-icon></span><span class="tree-node__label">{{ node.label }}</span></span>
          </template>
        </el-tree>
      </el-col>
      <el-col :span="16" class="problem-panel">
        <div class="panel-heading"><div><strong>{{ selectedName || '请选择题单' }}</strong><small>{{ selectedName ? `${tableList.length} 道题目` : '从左侧目录进入一个开放题单' }}</small></div><el-icon><Reading /></el-icon></div>
        <el-table class="list-table" v-loading="isLoading" :data="orderedTableList" v-show="tableList.length > 0" table-layout="auto" stripe>
          <el-table-column label="状态" align="center" prop="finish">
            <template v-slot="scope">
              <el-tooltip v-if="scope.row.finish" content="已完成" placement="top">
                <el-icon color="var(--el-color-success)">
                  <CircleCheck/>
                </el-icon>
              </el-tooltip>
              <div v-else></div>
            </template>
          </el-table-column>
          <el-table-column label="问题ID" align="center" prop="problemId" show-overflow-tooltip/>
          <el-table-column label="题目" prop="title">
            <template v-slot="scope">
              <el-link type="primary"  @click="handleClickProblem(scope.row.problemId)">{{ scope.row.title }}</el-link>
            </template>
          </el-table-column>
          <el-table-column label="问题来源" align="center" prop="source" show-overflow-tooltip />
          <el-table-column label="问题类型" align="center" prop="type">
            <template v-slot="scope">
              <el-tag type="primary">{{ problemTypeToString(scope.row.type) }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
        <el-empty class="empty-status" v-show="!isLoading && tableList.length === 0" :description="loadError || '选择题单后开始练习'"/>
      </el-col>
    </el-row>
  </main>
</template>

<script lang="ts" setup>
import {computed, reactive, ref} from 'vue'
import {FolderType, getTreedFolderView, type TreedFolderView} from "@/api/folder";
import {getProblems, type ProblemInListView} from "@/api/list";
import {useRouter} from "vue-router";
import {problemTypeToString} from "@/utils/problem";
import {CircleCheck, Collection, Document, List, Reading} from "@element-plus/icons-vue";
import {isNullObj} from "@/utils/valueutil.ts";
import {ElNotification} from "element-plus";
import type {IdType} from "@/api/common.ts";

const router = useRouter();
const defaultProps = {
  children: (x: TreedFolderView) => x.children,
  label: (x: TreedFolderView) => x.folder.folderName,
}

const treedViews = reactive<TreedFolderView[]>([])

const tableList = reactive<ProblemInListView[]>([])
const selectedName = ref("");
const orderedTableList = computed(() => [...tableList].sort((a, b) => <number>a.problemOrder - <number>b.problemOrder));

const isLoading = ref<boolean>(false);
const loadError = ref('');

const isFile = (type: string) => {
  return type === FolderType.FILE;
}

const handleClickProblem = (id: IdType) => {
  const routeUrl = router.resolve({
    name: "problem",
    params: {id: id}
  })
  window.open(routeUrl.href, '_blank')
}

const handleNodeClick = (node: TreedFolderView) => {
  selectedName.value = node.folder.folderName;
  tableList.length = 0;
  loadError.value = '';
  if (isFile(node.folder.folderType)) {
    const listId = node.folder.listId;
    if (listId === undefined || listId === null || isNullObj(listId)) {
      ElNotification.info("该题单还未开放");
      return;
    }
    isLoading.value = true;
    getProblems(listId)
        .then(data => {
          tableList.push(...data);
        })
        .catch(() => {
          loadError.value = '题单内容加载失败，请稍后重试';
        })
        .finally(() => {
          isLoading.value = false;
        })
  } else {
    loadError.value = '请选择一个题单文件开始练习';
  }
}

// created
isLoading.value = true;
getTreedFolderView()
    .then((data) => treedViews.push(...data))
    .catch(() => { loadError.value = '目录加载失败，请稍后重试'; })
    .finally(() => { isLoading.value = false; });


</script>

<style scoped>
.list-container { margin: auto; padding-bottom: 28px; color: var(--el-text-color-primary); }.list-header { display: flex; justify-content: space-between; align-items: center; min-height: 132px; margin-bottom: 18px; padding: 24px 32px; overflow: hidden; border: 1px solid var(--el-border-color-light); border-radius: 18px; background: linear-gradient(135deg, var(--el-color-primary-light-9), var(--el-bg-color)); }.eyebrow { margin: 0 0 7px; color: var(--el-color-primary); font-size: 11px; font-weight: 800; letter-spacing: .18em; }.list-header h1 { margin: 0; font-size: 30px; letter-spacing: -.04em; }.list-header p:last-child { margin: 9px 0 0; color: var(--el-text-color-secondary); }.list-header__mark { margin-right: 8%; color: var(--el-color-primary-light-5); font-size: 78px; transform: rotate(-10deg); opacity: .45; }.list-layout { align-items: stretch; }.directory-panel, .problem-panel { min-height: calc(var(--in-main-content-height) - 168px); padding: 24px; border: 1px solid var(--el-border-color-light); border-radius: 18px; background: var(--el-bg-color); }.panel-heading { display: flex; align-items: center; justify-content: space-between; margin-bottom: 18px; }.panel-heading strong, .panel-heading small { display: block; }.panel-heading small { margin-top: 4px; color: var(--el-text-color-secondary); font-size: 12px; }.panel-heading > .el-icon { color: var(--el-color-primary); font-size: 22px; }.directory-tree { padding: 4px; background: var(--el-fill-color-lighter); border-radius: 14px; }.directory-tree :deep(.el-tree-node__content) { height: 48px; margin: 3px 0; padding-right: 10px; border: 1px solid transparent; border-radius: 12px; transition: background .2s, border-color .2s, transform .2s; }.directory-tree :deep(.el-tree-node__content:hover) { background: var(--el-bg-color); border-color: var(--el-border-color-light); transform: translateX(2px); }.directory-tree :deep(.is-current > .el-tree-node__content) { background: var(--el-bg-color); border-color: var(--el-color-primary-light-5); box-shadow: 0 5px 14px color-mix(in srgb, var(--el-color-primary) 10%, transparent); }.directory-tree :deep(.el-tree-node__expand-icon) { color: var(--el-text-color-placeholder); }.tree-node { display: inline-flex; align-items: center; gap: 10px; color: var(--el-text-color-regular); font-size: 14px; }.tree-node__icon { display: grid; width: 30px; height: 30px; place-items: center; border-radius: 9px; color: var(--el-color-primary); background: var(--el-color-primary-light-9); }.tree-node__label { overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }.problem-panel { overflow: auto; }.list-table :deep(.el-table__cell) { padding: 13px 8px; }.list-table :deep(.el-table__inner-wrapper::before) { display: none; }.list-table :deep(.el-table__row:hover) { cursor: pointer; }.empty-status { min-height: 300px; }.list-table :deep(.el-link) { font-weight: 600; }
@media (max-width: 700px) { .list-container { padding: 8px 12px 28px; }.list-header { min-height: 108px; padding: 20px; }.list-header h1 { font-size: 25px; }.list-header p:last-child { font-size: 13px; }.list-header__mark { display: none; }.directory-panel, .problem-panel { min-height: auto; padding: 14px; }.directory-panel { margin-bottom: 14px; }.problem-panel { max-height: none; }.list-layout :deep(.el-col) { width: 100%; max-width: 100%; flex: 0 0 100%; }.list-table :deep(.el-table__cell) { padding: 9px 0; } }
</style>
