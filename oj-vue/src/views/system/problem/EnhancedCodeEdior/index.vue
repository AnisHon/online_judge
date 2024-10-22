<template>
  <div>
    <el-form :inline="true" :model="codeForm">
      <el-form-item>

        <el-button type="primary" @click="onHandleSubmit">
          提交判题
        </el-button>

      </el-form-item>
      <el-form-item label="语言">
        <el-select
            v-model="codeForm.languageId"
            placeholder="Select"
            style="width: 150px"
        >
          <el-option
              v-for="item in languages"
              :key="item.languageId"
              :label="item.languageName"
              :value="item.languageId"
          />
        </el-select>
      </el-form-item>

    </el-form>
    <code-editor :language="currLang" :height="800" ref="codeEditorRef" />
  </div>
</template>

<script setup lang="ts">

import CodeEditor from "@/components/CodeEditor/CodeEditor.vue";
import {onMounted, reactive, ref, watch} from "vue";
import type {Answer, JudgeForm} from "@/api/problem/judege";
import useLanguage from "@/stores/useLanguage";
import type {LanguageView} from "@/api/language";

const languageStore = useLanguage();

const codeEditorRef = ref<InstanceType<typeof CodeEditor> | null>(null);

const codeForm = reactive<JudgeForm>({
  contestId: undefined,
  languageId: -1,
  problemId: 0,
  answers: [{answer: codeEditorRef.value?.code || "", index: 1}]
});

const currLang = ref<string>("")

const languages = ref<LanguageView[] | null>(null);

const onHandleSubmit = () => {
};



watch(() => codeForm.languageId, () => {
  languages.value?.forEach((item) => {
    if (item.languageId === codeForm.languageId) {
      currLang.value = item.languageName;
    }
  })
})


onMounted(() => {
  languageStore.getLanguages()
      .then((languageArray) => {
        languages.value = languageArray;
        codeForm.languageId = languageArray[0].languageId;
      })
})



</script>

<style scoped>

</style>