<script setup lang="ts">
import {onMounted, ref} from 'vue'
import {categoryCoreApiClient, sentenceCoreApiClient} from '@/api'
import type {Category, Sentence} from '@/api/generated'
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
  VPagination,
  VSpace,
  VTag,
} from '@halo-dev/components'

const loading = ref(false)
const sentences = ref<Sentence[]>([])
const categories = ref<Category[]>([])
const saving = ref(false)

// Pagination
const page = ref(1)
const size = ref(20)
const total = ref(0)

// Filter
const selectedCategoryName = ref('')

// Modal state
const showFormModal = ref(false)
const isEditing = ref(false)
const formSentence = ref<Sentence | null>(null)

// Form data
const formData = ref({
  categoryName: '',
  content: '',
  author: '',
  source: '',
})

// Fetch categories for dropdown
async function fetchCategories() {
  try {
    const { data } = await categoryCoreApiClient.category.listCategory({
      page: 0,
      size: 0,
    })
    categories.value = data.items || []
  } catch (e) {
    console.error('Failed to fetch categories', e)
  }
}

// Fetch sentences
async function fetchSentences() {
  loading.value = true
  try {
    const fieldSelector: string[] = []
    if (selectedCategoryName.value) {
      fieldSelector.push(`spec.categoryName=${selectedCategoryName.value}`)
    }
    const { data } = await sentenceCoreApiClient.sentence.listSentence({
      page: page.value - 1,
      size: size.value,
      fieldSelector: fieldSelector.length > 0 ? fieldSelector : undefined,
    })
    sentences.value = data.items || []
    total.value = data.total || 0
  } catch (e) {
    console.error('Failed to fetch sentences', e)
    Toast.error('加载句子列表失败')
  } finally {
    loading.value = false
  }
}

// Page change
function handlePageChange(value: { page: number; size: number }) {
  page.value = value.page
  size.value = value.size
  fetchSentences()
}

// Open create modal
function handleCreate() {
  isEditing.value = false
  formSentence.value = null
  formData.value = {
    categoryName: '',
    content: '',
    author: '',
    source: '',
  }
  showFormModal.value = true
}

// Open edit modal
function handleEdit(sentence: Sentence) {
  isEditing.value = true
  formSentence.value = sentence
  formData.value = {
    categoryName: sentence.spec.categoryName || '',
    content: sentence.spec.content || '',
    author: sentence.spec.author || '',
    source: sentence.spec.source || '',
  }
  showFormModal.value = true
}

// Save sentence
async function handleSave() {
  if (!formData.value.content.trim()) {
    Toast.warning('请输入句子内容')
    return
  }
  if (!formData.value.categoryName) {
    Toast.warning('请选择分类')
    return
  }
  saving.value = true
  try {
    if (isEditing.value && formSentence.value) {
      const updated: Sentence = {
        ...formSentence.value,
        spec: {
          ...formSentence.value.spec,
          categoryName: formData.value.categoryName,
          content: formData.value.content.trim(),
          author: formData.value.author.trim() || undefined,
          source: formData.value.source.trim() || undefined,
        },
      }
      await sentenceCoreApiClient.sentence.updateSentence({
        name: formSentence.value.metadata.name,
        sentence: updated,
      })
      Toast.success('更新句子成功')
    } else {
      const newSentence: Sentence = {
        apiVersion: 'hitokotohub.puresky.top/v1alpha1',
        kind: 'Sentence',
        metadata: {
          name: '',
          generateName: 'sentence-',
        },
        spec: {
          categoryName: formData.value.categoryName,
          content: formData.value.content.trim(),
          author: formData.value.author.trim() || undefined,
          source: formData.value.source.trim() || undefined,
        },
      }
      await sentenceCoreApiClient.sentence.createSentence({
        sentence: newSentence,
      })
      Toast.success('创建句子成功')
    }
    showFormModal.value = false
    await fetchSentences()
  } catch (e) {
    console.error('Failed to save sentence', e)
    Toast.error(isEditing.value ? '更新句子失败' : '创建句子失败')
  } finally {
    saving.value = false
  }
}

// Delete sentence
function handleDelete(sentence: Sentence) {
  Dialog.warning({
    title: '删除确认',
    description: `确定要删除该句子吗？该操作不可撤销。`,
    confirmType: 'danger',
    confirmText: '删除',
    cancelText: '取消',
    onConfirm: async () => {
      try {
        await sentenceCoreApiClient.sentence.deleteSentence({
          name: sentence.metadata.name,
        })
        Toast.success('删除句子成功')
        await fetchSentences()
      } catch (e) {
        console.error('Failed to delete sentence', e)
        Toast.error('删除句子失败')
      }
    },
  })
}

// Get category display name
function getCategoryDisplayName(categoryName: string): string {
  const cat = categories.value.find((c) => c.metadata.name === categoryName)
  return cat ? cat.spec.name : categoryName
}

// Filter by category
function handleCategoryFilter() {
  page.value = 1
  fetchSentences()
}

// Truncate text
function truncate(text: string, maxLen: number): string {
  if (!text) return ''
  return text.length > maxLen ? text.substring(0, maxLen) + '...' : text
}

onMounted(() => {
  fetchCategories()
  fetchSentences()
})
</script>

<template>
  <div class="sentence-list">
    <!-- Header -->
    <div class="flex flex-col gap-3 p-4 sm:flex-row sm:items-center sm:justify-between">
      <div class="flex items-center gap-3">
        <span class="text-sm text-gray-500">
          共 {{ total }} 条句子
        </span>
        <select
          v-model="selectedCategoryName"
          class="rounded-md border border-gray-300 px-3 py-1.5 text-sm transition-colors focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          @change="handleCategoryFilter"
        >
          <option value="">全部分类</option>
          <option
            v-for="cat in categories"
            :key="cat.metadata.name"
            :value="cat.metadata.name"
          >
            {{ cat.spec.name }}
          </option>
        </select>
      </div>
      <VButton type="secondary" @click="handleCreate">
        <template #icon>
          <IconAddCircle class="h-full w-full" />
        </template>
        新建句子
      </VButton>
    </div>

    <!-- Loading -->
    <VLoading v-if="loading" />

    <!-- Empty -->
    <VEmpty
      v-else-if="sentences.length === 0"
      title="暂无句子"
      message="点击「新建句子」创建你的第一条一言语录"
    />

    <!-- Sentence List -->
    <template v-else>
      <VEntityContainer>
        <VEntity
          v-for="sentence in sentences"
          :key="sentence.metadata.name"
        >
          <template #start>
            <VEntityField
              :title="truncate(sentence.spec.content, 60)"
              :description="sentence.spec.author ? `—— ${sentence.spec.author}` : ''"
            />
          </template>
          <template #end>
            <VEntityField>
              <template #description>
                <VTag>
                  {{ getCategoryDisplayName(sentence.spec.categoryName) }}
                </VTag>
              </template>
            </VEntityField>
            <VEntityField v-if="sentence.spec.source">
              <template #description>
                <span class="text-xs text-gray-400" :title="sentence.spec.source">
                  {{ truncate(sentence.spec.source, 16) }}
                </span>
              </template>
            </VEntityField>
            <VEntityField>
              <template #description>
                <div class="flex items-center gap-2 text-xs text-gray-400">
                  <span v-if="sentence.status?.likeCount !== undefined">
                    👍 {{ sentence.status.likeCount }}
                  </span>
                  <span v-if="sentence.status?.viewCount !== undefined">
                    👁 {{ sentence.status.viewCount }}
                  </span>
                </div>
              </template>
            </VEntityField>
            <VEntityField>
              <template #description>
                <VDropdown>
                  <VButton size="sm" type="default">操作</VButton>
                  <template #popper>
                    <VDropdownItem @click="handleEdit(sentence)">编辑</VDropdownItem>
                    <VDropdownItem type="danger" @click="handleDelete(sentence)">删除</VDropdownItem>
                  </template>
                </VDropdown>
              </template>
            </VEntityField>
          </template>
        </VEntity>
      </VEntityContainer>

      <!-- Pagination -->
      <div
        v-if="total > size"
        class="flex items-center justify-center border-t border-gray-100 px-4 py-3"
      >
        <VPagination
          :page="page"
          :size="size"
          :total="total"
          @change="handlePageChange"
        />
      </div>
    </template>

    <!-- Create/Edit Modal -->
    <VModal
      v-model:visible="showFormModal"
      :title="isEditing ? '编辑句子' : '新建句子'"
      :width="600"
    >
      <div class="space-y-4">
        <div>
          <label class="mb-2 block text-sm font-medium text-gray-700">
            句子内容 <span class="text-red-500">*</span>
          </label>
          <textarea
            v-model="formData.content"
            rows="4"
            class="w-full rounded-md border border-gray-300 px-3 py-2 text-sm transition-colors focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            placeholder="请输入句子内容"
            aria-required="true"
          />
        </div>
        <div>
          <label class="mb-2 block text-sm font-medium text-gray-700">
            所属分类 <span class="text-red-500">*</span>
          </label>
          <select
            v-model="formData.categoryName"
            class="w-full rounded-md border border-gray-300 px-3 py-2 text-sm transition-colors focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            aria-required="true"
          >
            <option value="" disabled>请选择分类</option>
            <option
              v-for="cat in categories"
              :key="cat.metadata.name"
              :value="cat.metadata.name"
            >
              {{ cat.spec.name }}
            </option>
          </select>
        </div>
        <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <div>
            <label class="mb-2 block text-sm font-medium text-gray-700">
              作者
            </label>
            <input
              v-model="formData.author"
              type="text"
              class="w-full rounded-md border border-gray-300 px-3 py-2 text-sm transition-colors focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
              placeholder="句子作者（可选）"
            />
          </div>
          <div>
            <label class="mb-2 block text-sm font-medium text-gray-700">
              来源
            </label>
            <input
              v-model="formData.source"
              type="text"
              class="w-full rounded-md border border-gray-300 px-3 py-2 text-sm transition-colors focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
              placeholder="句子来源（可选）"
            />
          </div>
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
.sentence-list {
  min-height: 200px;
}
</style>
