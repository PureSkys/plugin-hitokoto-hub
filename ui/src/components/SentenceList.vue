<template>
  <div>
    <VCard>
      <template #header>
        <div class=" flex w-full bg-gray-50 px-4 py-3">
          <div class="flex w-full flex-1 items-center gap-6 sm:w-auto">
            <SearchInput v-model="keyword"/>
            <VSpace spacing="lg" class=":uno: flex-wrap">
              <FilterCleanButton
                      v-if="hasFilters"
                      @click="handleClearFilters"
              />
              <FilterDropdown
                      v-model="selectedCategory"
                      label="分类"
                      :items="categoryNameList"
              />
              <FilterDropdown
                      v-model="selectedSort"
                      label="排序"
                      :items="[
                  {
                    label: '默认',
                  },
                  {
                    label: '较近创建',
                    value: 'metadata.creationTimestamp,desc',
                  },
                  {
                    label: '较早创建',
                    value: 'metadata.creationTimestamp,asc',
                  },
                ]"
              />
              <div class="flex flex-row gap-2">
                <div class="group cursor-pointer rounded p-1 hover:bg-gray-200">
                  <IconRefreshLine
                          v-tooltip="'刷新'"
                          class="h-4 w-4 text-gray-600 group-hover:text-gray-900"
                  />
                </div>
              </div>
            </VSpace>
          </div>
          <div class="flex flex-row gap-2 items-center">
            <VButton size="sm">新建句子</VButton>
          </div>
        </div>

      </template>
      <template #footer>
        <VPagination
                v-model:page="page"
                v-model:size="size"
                page-label="页"
                size-label="条 / 页"
                :total-label="`共 ${total} 项数据`"
                :total="total"
                :size-options="[20, 30, 50, 100]"
        />
      </template>
    </VCard>
  </div>
</template>

<script setup lang="ts">

import {VButton, IconRefreshLine, VCard, VPagination, VSpace} from "@halo-dev/components";
import {computed, onMounted, ref} from "vue";
import {categoryCoreApiClient, sentenceCoreApiClient} from "@/api"


const page = ref(1);
const size = ref(20);
const total = ref(0);
const keyword = ref("");
const categoryNameList = ref<{ label: string; value: string | undefined }[]>([{
  label: '全部',
  value: undefined,
}]);
const selectedCategory = ref(undefined)
const selectedSort = ref(undefined);

onMounted(() => {
  initData();
})
const initData = async () => {
  // 初始化分类列表
  await categoryCoreApiClient.category.listCategory({page: 1, size: 100})
          .then((res) => {
            res.data.items.forEach((item) => {
              console.log(item.spec.name);
              categoryNameList.value.push({
                label: item.spec.name,
                value: item.metadata.name,
              });
            });
          })
};
const handleClearFilters = () => {
  selectedCategory.value = undefined;
  selectedSort.value = undefined;
};
const hasFilters = computed(() => {
  return (
          selectedCategory.value ||
          selectedSort.value
  );
});


</script>

<style scoped>

</style>
