<template>
  <themeclean-components-block v-bind:model="model">
    <div>
      <!-- Tab List -->
      <ul class="nav nav-pills d-flex justify-content-center" id="myTab" role="tablist">
        <li class="nav-item" v-for="(item,i) in model.tabs" :key="i">
          <a data-toggle="pill" role="tab" aria-expanded="true" v-bind:href="`#tab${_uid}${parseInt(i)+1}`"
          v-bind:class="i == 0 ? 'nav-link active' : 'nav-link'"
          v-bind:id="`tab-control-${_uid}${parseInt(i)+1}`" v-bind:aria-controls="`tab${_uid}${parseInt(i)+1}`"
          v-html="item.title"></a>
        </li>
      </ul>
      <div class="perIsEditAndEmpty" v-if="isEditAndEmpty">no content defined for component</div>
      <!-- Tab Content -->
      <div class="tab-content" id="myTabContent">
        <div role="tabpanel" v-for="(item,i) in model.tabs" :key="i" v-bind:id="`tab${_uid}${parseInt(i)+1}`"
        v-bind:aria-labelledby="`tablabel${_uid}${parseInt(i)+1}`" v-bind:class="i == 0 ? 'tab-pane fade show active' : 'tab-pane fade'">
          <div class="d-flex justify-content-center">
            <div class="article col-12 col-md-8 py-3" v-html="item.text"></div>
          </div>
        </div>
      </div>
    </div>
  </themeclean-components-block>
</template>

<script>
    export default {
        props: ['model']
    }
</script>

