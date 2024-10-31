<template>
  <div class="index">

    <el-carousel class="carousel" autoplay height="607px">
      <el-carousel-item v-for="item of data">
        <div class="carousel-content">
          <div class="text-wrapper">
            <div class="text">
              <h1>{{ item.title }}</h1>
              <p>{{ item.description }}</p>
              <el-link class="detail-button" type="primary" target="_blank" :href="item.href">
                显示详情
              </el-link>
            </div>
          </div>
          <el-image fit="contain" :src="item.url" />
        </div>

      </el-carousel-item>
    </el-carousel>

    <div class="col-wrapper">
      <el-row :gutter="20" style="width: 100%">
        <el-col :span="8" v-for="item of 9" style="margin: 10px 0">
          <el-card shadow="hover" style="height: 100%">
            <el-image src="https://tse1-mm.cn.bing.net/th/id/OIP-C.n8pa_ux7uUyU9CJrzb1scAHaHa?rs=1&pid=ImgDetMain" />

            <template #footer>
              <el-row>
                <el-col :span="16">
                  Sun Microsystems 公司 特产Java咖啡
                </el-col>
                <el-col :span="8" style="display: flex;">
                  <el-button style="margin-left: auto" round>查看详情</el-button>
                </el-col>
              </el-row>

            </template>
          </el-card>
        </el-col>
      </el-row>
    </div>

  </div>
</template>

<script setup lang="ts">
  const data = [
    {
      url: "https://images.samsung.com.cn/is/image/samsung/assets/cn/homepage/2-shuang11-preorde-main-kv-pc.jpg?imwidth=1366",
      title: "2024全线新品",
      description: "享受30天质保",
      href: ""
    },
    {
      url: "https://images.samsung.com.cn/is/image/samsung/assets/cn/homepage/202241101-w-main-kv-pc.jpg?imwidth=1366",
      title: "Bespoke AI",
      description: "AI解决方案",
      href: ""
    },
    {
      url: "https://images.samsung.com.cn/is/image/samsung/assets/cn/homepage/20241008-tabs10-main-kv-pc.jpg?imwidth=1366",
      title: "超级星动",
      description: "5折扣",
      href: ""
    }
  ]
</script>

<style scoped lang="scss">

.index {
  max-width: 1366px;
  margin: 0 auto;
}
.carousel-content {
  position: relative;
  .text-wrapper {
    display: flex;
    justify-content: center;
    align-items: center;
    position: absolute;
    width: 50%;
    height: 100%;
    z-index: 1;

    .text {

      transform: translate(-25%, -25%);

      h1 {
        font-size: 36px;
      }

      p {
        margin: 0;
        padding: 0;
      }

    }

  }


}

.col-wrapper > * {
  margin: 20px 0;
}


</style>