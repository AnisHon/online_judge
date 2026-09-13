<template>
  <main class="auth-page">
    <section class="brand-panel">
      <div class="brand-copy">
        <div class="brand-mark">延</div>
        <p class="eyebrow">ONLINE JUDGE PLATFORM</p>
        <h1>让每一次提交<br><span>都更接近答案</span></h1>
        <p class="brand-description">练习、竞赛与分享，构建属于你的编程成长空间。</p>
      </div>
      <div class="glow glow-one"></div><div class="glow glow-two"></div>
    </section>
    <section class="form-panel">
      <div class="form-shell">
        <div class="mobile-brand"><div class="brand-mark small">延</div><span>{{ siteConfig.config.siteName }}</span></div>
        <div class="header"><el-image src="/auth/auth_logo.webp" class="logo" fit="contain" /></div>
        <router-view v-slot="{ Component }">
          <transition name="el-fade-in" mode="out-in"><component :is="Component" /></transition>
        </router-view>
        <el-divider class="auth-divider" />
        <el-row justify="space-between" class="auth-links">
          <el-col :span="12"><router-link :to="routerTo.name">{{ routerTo.text }}</router-link></el-col>
          <el-col :span="12" v-show="showForgetPass"><router-link class="right-link" :to="{name: 'forget-password'}">忘记密码</router-link></el-col>
        </el-row>
        <footer class="auth-footer">
          <span>{{ siteConfig.config.siteName }}</span><span class="footer-dot">·</span>
          <a href="https://beian.miit.gov.cn/" target="_blank" rel="noopener noreferrer">{{ siteConfig.config.icpNumber }}</a>
        </footer>
      </div>
    </section>
  </main>
</template>

<script setup lang="ts">
import {computed, ref, watch} from "vue";
import {useRoute} from "vue-router";
import {useSiteConfig} from "@/stores/useSiteConfig";

const route = useRoute()
const url = ref(route.path)
const siteConfig = useSiteConfig()
const lastLocation = computed(() => url.value.split("/").pop() || "")
const routerTo = computed<{name: string, text: string}>(() => lastLocation.value.includes("login") ? {name: "sign-up", text: "去注册"} : {name: "login", text: "去登录"})
const showForgetPass = computed(() => lastLocation.value.includes("login"))
watch(() => route.path, value => { url.value = value }, {immediate: true})
</script>

<style scoped lang="scss">
.auth-page { min-height: 100vh; display: grid; grid-template-columns: minmax(420px, 1.12fr) minmax(420px, .88fr); overflow: hidden; background: #f7f9fc; }
.brand-panel { position: relative; display: flex; align-items: center; padding: clamp(48px, 8vw, 120px); overflow: hidden; color: #fff; background: linear-gradient(145deg, #172554 0%, #1e3a8a 48%, #2563eb 100%); }
.brand-copy { position: relative; z-index: 1; max-width: 560px; }
.brand-mark { width: 58px; height: 58px; display: grid; place-items: center; border: 1px solid rgba(255,255,255,.4); border-radius: 18px; color: #dbeafe; font-size: 30px; font-weight: 800; background: rgba(255,255,255,.14); backdrop-filter: blur(12px); }
.brand-mark.small { width: 34px; height: 34px; border-radius: 10px; font-size: 18px; }
.eyebrow { margin: 34px 0 18px; color: #bfdbfe; letter-spacing: .18em; font-size: 12px; font-weight: 700; }
.brand-copy h1 { margin: 0; font-size: clamp(38px, 4.5vw, 68px); line-height: 1.12; letter-spacing: -.04em; }
.brand-copy h1 span { color: #93c5fd; }.brand-description { margin-top: 26px; color: #dbeafe; font-size: 16px; line-height: 1.8; }
.glow { position: absolute; border-radius: 50%; background: rgba(147,197,253,.2); }.glow-one { width: 440px; height: 440px; right: -180px; top: -160px; }.glow-two { width: 320px; height: 320px; left: -180px; bottom: -160px; background: rgba(96,165,250,.22); }
.form-panel { display: flex; align-items: center; justify-content: center; padding: 36px; }.form-shell { width: min(100%, 440px); }.header { display: flex; justify-content: center; margin-bottom: 24px; }.logo { width: 132px; height: 62px; }.mobile-brand { display: none; align-items: center; gap: 10px; margin-bottom: 26px; color: #172554; font-size: 18px; font-weight: 800; }
.auth-divider { margin: 28px 0 18px; }.auth-links { font-size: 14px; }.auth-links a { color: #64748b; text-decoration: none; transition: color .2s; }.auth-links a:hover { color: #2563eb; }.right-link { float: right; }.auth-footer { margin-top: 54px; color: #94a3b8; font-size: 12px; text-align: center; }.auth-footer a { color: inherit; text-decoration: none; }.auth-footer a:hover { color: #2563eb; text-decoration: underline; }.footer-dot { padding: 0 8px; }
@media (max-width: 800px) { .auth-page { display: block; background: linear-gradient(160deg, #eff6ff, #f8fafc 42%); }.brand-panel { display: none; }.form-panel { min-height: 100vh; padding: 28px 22px; }.mobile-brand { display: flex; }.logo { display: none; }.auth-footer { margin-top: 40px; } }
</style>
