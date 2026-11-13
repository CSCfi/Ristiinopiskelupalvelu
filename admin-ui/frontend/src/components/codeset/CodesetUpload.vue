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
          <input type="file" id="file" ref="fileInput" v-on:change="handleFileUpload()" />
        </v-flex>
      </v-card-text>
      <v-card-actions>
        <v-btn @click="submitFile" small>{{ $t('send') }}</v-btn>
      </v-card-actions>

      <v-alert v-if="success" type="success">
        {{ $t('codesetUpdatedMessage') }}
      </v-alert>
      <v-alert v-if="error" type="error">
        {{ $t('codesetFailedMessage') }}
      </v-alert>
    </v-card>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useCodesetStore } from '../../stores/codesets';

const emit = defineEmits(['refresh']);
const fileInput = ref(null);
const file = ref('');
const success = ref(false);
const error = ref(false);
const codesetStore = useCodesetStore();

const submitFile = () => {
  const formData = new FormData();
  formData.append('file', file.value);
  codesetStore
    .importCodes(formData)
    .then(() => {
      success.value = true;
      emit('refresh');
      setTimeout(() => {
        success.value = false;
      }, 2000);
    })
    .catch(() => {
      error.value = true;
      setTimeout(() => {
        error.value = false;
      }, 2000);
    });
};

const handleFileUpload = () => {
  file.value = fileInput.value?.files?.[0] || '';
};
</script>
