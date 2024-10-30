<template>
  <el-row style="height: 100vh;">
    <el-col :span="16" class="background">

    </el-col>
    <el-col :span="8" style="display: flex; align-self: center; justify-content: center; padding: 10px;">
      <div style="min-width: 360px; max-width: 540px">

        <div class="header" style="width: 100%; display: flex; justify-content: center;">
          <el-image src="/auth/auth_logo.webp" style="width: 30%"/>
        </div>

        <router-view v-slot="{ Component }" style="box-sizing: content-box;">
          <transition name="el-fade-in" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>

        <el-divider />
        <el-row justify="space-between">

          <el-col :span="12">
            <router-link :to="routerTo.name">{{ routerTo.text }}</router-link>
          </el-col>

          <el-col :span="12" style="position: relative;" v-show="showForgetPass">
                <router-link style="position: absolute; right: 0" :to="{name: 'forget-password'}">忘记密码</router-link>
          </el-col>
        </el-row>

      </div>

    </el-col>
  </el-row>

</template>


<script setup lang="ts">
import {computed, ref, watch} from "vue";
import {useRoute} from "vue-router";

  const route = useRoute()

  const url = ref()



  const lastLocation = computed(() => {
    const arr = url.value.split("/");
    return arr[arr.length - 1];
  })

  const routerTo = computed<{name: string, text: string}>(() => {
    if (lastLocation.value.includes("login")) {
      return {name: "sign-up", text: "去注册"};
    } else {
      return {name: "login", text: "去登录"};
    }
  });

  watch(() => route.path, (value) => {
    url.value = value;
  },{immediate: true});

  const showForgetPass = computed(() => {
    return lastLocation.value.includes("login");
  })
</script>

<style scoped>
.background {
  background-image: url("/auth/auth_background.png");
}



</style>