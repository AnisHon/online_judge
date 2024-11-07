<template>

  <transition name="el-zoom-in-top">
    <div class="common-max-width-page" style="margin: auto" v-if="!!userStore.user">
      <el-card>
        <header class="setting-header">
          <el-page-header @back="goBack" style="margin: 10px 0">
            <template #content>
              <span class="text-large font-600 mr-3"> 个人信息 </span>
            </template>
          </el-page-header>
          <el-descriptions border>
            <el-descriptions-item
                :rowspan="2"
                :width="140"
                label="头像"
                align="center"
            >
              <el-image
                  style="width: 100px; height: 100px"
                  src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png"
              />
            </el-descriptions-item>
            <el-descriptions-item label="用户名">{{ user.userName }}</el-descriptions-item>
            <el-descriptions-item label="昵称">{{ user.nikeName }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ user.email }}</el-descriptions-item>
            <el-descriptions-item label="身份">
              <el-space wrap>
                <el-tag size="small">School</el-tag>
              </el-space>
            </el-descriptions-item>
            <el-descriptions-item label="个性签名">
              <el-tag type="danger">未开放</el-tag>
            </el-descriptions-item>
          </el-descriptions>

          <el-tabs class="demo-tabs">
            <el-tab-pane label="User" name="first">
              <el-card>
                123
              </el-card>
            </el-tab-pane>
            <el-tab-pane label="Config" name="second"><el-card>
              2
            </el-card></el-tab-pane>
            <el-tab-pane label="Role" name="third">
              <el-card>
                3
              </el-card>
            </el-tab-pane>
            <el-tab-pane label="Task" name="fourth">
              <el-card>
              4
              </el-card>
            </el-tab-pane>
          </el-tabs>

        </header>

      </el-card>
    </div>

  </transition>

</template>
<script setup lang="ts">
import {type LoginUser, useUserStore} from "@/stores/useUserStore";
import {computed} from "vue";

const userStore = useUserStore();
userStore.loadUser();

const user = computed((): LoginUser => {

  return <LoginUser> userStore.user;
})

const goBack = () => {

}

</script>


<style scoped>

</style>

<style>

</style>
