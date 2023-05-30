<template>
  <div v-if="currNetwork">
    <v-alert class="alert" :value="success" type="success">
      {{ $t('networkUpdated') }}
    </v-alert>
    <v-alert class="alert" :value="error" type="error">
      {{ $t('networkUpdateError') }}
    </v-alert>
    <v-alert class="alert" :value="errorEmptyFields" type="error">
      {{ $t('networkFormError') }}
    </v-alert>
    <v-alert class="alert" :value="errorOrganisations" type="error">
      {{ $t('networkFormErrorOrganisations') }}
    </v-alert>

    <v-form id="network-form" ref="form" :lazy-validation="false" v-model="valid">
      <v-btn
        v-if="!newNetwork"
        small
        color="success"
        class="save right"
        @click="saveNetworkClicked"
      >
        {{ $t('save') }}
      </v-btn>
      <v-btn
        v-if="newNetwork"
        small
        color="success"
        class="save right"
        @click="saveNewNetworkClicked"
      >
        {{ $t('saveNew') }}
      </v-btn>

      <v-text-field
        v-model="currNetwork.id"
        :label="$t('networkIdentifier')"
        disabled
      ></v-text-field>

      <v-switch
        v-model="currNetwork.published"
        :label="$t('networkPublished')"
        :disabled="alreadyPublished"
      ></v-switch>

      <v-select
        v-model="currNetwork.networkType"
        :items="networkTypes"
        filled
        :rules="[requiredRule]"
        :label="$t('network.type')"
        required
      ></v-select>

      <v-text-field
        v-model="currNetwork.abbreviation"
        :rules="[requiredRule]"
        :label="$t('abbreviation')"
        required
      ></v-text-field>

      <div class="layout wrap">
        <h2 class="header-left">{{ $t('network.name') }}</h2>
      </div>
      <v-text-field
        v-model="currNetwork.name.values.fi"
        :label="$t('fi')"
        :rules="[nameRequired(currNetwork.name)]"
      ></v-text-field>
      <v-text-field
        v-model="currNetwork.name.values.sv"
        :label="$t('sv')"
        :rules="[nameRequired(currNetwork.name)]"
      ></v-text-field>
      <v-text-field
        v-model="currNetwork.name.values.en"
        :label="$t('en')"
        :rules="[nameRequired(currNetwork.name)]"
      ></v-text-field>

      <div class="layout wrap">
        <h2 class="header-left">{{ $t('network.description') }}</h2>
      </div>
      <v-textarea v-model="currNetwork.description.values.fi" :label="$t('fi')"></v-textarea>
      <v-textarea v-model="currNetwork.description.values.sv" :label="$t('sv')"></v-textarea>
      <v-textarea v-model="currNetwork.description.values.en" :label="$t('en')"></v-textarea>

      <div class="layout wrap">
        <h2 class="header-left">{{ $t('network.furtherInformation') }}</h2>
      </div>
      <v-textarea v-model="currNetwork.furtherInformation.values.fi" :label="$t('fi')"></v-textarea>
      <v-textarea v-model="currNetwork.furtherInformation.values.sv" :label="$t('sv')"></v-textarea>
      <v-textarea v-model="currNetwork.furtherInformation.values.en" :label="$t('en')"></v-textarea>

      <div class="layout wrap">
        <h2 class="header-left">{{ $t('organisations') }}</h2>
        <v-btn class="btn-right" flat @click="addOrg">
          {{ $t('addOrganisation') }}
        </v-btn>
      </div>
      <v-layout
        class="organisation"
        row
        wrap
        v-for="(org, ix) in currNetwork.organisations"
        :key="ix"
      >
        <v-flex xs6>
          <v-select
            v-model="org.organisationTkCode"
            :items="organisationMenuItems"
            filled
            :label="$t('organisationIdentifier')"
            :rules="[requiredRule]"
            :disabled="currentUserOrganisation(org.organisationTkCode)"
            required
            @change="filterOrganisationList()"
          ></v-select>
        </v-flex>
        <v-flex xs6>
          <v-switch
            v-model="org.isCoordinator"
            :label="$t('isCoordinator')"
            :disabled="currentUserOrganisation(org.organisationTkCode)"
          ></v-switch>
        </v-flex>
        <v-flex xs4>
          <v-menu
            ref="dateStart"
            v-model="menuItems[ix + 'modelMenuStart']"
            :close-on-content-click="false"
            :nudge-right="40"
            :return-value.sync="org.validityInNetwork.start"
            lazy
            transition="scale-transition"
            offset-y
            full-width
            min-width="290px"
          >
            <template v-slot:activator="{ on }">
              <v-text-field
                v-model="org.validityInNetwork.start"
                :label="$t('startDate')"
                prepend-icon="event"
                readonly
                :rules="[requiredRule]"
                v-on="on"
              ></v-text-field>
            </template>
            <v-date-picker
              :locale="locale"
              v-model="org.validityInNetwork.start"
              no-title
              scrollable
              @input="
                $refs.dateStart[ix].save(computedDateFormattedMomentjs(org.validityInNetwork.start))
              "
            >
            </v-date-picker>
          </v-menu>
        </v-flex>
        <v-flex xs4>
          <v-menu
            ref="dateEnd"
            v-model="menuItems[ix + 'modelMenuEnd']"
            :close-on-content-click="false"
            :nudge-right="40"
            :return-value.sync="org.validityInNetwork.end"
            lazy
            transition="scale-transition"
            offset-y
            full-width
            min-width="290px"
          >
            <template v-slot:activator="{ on }">
              <v-text-field
                v-model="org.validityInNetwork.end"
                :label="$t('endDate')"
                :rules="[validityEndRequired(org.validityInNetwork)]"
                prepend-icon="event"
                readonly
                :disabled="org.validityInNetwork.continuity === continuityIndetifitely"
                v-on="on"
              ></v-text-field>
            </template>
            <v-date-picker
              :locale="locale"
              v-model="org.validityInNetwork.end"
              no-title
              scrollable
              @input="
                $refs.dateEnd[ix].save(computedDateFormattedMomentjs(org.validityInNetwork.end))
              "
            >
            </v-date-picker>
          </v-menu>
        </v-flex>
        <v-flex xs12>
          <v-select
            v-model="org.validityInNetwork.continuity"
            :items="continuity"
            filled
            :label="$t('continuity')"
            :rules="[requiredRule]"
            @change="validityContinuityChanged($event, org.validityInNetwork)"
            required
          ></v-select>

          <v-btn
            class="btn-right"
            flat
            color="error"
            :disabled="currentUserOrganisation(org.organisationTkCode)"
            @click="deleteOrg(ix)"
          >
            {{ $t('delete') }}
          </v-btn>
        </v-flex>
      </v-layout>

      <div class="layout wrap">
        <h2 class="header-left">{{ $t('network.validity') }}</h2>
      </div>
      <div>
        <v-menu
          ref="dateValidityStart"
          v-model="menuItems['modelMenuValidityStart']"
          :close-on-content-click="false"
          :nudge-right="40"
          :return-value.sync="currNetwork.validity.start"
          lazy
          transition="scale-transition"
          offset-y
          full-width
          min-width="290px"
        >
          <template v-slot:activator="{ on }">
            <v-text-field
              v-model="currNetwork.validity.start"
              :label="$t('startDate')"
              prepend-icon="event"
              :rules="[requiredRule]"
              readonly
              v-on="on"
            ></v-text-field>
          </template>
          <v-date-picker
            :locale="locale"
            v-model="currNetwork.validity.start"
            no-title
            scrollable
            @input="
              $refs.dateValidityStart.save(
                computedDateFormattedMomentjs(currNetwork.validity.start)
              )
            "
          >
          </v-date-picker>
        </v-menu>

        <v-menu
          ref="dateValidityEnd"
          v-model="menuItems['modelMenuValidityEnd']"
          :close-on-content-click="false"
          :nudge-right="40"
          :return-value.sync="currNetwork.validity.end"
          lazy
          transition="scale-transition"
          offset-y
          full-width
          min-width="290px"
        >
          <template v-slot:activator="{ on }">
            <v-text-field
              v-model="currNetwork.validity.end"
              :label="$t('endDate')"
              prepend-icon="event"
              :rules="[validityEndRequired(currNetwork.validity)]"
              :disabled="currNetwork.validity.continuity === continuityIndetifitely"
              readonly
              clearable
              v-on="on"
            ></v-text-field>
          </template>
          <v-date-picker
            :locale="locale"
            v-model="currNetwork.validity.end"
            no-title
            scrollable
            @input="
              $refs.dateValidityEnd.save(computedDateFormattedMomentjs(currNetwork.validity.end))
            "
          >
          </v-date-picker>
        </v-menu>

        <v-select
          v-model="currNetwork.validity.continuity"
          :items="continuity"
          filled
          :label="$t('continuity')"
          :rules="[requiredRule]"
          required
          @change="validityContinuityChanged($event, currNetwork.validity)"
        ></v-select>
      </div>
      <div class="layout wrap">
        <h2 class="header-left">{{ $t('targetGroups') }}</h2>
        <v-btn class="btn-right" flat @click="addTargetGroup">
          {{ $t('addTargetGroup') }}
        </v-btn>
      </div>
      <v-layout
        class="targetGroup"
        row
        wrap
        v-for="(group, index) in currNetwork.targetGroups"
        :key="index + '-targetGroup'"
      >
        <v-flex xs4>
          <v-select
            v-model="group.key"
            :items="studyRightTypeCodeKeyValues"
            :label="$t('codeKey')"
            :rules="[requiredRule]"
          ></v-select>
        </v-flex>
        <v-flex xs4>
          <v-text-field
            v-model="group.codeSetKey"
            :label="$t('codeSetKey')"
            :disabled="true"
            :readonly="true"
            :rules="[requiredRule]"
          ></v-text-field>
        </v-flex>

        <v-flex xs4>
          <v-btn color="error" flat @click="deleteTargetGroup(index)">
            {{ $t('delete') }}
          </v-btn>
        </v-flex>
      </v-layout>

      <div class="layout wrap">
        <h2 class="header-left">{{ $t('restrictions') }}</h2>
        <v-btn class="btn-right" flat @click="addRestriction">
          {{ $t('addRestriction') }}
        </v-btn>
      </div>
      <v-layout
        class="restriction"
        row
        wrap
        align-center
        justify-space-between
        v-for="(restriction, index) in currNetwork.restrictions"
        :key="index + '-restriction'"
      >
        <v-flex xs4>
          <v-select
            v-model="restriction.key"
            :items="studyRightTypeCodeKeyValues"
            :label="$t('codeKey')"
            :rules="[requiredRule]"
          ></v-select>
        </v-flex>

        <v-flex xs4>
          <v-text-field
            v-model="restriction.codeSetKey"
            :disabled="true"
            :readonly="true"
            :label="$t('codeSetKey')"
            :rules="[requiredRule]"
          ></v-text-field>
        </v-flex>

        <v-flex xs4>
          <v-btn color="error" flat @click="deleteRestriction(index)">
            {{ $t('delete') }}
          </v-btn>
        </v-flex>
      </v-layout>

      <div class="layout wrap">
        <h2 class="header-left">{{ $t('expenses') }}</h2>
      </div>
      <v-switch
        v-model="currNetwork.expenses.pay"
        :label="$t('pay')"
        @change="payChanged()"
      ></v-switch>
      <v-select
        v-model="currNetwork.expenses.pricingBasis"
        :items="pricingBasis"
        filled
        :label="$t('pricingBasis')"
        :rules="[expensesRequired]"
        :disabled="!currNetwork.expenses.pay"
      ></v-select>
      <v-text-field
        v-model="currNetwork.expenses.price"
        :label="$t('price')"
        :rules="[expensesRequired]"
        :disabled="!currNetwork.expenses.pay"
      ></v-text-field>
    </v-form>
    <v-dialog v-model="publishDialogVisible" width="600px">
      <v-card>
        <v-card-title class="headline">
          {{ $t('networkPublishConfirmation') }}
        </v-card-title>
        <v-card-text>
          {{ $t('networkPublishConfirmationInfo') }}
        </v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn color="green darken-1" flat="flat" @click="publishDialogVisible = false">
            {{ $t('cancel') }}
          </v-btn>
          <v-btn v-if="!newNetwork" color="green darken-1" flat="flat" @click="saveNetwork">
            {{ $t('yes') }}
          </v-btn>
          <v-btn v-if="newNetwork" color="green darken-1" flat="flat" @click="saveNewNetwork">
            {{ $t('yes') }}
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<script>
import axios from 'axios';
import emptyNetwork from '../../assets/networkSchemas/emptyNetwork.json';
import moment from 'moment';
import _ from 'lodash';
import languageUtils from '../../utils/languageUtils';

export default {
  computed: {},
  components: {},
  props: {
    value: {
      default() {
        return { id: '' };
      }
    },
    active: {
      default() {
        return '';
      }
    },
    locale: {
      default() {
        return 'fi';
      }
    },
    newNetwork: {
      default() {
        return false;
      }
    },
    user: Object
  },
  methods: {
    nameRequired(name) {
      let noNamesGiven = true;
      Object.keys(name.values).forEach(function(key) {
        if (name.values[key]) {
          noNamesGiven = false;
          return;
        }
      });
      return noNamesGiven ? this.$t('nameRequired') : false;
    },
    validityEndRequired(validity) {
      if (validity.continuity === this.continuityFixed && !validity.end) {
        return this.$t('requiredField');
      }
      return false;
    },
    requiredRule(value) {
      return !!value || this.$t('requiredField');
    },
    expensesRequired(value) {
      if (this.currNetwork.expenses.pay && !value) {
        return this.$t('expensesRequired');
      }
      return false;
    },
    resetValidation() {
      if (!this.$refs.form) {
        return;
      }
      this.$refs.form.resetValidation();
    },
    computedDateFormattedMomentjs(date) {
      return date ? moment(date).format('YYYY-MM-DDTHH:mm:ssZ') : '';
    },
    cleanUpData() {
      delete this.currNetwork.version;
      delete this.currNetwork.deleted;
      delete this.currNetwork.deletedTimestamp;
    },
    saveNetworkClicked() {
      if (this.currNetwork.published && !this.alreadyPublished) {
        this.publishDialogVisible = true;
      } else {
        this.saveNetwork();
      }
    },
    async saveNetwork() {
      this.publishDialogVisible = false;
      this.cleanUpData();
      if (this.validate()) {
        try {
          await axios.post('/admin-ui/api/networks/update', this.currNetwork);
          this.$emit('savedNetwork');
          this.alreadyPublished = this.currNetwork.published;
          this.success = true;
          setTimeout(() => {
            this.success = false;
          }, 5000);
        } catch (err) {
          this.error = true;
          setTimeout(() => {
            this.error = false;
          }, 5000);
        }
      }
    },
    saveNewNetworkClicked() {
      if (this.currNetwork.published) {
        this.publishDialogVisible = true;
      } else {
        this.saveNewNetwork();
      }
    },
    async saveNewNetwork() {
      this.publishDialogVisible = false;
      this.cleanUpData();
      if (this.validate()) {
        try {
          await axios.post('/admin-ui/api/networks/add', this.currNetwork);
          this.$emit('savedNewNetwork', this.$event, this.currNetwork.id);
          this.alreadyPublished = this.currNetwork.published;
          this.success = true;
          setTimeout(() => {
            this.success = false;
          }, 5000);
        } catch (err) {
          this.error = true;
          setTimeout(() => {
            this.error = false;
          }, 5000);
        }
      }
    },
    async addOrg() {
      await this.updateOrganisations();
      this.currNetwork.organisations.push({
        organisationTkCode: '',
        isCoordinator: false,
        validityInNetwork: {
          start: '',
          end: ''
        }
      });
    },
    currentUserOrganisation(organisationId) {
      return this.user.userOrganisation ? this.user.userOrganisation.id === organisationId : false;
    },
    deleteOrg(index) {
      if (this.currNetwork.organisations && this.currNetwork.organisations.length > 0) {
        this.currNetwork.organisations.splice(index, 1);
        this.filterOrganisationList();
      }
    },
    addTargetGroup() {
      if (!this.currNetwork.targetGroups) {
        this.currNetwork.targetGroups = [];
      }
      this.currNetwork.targetGroups.push({ key: '', codeSetKey: this.studyRightTypeCodeSet });
    },
    deleteTargetGroup(index) {
      if (this.currNetwork.targetGroups && this.currNetwork.targetGroups.length > 0) {
        this.currNetwork.targetGroups.splice(index, 1);
      }
    },
    addRestriction() {
      if (!this.currNetwork.restrictions) {
        this.currNetwork.restrictions = [];
      }
      this.currNetwork.restrictions.push({ key: '', codeSetKey: this.studyRightTypeCodeSet });
    },
    deleteRestriction(index) {
      if (this.currNetwork.restrictions || this.currNetwork.restrictions.length > 0) {
        this.currNetwork.restrictions.splice(index, 1);
      }
    },
    filterOrganisationList() {
      _.forEach(this.organisationMenuItems, menuItem => {
        menuItem.disabled = false;
      });

      _.forEach(this.currNetwork.organisations, org => {
        let menuItem = _.find(
          this.organisationMenuItems,
          menuOrg => menuOrg.value === org.organisationTkCode
        );

        if (menuItem) {
          menuItem.disabled = true;
        }
      });
    },
    validate() {
      if (this.$refs.form.validate()) {
        if (this.currNetwork.organisations.length > 1) {
          return true;
        } else {
          this.errorOrganisations = true;
          setTimeout(() => {
            this.errorOrganisations = false;
          }, 4000);
        }
      } else {
        this.errorEmptyFields = true;
        setTimeout(() => {
          this.errorEmptyFields = false;
        }, 4000);
        return false;
      }
    },
    validityContinuityChanged(item, validity) {
      if (item === this.continuityIndetifitely) {
        validity.end = '';
      }
    },
    payChanged(pay) {
      if (!pay) {
        this.currNetwork.expenses.pricingBasis = null;
        this.currNetwork.expenses.price = null;
        this.resetValidation();
      }
    },
    localizeTexts() {
      this.networkTypes = [
        { text: this.$t('curriculumLevel'), value: 'CURRICULUM_LEVEL' },
        { text: this.$t('freedomOfChoice'), value: 'FREEDOM_OF_CHOICE' }
      ];

      this.continuity = [
        { text: this.$t('indefinitely'), value: this.continuityIndetifitely },
        { text: this.$t('fixed'), value: this.continuityFixed }
      ];

      this.pricingBasis = [
        { text: this.$t('euroPerCredit'), value: 'EURO_PER_CREDIT' },
        { text: this.$t('otherPricingBasis'), value: 'OTHER_PRICING_BASIS' }
      ];
    },
    async updateOrganisations() {
      try {
        let organisationResponse = await axios.get('/admin-ui/api/organisations');
        this.organisations = organisationResponse.data;
      } catch (err) {
        console.log('Unable to get organisations.', err);
      }

      this.organisationMenuItems = [];
      _.forEach(this.organisations, org => {
        let name = languageUtils.resolveText(this.locale, org.organisationName.values);

        this.organisationMenuItems.push({
          text: `${name}(${org.id})`,
          value: org.id
        });
      });

      _.sortBy(this.organisationMenuItems, 'text');
    }
  },
  data: () => ({
    continuityFixed: 'FIXED',
    continuityIndetifitely: 'INDEFINITELY',
    studyRightTypeCodeSet: 'study_right_type',
    studyRightTypeCodeKeyValues: [],
    menuItems: [],
    valid: false,
    currNetwork: null,
    emptyNetwork: emptyNetwork,
    error: false,
    errorEmptyFields: false,
    errorOrganisations: false,
    success: false,
    organisations: [],
    organisationMenuItems: [],
    networkTypes: [],
    continuity: [],
    pricingBasis: [],
    alreadyPublished: false,
    publishDialogVisible: false
  }),
  watch: {
    value: function(val) {
      if (!val) {
        // if value is null, currently open network was deleted -> null currNetwork to hide editor
        this.currNetwork = null;
        return;
      }
      // Takes the base from emptyNetwork
      this.currNetwork = { ...this.emptyNetwork, ...val };
      // Null checks, so that form is complete even if some nested objects are null
      if (this.currNetwork.published == null) {
        this.currNetwork.published = false;
      }
      if (this.currNetwork.furtherInformation == null) {
        this.currNetwork.furtherInformation = this.emptyNetwork.furtherInformation;
      }
      if (this.currNetwork.description == null) {
        this.currNetwork.description = this.emptyNetwork.description;
      }
      if (this.currNetwork.validity == null) {
        this.currNetwork.validity = this.emptyNetwork.validity;
      }
      if (this.currNetwork.expenses == null) {
        this.currNetwork.expenses = this.emptyNetwork.expenses;
      }
      this.alreadyPublished = this.currNetwork.published;
      this.resetValidation();
      this.filterOrganisationList();
    },
    locale: function() {
      this.localizeTexts();
    }
  },
  created: async function() {
    await this.updateOrganisations();
    try {
      let studyRightTypeCodesResponse = await axios.get(
        `/admin-ui/api/codes/codesets/${this.studyRightTypeCodeSet}`
      );
      this.studyRightTypeCodeKeyValues = studyRightTypeCodesResponse.data.map(code => code.key);
    } catch (err) {
      console.log(`Unable to get codeset '${this.studyRightTypeCodeSet}' values`);
    }
    this.localizeTexts();
  }
};
</script>

<style lang="css">
form {
  margin: 10px;
}
.organisation {
  border: 2px solid #d4d4d4;
  padding: 5px !important;
  margin-bottom: 5px !important;
}
#form-base {
  display: block !important;
}
#network-form {
  margin-top: 0;
  padding: 8px 0px;
}
.btn-right {
  width: 30%;
  float: right;
}
.header-left {
  width: 60%;
  float: left;
  margin-top: 8px;
}
.error-validation {
  border: 1px solid rgb(255, 88, 88) !important;
  border-radius: 3px;
}
label {
  padding: 0 8px;
}
.alert {
  margin-top: 0;
}
.theme--light.v-select .v-select__selection--disabled {
  color: inherit
}
</style>
