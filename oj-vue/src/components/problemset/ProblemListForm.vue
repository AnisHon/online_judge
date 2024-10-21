<template>

  <div>
    <el-form :inline="true" :model="queryForm" style="display: flex; justify-content: center; margin: 20px">
      <el-form-item label="搜索ID" style="width: 20%">
        <el-input v-model="queryForm.id" placeholder="搜索ID" clearable />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" @click="handleQuery" >查询</el-button>
      </el-form-item>

    </el-form>

    <el-row justify="space-between" style="margin: 20px">
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
        <el-link @click="handleChooseTag" >选择标签</el-link>
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


import {onMounted, reactive, ref} from "vue";
import {getAllTags} from "@/api/problem/label";
import {type TagView} from "@/api/problem/label"



const tagsMap = new Map<number, TagView>();

const queryForm = reactive({
  id: "",
  tagIds: []
});

const tagDialogVisible = ref(false);

const emit = defineEmits(['query'])

const getTag = (id: number): TagView => {
  return <TagView>tagsMap.get(id)
};

const handleChooseTag = () => {
  tagDialogVisible.value = true;
}

const handleCheckTag = (id: number) => {
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