<script setup lang="ts">
import {onMounted, ref} from 'vue'
import {categoryCoreApiClient} from '@/api'
import type {Category} from '@/api/generated'
import {
  Dialog,
  IconAddCircle,
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
  VSpace,
  VTag,
} from '@halo-dev/components'

const loading = ref(false)
const categories = ref<Category[]>([])
const saving = ref(false)

// Modal state
const showFormModal = ref(false)
const isEditing = ref(false)
const formCategory = ref<Category | null>(null)

// Form data
const formData = ref({
  specName: '',
  description: '',
})

// Fetch categories
async function fetchCategories() {
  loading.value = true
  try {
    const { data } = await categoryCoreApiClient.category.listCategory({
      page: 0,
      size: 0,
    })
    categories.value = data.items || []
  } catch (e) {
    console.error('Failed to fetch categories', e)
    Toast.error('加载分类列表失败')
  } finally {
    loading.value = false
  }
}

// Open create modal
function handleCreate() {
  isEditing.value = false
  formCategory.value = null
  formData.value = { specName: '', description: '' }
  showFormModal.value = true
}

// Open edit modal
function handleEdit(category: Category) {
  isEditing.value = true
  formCategory.value = category
  formData.value = {
    specName: category.spec.name || '',
    description: category.spec.description || '',
  }
  showFormModal.value = true
}

// Save category (create or update)
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

// Delete category
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
        Toast.success('删除分类成功')
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

<template>
  <div class="category-list">
    <!-- Header -->
    <div class="flex items-center justify-between p-4">
      <div class="text-sm text-gray-500">
        共 {{ categories.length }} 个分类
      </div>
      <VButton type="secondary" @click="handleCreate">
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
    <VEntityContainer v-else>
      <VEntity
        v-for="category in categories"
        :key="category.metadata.name"
      >
        <template #start>
          <VEntityField
            :title="category.spec.name"
            :description="category.spec.description || '暂无描述'"
          />
        </template>
        <template #end>
          <VEntityField>
            <template #description>
              <VTag>
                {{ category.status?.sentenceCount ?? 0 }} 条句子
              </VTag>
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

    <!-- Create/Edit Modal -->
    <VModal
      v-model:visible="showFormModal"
      :title="isEditing ? '编辑分类' : '新建分类'"
      :width="520"
    >
      <div class="space-y-4">
        <div>
          <label class="mb-2 block text-sm font-medium text-gray-700">
            分类名称 <span class="text-red-500">*</span>
          </label>
          <input
            v-model="formData.specName"
            type="text"
            class="w-full rounded-md border border-gray-300 px-3 py-2 text-sm transition-colors focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            placeholder="请输入分类名称"
            aria-required="true"
          />
        </div>
        <div>
          <label class="mb-2 block text-sm font-medium text-gray-700">
            分类描述
          </label>
          <textarea
            v-model="formData.description"
            rows="3"
            class="w-full rounded-md border border-gray-300 px-3 py-2 text-sm transition-colors focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            placeholder="请输入分类描述（可选）"
          />
        </div>
      </div>
      <template #footer>
        <VSpace>
          <VButton @click="showFormModal = false">取消</VButton>
          <VButton type="secondary" :loading="saving" @click="handleSave">
            保存
          </VButton>
        </VSpace>
      </template>
    </VModal>
  </div>
</template>

<style scoped>
.category-list {
  min-height: 200px;
}
</style>
