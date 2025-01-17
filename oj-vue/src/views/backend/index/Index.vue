<template>
  <div class="app-container">
    <div class="top">
      <el-row :gutter="40">
        <el-col :span="6">
          <el-card shadow="hover" style="height: 100%">
            <div class="statistic-card">
              <el-statistic title="在线用户" :value="online"/>
            </div>
          </el-card>
        </el-col>

        <el-col :span="6">
          <el-card shadow="hover" style="height: 100%">
            <div class="statistic-card">
              <el-statistic title="题目个数" :value="problemNumber"/>
            </div>
          </el-card>
        </el-col>

        <el-col :span="6">

          <el-card shadow="hover" style="height: 100%">
            <div class="statistic-card">
              <el-statistic title="剩余空间" :value="freeMemory">
                <template #suffix>
                  <span>
                    GB
                    <span style="font-size: 14px">
                    / {{ totalMemory }} GB
                    </span>
                  </span>
                </template>
              </el-statistic>
              <div class="statistic-footer">
                <div class="footer-item">
                  <span>已经使用 </span>
                  <span class="green">
                  {{ usedMemoryPercentage }}%
                </span>
                </div>
              </div>
            </div>
          </el-card>

        </el-col>

        <el-col :span="6">
          <el-card shadow="hover" style="height: 100%">
            <div class="statistic-card">
              <el-statistic title="剩余空间" :value="problemNumber">

              </el-statistic>
              <div class="statistic-footer">
                <div class="footer-item">
                  <span>than yesterday</span>
                  <span class="green">
                  24%
                  <el-icon>
                    <CaretTop />
                  </el-icon>
                </span>
                </div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <div class="content">
      <el-row :gutter="40">
        <el-col :span="10">
          <custom-card title="使用教程">
            <MarkdownPreview :text="help"/>
          </custom-card>
        </el-col>
        <el-col :span="14">
          <custom-card title="快捷链接">
            <div class="buttons">
              <el-space wrap>
                <el-tooltip
                    v-for="item of copyButtons"
                    :content="item.content"
                    placement="top"
                >
                  <el-button style="color: white" @click="handleCopy(item.port)" :color="item.color" size="large" icon="DocumentCopy">
                    {{ item.buttonText }}
                  </el-button>
                </el-tooltip>
              </el-space>
            </div>

            <el-divider>
              客户端下载
            </el-divider>
            <div class="client-download">
              <el-row >
                <el-col class="download-btn-wrapper" :span="8" v-for="item of downloadButtons">
                  <el-button class="download-btn" @click="jumpTo('https://cdn.openbsd.org/pub/OpenBSD/OpenSSH/portable/')">
                    <template #icon>
                      <el-icon size="32px">
                        <Component :is="item.icon"/>
                      </el-icon>

                    </template>
                  </el-button>
                  <p>{{ item.text }}</p>
                </el-col>
              </el-row>

            </div>
          </custom-card>

          <custom-card style="margin-top: 40px" title="公告">
            <el-card v-for="i of 20">
              132
            </el-card>
          </custom-card>

        </el-col>

      </el-row>

    </div>

  </div>
</template>

<script setup lang="ts">
import CustomCard from "@/components/CustomCard/CustomCard.vue";
import {computed, markRaw, ref} from "vue";
import IconWindows from "@/assets/icons/IconWindows.vue";
import IconMacOs from "@/assets/icons/IconMacOs.vue";
import IconLinux from "@/assets/icons/IconLinux.vue";
import {CaretTop} from "@element-plus/icons-vue";
import {countProblems} from "@/api/problem";
import {ElNotification} from "element-plus";
import {copyTextToClipboard} from "@/utils/clipboard.ts";
import MarkdownPreview from "@/components/MarkdownPreview.vue";
import {countOnline, freeDisk} from "@/api/file";

const help = `
#### 安装SSh

Linux系统使用

\`\`\`shell
# debian
sudo apt install openssh-server ssh
# redhat
sudo yum install openssh-server ssh
\`\`\`

windows macos自行搜索

#### 保证端口可用

对于Linux/MacOs都可以使用一下指令查看端口占用

\`\`\`shell
lsof -i:<需要查看的端口>
\`\`\`

对于windows使用netstat查看端口占用

\`\`\`shell
netstat -aon|findstr "需要查看的端口"
\`\`\`

通过以上指令可以获取到PID，使用kill命令关闭该程序

#### 复制指令

旁边的按钮可以复制对应的ssh指令

#### 链接服务器端口

在终端中输入该指令，输入后远程服务器的端口将映射到自己的主机上

> 注意：使用SSH链接服务器需要在服务器的authorized_keys里注册自己的公钥，或者使用服务器的私钥，服务器关闭了密码登录
`

// 复制ssh链接用的button
const copyButtons = ref([
  {
    content: "使用SSH链接所有端口",
    buttonText: "复制全部 SSH 链接",
    color: "#377DFF",
    port: [8848, 15672, 3306, 6379, 9090]
  },
  {
    content: "服务发现，配置中心(8848端口)",
    buttonText: "复制 Nacos 链接",
    color: "#17A2B8",
    port: [8848]
  },
  {
    content: "消息队列WEB管理页面(15672端口)",
    buttonText: " 复制 RabbitMQ 链接",
    color: "#6610F2",
    port: [15672]
  },
  {
    content: "Mysql数据库接口(3306端口)",
    buttonText: "复制 Mysql 链接",
    color: "#6F42C1",
    port: [3306]
  },
  {
    content: "Redis数据库接口(6379端口)",
    buttonText: "复制 Redis 链接",
    color: "#D63384",
    port: [6379]
  },
  {
    content: "分布式文件服务管理页面(9090端口)",
    buttonText: "复制 Minio 链接",
    color: "#FD7E14",
    port: [9090]
  },
])

// 下载ssh客户端的button
const downloadButtons = ref([
  {
    icon: markRaw(IconWindows),
    text: "Windows"
  },
  {
    icon: markRaw(IconMacOs),
    text: "MacOS"
  },
  {
    icon: markRaw(IconLinux),
    text: "Linux"
  }
])

// 在线数量
const online = ref(0);
// 题目数量
const problemNumber = ref(0);


const freeMemory = ref(100);

const totalMemory = ref(500);

const usedMemoryPercentage = computed(() => {
  return (1 - freeMemory.value / totalMemory.value) * 100
})


const jumpTo = (url: string) => {
  window.open(url, "_blank");
}

const handleCopy = (ports: number[]) => {
  const host = window.location.hostname;

  const content = ["ssh"];
  content.push(...ports.map(port => `-L ${port}:${host}:${port}`));
  content.push(`root@${host}`);

  const copy = content.join(" ");

  copyTextToClipboard(copy)
      .then(() => {
        ElNotification({
          type: "success",
          message: "复制成功",
          duration: 1000,
        })
      })
      .catch(() => {
        ElNotification({
          type: "error",
          message: "复制失败",
          duration: 1000,
        })
      })


}

const init = async () => {
  problemNumber.value = await countProblems();
  online.value = await countOnline();
  const {free, total} = await freeDisk();
  freeMemory.value = free;
  totalMemory.value = total;
}




init();


</script>

<style scoped>
.app-container>div {
  margin-bottom: 40px;
}

.buttons {
  margin-bottom: 40px;
}

.download-btn-wrapper {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}

.download-btn {
  height: 60px;
  width: 60px;
}

.green {
  color: var(--el-color-success);
}
.red {
  color: var(--el-color-error);
}
</style>