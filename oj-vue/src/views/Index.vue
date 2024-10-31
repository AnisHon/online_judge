<template>
  <div class="block text-center">
    <el-carousel height="150px">
      <el-carousel-item v-for="item in 4" :key="item">
        <h3 class="small justify-center">{{ item }}</h3>
      </el-carousel-item>
    </el-carousel>
  </div>
</template>

<script setup lang="ts">
  const data = [
    {
      url: "https://images.samsung.com.cn/is/image/samsung/assets/cn/homepage/2-shuang11-preorde-main-kv-pc.jpg?imwidth=1366",
      title: "123",
      description: "123"
    },
    {
      url: "https://images.samsung.com.cn/is/image/samsung/assets/cn/homepage/202241101-w-main-kv-pc.jpg?imwidth=1366",
      title: "123",
      description: "123"
    },
    {
      url: "https://images.samsung.com.cn/is/image/samsung/assets/cn/homepage/20241008-tabs10-main-kv-pc.jpg?imwidth=1366",
      title: "123",
      description: "123"
    }
  ]
</script>

<style scoped>

</style>