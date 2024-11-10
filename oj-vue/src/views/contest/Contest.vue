<template>
  <div class="contest-container">

    <div class="contest-list" v-loading="isLoading || isJoinedLoading">
      <el-card v-for="item of list" shadow="hover" class="card">
        <el-row>
          <el-col :span="3">
            <el-image src="https://ts3.cn.mm.bing.net/th?id=OIP-C.ujS7-Ed_Im1_RLuDiZPGhAAAAA&w=200&h=150&c=8&rs=1&bgcl=687b85&r=0&o=6&pid=5.1" />
          </el-col>
          <el-col :span="15">
            <div>
              <el-badge :value="item.joinedNumber" :offset="[10, 0]">
                <h2 style="margin: 0">{{ item.title }}</h2>
              </el-badge>

              <div>
                <div class="time">
                  <span>
                    <el-icon style="vertical-align: middle" color="#409EFF">
                      <Calendar />
                    </el-icon>
                    <span style="font-size: 12px"><strong>日期</strong> {{ formatDate(item.startTime) }} ～ {{ formatDate(item.endTime) }}</span>
                  </span>
                </div>
                <div class="time">
                  <span>
                    <el-icon style="vertical-align: middle" color="#409EFF">
                      <Clock />
                    </el-icon>
                    <span style="font-size: 12px"><strong>时间</strong> {{ differ(item.startTime, item.endTime) }} 小时</span>
                  </span>
                </div>
              </div>
            </div>

          </el-col>
          <el-col :span="6" style="display: flex; justify-content: center; align-items: center;">
            <el-tag size="large" :type="authTagType(item.auth)">{{ authText(item.auth) }}</el-tag>
            <el-button style="margin-left: auto" type="danger" v-if="isContestOver(item.endTime)" @click="joinContest(item)">
              已结束
            </el-button>
            <el-button style="margin-left: auto" type="primary" v-else-if="isNotStart(item.startTime)">
              未开始
            </el-button>
            <el-button style="margin-left: auto" type="success" v-else @click="joinContest(item)">
              参加
            </el-button>
          </el-col>
        </el-row>
      </el-card>
    </div>

    <div class="pagination">
      <pagination
          v-show="total > 0"
          :total="total"
          :background="false"
          v-model:page="page.currentPage"
          v-model:limit="page.pageSize"
          @pagination="getList"
      />
    </div>


    <el-dialog v-model="open" title="请输入密码" width="800">
      <el-form :model="form">
        <el-form-item label="密码">
          <el-input type="password" placeholder="请输入密码" v-model="form.password" autocomplete="off" maxlength="32" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submit" >确 定</el-button>
        <el-button @click="open = false">取 消</el-button>
      </template>
    </el-dialog>
  </div>

</template>

<script setup lang="ts">

import {
  ContestAuth,
  type ContestView,
  debouncedGetContest,
  debouncedIsJoined, debouncedJoin,
  type JoinContestRequest
} from "@/api/contest";
import {reactive, ref} from "vue";
import type {PagedType} from "@/api/pagedType";
import Pagination from "@/components/pageination/Pagination.vue";
import {Calendar, Clock} from "@element-plus/icons-vue";
import {authTagType, authText, differ, isContestOver, isNotStart} from "@/utils/contest";
import {useRouter} from "vue-router";
import {formatDate} from "compatx";


const router = useRouter();

const list = reactive<ContestView[]>([]);

const open = ref(false);

const total = ref(0);
const page = reactive<PagedType>({
  pageSize: 10,
  currentPage: 1,
});

const form = reactive<JoinContestRequest>({
  contestId: -1,
  password: undefined
})

const enter = (contestId: number) => {

  router.push({name: "contest-problems", params: {id: contestId}});
}

const {isLoading, loading, get} = debouncedGetContest(page, (data) => {
  total.value = data.totalRecords;
  list.length = 0;
  list.push(...data.data);
});


const currentContest = ref<ContestView>();


const {post: join} = debouncedJoin(form, (data) => {
  if (data.success) {
    ElMessage.success("加入成功");
    enter(currentContest.value!.contestId);
  } else {
    ElMessage.error(data.message);
  }
  open.value = false;
  enter(currentContest.value!.contestId);
});

const submit = () => {
  join();
}

const {isLoading: isJoinedLoading, loading: joinedLoading, get: joinedGet} = debouncedIsJoined((success) => {
  // 比赛结束且未参加，不允许进入
  if (isContestOver(currentContest.value!.endTime) && !success) {
    ElMessage.warning("您未参加该场比赛");
    return;
  }

  // 无论是否结束已经参加，直接进入
  if (success) {
    ElMessage.success("已加入，进入比赛");
    enter(currentContest.value!.contestId);
    return;
  }

  // 比赛进行中，没有参加，判断是否需要输入密码
  if (currentContest.value?.auth === ContestAuth.PUBLIC) {
    // 公共比赛加入比赛
    join();
  } else if (currentContest.value?.auth === ContestAuth.PRIVATE) {
    // 私有比赛输入密码
    open.value = true;
  } else {
    ElMessage.warning("白名单赛制，无法参加")
  }

})

const joinContest = (contest: ContestView) => {
  form.contestId = contest.contestId;
  currentContest.value = contest;
  joinedLoading();
  joinedGet(contest.contestId)
}

const getList = () => {
  loading();
  get();
}

getList();


</script>


<style scoped>

.contest-container {
  max-width: var(--page-max-width);
  margin: 0 auto;

  .contest-list {
    padding: 10px;

  }
  .card {
    margin: 10px 0;
  }

  .time {
    margin: 5px;
  }

}

</style>