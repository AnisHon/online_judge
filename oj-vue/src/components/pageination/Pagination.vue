<template>
  <div :class="{'hidden':hidden}" class="pagination-container">
    <el-pagination
        style="margin-left: auto"
        :background="background"
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :layout="layout"
        :page-sizes="pageSizes"
        :pager-count="pagerCount"
        :total="total"
        v-bind="$attrs"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
    />
  </div>
</template>

<script lang="ts">
import { scrollTo } from '@/utils/scroll-to'


export default {
  name: 'Pagination',
  props: {
    total: {
      required: true,
      type: Number
    },
    page: {
      type: Number,
      default: 1
    },
    limit: {
      type: Number,
      default: 20
    },
    pageSizes: {
      type: Array,
      default() {
        return [10, 20, 30, 50]
      }
    },
    // 移动端页码按钮的数量端默认值5
    pagerCount: {
      type: Number,
      default: document.body.clientWidth < 992 ? 5 : 7
    },
    layout: {
      type: String,
      default: 'total, sizes, prev, pager, next, jumper'
    },
    background: {
      type: Boolean,
      default: true
    },
    autoScroll: {
      type: Boolean,
      default: true
    },
    hidden: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
    };
  },
  computed: {
    currentPage: {
      get() {
        return this.page
      },
      set(val: any) {
        this.$emit('update:page', val)
      }
    },
    pageSize: {
      get() {
        return this.limit
      },
      set(val: any) {
        this.$emit('update:limit', val)
      }
    }
  },
  inject: ['elMain'],
  methods: {
    handleSizeChange(val: any) {
      if (this.currentPage * val > this.total) {
        this.currentPage = 1;
      }
      this.$emit('pagination', { page: this.currentPage, limit: val });
      if (this.autoScroll) {
        // @ts-ignore
        scrollTo(0, 800, undefined, this.elMain?.elMainRef.value?.$el);
      }
    },
    handleCurrentChange(val: any) {
      this.$emit('pagination', { page: val, limit: this.pageSize });
      if (this.autoScroll) {
        // @ts-ignore
        scrollTo(0, 800, undefined, this.elMain?.elMainRef.value?.$el);
      }
    }
  },
}
</script>

<style scoped>
.pagination-container {
  display: flex;
  padding: 32px 16px;
}
.pagination-container.hidden {
  display: none;
}
</style>
