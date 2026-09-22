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
import {ref, reactive, computed, watch, onBeforeUnmount} from "vue";
import { type Editor, type EditorConfiguration } from "codemirror";
import Codemirror from "codemirror-editor-vue3";

import 'codemirror/lib/codemirror.css';


// mode
// theme

// import "codemirror/theme/dracula.css"
// import 'codemirror/theme/blackboard.css';
// import 'codemirror/theme/cobalt.css';
import 'codemirror/theme/eclipse.css';
import 'codemirror/theme/material-darker.css';

import 'codemirror/mode/javascript/javascript.js';
import 'codemirror/mode/css/css.js';
import 'codemirror/mode/clike/clike.js';
import 'codemirror/mode/python/python.js';
import 'codemirror/mode/go/go.js';


// 引入代码自动提示插件
import 'codemirror/addon/hint/show-hint.css';
import 'codemirror/addon/hint/show-hint';
import 'codemirror/addon/hint/anyword-hint'

import 'codemirror/addon/edit/matchbrackets'
import 'codemirror/addon/edit/closebrackets'

import {useDark} from "@vueuse/core";

const isDark = useDark();

const code = defineModel<string>({required: true})



const {language, height} = defineProps<{
  language: string,
  height: number,
}>()

const resolveMode = (value: string) => {
  const name = value.toLowerCase().replace(/[\s_-]+/g, '');
  if (name.includes('c++') || name.includes('cpp')) return 'text/x-c++src';
  if (name === 'c' || name.startsWith('cwith')) return 'text/x-csrc';
  if (name.includes('java') || name.includes('kotlin')) return 'text/x-java';
  if (name.includes('python')) return 'text/x-python';
  if (name.includes('go')) return 'text/x-go';
  if (name.includes('javascript') || name === 'js' || name.includes('typescript') || name === 'ts') return 'text/javascript';
  return 'text/plain';
};

const mode = computed((): string => resolveMode(language || ''));

const theme = computed(() => {
  return isDark.value ? "material-darker" : "eclipse";
})



const cmOptions: EditorConfiguration = reactive({

  mode: mode.value,
  theme: theme.value,
  readOnly: false,
  tabSize: 4,
  indentUnit: 4,
  indentWithTabs: true,
  lineNumbers: true,
  lineWiseCopyCut: true,
  gutters: ["CodeMirror-lint-markers"],
  lint: true,
  autofocus: true,
  autoCloseBrackets: true,
  autoCloseTags: true,
  matchBrackets: true,
  extraKeys: {'Ctrl-Space': 'autocomplete', 'Cmd-Space': 'autocomplete'},
  hintOptions: {completeSingle: false}
});


const cminstance = ref<Editor | null>(null);
let hintTimer: ReturnType<typeof setTimeout> | undefined;
const onReady = (cm: Editor) => {
  cminstance.value = cm;
  cm.on('inputRead', (_instance, change) => {
    if (!change.origin || !change.origin.startsWith('+')) return;
    if (hintTimer) clearTimeout(hintTimer);
    hintTimer = setTimeout(() => cm.showHint({completeSingle: false}), 180);
  });
};

watch(mode, () => {
  cminstance.value?.setOption('mode', mode.value);

}, {immediate: true});

watch(theme, () => {
  cminstance.value?.setOption('theme', theme.value);
  cminstance.value?.refresh()
})

onBeforeUnmount(() => {
  if (hintTimer) clearTimeout(hintTimer);
  cminstance.value?.closeHint?.();
});



defineExpose({code})


</script>
<style scoped>
.CodeMirror,
.CodeMirror pre,
.CodeMirror textarea,
.cm-component {
  display: block;
  min-height: 0;
  max-width: 100%;
  max-height: 100%;
  overflow: hidden;
  font-family: "OJCodeFont", "JetBrains Mono", "Cascadia Code", Consolas,
    "Liberation Mono", "DejaVu Sans Mono", monospace !important;
  font-variant-ligatures: none;
}


</style>

<style>
.CodeMirror-scroll {
  font-family: "OJCodeFont", "JetBrains Mono", "Cascadia Code", Consolas,
    "Liberation Mono", "DejaVu Sans Mono", monospace !important;
}
</style>
