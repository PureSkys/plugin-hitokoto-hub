<template>
  <div>
    <VCard>
      <template #header>
        <div class="flex w-full bg-gray-50 px-4 py-3">
          <div class="flex w-full flex-1 items-center gap-6 sm:w-auto">
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
                  { label: '默认' },
                  { label: '较近创建', value: 'metadata.creationTimestamp,desc' },
                  { label: '较早创建', value: 'metadata.creationTimestamp,asc' },
                ]"
              />
              <div class="flex flex-row gap-2">
                <div class="group cursor-pointer rounded p-1 hover:bg-gray-200"
                     @click="refreshData">
                  <IconRefreshLine
                          v-tooltip="'刷新'"
                          class="h-4 w-4 text-gray-600 group-hover:text-gray-900"
                  />
                </div>
              </div>
            </VSpace>
          </div>
          <div class="flex flex-row gap-2 items-center">
            <VButton size="sm" @click="handleBatchImport">批量导入</VButton>
            <VButton size="sm" type="secondary" @click="handleCreate">新建句子</VButton>
          </div>
        </div>
      </template>

      <div v-if="loading" class="flex items-center justify-center py-20">
        <VLoading/>
      </div>

      <VEmpty
              v-else-if="sentences.length === 0"
              title="暂无句子"
              message="点击「新建句子」创建你的第一条句子"
      />

      <VEntityContainer v-else>
        <VEntity
                v-for="sentence in sentences"
                :key="sentence.metadata.name"
        >
          <template #start>
            <VEntityField>
              <template #title>
                <div class="flex items-center gap-2">
                  <span
                          v-if="isDeleting(sentence)"
                          class="inline-block h-2 w-2 rounded-full bg-red-500 animate-pulse"
                          v-tooltip="'删除中'"
                  />
                  <span class="text-sm font-medium text-gray-900">
                    {{ sentence.spec.content }}
                  </span>
                </div>
              </template>
              <template #description>
                <div class="mt-1.5 flex flex-wrap items-center gap-1.5">
                  <span class="inline-flex items-center rounded-full bg-gray-100 px-2.5 py-0.5 text-xs text-gray-600">
                    作者：{{ sentence.spec.author || '匿名' }}
                  </span>
                  <span class="inline-flex items-center rounded-full bg-gray-100 px-2.5 py-0.5 text-xs text-gray-600">
                    来源：{{ sentence.spec.source || '未知' }}
                  </span>
                  <span class="inline-flex items-center rounded-full bg-gray-100 px-2.5 py-0.5 text-xs text-gray-600">
                    分类：{{ getCategoryName(sentence.spec.categoryName) }}
                  </span>
                </div>
              </template>
            </VEntityField>
          </template>
          <template #end>
            <VEntityField>
              <template #description>
                <VTag :theme="sentence.status?.published ? 'primary' : 'default'">
                  {{ sentence.status?.published ? '已发布' : '未发布' }}
                </VTag>
              </template>
            </VEntityField>
            <VEntityField>
              <template #description>
                <div class="flex items-center gap-1 text-gray-500">
                  <el-icon :size="14">
                    <StarFilled/>
                  </el-icon>
                  <span class="text-sm">{{ sentence.status?.likeCount ?? 0 }}</span>
                </div>
              </template>
            </VEntityField>
            <VEntityField>
              <template #description>
                <div class="flex items-center gap-1 text-gray-500">
                  <el-icon :size="14">
                    <View/>
                  </el-icon>
                  <span class="text-sm">{{ sentence.status?.viewCount ?? 0 }}</span>
                </div>
              </template>
            </VEntityField>
            <VEntityField>
              <template #description>
                <VDropdown>
                  <VButton size="sm" type="default">操作</VButton>
                  <template #popper>
                    <VDropdownItem @click="handleEdit(sentence)">编辑</VDropdownItem>
                    <VDropdownItem type="danger" @click="handleDelete(sentence)">删除
                    </VDropdownItem>
                  </template>
                </VDropdown>
              </template>
            </VEntityField>
          </template>
        </VEntity>
      </VEntityContainer>

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

    <!-- 创建/编辑句子弹窗 -->
    <VModal
            v-model:visible="showFormModal"
            :title="isEditing ? '编辑句子' : '新建句子'"
            :width="600"
    >
      <div class="form-modal-body">
        <FormKit
                v-model="formData.content"
                type="textarea"
                label="句子内容"
                validation="required"
                validation-message="请输入句子内容"
                placeholder="请输入句子内容"
                :rows="4"
        />
        <FormKit
                v-model="formData.categoryName"
                type="select"
                label="分类"
                validation="required"
                validation-message="请选择分类"
                placeholder="请选择分类"
                :options="categorySelectOptions"
        />
        <FormKit
                v-model="formData.author"
                type="text"
                label="作者"
                placeholder="请输入作者（默认为匿名）"
        />
        <FormKit
                v-model="formData.source"
                type="text"
                label="来源"
                placeholder="请输入来源（默认为未知）"
        />
        <FormKit
                v-if="isEditing"
                v-model="formData.published"
                type="checkbox"
                label="发布状态"
                help="勾选后公开对外可见，未勾选则仅管理员可见"
        >
          已发布
        </FormKit>
      </div>
      <template #footer>
        <div class="modal-footer">
          <VButton @click="showFormModal = false">取消</VButton>
          <VButton type="secondary" :loading="saving" @click="handleSave">
            {{ isEditing ? '保存修改' : '创建' }}
          </VButton>
        </div>
      </template>
    </VModal>

    <!-- 批量导入弹窗 -->
    <VModal
            v-model:visible="showBatchImportModal"
            title="批量导入句子"
            :width="900"
    >
      <div class="form-modal-body">
        <FormKit
                v-model="batchImportForm.categoryName"
                type="select"
                label="目标分类"
                validation="required"
                validation-message="请选择分类"
                placeholder="请选择分类"
                :options="categorySelectOptions"
        />

        <FormKit
                v-model="batchImportForm.jsonText"
                type="textarea"
                label="粘贴 JSON 数据"
                validation="required"
                validation-message="请粘贴 JSON 数据"
                placeholder='[
  {
    "hitokoto": "句子内容",
    "from": "来源",
    "from_who": "作者"
  }
]'
                help="支持 JSON 数组或单个对象"
                :rows="10"
        />

        <div class="mt-4 rounded-lg border border-gray-200 bg-gray-50 p-4">
          <h4 class="mb-3 text-sm font-medium text-gray-700">字段映射</h4>
          <div class="grid grid-cols-3 gap-4">
            <div>
              <label class="mb-1.5 block text-xs font-medium text-gray-500">
                句子内容
                <span class="text-red-400">*</span>
              </label>
              <select
                      v-model="batchImportForm.contentField"
                      class="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
              >
                <option value="">不映射</option>
                <option v-for="key in availableKeys" :key="key" :value="key">{{ key }}</option>
              </select>
            </div>
            <div>
              <label class="mb-1.5 block text-xs font-medium text-gray-500">作者</label>
              <select
                      v-model="batchImportForm.authorField"
                      class="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
              >
                <option value="">不映射</option>
                <option v-for="key in availableKeys" :key="key" :value="key">{{ key }}</option>
              </select>
            </div>
            <div>
              <label class="mb-1.5 block text-xs font-medium text-gray-500">来源</label>
              <select
                      v-model="batchImportForm.sourceField"
                      class="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
              >
                <option value="">不映射</option>
                <option v-for="key in availableKeys" :key="key" :value="key">{{ key }}</option>
              </select>
            </div>
          </div>
        </div>

        <div class="mt-4">
          <div class="mb-2 flex items-center justify-between">
            <label class="text-sm font-medium text-gray-700">解析预览</label>
            <span class="text-xs text-gray-400">共 {{ parsedSentences.length }} 条</span>
          </div>
          <div class="max-h-60 overflow-y-auto rounded-md border border-gray-200">
            <div v-if="parsedSentences.length === 0" class="py-8 text-center text-sm text-gray-400">
              请粘贴有效的 JSON 数据并选择字段映射
            </div>
            <div v-else class="divide-y divide-gray-100">
              <div
                      v-for="(item, index) in parsedSentences"
                      :key="index"
                      class="flex items-start justify-between px-4 py-3 hover:bg-gray-50"
              >
                <div class="flex-1 min-w-0">
                  <div class="text-sm font-medium text-gray-900">
                    {{ item.content || '(无内容)' }}
                  </div>
                  <div class="mt-1 flex flex-wrap items-center gap-1.5">
            <span class="inline-flex items-center rounded-full bg-gray-100 px-2.5 py-0.5 text-xs text-gray-600">
              作者：{{ item.author || '匿名' }}
            </span>
                    <span class="inline-flex items-center rounded-full bg-gray-100 px-2.5 py-0.5 text-xs text-gray-600">
              来源：{{ item.source || '未知' }}
            </span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <div class="modal-footer">
          <VButton @click="showBatchImportModal = false">取消</VButton>
          <VButton
                  type="secondary"
                  :loading="batchImporting"
                  :disabled="parsedSentences.length === 0"
                  @click="handleBatchSave"
          >
            开始导入 ({{ parsedSentences.length }} 条)
          </VButton>
        </div>
      </template>
    </VModal>
  </div>
</template>

<script setup lang="ts">
import {
  Dialog,
  IconRefreshLine,
  Toast,
  VButton,
  VCard,
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
import {StarFilled, View} from '@element-plus/icons-vue'
import {computed, onMounted, ref, watch} from 'vue'
import {categoryCoreApiClient, sentenceCoreApiClient} from '@/api'
import type {BatchCreateSentenceResult, Category, Sentence} from '@/api/generated'

const page = ref(1)
const size = ref(20)
const total = ref(0)
const loading = ref(false)
const sentences = ref<Sentence[]>([])
const categories = ref<Category[]>([])

const categoryNameList = ref<{ label: string; value: string | undefined }[]>([
  {label: '全部', value: undefined},
])
const selectedCategory = ref<string | undefined>(undefined)
const selectedSort = ref<string | undefined>(undefined)

const showFormModal = ref(false)
const isEditing = ref(false)
const saving = ref(false)
const editingSentenceName = ref('')
const editingOriginalSentence = ref<Sentence | null>(null)
const formData = ref({
  content: '',
  categoryName: '',
  author: '匿名',
  source: '未知',
  published: true,
})

const showBatchImportModal = ref(false)
const batchImporting = ref(false)
const batchImportForm = ref({
  jsonText: '',
  categoryName: '',
  contentField: '',
  authorField: '',
  sourceField: '',
})

const getCategoryName = (categoryId: string): string => {
  const category = categories.value.find(c => c.metadata.name === categoryId)
  return category?.spec.name || categoryId
}

const availableKeys = computed(() => {
  const text = batchImportForm.value.jsonText.trim()
  if (!text) return []

  try {
    const parsed = JSON.parse(text)
    const data = Array.isArray(parsed) ? parsed : [parsed]
    const firstObject = data.find((item: any) => item && typeof item === 'object')
    if (!firstObject) return []

    return Object.keys(firstObject)
  } catch {
    return []
  }
})

const parsedSentences = computed(() => {
  const text = batchImportForm.value.jsonText.trim()
  if (!text) return []

  try {
    let data: any[] = []
    const parsed = JSON.parse(text)

    if (Array.isArray(parsed)) {
      data = parsed
    } else if (typeof parsed === 'object' && parsed !== null) {
      data = [parsed]
    } else {
      return []
    }

    const contentField = batchImportForm.value.contentField
    const authorField = batchImportForm.value.authorField
    const sourceField = batchImportForm.value.sourceField

    return data
            .filter(item => item && typeof item === 'object')
            .map(item => ({
              content: contentField ? String(item[contentField] ?? '') : '',
              author: authorField ? String(item[authorField] ?? '') : '',
              source: sourceField ? String(item[sourceField] ?? '') : '',
            }))
            .filter(item => item.content)
  } catch {
    return []
  }
})

const isDeleting = (sentence: Sentence): boolean => {
  const finalizers = sentence.metadata?.finalizers
  return Array.isArray(finalizers) && finalizers.length > 0
}

const categorySelectOptions = computed(() =>
        categories.value.map((c) => ({
          label: c.spec.name,
          value: c.metadata.name,
        }))
)

const initCategories = async () => {
  try {
    const {data} = await categoryCoreApiClient.category.listCategory({page: 1, size: 100})
    categories.value = data.items || []
    categoryNameList.value = [
      {label: '全部', value: undefined},
      ...data.items.map((item) => ({
        label: item.spec.name,
        value: item.metadata.name,
      })),
    ]
  } catch (e) {
    console.error('获取分类列表失败', e)
  }
}

const fetchSentences = async () => {
  loading.value = true
  try {
    const params: any = {
      page: page.value,
      size: size.value,
    }

    if (selectedCategory.value) {
      params.fieldSelector = [`spec.categoryName=${selectedCategory.value}`]
    }

    if (selectedSort.value) {
      params.sort = [selectedSort.value]
    }

    const {data} = await sentenceCoreApiClient.sentence.listSentence(params)
    sentences.value = data.items || []
    total.value = data.total || 0
  } catch (e) {
    console.error('获取句子列表失败', e)
    Toast.error('加载句子列表失败')
  } finally {
    loading.value = false
  }
}

const refreshData = () => {
  fetchSentences()
}

const handleClearFilters = () => {
  selectedCategory.value = undefined
  selectedSort.value = undefined
  page.value = 1
  fetchSentences()
}

const hasFilters = computed(() => {
  return selectedCategory.value || selectedSort.value
})

watch(page, () => fetchSentences())
watch(size, () => {
  page.value = 1;
  fetchSentences()
})
watch([selectedCategory, selectedSort], () => {
  page.value = 1;
  fetchSentences()
})

const handleCreate = () => {
  isEditing.value = false
  editingSentenceName.value = ''
  editingOriginalSentence.value = null
  formData.value = {
    content: '',
    categoryName: categories.value[0]?.metadata.name || '',
    author: '匿名',
    source: '未知',
    published: true,
  }
  showFormModal.value = true
}

const handleBatchImport = async () => {
  await initCategories()
  batchImportForm.value = {
    jsonText: '',
    categoryName: categories.value[0]?.metadata.name || '',
    contentField: '',
    authorField: '',
    sourceField: '',
  }
  showBatchImportModal.value = true
}

const buildSentence = (content: string, categoryName: string, author?: string, source?: string): Sentence => ({
  apiVersion: 'hitokotohub.puresky.top/v1alpha1',
  kind: 'Sentence',
  metadata: {generateName: 'sentence-', name: ''},
  spec: {
    content,
    categoryName,
    author: author || '匿名',
    source: source || '未知',
  },
})

const batchCreate = async (sentenceList: Sentence[]): Promise<BatchCreateSentenceResult> => {
  const {data} = await sentenceCoreApiClient.sentence.batchCreateSentence({
    sentence: sentenceList,
  })
  return data as BatchCreateSentenceResult
}

const handleSave = async () => {
  if (!formData.value.content || !formData.value.categoryName) {
    Toast.warning('请填写句子内容和分类')
    return
  }

  saving.value = true
  try {
    if (isEditing.value && editingOriginalSentence.value) {
      const updated: Sentence = {
        ...editingOriginalSentence.value,
        spec: {
          ...editingOriginalSentence.value.spec,
          content: formData.value.content,
          categoryName: formData.value.categoryName,
          author: formData.value.author,
          source: formData.value.source,
        },
        status: {
          ...editingOriginalSentence.value.status,
          published: formData.value.published,
        },
      }
      await sentenceCoreApiClient.sentence.updateSentence({
        name: editingSentenceName.value,
        sentence: updated,
      })
      Toast.success('更新成功')
    } else {
      const sentence = buildSentence(
              formData.value.content,
              formData.value.categoryName,
              formData.value.author,
              formData.value.source
      )
      await batchCreate([sentence])
      Toast.success('创建成功')
    }
    showFormModal.value = false
    fetchSentences()
  } catch (e) {
    console.error('保存失败', e)
    Toast.error(isEditing.value ? '更新失败' : '创建失败')
  } finally {
    saving.value = false
  }
}

const handleBatchSave = async () => {
  if (!batchImportForm.value.categoryName) {
    Toast.warning('请选择目标分类')
    return
  }

  if (parsedSentences.value.length === 0) {
    Toast.warning('没有解析到有效的句子数据')
    return
  }

  batchImporting.value = true
  try {
    const sentenceList = parsedSentences.value.map(item =>
            buildSentence(item.content, batchImportForm.value.categoryName, item.author, item.source)
    )

    const result = await batchCreate(sentenceList)
    Toast.success(`导入完成！成功: ${result.success || 0}，失败: ${result.failed || 0}`)
    showBatchImportModal.value = false
    fetchSentences()
  } catch (e) {
    console.error('批量导入失败', e)
    Toast.error('批量导入失败')
  } finally {
    batchImporting.value = false
  }
}

const handleEdit = (sentence: Sentence) => {
  isEditing.value = true
  editingSentenceName.value = sentence.metadata.name
  editingOriginalSentence.value = sentence
  formData.value = {
    content: sentence.spec.content,
    categoryName: sentence.spec.categoryName,
    author: sentence.spec.author || '匿名',
    source: sentence.spec.source || '未知',
    published: sentence.status?.published ?? true,
  }
  showFormModal.value = true
}

const handleDelete = (sentence: Sentence) => {
  Dialog.warning({
    title: '删除确认',
    description: `确定要删除句子「${sentence.spec.content.slice(0, 30)}${sentence.spec.content.length > 30 ? '...' : ''}」吗？该操作不可撤销。`,
    confirmType: 'danger',
    confirmText: '删除',
    cancelText: '取消',
    onConfirm: async () => {
      try {
        await sentenceCoreApiClient.sentence.deleteSentence({name: sentence.metadata.name})
        Toast.success('删除成功')
      } catch (e) {
        console.error('删除失败', e)
        Toast.error('删除失败')
      }
    },
  })
}

onMounted(() => {
  initCategories()
  fetchSentences()
})
</script>

<style scoped>
.form-modal-body {
  padding: 4px 0;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
