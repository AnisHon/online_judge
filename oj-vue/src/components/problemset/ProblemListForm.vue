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
          <el-radio value="OJ">OJ</el-radio>
          <el-radio value="FILL">填空</el-radio>
          <el-radio value="CHOICE">选择</el-radio>
          <el-radio value="MULTI_CHOICE">多选</el-radio>
        </el-radio-group>
      </div>
      <el-col :span="18">
        <span>选中标签：</span>
        <el-space wrap>
          <el-tag v-for="tagId of queryForm.tagIds" :color="getTag(tagId).tagColor" :key="tagId">
              <span style="color: white">
                {{ getTag(tagId).tagName }}
              </span>

          </el-tag>
        </el-space>
      </el-col>
      <el-col :span="6">
        <el-link @click="handleChooseTag" type="primary">选择标签</el-link>
      </el-col>
    </el-row>
    <el-dialog v-model="tagDialogVisible" title="选择ID">
      <el-space wrap>
        <el-check-tag
            v-for="[key, value] of tagsMap"
            :key="key"
            :checked="queryForm.tagIds.includes(key)"
            @click="handleCheckTag(key)"
        >
          {{ value.tagName }}
        </el-check-tag>
      </el-space>

    </el-dialog>
  </div>



</template>
<script setup lang="ts">


import {computed, onMounted, reactive, ref} from "vue";
import {getAllTags} from "@/api/problem/label";
import {type TagView} from "@/api/problem/label"
import type {ProblemType} from "@/api/problem";
import type {IdType} from "@/api/common.ts";



const tagsMap = new Map<IdType, TagView>();

const queryForm = reactive<{id: IdType, tagIds: IdType[], title: string, type?: ProblemType}>({
  id: "",
  tagIds: [],
  title: "",
  type: undefined
});

const select = ref("1");

const tagDialogVisible = ref(false);

const emit = defineEmits(['query'])

const input = computed({
  get: () => {
    return select.value === '1' ? queryForm.title : queryForm.id;
  },
  set: (value: string) => {
    if (select.value === '1') {
      queryForm.title = value
    } else {
      queryForm.id = value
    }
  }
})

const getTag = (id: IdType): TagView => {
  return <TagView>tagsMap.get(id)
};

const onSelectChange = () => {

    queryForm.id = "";
    queryForm.title = "";

}

const handleChooseTag = () => {
  tagDialogVisible.value = true;
}

const onResetHandler = () => {
  queryForm.id = "";
  queryForm.title = "";
  queryForm.tagIds = [];
  queryForm.type = undefined;
}

const handleCheckTag = (id: IdType) => {
  const index = queryForm.tagIds.indexOf(id);
  if (index === -1) {
    queryForm.tagIds.push(id);
  } else {
    queryForm.tagIds.splice(index, 1);
  }
}

const handleQuery = () => {
  emit("query", queryForm);
}

onMounted(() => {
  getAllTags().then((data) => {
    data.forEach((tag: TagView) => {
      tagsMap.set(tag.tagId, tag);
    })
  })
})


</script>