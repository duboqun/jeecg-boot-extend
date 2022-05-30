import {filterObj} from '@/utils/util';
import {deleteAction, downFile, getAction} from '@/api/manage'
import Vue from 'vue'
import {ACCESS_TOKEN} from "@/store/mutation-types"
import JEllipsis from '@/components/jeecg/JEllipsis'

export const activitiMixin = {
  components: {
    JEllipsis
  },
  data(){
    return {
      //token header
      tokenHeader: {'X-Access-Token': Vue.ls.get(ACCESS_TOKEN)},
      allComponent: [],
      url: {
        listFormComponent: "/activiti/formComponent/list"
      },
    }
  },
  computed:{
    historicDetail:function () {
      return () => import(`@/views/activiti/historicDetail`)
    }
  },
  created() {
    this.getAllFormComponent();
  },
  methods:{
    getAllFormComponent() {
      getAction(this.url.listFormComponent).then((res) => {
        if(res.success){
          this.allComponent = res.result;
        }
      }).catch(()=>{
        console.log("表单组件加载失败");
      });
    },
    getFormComponentByName(routeName, businessTable){
      return _.find(this.allComponent,{routeName:routeName, businessTable: businessTable})||{};
    },
    getFormComponentById(routeId){
      return _.find(this.allComponent,{id:routeId})||{};
    },
    millsToTime(mills) {
      if (!mills) {
        return "";
      }
      let s = mills / 1000;
      if (s < 60) {
        return s.toFixed(0) + " 秒"
      }
      let m = s / 60;
      if (m < 60) {
        return m.toFixed(0) + " 分钟"
      }
      let h = m / 60;
      if (h < 24) {
        return h.toFixed(0) + " 小时"
      }
      let d = h / 24;
      if (d < 30) {
        return d.toFixed(0) + " 天"
      }
      let month = d / 30
      if (month < 12) {
        return month.toFixed(0) + " 个月"
      }
      let year = month / 12
      return year.toFixed(0) + " 年"

    },
    handleTableChange(pagination, filters, sorter) {
      //分页、排序、筛选变化时触发
      //TODO 筛选
      if (Object.keys(sorter).length > 0) {
        this.isorter.column = sorter.field;
        this.isorter.order = "ascend" == sorter.order ? "asc" : "desc"
      }
      this.ipagination = pagination;
      // this.loadData();
    },
    //根据key 获取流程定义数据
    getProcessDefByKey(key){
      let procDef = null;
      let newestProcessList = JSON.parse(window.sessionStorage.getItem("newestProcessList"));
      newestProcessList.forEach(function(item) {
        if(item.processKey === key){
          procDef = item;
        }
      });
      return procDef;
    },
  }

}