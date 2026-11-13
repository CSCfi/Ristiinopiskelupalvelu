import { defineStore } from 'pinia';
import axios from 'axios';

export const useCodesetStore = defineStore('codesets', {
  state: () => ({
    codesets: [],
    codesetItems: {},
    codes: {}
  }),
  actions: {
    async fetchCodesets() {
      const response = await axios.get('/admin-ui/api/codes/codesets');
      this.codesets = response.data || [];
      return this.codesets;
    },
    async fetchCodesetItems(key) {
      const response = await axios.get(`/admin-ui/api/codes/codesets/${key}`);
      this.codesetItems[key] = response.data || [];
      return this.codesetItems[key];
    },
    async fetchCode(id) {
      const response = await axios.get(`/admin-ui/api/codes/code/${id}`);
      this.codes[id] = response.data;
      return response.data;
    },
    async addCodeset(name) {
      const response = await axios.put(`/admin-ui/api/codes/codesets/${name}`);
      await this.fetchCodesets();
      return response.data;
    },
    async deleteCodeset(name) {
      await axios.delete(`/admin-ui/api/codes/codesets/${name}`);
      await this.fetchCodesets();
    },
    async addCodeToCodeset(key) {
      const response = await axios.put(`/admin-ui/api/codes/codesets/${key}`);
      await this.fetchCodesetItems(key);
      return response.data;
    },
    async deleteCode(id) {
      await axios.delete(`/admin-ui/api/codes/code/${id}`);
    },
    async updateCode(payload) {
      await axios.post('/admin-ui/api/codes/update', JSON.stringify(payload), {
        headers: {
          'Content-Type': 'application/json;charset=UTF-8'
        }
      });
    },
    async importCodes(formData) {
      return axios.post('/admin-ui/api/codes/import', formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      });
    },
    async fetchCodesetValues(key) {
      const response = await axios.get(`/admin-ui/api/codes/codesets/${key}`);
      return response.data || [];
    }
  }
});
