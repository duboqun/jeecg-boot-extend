<template>
  <div>
    <x-form ref="xForm" v-model="formData" :config="formConfig">
      <template #executionListener>
        <el-badge :value="executionListenerLength">
          <el-button size="small" @click="dialogName = 'executionListenerDialog'">编辑</el-button>
        </el-badge>
      </template>
      <template #taskListener>
        <el-badge :value="taskListenerLength">
          <el-button size="small" @click="dialogName = 'taskListenerDialog'">编辑</el-button>
        </el-badge>
      </template>
      <template #multiInstance>
        <el-badge :is-dot="hasMultiInstance">
          <el-button size="small" @click="dialogName = 'multiInstanceDialog'">编辑</el-button>
        </el-badge>
      </template>
    </x-form>
    <executionListenerDialog
      v-if="dialogName === 'executionListenerDialog'"
      :element="element"
      :modeler="modeler"
      @close="finishExecutionListener"
    />
    <taskListenerDialog
      v-if="dialogName === 'taskListenerDialog'"
      :element="element"
      :modeler="modeler"
      @close="finishTaskListener"
    />
    <multiInstanceDialog
      v-if="dialogName === 'multiInstanceDialog'"
      :element="element"
      :modeler="modeler"
      @close="finishMultiInstance"
    />
    <a-form :form="nodeForm" class="aprove" >
      <a-form-item :label-col="labelCol" :wrapper-col="wrapperCol" label="人员类型">
        <a-radio-group @change="spryType" v-model="spryTypes" >
            <!-- 0角色 1用户 2部门 3发起人 4发起人的部门负责人-->
          <a-radio value="0"> 根据角色选择 </a-radio>
          <br/>
          <a-radio value="1"> 直接选择人员 </a-radio>
          <br/>
          <a-radio value="2"> 部门 </a-radio>
          <br/>
          <a-radio value="5"> 部门负责人 </a-radio>
          <br/>
          <a-radio value="3">
            发起人
            <a-tooltip placement="topLeft" title="自动获取发起人">
              <a-icon type="exclamation-circle" />
            </a-tooltip>
          </a-radio>
          <br/>
          <a-radio value="4">
            发起人的部门负责人
            <a-tooltip placement="topLeft" title="自动获取发起人所在部门的负责人，即其上级领导。（如果其本身就是部门负责人，则指向发起人自己。）">
              <a-icon type="exclamation-circle" />
            </a-tooltip>
          </a-radio>
          <br/>
          <a-radio value="6">
            表单变量
            <a-tooltip placement="topLeft" title="填写与表单中相对应的变量，role:角色，user:人员：dept:部门：deptManage:部门负责人; 例如：variable:role,variable2:user;">
              <a-icon type="exclamation-circle" />
            </a-tooltip>
          </a-radio>
        </a-radio-group>
      </a-form-item>

      <a-form-item :label-col="labelCol" :wrapper-col="wrapperCol" label="选择角色" v-if="spryTypes ==='0'">
        <j-select-role v-model="asignNode.spry.roleIds" />
      </a-form-item>
      <a-form-item :label-col="labelCol" :wrapper-col="wrapperCol" label="选择人员" v-if="spryTypes ==='1'">
        <!--  通过部门选择用户控件 -->
        <j-select-user-by-dep v-model="asignNode.spry.userIds" :multi="true"></j-select-user-by-dep>
      </a-form-item>
      <a-form-item :label-col="labelCol" :wrapper-col="wrapperCol" label="选择部门" v-if="spryTypes ==='2'">
        <j-select-depart v-model="asignNode.spry.departmentIds" :multi="true"></j-select-depart>
      </a-form-item>
      <a-form-item
        :label-col="labelCol"
        :wrapper-col="wrapperCol"
        label="选择部门负责人"
        v-if="spryTypes === '5'"
      >
        <j-select-depart v-model="asignNode.spry.departmentManageIds" :multi="true"></j-select-depart>
      </a-form-item>
      <a-form-item
        :label-col="labelCol"
        :wrapper-col="wrapperCol"
        label="输入表单变量"
        v-if="spryTypes === '6'"
      >
        <a-input v-model="asignNode.spry.formVariables" :multi="true"></a-input>
      </a-form-item>
    </a-form>
  </div>
</template>

<script>
import mixinPanel from '../../common/mixinPanel'
import executionListenerDialog from './property/executionListener'
import taskListenerDialog from './property/taskListener'
import multiInstanceDialog from './property/multiInstance'
import { commonParse, userTaskParse } from '../../common/parseElement'
import {mapMutations} from 'vuex'
export default {
  components: {
    executionListenerDialog,
    taskListenerDialog,
    multiInstanceDialog
  },
  mixins: [mixinPanel],
  props: {
    users: {
      type: Array,
      required: true
    },
    groups: {
      type: Array,
      required: true
    }
  },
  data() {
    return {
      userTypeOption: [
        { label: '指定人员', value: 'assignee' },
        { label: '候选人员', value: 'candidateUsers' },
        { label: '候选组', value: 'candidateGroups' }
      ],
      dialogName: '',
      executionListenerLength: 0,
      taskListenerLength: 0,
      hasMultiInstance: false,
      formData: {},
      asignNode: {
        nodeId: '',
        spry: {
          //选中的用户
          userIds: '',
          roleIds: '',
          departmentIds: '',
          departmentManageIds: '',
          formVariables: '',
          chooseSponsor: false,
          chooseDepHeader: false
        },
      },
      spryTypes: '1',
      nodeForm: this.$form.createForm(this),
      // 表头
      labelCol: {
        xs: { span: 8 },
        sm: { span: 8 },
      },
      wrapperCol: {
        xs: { span: 16 },
        sm: { span: 16 },
      },
    }
  },
  computed: {
    formConfig() {
      const _this = this
      return {
        inline: false,
        item: [
          {
            xType: 'input',
            name: 'id',
            label: '节点 id',
            rules: [{ required: true, message: 'Id 不能为空' }]
          },
          {
            xType: 'input',
            name: 'name',
            label: '节点名称'
          },
          {
            xType: 'input',
            name: 'documentation',
            label: '节点描述'
          },
          // {
          //   xType: 'slot',
          //   name: 'executionListener',
          //   label: '执行监听器'
          // },
          {
            xType: 'slot',
            name: 'taskListener',
            label: '任务监听器',
            show: !!_this.showConfig.taskListener
          },
          {
            xType: 'select',
            name: 'userType',
            label: '人员类型',
            dic: _this.userTypeOption,
            show: !!_this.showConfig.userType
          },
          {
            xType: 'select',
            name: 'assignee',
            label: '指定人员',
            allowCreate: true,
            filterable: true,
            dic: { data: _this.users, label: 'name', value: 'id' },
            show: !!_this.showConfig.assignee && _this.formData.userType === 'assignee'
          },
          {
            xType: 'select',
            name: 'candidateUsers',
            label: '候选人员',
            multiple: true,
            allowCreate: true,
            filterable: true,
            dic: { data: _this.users, label: 'name', value: 'id' },
            show: !!_this.showConfig.candidateUsers && _this.formData.userType === 'candidateUsers'
          },
          {
            xType: 'select',
            name: 'candidateGroups',
            label: '候选组',
            multiple: true,
            allowCreate: true,
            filterable: true,
            dic: { data: _this.groups, label: 'name', value: 'id' },
            show: !!_this.showConfig.candidateGroups && _this.formData.userType === 'candidateGroups'
          },
          {
            xType: 'slot',
            name: 'multiInstance',
            label: '多实例'
          },
          {
            xType: 'switch',
            name: 'async',
            label: '异步',
            activeText: '是',
            inactiveText: '否',
            show: !!_this.showConfig.async
          },
          {
            xType: 'input',
            name: 'priority',
            label: '优先级',
            show: !!_this.showConfig.priority
          },
          {
            xType: 'input',
            name: 'formKey',
            label: '表单标识key',
            show: !!_this.showConfig.formKey
          },
          {
            xType: 'input',
            name: 'skipExpression',
            label: '跳过表达式',
            show: !!_this.showConfig.skipExpression
          },
          {
            xType: 'switch',
            name: 'isForCompensation',
            label: '是否为补偿',
            activeText: '是',
            inactiveText: '否',
            show: !!_this.showConfig.isForCompensation
          },
          {
            xType: 'switch',
            name: 'triggerable',
            label: '服务任务可触发',
            activeText: '是',
            inactiveText: '否',
            show: !!_this.showConfig.triggerable
          },
          {
            xType: 'switch',
            name: 'autoStoreVariables',
            label: '自动存储变量',
            activeText: '是',
            inactiveText: '否',
            show: !!_this.showConfig.autoStoreVariables
          },
          {
            xType: 'input',
            name: 'ruleVariablesInput',
            label: '输入变量',
            show: !!_this.showConfig.ruleVariablesInput
          },
          {
            xType: 'input',
            name: 'rules',
            label: '规则',
            show: !!_this.showConfig.rules
          },
          {
            xType: 'input',
            name: 'resultVariable',
            label: '结果变量',
            show: !!_this.showConfig.resultVariable
          },
          {
            xType: 'switch',
            name: 'exclude',
            label: '排除',
            activeText: '是',
            inactiveText: '否',
            show: !!_this.showConfig.exclude
          },
          {
            xType: 'input',
            name: 'class',
            label: '类',
            show: !!_this.showConfig.class
          },
          {
            xType: 'datePicker',
            type: 'datetime',
            name: 'dueDate',
            label: '到期时间',
            show: !!_this.showConfig.dueDate
          }
        ]
      }
    }
  },
  watch: {
    // 'formData.userType': function(val, oldVal) {
    //   if (oldVal) {
    //     const types = ['assignee', 'candidateUsers', 'candidateGroups']
    //     types.forEach(type => {
    //       delete this.element.businessObject.$attrs[`flowable:${type}`]
    //       delete this.formData[type]
    //     })
    //   }
    // },
    // 'formData.assignee': function(val) {
    //   if (this.formData.userType !== 'assignee') {
    //     delete this.element.businessObject.$attrs[`flowable:assignee`]
    //     return
    //   }
    //   this.updateProperties({ 'flowable:assignee': val })
    // },
    // 'formData.candidateUsers': function(val) {
    //   if (this.formData.userType !== 'candidateUsers') {
    //     delete this.element.businessObject.$attrs[`flowable:candidateUsers`]
    //     return
    //   }
    //   this.updateProperties({ 'flowable:candidateUsers': val?.join(',') })
    // },
    // 'formData.candidateGroups': function(val) {
    //   if (this.formData.userType !== 'candidateGroups') {
    //     delete this.element.businessObject.$attrs[`flowable:candidateGroups`]
    //     return
    //   }
    //   this.updateProperties({ 'flowable:candidateGroups': val?.join(',') })
    // },
    'formData.async': function(val) {
      if (val === '') val = null
      this.updateProperties({ 'flowable:async': true })
    },
    'formData.dueDate': function(val) {
      if (val === '') val = null
      this.updateProperties({ 'flowable:dueDate': val })
    },
    'formData.formKey': function(val) {
      if (val === '') val = null
      this.updateProperties({ 'flowable:formKey': val })
    },
    'formData.priority': function(val) {
      if (val === '') val = null
      this.updateProperties({ 'flowable:priority': val })
    },
    'formData.skipExpression': function(val) {
      if (val === '') val = null
      this.updateProperties({ 'flowable:skipExpression': val })
    },
    'formData.isForCompensation': function(val) {
      if (val === '') val = null
      this.updateProperties({ 'isForCompensation': val })
    },
    'formData.triggerable': function(val) {
      if (val === '') val = null
      this.updateProperties({ 'flowable:triggerable': val })
    },
    'formData.class': function(val) {
      if (val === '') val = null
      this.updateProperties({ 'flowable:class': val })
    },
    'formData.autoStoreVariables': function(val) {
      if (val === '') val = null
      this.updateProperties({ 'flowable:autoStoreVariables': val })
    },
    'formData.exclude': function(val) {
      if (val === '') val = null
      this.updateProperties({ 'flowable:exclude': val })
    },
    'formData.ruleVariablesInput': function(val) {
      if (val === '') val = null
      this.updateProperties({ 'flowable:ruleVariablesInput': val })
    },
    'formData.rules': function(val) {
      if (val === '') val = null
      this.updateProperties({ 'flowable:rules': val })
    },
    'formData.resultVariable': function(val) {
      if (val === '') val = null
      this.updateProperties({ 'flowable:resultVariable': val })
    },
    'asignNode.spry.userIds': function(val) {
      if (val === '') val = null
      this.updateProperties({ 'flowable:candidateUsers': 'userIds|' + val })
      this.delAsignNode(this.formData.id)
      this.asignNode.nodeId = this.formData.id
      this.addAsignNode(this.asignNode)
    },
    'asignNode.spry.roleIds': function(val) {
      if (val === '') val = null
      this.updateProperties({ 'flowable:candidateUsers': 'roleIds|' + val })
      this.delAsignNode(this.formData.id)
      this.asignNode.nodeId = this.formData.id
      this.addAsignNode(this.asignNode)
      
    },
    'asignNode.spry.departmentIds': function(val) {
      if (val === '') val = null
      this.updateProperties({ 'flowable:candidateUsers': 'departmentIds|' + val })
      this.delAsignNode(this.formData.id)
      this.asignNode.nodeId = this.formData.id
      this.addAsignNode(this.asignNode)
    },
    'asignNode.spry.departmentManageIds': function(val) {
      if (val === '') val = null
      this.updateProperties({ 'flowable:candidateUsers': 'departmentManageIds|' + val })
      this.delAsignNode(this.formData.id)
      this.asignNode.nodeId = this.formData.id
      this.addAsignNode(this.asignNode)
    },
    'asignNode.spry.formVariables': function(val) {
      if (val === '') val = null
      this.updateProperties({ 'flowable:candidateUsers': 'formVariables|' + val })
      this.delAsignNode(this.formData.id)
      this.asignNode.nodeId = this.formData.id
      this.addAsignNode(this.asignNode)
    },
    'asignNode.spry.chooseSponsor': function(val) {
      if (val) {
        this.updateProperties({ 'flowable:candidateUsers': 'chooseSponsor|'})
        this.delAsignNode(this.formData.id)
        this.asignNode.nodeId = this.formData.id
        this.addAsignNode(this.asignNode)
      }
    },
    'asignNode.spry.chooseDepHeader': function(val) {
      if (val) {
        this.updateProperties({ 'flowable:candidateUsers': 'chooseDepHeader|'})
        this.delAsignNode(this.formData.id)
        this.asignNode.nodeId = this.formData.id
        this.addAsignNode(this.asignNode)
      }
    },
  
  },
  created() {
    let cache = commonParse(this.element)
    cache = userTaskParse(cache)
    this.formData = cache

    for (const key in cache) {
      if (key === 'candidateUsers') {
        let asignResult = cache[key]?.split('|') || []
        let asignType = asignResult[0]
        let asignValue = asignResult[1]
        if (asignType == 'roleIds') {
          this.asignNode.spry.roleIds = asignValue;
          this.spryTypes = '0'
        }
        if (asignType == 'userIds') {
          this.asignNode.spry.userIds = asignValue;
          this.spryTypes = '1'
        }
        if (asignType == 'departmentIds') {
          this.asignNode.spry.departmentIds = asignValue;
          this.spryTypes = '2'
        }
        if (asignType == 'departmentManageIds') {
          this.asignNode.spry.departmentManageIds = asignValue;
          this.spryTypes = '5'
        }
        if (asignType == 'formVariable') {
          this.asignNode.spry.formVariable = asignValue;
          this.spryTypes = '6'
        }
        if (asignType == 'chooseSponsor') {
          this.asignNode.spry.chooseSponsor = true;
          this.spryTypes = '3'
        }
        if (asignType == 'chooseDepHeader') {
          this.asignNode.spry.chooseDepHeader = true;
          this.spryTypes = '4'
        }
      }
    }
    // this.computedExecutionListenerLength()
    // this.computedTaskListenerLength()
    this.computedHasMultiInstance()
  },
  methods: {
    ...mapMutations(['addAsignNode', 'delAsignNode']),
    computedExecutionListenerLength() {
      this.executionListenerLength =
        this.element.businessObject.extensionElements?.values?.filter(
          item => item.$type === 'flowable:ExecutionListener'
        ).length ?? 0
    },
    computedTaskListenerLength() {
      this.taskListenerLength =
        this.element.businessObject.extensionElements?.values?.filter(item => item.$type === 'flowable:TaskListener')
          .length ?? 0
    },
    computedHasMultiInstance() {
      if (this.element.businessObject.loopCharacteristics) {
        this.hasMultiInstance = true
      } else {
        this.hasMultiInstance = false
      }
    },
    finishExecutionListener() {
      if (this.dialogName === 'executionListenerDialog') {
        this.computedExecutionListenerLength()
      }
      this.dialogName = ''
    },
    finishTaskListener() {
      if (this.dialogName === 'taskListenerDialog') {
        this.computedTaskListenerLength()
      }
      this.dialogName = ''
    },
    finishMultiInstance() {
      if (this.dialogName === 'multiInstanceDialog') {
        this.computedHasMultiInstance()
      }
      this.dialogName = ''
    },
    spryType(types){
      /* 0角色 1用户 2部门 3发起人 4发起人的部门负责人 5部门负责人*/
      // this.spryTypes = types;
      if (this.spryTypes.indexOf('0')==-1) this.asignNode.spry.roleIds = '';
      if (this.spryTypes.indexOf('1')==-1) this.asignNode.spry.userIds = '';
      if (this.spryTypes.indexOf('2')==-1) this.asignNode.spry.departmentIds = '';
      if (this.spryTypes.indexOf('5')==-1) this.asignNode.spry.departmentManageIds = '';
      if (this.spryTypes.indexOf('6')==-1) this.asignNode.spry.formVariable = '';
      //是否选中发起人
      this.asignNode.spry.chooseSponsor = this.spryTypes.indexOf('3')>-1 ;
      //是否选中发起人的部门领导
      this.asignNode.spry.chooseDepHeader = this.spryTypes.indexOf('4')>-1 ;

      console.log("this.asignNode.spry",this.asignNode.spry)
    },
  }
}
</script>


<style>
  .aprove{
    margin-top: 20px;
  }
</style>
