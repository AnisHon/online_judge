<template>
  <div class="list-container" >
    <el-row :gutter="20">
      <el-col :span="8" class="content">
        <h2 style="text-align: center">目录</h2>
        <el-tree
            :data="treedViews"
            :props="defaultProps"
            @node-click="handleNodeClick"
        >
          <template #default="{node, data}" style="margin-top: 20px">

            <span style="font-size: 20px;">
              <el-icon v-if="!data.file" size="large"><Folder /></el-icon>
              <el-icon v-else size="large"><Document /></el-icon>
              <span>
                {{ node.label }}
              </span>
            </span>


          </template>
        </el-tree>
      </el-col>
      <el-col :span="16" class="content">
        <el-table v-loading="isLoading" :data="orderedTableList" v-show="tableList.length > 0" stripe>
          <el-table-column label="问题ID" align="center" prop="problemId"/>
          <el-table-column label="题目" prop="title">
            <template v-slot="scope">
              <el-link type="primary"  @click="handleClickProblem(scope.row.problemId)">{{ scope.row.title }}</el-link>
            </template>
          </el-table-column>
          <el-table-column label="问题来源" align="center" prop="source" />
          <el-table-column label="问题类型" align="center" prop="type">
            <template v-slot="scope">
              <el-tag type="primary">{{ problemTypeToString(scope.row.type) }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
        <el-empty class="empty-status" v-show="tableList.length === 0" description="这里空空如野"/>
      </el-col>

    </el-row>


  </div>
</template>

<script lang="ts" setup>
import {computed, reactive, ref} from 'vue'
import {getTreedFolderView, type TreedFolderView} from "@/api/folder";
import {getProblems, type ProblemInListView} from "@/api/list";
import {useRouter} from "vue-router";
import {problemTypeToString} from "@/utils/problem";
import {Document, Folder} from "@element-plus/icons-vue";

const router = useRouter();
const defaultProps = {
  children: (x: TreedFolderView) => x.children,
  label: (x: TreedFolderView) => x.folder.folderName,
}

const treedViews = reactive<TreedFolderView[]>([])

const tableList = reactive<ProblemInListView[]>([])
const orderedTableList = computed(() => {

  tableList.sort((a, b) => <number>a.problemOrder - <number>b.problemOrder)
  return tableList;
})

const isLoading = ref<boolean>(false);

const handleClickProblem = (id: number) => {
  const routeUrl = router.resolve({
    name: "problem",
    params: {id: id}
  })
  window.open(routeUrl.href, '_blank')
}

const handleNodeClick = (node: TreedFolderView) => {
  if (node.file) {
    getProblems(node.folder.listId)
        .then(data => {
          tableList.length = 0;
          tableList.push(...data);
        })
  }
}

// created
getTreedFolderView()
    .then((data) => treedViews.push(...data));


</script>

<style lang="scss" scoped>
.list-container {
    .content {

      height: var(--in-main-content-height);
      overflow: auto;

      .empty-status {
        position: relative;
        top: 50%;
        transform: translateY(-50%);
      }

    }


}

</style>