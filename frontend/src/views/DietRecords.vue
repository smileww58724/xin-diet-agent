<template>
  <div class="diet-page">
    <div class="page-head">
      <div>
        <h1>饮食记录</h1>
        <div class="page-sub">记录每一餐，AI 实时帮你分析</div>
      </div>
      <div class="head-actions">
        <el-date-picker
          v-model="selectedDate"
          type="date"
          placeholder="选择日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          @change="loadRecords"
          :clearable="false"
          style="width: 140px"
        />
        <el-button type="primary" @click="showAddDialog = true">添加记录</el-button>
      </div>
    </div>

    <div v-if="records.length > 0" class="table-card">
      <el-table :data="records" stripe>
        <el-table-column prop="mealType" label="餐次" width="110">
          <template #default="{ row }">
            <el-tag>{{ mealLabel(row.mealType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="foodName" label="食物名称" min-width="140" />
        <el-table-column prop="portionSize" label="份量" width="110" />
        <el-table-column prop="calories" label="热量" width="100" sortable>
          <template #default="{ row }">{{ row.calories }} kcal</template>
        </el-table-column>
        <el-table-column prop="protein" label="蛋白质" width="90">
          <template #default="{ row }">{{ row.protein }}g</template>
        </el-table-column>
        <el-table-column prop="fat" label="脂肪" width="80">
          <template #default="{ row }">{{ row.fat }}g</template>
        </el-table-column>
        <el-table-column prop="carbohydrate" label="碳水" width="80">
          <template #default="{ row }">{{ row.carbohydrate }}g</template>
        </el-table-column>
        <el-table-column label="" width="70" align="right">
          <template #default="{ row }">
            <el-button type="danger" size="small" link @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div v-else class="empty-state">
      <h3>{{ mealDateLabel }}还没有记录</h3>
      <p>手动添加，或在 AI 对话里直接说"我吃了什么"帮你记录</p>
      <el-button type="primary" @click="showAddDialog = true">添加第一条记录</el-button>
    </div>

    <el-dialog v-model="showAddDialog" title="添加饮食记录" width="520px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="餐次">
          <el-select v-model="form.mealType" placeholder="请选择" style="width: 100%">
            <el-option label="早餐" value="breakfast" />
            <el-option label="午餐" value="lunch" />
            <el-option label="晚餐" value="dinner" />
            <el-option label="加餐" value="snack" />
          </el-select>
        </el-form-item>
        <el-form-item label="食物名称">
          <el-input v-model="form.foodName" placeholder="如：鸡蛋、全麦面包" />
        </el-form-item>
        <el-form-item label="份量">
          <el-input v-model="form.portionSize" placeholder="如：1个、100g" />
        </el-form-item>
        <el-form-item label="卡路里">
          <el-input-number v-model="form.calories" :min="0" />
        </el-form-item>
        <el-form-item label="蛋白质(g)">
          <el-input-number v-model="form.protein" :min="0" :precision="1" />
        </el-form-item>
        <el-form-item label="脂肪(g)">
          <el-input-number v-model="form.fat" :min="0" :precision="1" />
        </el-form-item>
        <el-form-item label="碳水(g)">
          <el-input-number v-model="form.carbohydrate" :min="0" :precision="1" />
        </el-form-item>
        <el-form-item label="用餐时间">
          <el-date-picker
            v-model="form.mealTime"
            type="datetime"
            placeholder="选择时间"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="handleAdd">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { dietAPI } from '../api'
import { ElMessage } from 'element-plus'

const mealTypeMap = {
  breakfast: '早餐',
  lunch: '午餐',
  dinner: '晚餐',
  snack: '加餐',
}
// AI 记录的餐次是中文（午餐/加餐），映射表只认英文 key，兜底直接显示原值
const mealLabel = (type) => mealTypeMap[type] || type

// 本地日期/时间（toISOString 是 UTC，会造成默认日期与用餐时间偏移 8 小时）
const localDateStr = (d = new Date()) =>
  `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
const localDateTimeStr = (d = new Date()) =>
  `${localDateStr(d)}T${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}:00`

const selectedDate = ref(localDateStr())
const records = ref([])
const showAddDialog = ref(false)

const mealDateLabel = computed(() =>
  selectedDate.value === localDateStr() ? '今天' : selectedDate.value
)

const form = reactive({
  mealType: 'breakfast',
  foodName: '',
  portionSize: '',
  calories: 0,
  protein: 0,
  fat: 0,
  carbohydrate: 0,
  mealTime: localDateTimeStr(),
})

onMounted(() => {
  loadRecords()
})

const loadRecords = async () => {
  try {
    records.value = await dietAPI.getRecords(selectedDate.value)
  } catch (e) {
    console.error(e)
  }
}

const handleAdd = async () => {
  if (!form.foodName) {
    ElMessage.warning('请输入食物名称')
    return
  }
  try {
    await dietAPI.addRecord(form)
    ElMessage.success('添加成功')
    showAddDialog.value = false
    loadRecords()
  } catch (e) {
    console.error(e)
  }
}

const handleDelete = async (id) => {
  try {
    await dietAPI.deleteRecord(id)
    ElMessage.success('删除成功')
    loadRecords()
  } catch (e) {
    console.error(e)
  }
}
</script>

<style scoped>
.diet-page {
  max-width: 960px;
  margin: 0 auto;
}
.head-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}
.table-card {
  background: var(--panel);
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  padding: 6px;
}
</style>
