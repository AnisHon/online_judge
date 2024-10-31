
<template>
  <div class="top-right-btn" :style="style">
    <el-row>
      <el-tooltip class="item" effect="dark" :content="showSearch ? '隐藏搜索' : '显示搜索'" placement="top" v-if="search">
        <el-button size="small" circle icon="Search" @click="toggleSearch()" />
      </el-tooltip>
      <el-tooltip class="item" effect="dark" content="刷新" placement="top">
        <el-button size="small" circle icon="Refresh" @click="refresh()" />
      </el-tooltip>
      <el-tooltip class="item" effect="dark" content="显隐列" placement="top" v-if="columns">
        <el-button size="small" circle icon="Menu" @click="showColumn()" v-if="showColumnsType == 'transfer'"/>
        <el-dropdown trigger="click" :hide-on-click="false" style="padding-left: 12px" v-if="showColumnsType == 'checkbox'">
          <el-button size="small" circle icon="Menu" />
          <template #dropdown>
            <el-dropdown-menu class="top-right-btn-dropdown">
              <template v-for="item in columns">
                <el-dropdown-item>
                  <template :key="item.key" #default>
                    <el-checkbox :checked="item.visible" @change="checkboxChange($event, item.label)" :label="item.label" size="small"/>
                  </template>
                </el-dropdown-item>
              </template>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-tooltip>
    </el-row>
    <el-dialog :title="title" :visible.sync="open" append-to-body>
      <el-transfer
          :titles="['显示', '隐藏']"
          v-model="value"
          :data="columns"
          @change="dataChange"
      ></el-transfer>
    </el-dialog>
  </div>
</template>
<script setup lang="ts">
import {computed, ref} from "vue";
import type {ColumnType} from "@/hooks/useColumn";

const value = ref<number[]>([]);
const title = ref("显示/隐藏");
const open = ref(false)

const {
  showSearch = true,
  columns,
  search = true,
  showColumnsType = "checkbox",
  gutter = 10
} = defineProps<{
  showSearch?: boolean,
  columns: ColumnType[]
  search?: boolean,
  showColumnsType?: 'checkbox' | 'transfer'
  gutter?: number
}>()

const style = computed(() => {
  const ret: {marginRight?: string} = {};
  if (gutter) {
    ret.marginRight = `${gutter / 2}px`;
  }
  return ret;
})

const emit = defineEmits<{
  (e: 'update:showSearch', showSearch: boolean): void,
  (e: 'queryTable'): void
}>()

// 搜索
const toggleSearch = () => {
  emit("update:showSearch", !showSearch);
};

// 刷新
const refresh = () => {
  emit("queryTable");
};
// 右侧列表元素变化
const dataChange = (data: number[]) => {
  for (let item in columns) {
    const key = columns[item].key;
    columns[item].visible = !data.includes(key);
  }
};
// 打开显隐列dialog
const showColumn = () => {
  open.value = true;
};
// 勾选
const checkboxChange = (event: boolean, label: string) => {
  columns.filter(item => item.label == label)[0].visible = event;
};



// create “钩子"
if (showColumnsType == 'transfer') {
  // 显隐列初始默认隐藏列
  for (let item in columns) {
    if (!columns[item].visible) {
      value.value.push(parseInt(item));
    }
  }
}

</script>

<style scoped lang="scss">
:deep(.el-transfer__button) {
  border-radius: 50%;
  padding: 12px;
  display: block;
  margin-left: 0;
}
:deep( .el-transfer__button:first-child) {
  margin-bottom: 10px;
}

</style>

<style>

.top-right-btn-dropdown {
  .el-dropdown-menu__item {
    padding-bottom: 0;
    padding-top: 0;
  }
}



</style>