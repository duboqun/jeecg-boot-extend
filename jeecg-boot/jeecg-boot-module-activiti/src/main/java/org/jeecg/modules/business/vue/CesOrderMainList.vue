<template>
  <a-card :bordered="false">
    <!-- 查询区域 -->
    <div class="table-page-search-wrapper">
      <a-form layout="inline" @keyup.enter.native="searchQuery">
        <a-row :gutter="24">
        </a-row>
      </a-form>
    </div>
    <!-- 查询区域-END -->
    
    <!-- 操作按钮区域 -->
    <div class="table-operator">
      <a-button  :disabled="disabled"  @click="handleAdd" type="primary" icon="plus">新增</a-button>
      <a-button  :disabled="disabled"  type="primary" icon="download" @click="handleExportXls('订单主表')">导出</a-button>
      <a-upload name="file" :showUploadList="false" :multiple="false" :headers="tokenHeader" :action="importExcelUrl" @change="handleImportExcel">
        <a-button  :disabled="disabled"  type="primary" icon="import">导入</a-button>
      </a-upload>
      <!-- 高级查询区域 -->
      <a-button :disabled="!isNew" @click="handleSubmit" type="danger" icon="login">发起流程</a-button>
      <a-button :disabled="!task" @click="passTask" type="danger" icon="check">审批通过</a-button>
      <a-button :disabled="!task" @click="backTask" type="danger" icon="rollback">审批驳回</a-button>
    </div>

    <!-- table区域-begin -->
    <div>
      <div class="ant-alert ant-alert-info" style="margin-bottom: 16px;">
        <i class="anticon anticon-info-circle ant-alert-icon"></i> 已选择 <a style="font-weight: 600">{{ selectedRowKeys.length }}</a>项
        <a style="margin-left: 24px" @click="onClearSelected">清空</a>
      </div>

      <a-table
        ref="table"
        size="middle"
        bordered
        rowKey="id"
        class="j-table-force-nowrap"
        :scroll="{x:true}"
        :columns="columns"
        :dataSource="dataSource"
        :pagination="ipagination"
        :loading="loading"
        :rowSelection="{selectedRowKeys: selectedRowKeys, onChange: onSelectChange}"
        @change="handleTableChange">

        <template slot="htmlSlot" slot-scope="text">
          <div v-html="text"></div>
        </template>
        <template slot="imgSlot" slot-scope="text">
          <span v-if="!text" style="font-size: 12px;font-style: italic;">无图片</span>
          <img v-else :src="getImgView(text)" height="25px" alt="" style="max-width:80px;font-size: 12px;font-style: italic;"/>
        </template>
        <template slot="fileSlot" slot-scope="text">
          <span v-if="!text" style="font-size: 12px;font-style: italic;">无文件</span>
          <a-button
            v-else
            :ghost="true"
            type="primary"
            icon="download"
            size="small"
            @click="downloadFile(text)">
            下载
          </a-button>
        </template>

        <span slot="action" slot-scope="text, record">
          <a @click="handleEdit(record)">编辑</a>

          <a-divider type="vertical" />
          <a-dropdown>
            <a class="ant-dropdown-link">更多 <a-icon type="down" /></a>
            <a-menu slot="overlay">
              <a-menu-item>
                <a @click="handleDetail(record)">详情</a>
              </a-menu-item>
              <a-menu-item  :disabled="disabled"  >
                <a-popconfirm title="确定删除吗?" @confirm="() => handleDelete(record.id)">
                  <a  :disabled="disabled" >删除</a>
                </a-popconfirm>
              </a-menu-item>
            </a-menu>
          </a-dropdown>
        </span>

      </a-table>
    </div>

    <ces-order-main-modal ref="modalForm" @ok="modalFormOk"/>
  </a-card>
</template>

<script>

  import { JeecgListMixin } from '@/mixins/JeecgListMixin'
  import CesOrderMainModal from './modules/CesOrderMainModal'
  import {filterMultiDictText} from '@/components/dict/JDictSelectUtil'
  import '@/assets/less/TableExpand.less'
  import { postAction } from '@/api/manage'

  export default {
    name: "CesOrderMainList",
    mixins:[JeecgListMixin],
    components: {
      CesOrderMainModal
    },
    data () {
      return {
        description: '订单主表管理页面',
        // 表头
        columns: [
          {
            title: '#',
            dataIndex: '',
            key:'rowIndex',
            width:60,
            align:"center",
            customRender:function (t,r,index) {
              return parseInt(index)+1;
            }
          },
          {
            title:'订单编码',
            align:"center",
            dataIndex: 'orderCode'
          },
          {
            title:'下单时间',
            align:"center",
            dataIndex: 'xdDate',
            customRender:function (text) {
              return !text?"":(text.length>10?text.substr(0,10):text)
            }
          },
          {
            title:'订单总额',
            align:"center",
            dataIndex: 'money'
          },
          {
            title:'备注',
            align:"center",
            dataIndex: 'remark'
          },
          {
            title:'流程状态',
            align:"center",
            dataIndex: 'bpmStatus'
          },
          {
            title:'流程标识',
            align:"center",
            dataIndex: 'bpmId'
          },
          {
            title: '操作',
            dataIndex: 'action',
            align:"center",
            fixed:"right",
            width:147,
            scopedSlots: { customRender: 'action' },
          }
        ],
        url: {
          list: "/business/cesOrderMain/list",
          delete: "/business/cesOrderMain/delete",
          deleteBatch: "/business/cesOrderMain/deleteBatch",
          exportXlsUrl: "/business/cesOrderMain/exportXls",
          importExcelUrl: "business/cesOrderMain/importExcel",
          startProcess: '/actBusiness/addOnline'
        },
        flowCode: 'dev_ces_order_main_001',
        dictOptions:{},
        superFieldList:[],
      }
    },
    created() {
      this.getSuperFieldList();
    },
    computed: {
      importExcelUrl: function(){
        return `${window._CONFIG['domianURL']}/${this.url.importExcelUrl}`;
      }
    },
    methods: {
      initDictConfig(){
      },
      startProcess(record){
        this.$confirm({
          title:'提示',
          content:'确认提交流程吗?',
          onOk:()=>{
            let params = {
              flowCode: this.flowCode,
              id: record.id,
              formUrl: 'business/modules/CesOrderMainForm',
              formUrlMobile: ''
            }
            postAction(this.url.startProcess, params).then(res=>{
              if(res.success){
                this.$message.success(res.message);
                this.loadData();
                this.onClearSelected();
              }else{
                this.$message.warning(res.message);
              }
            }).catch((e)=>{
              this.$message.warning('不识别的请求!');
            })
          }
        })
      },
      getSuperFieldList(){
        let fieldList=[];
         fieldList.push({type:'string',value:'orderCode',text:'订单编码',dictCode:''})
         fieldList.push({type:'date',value:'xdDate',text:'下单时间'})
         fieldList.push({type:'double',value:'money',text:'订单总额',dictCode:''})
         fieldList.push({type:'string',value:'remark',text:'备注',dictCode:''})
         fieldList.push({type:'string',value:'bpmStatus',text:'流程状态',dictCode:'bpm_status'})
         fieldList.push({type:'string',value:'bpmId',text:'流程标识',dictCode:''})
        this.superFieldList = fieldList
      }
    }
  }
</script>
<style scoped>
  @import '~@assets/less/common.less';
</style>