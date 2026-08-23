<template>
  <div class="diet-container">
    <el-page-header @back="$router.back()" content="饮食记录">
      <template #extra>
        <el-button type="primary" @click="showAddDialog = true">添加记录</el-button>
      </template>
    </el-page-header>

    <div class="date-picker">
      <el-date-picker
        v-model="selectedDate"
        type="date"
        placeholder="选择日期"
        format="YYYY-MM-DD"
        value-format="YYYY-MM-DD"
        @change="loadRecords"
      />
    </div>

    <el-card v-if="records.length > 0">
      <el-table :data="records" stripe>
        <el-table-column prop="mealType" label="餐次" width="100">
          <template #default="{ row }">
            <el-tag>{{ mealTypeMap[row.mealType] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="foodName" label="食物名称" />
        <el-table-column prop="portionSize" label="份量" width="120" />
        <el-table-column prop="calories" label="卡路里" width="100">
          <template #default="{ row }">{{ row.calories }} kcal</template>
        </el-table-column>
        <el-table-column prop="protein" label="蛋白质" width="80">
          <template #default="{ row }">{{ row.protein }}g</template>
        </el-table-column>
        <el-table-column prop="fat" label="脂肪" width="80">
          <template #default="{ row }">{{ row.fat }}g</template>
        </el-table-column>
        <el-table-column prop="carbohydrate" label="碳水" width="80">
          <template #default="{ row }">{{ row.carbohydrate }}g</template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button type="danger" size="small" link @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-empty v-else description="暂无记录" />

    <el-dialog v-model="showAddDialog" title="添加饮食记录" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="餐次">
          <el-select v-model="form.mealType" placeholder="请选择">
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
import { ref, reactive, onMounted } from 'vue'
import { dietAPI } from '../api'
import { ElMessage } from 'element-plus'

const mealTypeMap = {
  breakfast: '早餐',
  lunch: '午餐',
  dinner: '晚餐',
  snack: '加餐',
}

const selectedDate = ref(new Date().toISOString().split('T')[0])
const records = ref([])
const showAddDialog = ref(false)

const form = reactive({
  mealType: 'breakfast',
  foodName: '',
  portionSize: '',
  calories: 0,
  protein: 0,
  fat: 0,
  carbohydrate: 0,
  mealTime: new Date().toISOString().slice(0, 19),
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
.diet-container {
  padding: 20px;
}
.date-picker {
  margin: 20px 0;
}
</style>
