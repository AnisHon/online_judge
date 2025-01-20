<template>
 <div>
   <el-icon size="32">
     <Component :is="icon"/>
   </el-icon>
 </div>
</template>

<script setup lang="ts">
// @ts-ignore
// import FileIcons from 'file-icons-vue';

import IconFolder from "@/assets/icons/IconFolder.vue";
import {matchType, MatchType} from "@/utils/matchType.ts";
import {type Component, computed} from "vue";
import IconExcel from "@/assets/icons/IconExcel.vue";
import IconWord from "@/assets/icons/IconWord.vue";
import IconPdf from "@/assets/icons/IconPdf.vue";
import IconMusic from "@/assets/icons/IconMusic.vue";
import IconFileUnknown from "@/assets/icons/IconFileUnknown.vue";
import IconZip from "@/assets/icons/IconZip.vue";
import IconPPT from "@/assets/icons/IconPPT.vue";

const {isFolder, name} = defineProps<{isFolder: boolean, name: string}>();
const typeComponentMapping: Record<MatchType, string | Component> = {
  [MatchType.FOLDER]: IconFolder,
  [MatchType.IMAGE]: 'PictureFilled',
  [MatchType.TEXT]: 'Document',
  [MatchType.EXCEL]: IconExcel,
  [MatchType.WORD]: IconWord,
  [MatchType.PDF]: IconPdf,
  [MatchType.PPT]: IconPPT,
  [MatchType.VIDEO]: 'VideoPlay',
  [MatchType.RADIO]: IconMusic,
  [MatchType.ZIP]: IconZip,
  [MatchType.OTHER]: IconFileUnknown,
}

const icon = computed(() => {
  const type: MatchType = isFolder ? MatchType.FOLDER : matchType(name);
  return typeComponentMapping[type];
})

</script>


<style scoped>

</style>