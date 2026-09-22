<template>

  <div>
    <el-form :inline="true" :model="queryForm" style="display: flex; justify-content: center; margin: 20px">
      <el-form-item label="搜索ID">
        <el-select style="width: 100px" :default-first-option="true" v-model="select" @change="onSelectChange">
          <el-option value="1" label="标题" />
          <el-option value="2" label="ID" />
        </el-select>
      </el-form-item>
      <el-form-item style="width: 20%">
        <el-input v-model="input" placeholder="搜索" clearable  @keyup.enter="handleQuery"/>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery" >查询</el-button>
      </el-form-item>
      <el-form-item>
        <el-button type="danger" @click="onResetHandler" >重置</el-button>
      </el-form-item>

    </el-form>




    <el-row justify="space-between" style="margin: 20px">
      <div>
        <span>题目类型： </span>
        <el-radio-group v-model="queryForm.type">
          <el-radio :value="ProblemType.OJ">OJ</el-radio>
          <el-radio :value="ProblemType.FILL">填空</el-radio>
          <el-radio :value="ProblemType.CHOICE">选择</el-radio>
          <el-radio :value="ProblemType.MULTI_CHOICE">多选</el-radio>
        </el-radio-group>
      </div>
      <el-col :span="18">
        <span>选中标签：</span>
        <el-space wrap>
          <el-tag v-for="tagId of queryForm.tagIds" :color="getTag(tagId)?.tagColor" :key="tagId">
              <span style="color: white">
                {{ getTag(tagId)?.tagName }}
              </span>

          </el-tag>
        </el-space>
      </el-col>
      <el-col :span="6">
        <el-link @click="handleChooseTag" type="primary">选择标签</el-link>
      </el-col>
    </el-row>



    <el-dialog class="tag-dialog" v-model="tagDialogVisible" title="选择标签"
               width="min(680px, calc(100vw - 32px))" append-to-body destroy-on-close>
      <div class="tag-dialog-toolbar">
        <span>可以选择多个标签筛选题目</span>
        <el-button v-if="dialogTagIds.length" link type="primary" @click="dialogTagIds = []">清空选择</el-button>
      </div>
      <div v-if="tags.length" class="tag-card-grid" role="list">
        <button
            v-for="item of tags"
            :key="String(item.tagId)"
            type="button"
            class="tag-card"
            :class="{ 'is-selected': isDialogTagSelected(item.tagId) }"
            :style="{ '--tag-color': item.tagColor || '#64748b' }"
            role="listitem"
            :aria-pressed="isDialogTagSelected(item.tagId)"
            @click="handleCheckTag(item.tagId)"
        >
          <span class="tag-card__dot" aria-hidden="true" />
          <span class="tag-card__name">{{ item.tagName }}</span>
          <el-icon v-if="isDialogTagSelected(item.tagId)" class="tag-card__check"><Check /></el-icon>
        </button>
      </div>
      <el-empty v-else description="暂无标签" :image-size="64" />
      <template #footer>
        <el-button @click="cancelTagSelection">取消</el-button>
        <el-button type="primary" @click="applyTagSelection">应用筛选</el-button>
      </template>

    </el-dialog>
  </div>



</template>
<script setup lang="ts">


import {computed, type ModelRef, onMounted, reactive, ref} from "vue";
import {getAllTags} from "@/api/problem/label";
import {type TagView} from "@/api/problem/label"
import {ProblemType} from "@/api/problem"
import type {ListProblemQuery} from "@/api/list";
import __ from "lodash";
import type {IdType} from "@/api/common.ts";
import {Check} from '@element-plus/icons-vue';



const tags = reactive<TagView[]>([]);

const queryForm = <ModelRef<ListProblemQuery>>defineModel<ListProblemQuery>()

const select = ref("1");

const tagDialogVisible = ref(false);
const dialogTagIds = ref<IdType[]>([]);

const getTag = (id: IdType) => {
  return __.find(tags, x => x.tagId === id);
}

const emit = defineEmits<{
  (e: 'query'): void
}>();

const input = computed({
  get: () => {
    return select.value === '1' ? queryForm.value.title : queryForm.value.problemId;
  },
  set: (value: string) => {
    if (select.value === '1') {
      queryForm.value.title = value
    } else {
      queryForm.value.problemId = value
    }
  }
})


const onSelectChange = () => {

    queryForm.value.problemId = "";
    queryForm.value.title = "";

}

const handleChooseTag = () => {
  dialogTagIds.value = [...(queryForm.value.tagIds || [])];
  tagDialogVisible.value = true;
}

const onResetHandler = () => {
  queryForm.value.problemId = "";
  queryForm.value.title = "";
  queryForm.value.tagIds = [];
  dialogTagIds.value = [];
  queryForm.value.type = undefined;
  emit('query');
}

const handleCheckTag = (id: IdType) => {
  const index = dialogTagIds.value.findIndex(item => String(item) === String(id));
  if (index === -1 || index === undefined) {
    dialogTagIds.value.push(id);
  } else {
    dialogTagIds.value.splice(index, 1);
  }
}

const isDialogTagSelected = (id: IdType) => dialogTagIds.value.some(item => String(item) === String(id));
const cancelTagSelection = () => { tagDialogVisible.value = false; };
const applyTagSelection = () => {
  queryForm.value.tagIds = [...dialogTagIds.value];
  tagDialogVisible.value = false;
  handleQuery();
};

const handleQuery = () => {
  emit("query");
}

onMounted(() => {
  getAllTags().then((data) => {
    tags.push(...data);
  })
})


</script>

<style scoped>
.tag-dialog-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin: -4px 0 14px; color: var(--el-text-color-secondary); font-size: 12px; }
.tag-card-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(145px, 1fr)); gap: 10px; max-height: min(52vh, 460px); overflow-y: auto; padding: 3px; }
.tag-card { display: flex; min-width: 0; min-height: 48px; align-items: center; gap: 9px; padding: 9px 11px; border: 1px solid var(--el-border-color-lighter); border-radius: 11px; color: var(--el-text-color-primary); background: var(--el-bg-color); cursor: pointer; text-align: left; transition: border-color .18s ease, background-color .18s ease, transform .18s ease; }
.tag-card:hover { border-color: color-mix(in srgb, var(--tag-color) 52%, var(--el-border-color)); background: color-mix(in srgb, var(--tag-color) 7%, var(--el-bg-color)); transform: translateY(-1px); }
.tag-card.is-selected { border-color: color-mix(in srgb, var(--tag-color) 66%, var(--el-border-color)); background: color-mix(in srgb, var(--tag-color) 12%, var(--el-bg-color)); }
.tag-card__dot { width: 9px; height: 9px; flex: 0 0 auto; border-radius: 50%; background: var(--tag-color); box-shadow: 0 0 0 4px color-mix(in srgb, var(--tag-color) 14%, transparent); }
.tag-card__name { min-width: 0; overflow: hidden; flex: 1; font-size: 13px; text-overflow: ellipsis; white-space: nowrap; }
.tag-card__check { flex: 0 0 auto; color: var(--tag-color); }
</style>
