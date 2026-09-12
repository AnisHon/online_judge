<template>
  <div
      class="float-ball"
      :style="{ top: `${position.y}px`, left: `${position.x}px` }"
      @mousedown="startDrag"
      @mouseup="endDrag"
      ref="ball"

  >
    <el-button class="float-button" @click="toggleDrawer">
      <el-icon class="point-mark" size="30px">
        <IconCoin/>
      </el-icon>
      <span class="point-label"><small>我的积分</small><strong>{{point}}</strong></span>
      <el-icon class="point-arrow"><ArrowRight /></el-icon>
    </el-button>
    <el-drawer
        class="point-drawer"
        v-model="drawerVisible"
        direction="rtl"
        size="min(390px, 92vw)"
    >
      <template #header><div class="drawer-title"><span class="drawer-kicker">PREFERENCES</span><strong>显示偏好</strong><small>调整阅读体验，设置会自动保存</small></div></template>
      <DrawerContent/>
      <!-- 抽屉内容 -->
    </el-drawer>
  </div>
</template>


<script>
//@ts-nocheck
import {ArrowRight, Coin} from "@element-plus/icons-vue";

import DrawerContent from "@/layout/component/FloatingBall/DrawerContent.vue";
import {onSse, SseEvent} from "@/utils/sse";
import {getMyPoint} from "@/api/user/index.ts";
import IconCoin from "@/assets/icons/IconCoin.vue";

export default {
  components: {IconCoin, DrawerContent, Coin, ArrowRight},
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
.float-button { display: flex; align-items: center; gap: 6px; min-width: 108px; height: 46px; padding: 4px 7px 4px 5px; border: 1px solid var(--el-border-color-light); border-radius: 14px; color: var(--el-text-color-primary); background: var(--el-bg-color); box-shadow: 0 8px 20px color-mix(in srgb, var(--el-color-primary) 16%, transparent); transition: transform .2s, border-color .2s, box-shadow .2s; }
.float-button:hover { border-color: var(--el-color-primary-light-5); color: var(--el-text-color-primary); background: var(--el-bg-color); transform: translateY(-2px); box-shadow: 0 14px 30px color-mix(in srgb, var(--el-color-primary) 23%, transparent); }
.point-mark { display: grid; flex: 0 0 34px; width: 34px; height: 34px; place-items: center; border-radius: 10px; color: #fff; background: linear-gradient(145deg, var(--el-color-primary), var(--el-color-success)); }
.point-mark :deep(svg) { width: 26px; height: 26px; }
.point-label { display: flex; flex-direction: column; align-items: flex-start; line-height: 1.1; }
.point-label small { color: var(--el-text-color-secondary); font-size: 10px; }
.point-label strong { margin-top: 2px; color: var(--el-text-color-primary); font-size: 15px; }
.point-arrow { margin-left: auto; color: var(--el-text-color-placeholder); }
.drawer-title { display: flex; flex-direction: column; gap: 4px; }
.drawer-title strong { color: var(--el-text-color-primary); font-size: 20px; letter-spacing: -.03em; }
.drawer-title small { color: var(--el-text-color-secondary); font-size: 12px; font-weight: 400; }
.drawer-kicker { color: var(--el-color-primary); font-size: 10px; font-weight: 800; letter-spacing: .16em; }
:deep(.point-drawer .el-drawer__header) { margin-bottom: 0; padding: 24px 24px 20px; border-bottom: 1px solid var(--el-border-color-lighter); }
:deep(.point-drawer .el-drawer__body) { padding: 22px 24px; background: var(--el-bg-color); }
@media (max-width: 560px) { .float-ball { right: 12px; }.float-button { min-width: 102px; }.point-label strong { font-size: 14px; } }
</style>
