<!--
  #%L
  admin base - UI Apps
  %%
  Copyright (C) 2017 headwire inc.
  %%
  Licensed to the Apache Software Foundation (ASF) under one
  or more contributor license agreements.  See the NOTICE file
  distributed with this work for additional information
  regarding copyright ownership.  The ASF licenses this file
  to you under the Apache License, Version 2.0 (the
  "License"); you may not use this file except in compliance
  with the License.  You may obtain a copy of the License at
  
  http://www.apache.org/licenses/LICENSE-2.0
  
  Unless required by applicable law or agreed to in writing,
  software distributed under the License is distributed on an
  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  KIND, either express or implied.  See the License for the
  specific language governing permissions and limitations
  under the License.
  #L%
  -->
<template>
<div class="container">
    <form-wizard 
      v-bind:title="'create a page'" 
      v-bind:subtitle="''" @on-complete="onComplete"
      error-color="#d32f2f"
      color="#546e7a">
        <tab-content title="select template" :before-change="leaveTabOne">
            <fieldset class="vue-form-generator">
                <div class="form-group required">
                    <label>Select Template</label>
                    <ul class="collection">
                        <li class="collection-item"
                            v-for="template in templates"
                            v-on:click.stop.prevent="selectTemplate(null, template.path)"
                            v-bind:class="isSelected(template.path) ? 'active' : ''">
                            <admin-components-action v-bind:model="{ command: 'selectTemplate', target: template.path }">{{template.name}}</admin-components-action>
                        </li>
                    </ul>
                    <div v-if="!formmodel.templatePath" class="errors">
                        <span track-by="index">selection required</span>
                    </div>
                </div>
            </fieldset>
        </tab-content>
        <tab-content title="choose name" :before-change="leaveTabTwo">
            <vue-form-generator 
              :model   ="formmodel"
              :schema  ="nameSchema"
              :options ="formOptions"
              ref      ="nameTab">
            </vue-form-generator>
        </tab-content>
        <tab-content title="verify">
            <vue-form-generator :model="formmodel"
                                :schema="pageSchema"
                                :options="formOptions"
                                ref="nameTab">

            </vue-form-generator>
        </tab-content>
    </form-wizard>
</div>
</template>

<script>
    export default {
        props: ['model'],
        data:
            function() {
                return {
                    formmodel: {
                        path: $perAdminApp.getNodeFromView('/state/tools/pages'),
                        name: '',
                        templatePath: ''

                    },
                    formOptions: {
                        validationErrorClass: "has-error",
                        validationSuccessClass: "has-success",
                        validateAfterChanged: true,
                        focusFirstField: true
                    },
                    nameSchema: {
                      fields: [
                        {
                            type: "input",
                            inputType: "text",
                            label: "Page Name",
                            model: "name",
                            required: true,
                            validator: this.nameAvailable
                        }
                      ]
                    }
                }

        }
        ,
        computed: {
            pageSchema: function() {
                console.log('getting schema')
                if(this.formmodel.templatePath !== '') {
                    const definitions = $perAdminApp.getNodeFromView('/admin/componentDefinitions')
                    if(definitions) {
                        // todo: component should be resolved through the template
                        const comp = 'pagerender-vue-structure-page'
                        const def = $perAdminApp.getNodeFromView('/admin/componentDefinitions')[comp]
                        console.log(def)
                        return def
                    }
                }
            },
            templates: function() {
                const templates = $perAdminApp.getNodeFromViewOrNull('/admin/templates/data')
                const siteRootParts = this.formmodel.path.split('/').slice(0,4)
                siteRootParts[2] = 'templates'
                const siteRoot = siteRootParts.join('/')
                console.log(siteRoot)
                return templates.filter( (item) => item.path.startsWith(siteRoot))
            }
        }
        ,
        methods: {
            selectTemplate: function(me, target){
                if(me === null) me = this
                me.formmodel.templatePath = target
            },
            isSelected: function(target) {
                return this.formmodel.templatePath === target
            },
            onComplete: function() {
                $perAdminApp.stateAction('createPage', { parent: this.formmodel.path, name: this.formmodel.name, template: this.formmodel.templatePath, data: this.formmodel })
            },
            leaveTabOne: function() {
                if('' !== ''+this.formmodel.templatePath) {
                    $perAdminApp.getApi().populateComponentDefinitionFromNode(this.formmodel.templatePath)
                }

                return ! ('' === ''+this.formmodel.templatePath)
            },
            nameAvailable(value) {
                if(!value || value.length === 0) {
                    return ['name is required']
                } else {
                    const folder = $perAdminApp.findNodeFromPath($perAdminApp.getView().admin.nodes, this.formmodel.path)
                    for(let i = 0; i < folder.children.length; i++) {
                        if(folder.children[i].name === value) {
                            return ['name aready in use']
                        }
                    }
                    return []
                }
            },
            leaveTabTwo: function() {
                return this.$refs.nameTab.validate()
            }

        }
    }
</script>
