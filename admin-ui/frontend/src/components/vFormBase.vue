<template>
  <div :id="refId" class="wrap" v-resize.quiet="onResize">
    <template v-for="(obj, index) in flatCombinedArraySorted" :key="index">
      <div
        v-if="!obj.schema.hidden"
        class="legacy-flex"
        :class="getClassName(obj)"
        v-touch="{
          left: () => onSwipe('left', obj),
          right: () => onSwipe('right', obj),
          up: () => onSwipe('up', obj),
          down: () => onSwipe('down', obj)
        }"
      >
        <slot :name="getTypeTopSlot(obj)"></slot>
        <slot :name="getKeyTopSlot(obj)"></slot>

        <slot :name="getTypeItemSlot(obj)">
          <slot :name="getKeyItemSlot(obj)">
            <v-menu
              v-if="obj.schema.type === 'time'"
              :close-on-content-click="false"
              :nudge-right="32"
              transition="scale-transition"
              offset-y
              full-width
              min-width="290px"
            >
              <template #activator="{ props }">
                <v-text-field
                  v-bind="{ ...obj.schema, ...props }"
                  :value="setValue(obj)"
                  @focus="onFocus($event, obj)"
                  @input="onInput($event, obj)"
                ></v-text-field>
              </template>
              <v-time-picker
                :model-value="setValue(obj)"
                @focus="onFocus($event, obj)"
                @update:model-value="onInput($event, obj)"
              ></v-time-picker>
            </v-menu>

            <v-menu
              v-else-if="obj.schema.type === 'date'"
              :close-on-content-click="false"
              :nudge-right="32"
              transition="scale-transition"
              offset-y
              full-width
              min-width="290px"
            >
              <template #activator="{ props }">
                <v-text-field
                  v-bind="{ ...obj.schema, ...props }"
                  :value="setValue(obj) ? formatDate(new Date(setValue(obj))) : setValue(obj)"
                  @focus="onFocus($event, obj)"
                  @input="onInput($event, obj)"
                ></v-text-field>
              </template>
              <v-date-picker
                :locale="obj.schema.locale"
                :model-value="setValue(obj) ? formatDate(new Date(setValue(obj))) : setValue(obj)"
                @focus="onFocus($event, obj)"
                @update:model-value="saveDate($event, obj)"
              ></v-date-picker>
            </v-menu>

            <v-radio-group
              v-else-if="obj.schema.type === 'radio'"
              v-bind="obj.schema"
              :model-value="setValue(obj)"
              @change="onInput($event, obj)"
            >
              <v-radio
                v-for="(o, ix) in obj.schema.options"
                v-bind="obj.schema"
                :key="ix"
                :label="o"
                :value="o"
              ></v-radio>
            </v-radio-group>

            <template v-else-if="obj.schema.type === 'array'">
              <div
                v-bind="obj.schema"
                :value="setValue(obj)"
                v-for="(item, ix) in setValue(obj)"
                :key="ix"
              >
                <slot :name="getKeyArraySlot(obj)" v-bind:item="item">
                  <v-form-base
                    v-bind="obj.schema"
                    :id="`${id}-${obj.key}-${ix}`"
                    :value="item"
                    :schema="obj.schema.schema"
                  >
                    <template #slot-bottom-key-validityInNetwork-redundant>
                      <v-btn class="btn-right" color="error" variant="text" @click="deleteOrg(item)">
                        {{ $t('delete') }}
                      </v-btn>
                    </template>
                  </v-form-base>
                </slot>
              </div>
            </template>

            <template v-else-if="obj.schema.type === 'list'">
              <v-toolbar v-if="obj.schema.label" v-bind="obj.schema" dark>
                <v-toolbar-title>{{ obj.schema.label }}</v-toolbar-title>
              </v-toolbar>
              <v-list v-bind="obj.schema">
                <v-list-item
                  v-for="(item, ix) in obj.schema.items"
                  :key="ix"
                  v-bind="obj.schema"
                  :class="setValue(obj) === item ? 'active' : 'inactive'"
                  @click="onInput(item, obj)"
                >
                  <v-list-item-title>{{ item }}</v-list-item-title>
                </v-list-item>
              </v-list>
            </template>

            <component
              v-else-if="obj.schema.type === 'switch' || obj.schema.type === 'checkbox'"
              :is="mapTypeToComponent(obj.schema.type)"
              v-bind="obj.schema"
              @click:append="onClick($event, obj, append)"
              @click:append-outer="onClick($event, obj, appendOuter)"
              @click:prepend="onClick($event, obj, prepend)"
              @click:prepend-inner="onClick($event, obj, prependInner)"
              :input-value="setValue(obj)"
              @change="onInput($event, obj)"
            />

            <component
              v-else
              :is="mapTypeToComponent(obj.schema.type)"
              v-bind="obj.schema"
              :value="setValue(obj)"
              @focus="onFocus($event, obj)"
              @click:append="onClick($event, obj, append)"
              @click:append-outer="onClick($event, obj, appendOuter)"
              @click:clear="onClick($event, obj, clear)"
              @click:prepend="onClick($event, obj, prepend)"
              @click:prepend-inner="onClick($event, obj, prependInner)"
              @input="onInput($event, obj)"
            />
          </slot>
        </slot>

        <slot :name="getTypeBottomSlot(obj)"></slot>
        <slot :name="getKeyBottomSlot(obj)"></slot>
      </div>

      <v-spacer v-if="obj.schema.spacer" :key="`s-${index}`"></v-spacer>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { get, isPlainObject, isFunction, orderBy } from 'lodash';

const typeToComponent = {
  text: 'v-text-field',
  password: 'v-text-field',
  email: 'v-text-field',
  tel: 'v-text-field',
  url: 'v-text-field',
  color: 'v-text-field',
  search: 'v-text-field',
  number: 'v-text-field',
  file: 'v-text-field',
  list: 'v-list',
  range: 'v-slider'
};

const orderDirection = 'ASC';
const pathDelimiter = '.';
const classKeyDelimiter = '-';
const defaultID = 'form-base';

const itemClassAppendix = 'item';
const typeClassAppendix = 'type';
const keyClassAppendix = 'key';

const arraySlotAppendix = 'slot-array';
const topSlotAppendix = 'slot-top';
const itemSlotAppendix = 'slot-item';
const bottomSlotAppendix = 'slot-bottom';

const clear = 'clear';
const append = 'append';
const appendOuter = 'append-outer';
const prepend = 'prepend';
const prependInner = 'prepend-inner';

const props = defineProps({
  id: {
    type: String,
    default: defaultID
  },
  value: {
    type: [Object, Array, String],
    required: true
  },
  schema: {
    type: [Object, Array],
    required: true
  }
});

const emit = defineEmits(['deleteOrg', 'input', 'update']);

const flatCombinedArray = ref([]);

const refId = computed(() => props.id);
const flatCombinedArraySorted = computed(() =>
  orderBy(flatCombinedArray.value, ['schema.sort'], [orderDirection])
);

const storeStateData = computed(() => {
  updateArrayFromState(props.value, props.schema);
  return props.value;
});

const storeStateSchema = computed(() => {
  updateArrayFromState(props.value, props.schema);
  return props.schema;
});

const deleteOrg = item => {
  emit('deleteOrg', item);
};

const formatDate = date => {
  return (
    date.getFullYear() +
    '-' +
    ('0' + (date.getMonth() + 1)).slice(-2) +
    '-' +
    ('0' + date.getDate()).slice(-2)
  );
};

const formatDateSave = date => {
  return new Date(date).toISOString();
};

const saveDate = (date, obj) => {
  onInput(formatDateSave(date), obj);
};

const mapTypeToComponent = type => {
  return typeToComponent[type] ? typeToComponent[type] : `v-${type}`;
};

const getKeyArraySlot = obj => {
  return getKeyClassNameWithAppendix(obj, arraySlotAppendix + '-key');
};
const getKeyItemSlot = obj => {
  return getKeyClassNameWithAppendix(obj, itemSlotAppendix + '-key');
};
const getKeyTopSlot = obj => {
  return getKeyClassNameWithAppendix(obj, topSlotAppendix + '-key');
};
const getKeyBottomSlot = obj => {
  return getKeyClassNameWithAppendix(obj, bottomSlotAppendix + '-key');
};

const getTypeItemSlot = obj => {
  return getTypeClassNameWithAppendix(obj, itemSlotAppendix + '-type');
};
const getTypeTopSlot = obj => {
  return getTypeClassNameWithAppendix(obj, topSlotAppendix + '-type');
};
const getTypeBottomSlot = obj => {
  return getTypeClassNameWithAppendix(obj, bottomSlotAppendix + '-type');
};

const getKeyClassNameWithAppendix = (obj, appendix) => {
  return `${appendix ? appendix + classKeyDelimiter : ''}${obj.key.replace(/\./g, '-')}`;
};
const getKeyClassName = obj => getKeyClassNameWithAppendix(obj, keyClassAppendix);
const getTypeClassNameWithAppendix = (obj, appendix) => {
  return `${appendix + classKeyDelimiter}${obj.schema.type}`;
};
const getTypeClassName = obj => getTypeClassNameWithAppendix(obj, typeClassAppendix);

const getFlexGridClassName = obj => {
  const keysToGridClassName = key =>
    Object.keys(key)
      .map(k => k + key[k])
      .join(' ');
  return obj.schema.flex
    ? isPlainObject(obj.schema.flex)
      ? keysToGridClassName(obj.schema.flex)
      : `xs${obj.schema.flex}`
    : '';
};
const getOffsetGridClassName = obj => {
  const keysToOffsetClassName = key =>
    Object.keys(key)
      .map(k => `offset-${k}${key[k]}`)
      .join(' ');
  return obj.schema.offset
    ? isPlainObject(obj.schema.offset)
      ? keysToOffsetClassName(obj.schema.offset)
      : `offset-xs${obj.schema.offset}`
    : '';
};
const getOrderGridClassName = obj => {
  const keysToOrderClassName = key =>
    Object.keys(key)
      .map(k => `order-${k}${key[k]}`)
      .join(' ');
  return obj.schema.order
    ? isPlainObject(obj.schema.order)
      ? keysToOrderClassName(obj.schema.order)
      : `order-xs${obj.schema.order}`
    : '';
};

const getGridClassName = obj => {
  return `${getFlexGridClassName(obj)} ${getOffsetGridClassName(obj)} ${getOrderGridClassName(obj)}`;
};

const getClassName = obj => {
  return `${itemClassAppendix} ${getTypeClassName(obj)} ${getKeyClassName(obj)} ${getGridClassName(
    obj
  )}`;
};

const toCtrl = params => {
  return isFunction(params.obj.schema.toCtrl) ? params.obj.schema.toCtrl(params) : params.value;
};
const fromCtrl = params => {
  return isFunction(params.obj.schema.fromCtrl) ? params.obj.schema.fromCtrl(params) : params.value;
};

const setValue = obj => {
  return toCtrl({
    value: obj.value,
    obj,
    data: storeStateData.value,
    schema: storeStateSchema.value
  });
};

const onInput = (value, obj) => {
  value = fromCtrl({
    value,
    obj,
    data: storeStateData.value,
    schema: storeStateSchema.value
  });

  value = value === '' ? null : value;

  setObjectByPath(storeStateData.value, obj.key, value);

  emitValue('input', {
    on: 'input',
    id: refId.value,
    parentId: null,
    key: obj.key,
    value,
    obj,
    data: storeStateData.value,
    schema: storeStateSchema.value
  });
};

const onClick = (event, obj, pos) => {
  emitValue('click', {
    on: 'click',
    id: refId.value,
    parentId: null,
    params: { text: event?.srcElement && event.srcElement.innerText, pos },
    key: obj.key,
    value: obj.value,
    obj,
    event,
    data: storeStateData.value,
    schema: storeStateSchema.value
  });
};

const onFocus = (event, obj) => {
  emitValue('focus', {
    on: 'focus',
    id: refId.value,
    parentId: null,
    key: obj.key,
    value: obj.value,
    obj,
    event,
    data: storeStateData.value,
    schema: storeStateSchema.value
  });
};

const onSwipe = (pos, obj) => {
  emitValue('swipe', {
    on: 'swipe',
    id: refId.value,
    parentId: null,
    key: obj.key,
    value: obj.value,
    obj,
    params: { pos },
    data: storeStateData.value,
    schema: storeStateSchema.value
  });
};

const onResize = () => {
  emitValue('resize', {
    on: 'resize',
    id: refId.value,
    params: { x: window.innerWidth, y: window.innerHeight },
    data: storeStateData.value,
    schema: storeStateSchema.value
  });
};

const emitValue = (emitName, val) => {
  emit(getEventName(emitName), val);
  emit(getEventName('update'), val);
};

const getEventName = eventName => {
  return refId.value !== defaultID ? `${eventName}:${refId.value}` : eventName;
};

const setObjectByPath = (object, path, value) => {
  let pathArray = path.split(pathDelimiter);
  let target = object;
  pathArray.forEach((p, ix) => {
    if (ix === pathArray.length - 1) {
      target[p] = value;
    } else {
      if (!target[p]) {
        target[p] = {};
      }
      target = target[p];
    }
  });
};

const updateArrayFromState = (data, schema) => {
  flatCombinedArray.value.forEach(obj => {
    obj.value = get(data, obj.key, null);
    obj.schema = get(schema, obj.key, null);
  });
};

const flattenObjects = (dat, sch) => {
  let data = {};
  let schema = {};

  Object.keys(dat).forEach(i => {
    if (
      (!Array.isArray(dat[i]) && dat[i] && typeof dat[i] === 'object') ||
      (Array.isArray(dat[i]) && Array.isArray(sch[i]))
    ) {
      let { data: flatData, schema: flatSchema } = flattenObjects(dat[i], sch[i]);
      Object.keys(flatData).forEach(ii => {
        data[i + pathDelimiter + ii] = flatData[ii];
        schema[i + pathDelimiter + ii] = flatSchema[ii];
      });
    } else {
      data[i] = dat[i];
      schema[i] = sch[i];
    }
  });
  return { data, schema };
};

const combineObjectsToArray = ({ data, schema }) => {
  let arr = [];
  Object.keys(data).forEach(key => {
        if (!schema[key]) {
          return;
        }
        if (!isPlainObject(schema[key])) {
          return;
        }
    arr.push({ key, value: data[key], schema: schema[key] });
  });
  return arr;
};

const flattenAndCombineToArray = (data, schema) => {
  let flattenedObjects = flattenObjects(data, schema);
  return combineObjectsToArray(flattenedObjects);
};

onMounted(() => {
  flatCombinedArray.value = flattenAndCombineToArray(storeStateData.value, storeStateSchema.value);
});
</script>
