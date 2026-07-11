<template>
  <!-- 根视图仅渲染当前激活路由对应的组件 -->
  <router-view />
</template>

<style>
/* 全局基础重置样式 */
html, body {
  margin: 0;
  padding: 0;
  width: 100%;
  height: 100%;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  background-color: #f8fafc;
  color: #0f172a;
}

#app {
  width: 100%;
  height: 100%;
}
</style>