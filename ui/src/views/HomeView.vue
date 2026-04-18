<script setup lang="ts">
import {markRaw, shallowRef} from "vue";
import SentenceList from "@/components/SentenceList.vue";
import CategoryList from "@/components/CategoryList.vue";
import Overview from "@/components/Overview.vue";
import {useRouteQuery} from "@vueuse/router";
import {VCard, VPageHeader, VTabbar,} from "@halo-dev/components";


const tabs = shallowRef([
  {
    id: "Overview",
    label: "概览",
    component: markRaw(Overview),
  },
  {
    id: "SentenceList",
    label: "句子列表",
    component: markRaw(SentenceList),
  },
  {
    id: "CategoryList",
    label: "分类列表",
    component: markRaw(CategoryList),
  }

]);

const activeIndex = useRouteQuery<string>("tab", tabs.value[0].id);

</script>

<template>

  <VPageHeader title="一言数据管理">
    <template #icon>
    </template>
  </VPageHeader>

  <div class="m-0 md:m-4">
    <VCard :body-class="['!p-0']">
      <template #header>
        <VTabbar
                v-model:active-id="activeIndex"
                :items="tabs.map((item) => ({ id: item.id, label: item.label }))"
                class="w-full rounded-none!"
                type="outline"
        ></VTabbar>
      </template>
      <div class="bg-white">
        <Overview ref="overview" v-if="activeIndex=='Overview'"/>
        <SentenceList ref="sentenceList" v-if="activeIndex=='SentenceList'"/>
        <CategoryList ref="categoryList" v-if="activeIndex=='CategoryList'"/>
      </div>
    </VCard>
  </div>


</template>
