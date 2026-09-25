<template>
  <main class="app-container common-max-width-page faq-page">
    <header class="faq-heading">
      <p class="faq-kicker">HELP CENTER</p>
      <h1>常见问题</h1>
      <p>关于平台使用的一些简明说明。</p>
    </header>

    <el-skeleton v-if="loading" :rows="5" animated />
    <el-collapse v-else-if="faqs.length" v-model="activeFaq" accordion class="faq-list">
      <el-collapse-item v-for="(faq, index) in faqs" :key="faq.faqId" :name="faq.faqId">
        <template #title>
          <span class="faq-index">{{ String(index + 1).padStart(2, '0') }}</span>
          <span class="faq-question">{{ faq.question }}</span>
        </template>
        <div class="faq-answer">
          <span class="faq-answer__mark">答</span>
          <p>{{ faq.answer }}</p>
        </div>
      </el-collapse-item>
    </el-collapse>
    <el-empty v-else description="暂时还没有常见问题" />
  </main>
</template>

<script setup lang="ts">
import {onMounted, ref} from "vue";
import {ElMessage} from "element-plus";
import {listFaq, type Faq} from "@/api/faq";

const faqs = ref<Faq[]>([]);
const activeFaq = ref<string>();
const loading = ref(true);

onMounted(async () => {
  try {
    faqs.value = await listFaq();
  } catch {
    ElMessage.error("常见问题暂时无法加载，请稍后重试");
  } finally {
    loading.value = false;
  }
});
</script>

<style scoped>
.faq-page { max-width: 920px; margin: 0 auto; padding-top: 24px; padding-bottom: 40px; }
.faq-heading { margin: 0 0 22px; }
.faq-kicker { margin: 0 0 7px; color: var(--el-color-primary); font-size: 10px; font-weight: 800; letter-spacing: .16em; }
.faq-heading h1 { margin: 0; color: var(--el-text-color-primary); font-size: clamp(26px, 4vw, 34px); letter-spacing: -.04em; }
.faq-heading > p:last-child { margin: 8px 0 0; color: var(--el-text-color-secondary); font-size: 13px; }
.faq-list { overflow: hidden; border: 1px solid var(--el-border-color-lighter); border-radius: 14px; background: var(--el-bg-color); }
.faq-list :deep(.el-collapse-item__header) { min-height: 58px; height: auto; padding: 10px 18px; color: var(--el-text-color-primary); font-size: 14px; font-weight: 600; line-height: 1.6; }
.faq-list :deep(.el-collapse-item__wrap) { background: var(--el-bg-color); }
.faq-list :deep(.el-collapse-item__content) { padding: 0 18px 18px; }
.faq-index { flex: 0 0 auto; margin-right: 12px; color: var(--el-color-primary); font: 700 11px var(--code-font-family, monospace); }
.faq-question { min-width: 0; padding-right: 8px; }
.faq-answer { display: grid; grid-template-columns: 24px minmax(0, 1fr); gap: 10px; padding: 14px 16px; border-radius: 10px; background: var(--el-fill-color-lighter); }
.faq-answer__mark { display: grid; width: 23px; height: 23px; place-items: center; border-radius: 7px; color: var(--el-color-primary); background: var(--el-color-primary-light-9); font-size: 12px; font-weight: 700; }
.faq-answer p { min-width: 0; margin: 1px 0 0; color: var(--el-text-color-regular); font-size: 13px; line-height: 1.85; white-space: pre-wrap; overflow-wrap: anywhere; }
@media (max-width: 600px) { .faq-page { padding: 18px 14px 30px; }.faq-list :deep(.el-collapse-item__header) { padding: 10px 13px; }.faq-list :deep(.el-collapse-item__content) { padding: 0 12px 13px; }.faq-answer { padding: 12px; } }
</style>
