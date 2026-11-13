import { defineStore } from 'pinia';
import axios from 'axios';

export const useUserStore = defineStore('user', {
  state: () => ({
    user: null,
    roles: []
  }),
  actions: {
    async fetchCurrentUser() {
      const response = await axios.get('/admin-ui/api/user');
      this.user = response.data;
      return this.user;
    },
    async fetchRoles() {
      const response = await axios.get('/admin-ui/api/user/roles');
      this.roles = response.data;
      return this.roles;
    }
  }
});
