<template>
  <div>
    <parser
      v-if="formConf.fields.length > 0 && formEditData !== undefined"
      v-loading="loading"
      :is-edit="!isNew"
      :form-conf="formConf"
      :form-edit-data="formEditData"
      :disabled="disabled"
      :btndisabled="btndisabled"
      :task="task"
      @submit="handlerSubmit"
      @resetForm="resetForm"
      @passTask="passTask"
      @backTask="backTask"
    />
  </div>
</template>

<script>
/**
   * 注意：和Parser唯一的区别就是这里仅仅传入表单配置id即可自动加载已配置的表单
   *      数据后渲染表单，
   *      其他业务和Parser保持一致
*/
import parser from '@/components/FormGenerator/components/parser/Parser'
import pick from "lodash.pick"
export default {
  name: "ZBParser",
  components: { parser },
  props: {
    /*全局禁用，可表示查看*/
    disabled: {
      type: Boolean,
      default: false,
      required: false
    },
    /*流程数据*/
    processData: {
      type: Object,
      default: () => {
        return {}
      },
      required: false
    },
    /*是否新增*/
    isNew: {
      type: Boolean,
      default: false,
      required: false
    },
    /*是否处理流程*/
    task: {
      type: Boolean,
      default: false,
      required: false
    }
  },
  data() {
    return {
      loading: false,
      formConf: { fields: [] },
      formData: {},
      formEditData: undefined,
      url: {
        getForm: '/actBusiness/getForm',
        addApply: '/actBusiness/add',
        editForm: '/actBusiness/editForm',
        queryDynamicForm: "/activiti/formComponent/queryDynamicForm",
      },
      btndisabled: false,
    }
  },
  mounted() {
    this.handlerGetFormConfig(this.formId)
    if (!this.isNew) {
      this.handlerGetFormData()
    }
    console.log(this.disabled);
    console.log(this.btndisabled);
  },
  methods: {
    handlerGetFormConfig() {
      const params = {
        businessTable: this.processData.businessTable
        }
      this.loading = true;
      this.getAction(this.url.queryDynamicForm, params).then((res) => {
        if (res.success) {
          let records = res.result||[];
          this.formConf = JSON.parse(records[0].formContent)
        }
        if(res.code===510){
          this.$message.warning(res.message)
        }
        this.loading = false;
      })
    },
    handlerSubmit(formValue) {
      let formData = Object.assign({}, formValue)
      formData.filedNames = Object.keys(formValue).join(",");
      formData.procDefId = this.processData.id;
      formData.procDeTitle = this.processData.name;
      if (!formData.tableName) formData.tableName = this.processData.businessTable;
      var url = this.url.addApply;
      if (!this.isNew) {
        url = this.url.editForm;
      }
      this.btndisabled = true;
      this.postFormAction(url, formData).then((res) => {
        if (res.success) {
          this.$message.success("保存成功！")
          //todo 将表单的数据传给父组件
          this.$emit('afterSubmit', formData)
        }
        else {
          this.$message.error(res.message)
        }
      }).finally(() => {
        this.btndisabled = false;
      })
    },
    resetForm(formValue){
      this.$emit('close')
    },
    /*获取数据*/
    handlerGetFormData() {
      var r = this.processData;
      if (!r.tableId) {
        return
      }
      this.btndisabled = true;
      this.getAction(this.url.getForm, {
        tableId: r.tableId,
        tableName: r.tableName,
      }).then((res) => {
        if (res.success) {
          let formDataInit = res.result;
          formDataInit.tableName = r.tableName;
          this.formEditData = Object.assign({}, formDataInit)
          this.btndisabled = false;
        }
        else {
          this.$message.error(res.message)
        }
      })
    },
    /*通过审批*/
    passTask() {
      this.$emit('passTask')
    },
    /*驳回审批*/
    backTask() {
      this.$emit('backTask')
    },
  }
}
</script>

<style scoped>

</style>
