<template>
  <div
      class="float-ball"
      :style="{ top: `${position.y}px`, left: `${position.x}px` }"
      @mousedown="startDrag"
      @mouseup="endDrag"
      ref="ball"

  >
    <el-button class="float-button" @click="toggleDrawer" color="#fff" style="padding: 0 5px 0 0; outline: none; border: 1px solid #0095FF; border-radius: 8px;">

      <el-icon style="background-color: #0095FF; border-radius: 8px" size="32px" color="white">
        <IconCoin/>
      </el-icon>


      <span style="color: #0095FF">bit币：{{point}}</span>

    </el-button>
    <el-drawer
        title="设置"
        v-model="drawerVisible"
        direction="rtl"
        size="30%"
    >
      <DrawerContent/>
      <!-- 抽屉内容 -->
    </el-drawer>
  </div>
</template>


<script>
//@ts-nocheck
import {Coin} from "@element-plus/icons-vue";

import DrawerContent from "@/layout/component/FloatingBall/DrawerContent.vue";
import {onSse, SseEvent} from "@/utils/sse";
import {getMyPoint} from "@/api/user/index.ts";
import IconCoin from "@/assets/icons/IconCoin.vue";

export default {
  components: {IconCoin, DrawerContent, Coin},
  data() {
    return {
      position: { x: window.innerWidth - 150, y: window.innerHeight / 2 },
      drawerVisible: false,
      isDragging: false,
      startPosY: 0,
      startPosX: 0,
      isDragged: false,
      point: 0
    };
  },
  computed: {
    width() {
      //@ts-ignore
      return this.$refs.ball.clientWidth;
    },
    height() {
      //@ts-ignore
      return this.$refs.ball.clientHeight;

    }

  },
  methods: {
    startDrag(event) {
      this.isDragging = true;
      this.startPosY = event.clientY - this.position.y;
      this.startPosX = event.clientX - this.position.x;
      document.addEventListener('mousemove', this.dragging);
      document.addEventListener('mouseup', this.endDrag)
    },
    dragging(event) {
      if (this.isDragging) {
        const newY = event.clientY - this.startPosY;
        const newX = event.clientX - this.startPosX;
        this.position.y = Math.max(0, Math.min(window.innerHeight - 50, newY));
        this.position.x = Math.max(0, Math.min(window.innerWidth - 150, newX));
        this.isDragged = true
      }

    },
    endDrag() {
      this.isDragging = false;
      document.removeEventListener('mousemove', this.dragging);
      document.removeEventListener('mouseup', this.endDrag)
    },
    toggleDrawer() {
      if (!this.isDragged) {
        this.drawerVisible = !this.drawerVisible;
      }
      this.isDragged = false;

    },
    changePoint(point) {
      this.point = point.point;
    }

  },
  mounted() {
    // initSSE("/user-api/sse")
    onSse(SseEvent.UPDATE_POINT, this.changePoint)


    getMyPoint()
        .then((point) => {
          this.point = point;
        })
  },





};
</script>

<style scoped>
.float-ball {
  position: fixed;
  right: 20px;
  z-index: 1000;
}
</style>