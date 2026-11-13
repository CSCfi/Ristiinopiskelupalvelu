<template>
  <ul>
    <li
      v-for="lang in languages"
      :key="lang"
      @click="changeLang(lang)"
      :class="lang === $i18n.locale ? 'active' : ''"
    >
      {{ lang }}
    </li>
  </ul>
</template>

<script setup>
import { useI18n } from 'vue-i18n';

const { locale } = useI18n();
const languages = ['fi', 'en', 'sv'];

const changeLang = lang => {
  locale.value = lang;
};
</script>

<style lang="css" scoped>
li {
  float: left;
  list-style: none;
  margin: 10px;
  border-right: 1px solid #ccc;
  padding-right: 20px;
  cursor: pointer;
}
li.active {
  font-weight: bold;
}
</style>
