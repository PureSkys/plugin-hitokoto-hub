<template>
  <div class="category-list p-4">
    <!-- Header -->
    <div class="flex items-center justify-between mb-4">
      <div class="flex items-center gap-3">
        <div class="text-sm text-gray-500">
          共 {{ total }} 个分类
        </div>
        <div
                class="group cursor-pointer rounded p-1 hover:bg-gray-200"
                v-tooltip="'刷新'"
                @click="refreshData"
        >
          <IconRefreshLine class="h-4 w-4 text-gray-600 group-hover:text-gray-900" />
        </div>
      </div>
      <VButton size="sm" type="secondary" @click="handleCreate">
        <template #icon>
          <IconAddCircle class="h-full w-full" />
        </template>
        新建分类
      </VButton>
    </div>

    <!-- Loading -->
    <VLoading v-if="loading" />

    <!-- Empty -->
    <VEmpty
            v-else-if="categories.length === 0"
            title="暂无分类"
            message="点击「新建分类」创建你的第一个分类"
    />

    <!-- Category List -->
    <template v-else>
      <VEntityContainer>
        <VEntity
                v-for="category in categories"
                :key="category.metadata.name"
        >
          <template #start>
            <VEntityField
                    :title="category.spec.name"
            >
              <template #description>
                <div class="flex flex-wrap items-center gap-1.5">
                  <span class="inline-flex items-center rounded-full bg-gray-100 px-2.5 py-0.5 text-xs text-gray-600 whitespace-nowrap">
                    描述：{{ category.spec.description || '暂无描述' }}
                  </span>
                </div>
              </template>
            </VEntityField>
          </template>
          <template #end>
            <VEntityField>
              <template #description>
                <div class="flex items-center gap-1.5">
                  <span
                          v-if="isDeleting(category)"
                          class="inline-block h-2 w-2 rounded-full bg-red-500 animate-pulse"
                          v-tooltip="'删除中'"
                  />
                  <VTag>
                    {{ category.status?.sentenceCount ?? 0 }} 条句子
                  </VTag>
                </div>
              </template>
            </VEntityField>
            <VEntityField>
              <template #description>
                <VDropdown>
                  <VButton size="sm" type="default">操作</VButton>
                  <template #popper>
                    <VDropdownItem @click="handleEdit(category)">编辑</VDropdownItem>
                    <VDropdownItem type="danger" @click="handleDelete(category)">删除</VDropdownItem>
                  </template>
                </VDropdown>
              </template>
            </VEntityField>
          </template>
        </VEntity>
      </VEntityContainer>

      <!-- Pagination -->
      <div class="mt-4 flex justify-center">
        <VPagination
                v-model:page="page"
                v-model:size="size"
                page-label="页"
                size-label="条 / 页"
                :total-label="`共 ${total} 项数据`"
                :total="total"
                :size-options="[20, 30, 50, 100]"
        />
      </div>
    </template>

    <!-- Create/Edit Modal -->
    <VModal
            v-model:visible="showFormModal"
            :title="isEditing ? '编辑分类' : '新建分类'"
            :width="520"
    >
      <div class="halo-form">
        <div class="halo-form-item">
          <label class="halo-form-label">
            分类名称 <span class="text-red-500">*</span>
          </label>
          <input
                  v-model="formData.specName"
                  type="text"
                  class="halo-input"
                  placeholder="请输入分类名称"
                  aria-required="true"
          />
        </div>
        <div class="halo-form-item">
          <label class="halo-form-label">分类描述</label>
          <textarea
                  v-model="formData.description"
                  rows="3"
                  class="halo-input halo-textarea"
                  placeholder="请输入分类描述（可选）"
          />
        </div>
      </div>
      <template #footer>
        <VSpace>
          <VButton size="sm" @click="showFormModal = false">取消</VButton>
          <VButton size="sm" type="secondary" :loading="saving" @click="handleSave">
            保存
          </VButton>
        </VSpace>
      </template>
    </VModal>
  </div>
</template>

<script setup lang="ts">
import {onMounted, ref, watch} from 'vue'
import {categoryCoreApiClient} from '@/api'
import type {Category} from '@/api/generated'
import {
  Dialog,
  IconAddCircle,
  IconRefreshLine,
  Toast,
  VButton,
  VDropdown,
  VDropdownItem,
  VEmpty,
  VEntity,
  VEntityContainer,
  VEntityField,
  VLoading,
  VModal,
  VPagination,
  VSpace,
  VTag,
} from '@halo-dev/components'

const loading = ref(false)
const categories = ref<Category[]>([])
const saving = ref(false)

const page = ref(1)
const size = ref(20)
const total = ref(0)

const showFormModal = ref(false)
const isEditing = ref(false)
const formCategory = ref<Category | null>(null)

const formData = ref({
  specName: '',
  description: '',
})

const isDeleting = (category: Category): boolean => {
  const finalizers = category.metadata?.finalizers
  return Array.isArray(finalizers) && finalizers.length > 0
}

async function fetchCategories() {
  loading.value = true
  try {
    const { data } = await categoryCoreApiClient.category.listCategory({
      page: page.value,
      size: size.value,
    })
    categories.value = data.items || []
    total.value = data.total || 0
  } catch (e) {
    console.error('Failed to fetch categories', e)
    Toast.error('加载分类列表失败')
  } finally {
    loading.value = false
  }
}

function refreshData() {
  fetchCategories()
}

watch(page, () => {
  fetchCategories()
})

watch(size, () => {
  page.value = 1
  fetchCategories()
})

function handleCreate() {
  isEditing.value = false
  formCategory.value = null
  formData.value = { specName: '', description: '' }
  showFormModal.value = true
}

function handleEdit(category: Category) {
  isEditing.value = true
  formCategory.value = category
  formData.value = {
    specName: category.spec.name || '',
    description: category.spec.description || '',
  }
  showFormModal.value = true
}

async function handleSave() {
  if (!formData.value.specName.trim()) {
    Toast.warning('请输入分类名称')
    return
  }
  saving.value = true
  try {
    if (isEditing.value && formCategory.value) {
      const updated: Category = {
        ...formCategory.value,
        spec: {
          ...formCategory.value.spec,
          name: formData.value.specName.trim(),
          description: formData.value.description.trim() || undefined,
        },
      }
      await categoryCoreApiClient.category.updateCategory({
        name: formCategory.value.metadata.name,
        category: updated,
      })
      Toast.success('更新分类成功')
    } else {
      const newCategory: Category = {
        apiVersion: 'hitokotohub.puresky.top/v1alpha1',
        kind: 'Category',
        metadata: {
          name: '',
          generateName: 'category-',
        },
        spec: {
          name: formData.value.specName.trim(),
          description: formData.value.description.trim() || undefined,
        },
      }
      await categoryCoreApiClient.category.createCategory({
        category: newCategory,
      })
      Toast.success('创建分类成功')
    }
    showFormModal.value = false
    await fetchCategories()
  } catch (e) {
    console.error('Failed to save category', e)
    Toast.error(isEditing.value ? '更新分类失败' : '创建分类失败')
  } finally {
    saving.value = false
  }
}

function handleDelete(category: Category) {
  Dialog.warning({
    title: '删除确认',
    description: `确定要删除分类「${category.spec.name}」吗？该操作不可撤销。`,
    confirmType: 'danger',
    confirmText: '删除',
    cancelText: '取消',
    onConfirm: async () => {
      try {
        await categoryCoreApiClient.category.deleteCategory({
          name: category.metadata.name,
        })
        Toast.success('删除成功')
        await fetchCategories()
      } catch (e) {
        console.error('Failed to delete category', e)
        Toast.error('删除分类失败')
      }
    },
  })
}

onMounted(() => {
  fetchCategories()
})
</script>

<style scoped>
.category-list {
  min-height: 20px;
}

.halo-form {
  padding: 4px 0;
}

.halo-form-item {
  margin-bottom: 16px;
}

.halo-form-item:last-child {
  margin-bottom: 0;
}

.halo-form-label {
  display: block;
  margin-bottom: 6px;
  font-size: 13px;
  font-weight: 500;
  color: #1e293b;
  line-height: 1.5;
}

.halo-input {
  display: block;
  width: 100%;
  padding: 7px 12px;
  font-size: 13px;
  line-height: 1.5;
  color: #1e293b;
  background-color: #ffffff;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  outline: none;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
  box-sizing: border-box;
  font-family: inherit;
}

.halo-input::placeholder {
  color: #9ca3af;
}

.halo-input:hover {
  border-color: #9ca3af;
}

.halo-input:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.halo-textarea {
  resize: vertical;
  min-height: 72px;
}
</style>
