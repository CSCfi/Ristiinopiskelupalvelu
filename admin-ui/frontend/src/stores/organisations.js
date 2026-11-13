import { defineStore } from 'pinia';
import axios from 'axios';

export const useOrganisationStore = defineStore('organisations', {
  state: () => ({
    organisations: [],
    schemaVersions: []
  }),
  actions: {
    async fetchOrganisations() {
      const response = await axios.get('/admin-ui/api/organisations');
      this.organisations = response.data || [];
      return this.organisations;
    },
    async deleteOrganisation(id) {
      await axios.delete(`/admin-ui/api/organisations/${id}`);
      this.organisations = this.organisations.filter(o => o.id !== id);
    },
    async updateOrganisation(id, payload) {
      const response = await axios.put(`/admin-ui/api/organisations/${id}`, payload);
      return response.data;
    },
    async addOrganisation(payload) {
      const response = await axios.post('/admin-ui/api/organisations', payload);
      return response.data;
    },
    async fetchSchemaVersions() {
      const response = await axios.get('/admin-ui/api/organisations/schemaversions');
      this.schemaVersions = response.data || [];
      return this.schemaVersions;
    }
  }
});
