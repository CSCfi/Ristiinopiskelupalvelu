import { defineStore } from 'pinia';
import axios from 'axios';

export const useNetworkStore = defineStore('networks', {
  state: () => ({
    networks: []
  }),
  actions: {
    async fetchNetworks() {
      const response = await axios.get('/admin-ui/api/networks');
      this.networks = response.data || [];
      return this.networks;
    },
    async updateNetwork(payload) {
      await axios.post('/admin-ui/api/networks/update', payload);
    },
    async addNetwork(payload) {
      const response = await axios.post('/admin-ui/api/networks', payload);
      const created = response.data;
      if (created) {
        this.networks = [...this.networks, created];
      }
      return created;
    },
    async deleteNetwork(id) {
      await axios.delete(`/admin-ui/api/networks/${id}`);
      this.networks = this.networks.filter(n => n.id !== id);
    }
  }
});
