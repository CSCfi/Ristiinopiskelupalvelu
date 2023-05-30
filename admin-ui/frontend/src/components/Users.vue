<template>
  <div class="users-container">
    <v-card>
      <v-list>
        <v-list-tile v-for="user in users" :key="user.title" @click="onClickRow">
          <v-list-tile-content>
            <v-list-tile-title v-text="user.title" :id="user.title"></v-list-tile-title>
          </v-list-tile-content>
        </v-list-tile>
      </v-list>
    </v-card>
  </div>
</template>

<script>
export default {
  data() {
    return {
      active: false
    };
  },
  computed: {
    // a computed getter
    activeMessage: function() {
      // `this` points to the vm instance
      return this.active;
    }
  },
  props: ['codesets', 'active'],
  methods: {
    onClickRow(event) {
      this.$emit('rowClicked', event.srcElement.id);
    }
  }
};
</script>

<style lang="css" scoped>
.active {
  background: rgba(0, 0, 0, 0.04);
}
</style>
