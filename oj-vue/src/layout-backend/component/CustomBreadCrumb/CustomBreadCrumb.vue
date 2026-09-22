<template>
  <nav class="backend-breadcrumb" aria-label="当前位置">
    <transition name="breadcrumb-fade" mode="out-in">
      <div :key="route.fullPath" class="breadcrumb-track">
      <span v-for="(item, index) in breadcrumbs" :key="item.key" class="breadcrumb-node">
        <span v-if="index > 0" class="breadcrumb-separator" aria-hidden="true">/</span>
        <router-link v-if="index !== breadcrumbs.length - 1" class="breadcrumb-item" :to="item.path">
          <el-icon v-if="index === 0"><House /></el-icon>
          <span>{{ item.title }}</span>
        </router-link>
        <span v-else class="breadcrumb-item current">
          <el-icon v-if="index === 0"><House /></el-icon>
          <span>{{ item.title }}</span>
        </span>
      </span>
      </div>
    </transition>
  </nav>
</template>

<script setup lang="ts">
import {computed} from "vue";
import {useRoute, useRouter} from "vue-router";
import {House} from "@element-plus/icons-vue";

const route = useRoute();
const router = useRouter();

const breadcrumbs = computed(() => route.matched
    .filter(item => !!item.meta?.name && item.name !== 'backend')
    .map((item, index) => {
      let path = item.path || '/backend';
      if (item.name) {
        try {
          path = router.resolve({name: item.name, params: route.params, query: route.query}).fullPath;
        } catch (_) {
          // 某些历史菜单只有 path，没有可解析的命名路由，保留原始路径。
        }
      }
      return {
      key: `${String(item.name)}-${item.path}-${index}-${route.fullPath}`,
      title: String(item.meta?.name),
      path
    };
    }));
</script>

<style scoped>
.backend-breadcrumb { min-width: 0; max-width: min(52vw, 680px); overflow: hidden; }
.breadcrumb-track { display: flex; align-items: center; min-width: 0; height: 32px; overflow: hidden; white-space: nowrap; }
.breadcrumb-node { display: inline-flex; align-items: center; min-width: 0; }
.breadcrumb-item { display: inline-flex; flex: 0 1 auto; align-items: center; gap: 5px; min-width: 0; padding: 6px 9px; border-radius: 8px; color: var(--el-text-color-secondary); font-size: 13px; text-decoration: none; transition: color .2s, background .2s; }
.breadcrumb-item span { overflow: hidden; text-overflow: ellipsis; }
.breadcrumb-item:hover { color: var(--el-color-primary); background: var(--el-fill-color-lighter); }
.breadcrumb-item.current { color: var(--el-text-color-primary); font-weight: 600; }
.breadcrumb-separator { flex: 0 0 auto; margin: 0 2px; color: var(--el-text-color-placeholder); font-size: 13px; }
.breadcrumb-fade-enter-active, .breadcrumb-fade-leave-active { transition: opacity .14s ease; }
.breadcrumb-fade-enter-from, .breadcrumb-fade-leave-to { opacity: 0; }
@media (max-width: 720px) { .backend-breadcrumb { max-width: 44vw; }.breadcrumb-item { padding: 6px 5px; font-size: 12px; }.breadcrumb-separator { margin: 0; } }
</style>
