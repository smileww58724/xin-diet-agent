<template>
  <div class="fav-page">
    <div class="page-head">
      <div>
        <h1>偏好食物</h1>
        <div class="page-sub">标记你爱吃的食物，AI 的推荐、食谱和所有食物话题都会优先围绕它们</div>
      </div>
      <el-button type="primary" @click="openAdd">添加偏好</el-button>
    </div>

    <div v-if="foods.length > 0" class="fav-grid">
      <div v-for="f in foods" :key="f.id" class="fav-card">
        <div class="fav-top">
          <span class="fav-name">{{ f.foodName }}</span>
          <el-tag v-if="f.category">{{ f.category }}</el-tag>
        </div>
        <p v-if="f.note" class="fav-note">{{ f.note }}</p>
        <div v-if="hasNutrition(f)" class="fav-nutrition">
          <span v-if="f.caloriesPer100g != null">{{ f.caloriesPer100g }} kcal</span>
          <span v-if="f.proteinPer100g != null">蛋白 {{ f.proteinPer100g }}g</span>
          <span v-if="f.fatPer100g != null">脂肪 {{ f.fatPer100g }}g</span>
          <span v-if="f.carbPer100g != null">碳水 {{ f.carbPer100g }}g</span>
          <span class="per">/ 100g</span>
        </div>
        <div class="fav-actions">
          <el-button size="small" link @click="openEdit(f)">编辑</el-button>
          <el-button type="danger" size="small" link @click="handleDelete(f)">删除</el-button>
        </div>
      </div>
    </div>

    <div v-else class="empty-state">
      <h3>还没有标记偏好食物</h3>
      <p>告诉系统你爱吃什么，AI 推荐会更合你的口味</p>
      <el-button type="primary" @click="openAdd">添加第一个偏好</el-button>
    </div>

    <el-dialog v-model="showDialog" :title="editingId ? '编辑偏好' : '添加偏好'" width="480px">
      <el-form :model="form" label-position="top">
        <el-form-item label="食物名称" required>
          <el-input v-model="form.foodName" placeholder="如：鸡胸肉、燕麦、三文鱼" maxlength="100" />
        </el-form-item>
        <el-form-item label="分类（可选）">
          <el-input v-model="form.category" placeholder="如：高蛋白、粗粮、低脂" maxlength="30" />
        </el-form-item>
        <div class="nutrition-row">
          <el-form-item label="热量/100g">
            <el-input-number v-model="form.caloriesPer100g" :min="0" controls-position="right" style="width: 100%" />
          </el-form-item>
          <el-form-item label="蛋白质(g)">
            <el-input-number v-model="form.proteinPer100g" :min="0" :precision="1" controls-position="right" style="width: 100%" />
          </el-form-item>
        </div>
        <div class="nutrition-row">
          <el-form-item label="脂肪(g)">
            <el-input-number v-model="form.fatPer100g" :min="0" :precision="1" controls-position="right" style="width: 100%" />
          </el-form-item>
          <el-form-item label="碳水(g)">
            <el-input-number v-model="form.carbPer100g" :min="0" :precision="1" controls-position="right" style="width: 100%" />
          </el-form-item>
        </div>
        <el-form-item label="备注（可选）">
          <el-input v-model="form.note" placeholder="如：减脂期主力、周末才吃" maxlength="200" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { favoritesAPI } from '../api'
import { ElMessage } from 'element-plus'

const foods = ref([])
const showDialog = ref(false)
const editingId = ref(null)

const form = reactive({
  foodName: '',
  category: '',
  note: '',
  caloriesPer100g: null,
  proteinPer100g: null,
  fatPer100g: null,
  carbPer100g: null,
})

const hasNutrition = (f) =>
  f.caloriesPer100g != null || f.proteinPer100g != null || f.fatPer100g != null || f.carbPer100g != null

const loadFoods = async () => {
  try {
    foods.value = await favoritesAPI.list()
  } catch (e) {
    console.error(e)
  }
}

const openAdd = () => {
  editingId.value = null
  Object.assign(form, {
    foodName: '',
    category: '',
    note: '',
    caloriesPer100g: null,
    proteinPer100g: null,
    fatPer100g: null,
    carbPer100g: null,
  })
  showDialog.value = true
}

const openEdit = (f) => {
  editingId.value = f.id
  Object.assign(form, {
    foodName: f.foodName,
    category: f.category || '',
    note: f.note || '',
    caloriesPer100g: f.caloriesPer100g,
    proteinPer100g: f.proteinPer100g,
    fatPer100g: f.fatPer100g,
    carbPer100g: f.carbPer100g,
  })
  showDialog.value = true
}

const handleSubmit = async () => {
  if (!form.foodName.trim()) {
    ElMessage.warning('请输入食物名称')
    return
  }
  try {
    if (editingId.value) {
      await favoritesAPI.update(editingId.value, form)
      ElMessage.success('修改成功')
    } else {
      await favoritesAPI.add(form)
      ElMessage.success('添加成功')
    }
    showDialog.value = false
    loadFoods()
  } catch (e) {
    console.error(e)
  }
}

const handleDelete = async (f) => {
  try {
    await favoritesAPI.remove(f.id)
    ElMessage.success('已删除')
    loadFoods()
  } catch (e) {
    console.error(e)
  }
}

onMounted(loadFoods)
</script>

<style scoped>
.fav-page {
  max-width: 860px;
  margin: 0 auto;
}
.fav-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}
@media (max-width: 760px) {
  .fav-grid {
    grid-template-columns: 1fr;
  }
}
.fav-card {
  background: var(--panel);
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  padding: 18px 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.fav-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.fav-name {
  font-size: 15px;
  font-weight: 700;
  color: var(--ink);
}
.fav-note {
  margin: 0;
  font-size: 12px;
  color: var(--text-3);
  line-height: 1.6;
}
.fav-nutrition {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 12px;
  font-size: 12px;
  color: var(--text-2);
}
.fav-nutrition .per {
  color: var(--text-3);
}
.fav-actions {
  display: flex;
  justify-content: flex-end;
  gap: 4px;
  margin-top: 2px;
}
.nutrition-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
</style>
