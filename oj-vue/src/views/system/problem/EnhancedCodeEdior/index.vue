<template>
  <div>
    <el-form :inline="true" ref="formRef" :model="codeForm">
      <el-form-item>
        <el-button-group>
          <el-button type="success" @click="onHandleSubmit" :icon="Promotion">
            运行
          </el-button>
          <el-button type="success" @click="onHandleSubmit" :icon="Upload">
            提交
          </el-button>
        </el-button-group>


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

      <el-form-item>
        <el-button type="primary" :icon="FullScreen" @click="onHandleFullScreen">全屏</el-button>
      </el-form-item>


    </el-form>
    <code-editor :language="currLang" :theme="theme" :height="codeEditHeight" ref="codeEditorRef" />
  </div>
</template>

<script setup lang="ts">
import CodeEditor from "@/components/CodeEditor/CodeEditor.vue";
import {computed, onMounted, reactive, ref, watch} from "vue";
import type {JudgeForm} from "@/api/problem/judge";
import useLanguage from "@/stores/useLanguage";
import type {LanguageView} from "@/api/language";
import {FullScreen, Promotion, Upload} from "@element-plus/icons-vue";

const languageStore = useLanguage();

const codeEditorRef = ref<InstanceType<typeof CodeEditor> | null>(null);

const formRef = ref<InstanceType<any>>(null)

const codeForm = reactive<JudgeForm>({
  contestId: undefined,
  languageId: -1,
  problemId: 0,
  answers: [{answer: codeEditorRef.value?.code || "", index: 1}]
});

const theme = ref("eclipse");

const currLang = ref<string>("");

const languages = ref<LanguageView[] | null>(null);

const {heightProp} = defineProps<{heightProp: number}>()

const emit = defineEmits<{
  (e: 'submit', form: JudgeForm): void;
  (e: 'fullScreen'): void;
}>()

const codeEditHeight = computed(() => {
  return heightProp - formRef.value?.$el.offsetHeight - 10;
})

const onHandleSubmit = () => {
  emit('submit', codeForm)
};

const onHandleFullScreen = () => {
  emit('fullScreen');
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