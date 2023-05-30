<template>
  <div class="codeset-container">
    <v-card>
      <v-card-title primary-title>
        <h3>{{ $t('codesetUploadInfo') }}</h3>
      </v-card-title>
      <v-card-text>
        <span>{{ $t('codesetUploadInfo2') }}</span>
      </v-card-text>
      <v-card-text>
        <v-flex xs12 sm6 md3>
          <input type="file" id="file" ref="file" v-on:change="handleFileUpload()" />
        </v-flex>
      </v-card-text>
      <v-card-actions>
        <v-btn @click="submitFile" small>{{ $t('send') }}</v-btn>
      </v-card-actions>

      <v-alert :value="success" type="success">
        {{ $t('codesetUpdatedMessage') }}
      </v-alert>
      <v-alert :value="error" type="error">
        {{ $t('codesetFailedMessage') }}
      </v-alert>
    </v-card>
  </div>
</template>

<script>
import axios from 'axios';
import { setTimeout } from 'timers';

export default {
  data() {
    return {
      codeUrl: null,
      file: '',
      success: false,
      error: false
    };
  },
  methods: {
    submitFile() {
      let formData = new FormData();
      formData.append('file', this.file);
      axios
        .post('/admin-ui/api/codes/import', formData, {
          headers: {
            'Content-Type': 'multipart/form-data'
          }
        })
        .then(() => {
          this.success = true;
          this.$emit('refresh');
          setTimeout(() => {
            this.success = false;
          }, 2000);
        })
        .catch(() => {
          this.error = true;
          setTimeout(() => {
            this.error = false;
          }, 2000);
        });
    },
    handleFileUpload() {
      this.file = this.$refs.file.files[0];
    }
  }
};
</script>
