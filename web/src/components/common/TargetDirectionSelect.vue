<script setup lang="ts">
import { TARGET_DIRECTIONS } from '@/constants/targetDirections'

const model = defineModel<string>({ required: true })
</script>

<template>
  <el-form label-position="top">
    <el-form-item label="目标方向">
      <el-select v-model="model" allow-create filterable default-first-option placeholder="选择或输入目标方向" style="width: 100%">
        <el-option v-for="item in TARGET_DIRECTIONS" :key="item" :label="item" :value="item" />
      </el-select>
    </el-form-item>
  </el-form>
</template>
