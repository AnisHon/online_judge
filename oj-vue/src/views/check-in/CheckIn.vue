<template>
  <div class="check-in-container">
    <el-container>
      <el-header height="auto">
        <el-container>
          <el-aside class="absoluteCenter">
            <h3>{{ checkInText }}</h3>
            <el-button
                type="success"
                class="check-in-button"
                size="large" icon="Edit"
                v-if="!isCheckIn"
                @click="handleClick"
                :loading="isLoading"
            >
              请签到
            </el-button>
            <el-button
                type="success"
                class="check-in-button"
                size="large"
                icon="Select"
                disabled="disabled"
                v-else
            >
              已签到
            </el-button>
          </el-aside>
          <el-main>
            <h1>
              您好, {{ nikeName }}
            </h1>
          </el-main>
          <el-main>
            <h1>
              今日已签{{ todayCheckInCount }}人
            </h1>
          </el-main>
        </el-container>
      </el-header>
      <el-main>
        <el-table :data="checkInRecords" style="width: 100%">
          <el-table-column prop="id" label="id"/>
          <el-table-column prop="userId" label="用户ID"/>
          <el-table-column prop="continuityDays" label="连续天数"/>
          <el-table-column prop="rewardPoint" label="奖励积分"/>
          <el-table-column prop="currentTime" label="签到时间"/>
        </el-table>

      </el-main>
    </el-container>



    <el-dialog
        v-model="centerDialogVisible"
        width="500"
        align-center
        class="absoluteCenter"
    >
      <div class="absoluteCenter">
        <el-icon size="50" color="#67C23A"><Select/></el-icon>
        <h3>{{ dialogTitle }}</h3>
        <p>{{ dialogBodyText }}</p>
      </div>

    </el-dialog>
  </div>
</template>

<script setup lang="ts">

import {computed, onMounted, ref} from "vue";
import {useUserStore} from "@/stores/useUserStore";
import {checkInFetcher, checkInList, isCheckedIn, todayCount, type UserCheckIn} from "@/api/check-in";
import {Select} from "@element-plus/icons-vue";

const checkInRecords = ref<UserCheckIn[]>([]);

const centerDialogVisible = ref(false);

const todayCheckInCount = ref(0);

const dialogTitle = ref<string>("")
const dialogBodyText = ref<string>("")

const nikeName = computed(() => {
  const userStore = useUserStore();
  return userStore.user?.nikeName;
});

const isCheckIn = ref(false)
const checkInText = computed(() => {
  return isCheckIn.value ? "您已经签到" : "您还未签到";
});
const {isLoading, loading, sendCheckIn} = checkInFetcher((data) => {
  // @ts-ignore
  data.userCheckIn.currentTime = new Date(Date.now()).toDateString();
  checkInRecords.value.unshift(data.userCheckIn);

  centerDialogVisible.value = true;
  dialogTitle.value = data.success ? "签到成功！" : "签到失败";
  dialogBodyText.value = data.success ?`${data.msg},得到${data.award}积分` : data.msg;
  isCheckIn.value = true;

  refresh();
});
const handleClick = () => {
  loading();
  sendCheckIn();
}

const refresh = () => {
  isCheckedIn().then((data) => {
    isCheckIn.value = data;
  });

  todayCount().then((data) => {todayCheckInCount.value = data;});
  checkInList().then(data => {
    checkInRecords.value = data;
  });
}


refresh();


</script>


<style scoped>

.check-in-container {
  margin: auto;
  width: 100%;
  height: 100%;
  max-width: var(--page-max-width);

}
</style>

<style>
.check-in-button {
  width: 200px;
  height: 50px;
  border-radius: 50px;
  font-size: 20px;

}
</style>