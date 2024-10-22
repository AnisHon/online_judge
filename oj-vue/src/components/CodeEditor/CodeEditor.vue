<template>
  <Codemirror
      v-model:value="code"
      :options="cmOptions"
      :height="height"
      width="100%"
      class="cm-component"
      :border="true"
      @ready="onReady"
  />
</template>
<script lang="ts" setup>
import {ref, reactive, computed} from "vue";
import { type Editor, type EditorConfiguration } from "codemirror";
import Codemirror from "codemirror-editor-vue3";
// mode
import "codemirror/mode/python/python.js"
import "codemirror/mode/clike/clike.js"

// theme
import "codemirror/theme/dracula.css"

const code = ref("");

const {language, height} = defineProps<{
  language: 'java' | 'c' | 'c++' | 'python'
  height: number;
}>()

const modeMap = {
  'java' :'text/x-java',
  'c': "text/x-csrc",
  'c++': 'text/x-c++src',
  'python': 'text/x-python',
}

const mode = computed(() => {
  return modeMap[language]
})

const cmOptions: EditorConfiguration = reactive({
  mode: mode,
  theme: "dracula",
  readOnly: false,
  lineNumbers: true,
  lineWiseCopyCut: true,
  gutters: ["CodeMirror-lint-markers"],
  lint: true,
});

const cminstance = ref<Editor | null>(null);
const onReady = (cm: Editor) => {
  cminstance.value = cm;
  console.log(cm.getValue());
};




</script>
<style scoped>
.cm-component {
  font-family: monospace;
}
</style>
