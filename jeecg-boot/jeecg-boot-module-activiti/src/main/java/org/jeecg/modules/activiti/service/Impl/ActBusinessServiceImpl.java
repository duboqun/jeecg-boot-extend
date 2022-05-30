package org.jeecg.modules.activiti.service.Impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.activiti.engine.HistoryService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricIdentityLink;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.history.HistoricTaskInstanceQuery;
import org.activiti.engine.task.Comment;
import org.activiti.engine.task.Task;
import org.apache.shiro.SecurityUtils;
import org.jeecg.common.system.api.ISysBaseAPI;
import org.jeecg.common.system.vo.ComboModel;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.common.util.DateUtils;
import org.jeecg.common.util.dynamic.db.SqlUtils;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.activiti.entity.ActBusiness;
import org.jeecg.modules.activiti.entity.ActZprocess;
import org.jeecg.modules.activiti.entity.ActivitiConstant;
import org.jeecg.modules.activiti.entity.HistoricTaskVo;
import org.jeecg.modules.activiti.mapper.ActBusinessMapper;
import org.jeecg.modules.activiti.service.IActBusinessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

import static org.jeecg.common.util.dynamic.db.SqlUtils.DATABSE_TYPE_MYSQL;

/**
 * 流程业务扩展表
 *
 * @author: dongjb
 * @date: 2021/5/28
 */
@Service
public class ActBusinessServiceImpl extends ServiceImpl<ActBusinessMapper, ActBusiness> implements IActBusinessService {
    private static final String SPLIT_FLAG = ",";
    @Autowired
    private TaskService taskService;

    @Autowired
    private ActZprocessServiceImpl actZprocessService;

    @Autowired
    private ISysBaseAPI sysBaseApi;

    @Autowired
    private HistoryService historyService;

    public static final String TRUE = "true";
    /**
     * 查询我的流程列表
     *
     * @param request http request
     * @return 我的流程列表
     */
    public List<ActBusiness> approveList(HttpServletRequest request, ActBusiness param) {
        // 按时间排序
        LambdaQueryWrapper<ActBusiness> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.orderByDesc(ActBusiness::getCreateTime);
        if (StrUtil.isNotBlank(param.getTitle())) {
            queryWrapper.like(ActBusiness::getTitle, param.getTitle());
        }
        if (param.getStatus() != null) {
            queryWrapper.eq(ActBusiness::getStatus, param.getStatus());
        }
        // 流程定义key
        String procDefKey = request.getParameter("procDefKey");
        if (StrUtil.isNotBlank(procDefKey)) {
            queryWrapper.in(ActBusiness::getProcDefId, procDefKey);
        }

        if (param.getResult() != null) {
            queryWrapper.eq(ActBusiness::getResult, param.getResult());
        }
        String createTimeegin = request.getParameter("createTime_begin");
        if (StrUtil.isNotBlank(createTimeegin)) {
            queryWrapper.ge(ActBusiness::getCreateTime, createTimeegin);
        }
        String createTimeEnd = request.getParameter("createTime_end");
        if (StrUtil.isNotBlank(createTimeEnd)) {
            queryWrapper.le(ActBusiness::getCreateTime, createTimeEnd);
        }

        LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        queryWrapper.eq(ActBusiness::getUserId, loginUser.getUsername());

        //流程类型
        String type = request.getParameter("type");
        if (StrUtil.isNotBlank(type)) {
            List<String> actBusinessIdsByType = this.listByTypeApp(type);
            // 没有符合的 目的是上下面的查询条件也查不到
            if (actBusinessIdsByType.size() == 0) {
                queryWrapper.in(ActBusiness::getId, Lists.newArrayList(""));
            } else {
                queryWrapper.in(ActBusiness::getId, actBusinessIdsByType);
            }
        }
        List<ActBusiness> actBusinessList = this.list(queryWrapper);

        // 是否需要业务数据
        String needData = request.getParameter("needData");
        actBusinessList.forEach(e -> {
            if (StrUtil.isNotBlank(e.getProcDefId())) {
                // 获取流程定义表中 路由名称和流程名称
                ActZprocess actProcess = actZprocessService.getById(e.getProcDefId());
                e.setRouteName(actProcess.getRouteName());
                e.setProcessName(actProcess.getName());
            }
            // 流程正在处理中时
            if (ActivitiConstant.STATUS_DEALING.equals(e.getStatus())) {
                // 关联当前任务 查询当前待办
                List<Task> taskList = taskService.createTaskQuery().processInstanceId(e.getProcInstId()).list();
                if (taskList != null && taskList.size() == 1) {
                    e.setCurrTaskName(taskList.get(0).getName());
                } else if (taskList != null && taskList.size() > 1) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < taskList.size() - 1; i++) {
                        sb.append(taskList.get(i).getName()).append("、");
                    }
                    sb.append(taskList.get(taskList.size() - 1).getName());
                    e.setCurrTaskName(sb.toString());
                }
                // 查询审批历史，如果有的话，禁止撤回操作
                List<HistoricTaskInstance> list = historyService.createHistoricTaskInstanceQuery().processInstanceId(e.getProcInstId()).finished().list();
                if (list.size() > 0) {
                    e.setProcInstStatus(ActivitiConstant.PROC_INST_APPROVE);
                } else {
                    e.setProcInstStatus(ActivitiConstant.PROC_INST_NOT_APPROVE);
                }
            }
            // 需要业务数据
            if (StrUtil.equals(needData, TRUE)) {
                Map<String, Object> applyForm = this.getApplyForm(e.getTableId(), e.getTableName());
                e.setDataMap(applyForm);
            }
        });
        return actBusinessList;

    }

    public List<ActBusiness> findByProcDefId(String id) {
        return this.list(new LambdaQueryWrapper<ActBusiness>().eq(ActBusiness::getProcDefId, id));
    }

    /**
     * 保存业务表单数据到数据库表
     * <br>该方法相对通用，复杂业务单独定制，套路类似
     *
     * @param tableId 业务表中的数据id
     * @return 如果之前数据库没有 返回 true
     */
    public boolean saveApplyForm(String tableId, HttpServletRequest request) {
        String tableName = request.getParameter("tableName");
        String filedNames = request.getParameter("filedNames");
        Map<String, Object> busiData = this.baseMapper.getBusiData(tableId, tableName);
        String[] fileds = filedNames.split(",");
        //没有，新增逻辑
        if (MapUtil.isEmpty(busiData)) {
            StringBuilder filedsB = new StringBuilder("id");
            StringBuilder filedsVb = new StringBuilder("'" + tableId + "'");
            for (String filed : fileds) {
                String dbFiled = oConvertUtils.camelToUnderline(filed);
                if (!"undefined".equals(filed)) {
                    if (request.getParameter(filed) != null) {
                        filedsB.append(",").append(dbFiled);
                        filedsVb.append(",'").append(request.getParameter(filed)).append("'");
                    } else {
                        filedsB.append(",").append(dbFiled);
                        filedsVb.append(",").append(request.getParameter(filed));
                    }
                }
            }
            LoginUser sysUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
            String userName = sysUser.getUsername();
            filedsB.append("," + "create_by");
            filedsVb.append(",'").append(userName).append("'");
            filedsB.append("," + "create_time");
            filedsVb.append(",'").append(DateUtils.formatDate(new Date(), "yyyy-MM-dd")).append("'");
            this.baseMapper.insertBusiData(String.format("INSERT INTO %s (%s) VALUES (%s)", tableName, filedsB, filedsVb));
        } else { //有，修改
            StringBuilder setSql = new StringBuilder();
            for (String filed : fileds) {
                if (filed != null && !"undefined".equals(filed)) {
                    String parameter = request.getParameter(filed);
                    String dbFiled = oConvertUtils.camelToUnderline(filed);
                    if (parameter == null) {
                        setSql.append(String.format("%s = null,", dbFiled));
                    } else {
                        setSql.append(String.format("%s = '%s',", dbFiled, parameter));
                    }
                }
            }
            //去掉最后一个,号
            String substring = setSql.substring(0, setSql.length() - 1);
            LoginUser sysUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
            String userName = sysUser.getUsername();
            substring += (",update_by = " + "'" + userName + "'");
            substring += (",update_time = " + "'" + DateUtils.formatDate(new Date(), "yyyy-MM-dd") + "'");
            this.baseMapper.updateBusiData(String.format("update %s set %s where id = '%s'", tableName, substring, tableId));
        }
        return MapUtil.isEmpty(busiData);
    }

    public Map<String, Object> getApplyForm(String tableId, String tableName) {
        String id = Arrays.stream(tableId.split(SPLIT_FLAG)).findFirst().get();
        Map<String, Object> busiData = this.getBusiData(id, tableName);
        Object createBy = busiData.get("createBy");
        if (createBy != null) {
            String depName = sysBaseApi.getDepartNamesByUsername(createBy.toString()).get(0);
            busiData.put("createByDept", depName);
            LoginUser userByName = sysBaseApi.getUserByName(createBy.toString());
            busiData.put("createByName", userByName.getRealname());
            busiData.put("createByAvatar", userByName.getAvatar());
        }
        return busiData;
    }

    public void deleteBusiness(String tableName, String tableId) {
        this.baseMapper.deleteBusiData(tableId, tableName);
    }

    /**
     * 通过类型和任务id查找用户id
     */
    public List<String> findUserIdByTypeAndTaskId(String type, String taskId) {
        return baseMapper.findUserIdByTypeAndTaskId(type, taskId);
    }

    public void inserthiIdentitylink(String id, String type, String userId, String taskId, String procInstId) {
        this.baseMapper.insertHI_IDENTITYLINK(id, type, userId, taskId, procInstId);
    }

    public List<String> selectRunIdentity(String taskId, String type) {
        return baseMapper.selectIRunIdentity(taskId, type);
    }

    public List<String> selectRunIdentityByUser(String taskId, String type, String userName) {
        return baseMapper.selectRunIdentityByUser(taskId, type, userName);
    }

    /**
     * 修改业务表的流程字段
     */
    public void updateBusinessStatus(String tableName, String tableId, String actStatus) {
        try {
            baseMapper.updateBusinessStatus(tableName, tableId, actStatus);
        } catch (Exception e) {
            // 业务表需要有 act_status字段，没有会报错，不管他
            log.warn(e.getMessage());
        }
    }

    /**
     * 修改业务表的流程字段,流程标识
     */
    public void updateBusinessStatusAndId(String tableName, String tableId, String actStatus, String actId) {
        try {
            baseMapper.updateBusinessStatusAndId(tableName, tableId, actStatus, actId);
        } catch (Exception e) {
            // 业务表需要有 act_status字段，没有会报错，不管他
            log.warn(e.getMessage());
        }
    }

    /**
     * 获取业务表单数据并驼峰转换
     */
    public Map<String, Object> getBusiData(String tableId, String tableName) {
        Map<String, Object> busiData = this.baseMapper.getBusiData(tableId, tableName);
        if (busiData == null) {
            return null;
        }
        HashMap<String, Object> map = Maps.newHashMap();
        for (String key : busiData.keySet()) {
            String camelName = oConvertUtils.camelName(key);
            map.put(camelName, busiData.get(key));
        }
        return map;
    }

    public List<String> listByTypeApp(String type) {
        return this.baseMapper.listByTypeApp(type);
    }


    /**
     * 获取登陆人的已办
     *
     * @param req        http request
     * @param name       流程名
     * @param categoryId 流程类型
     * @param priority   优先级别
     * @return 登陆人的已办
     */
    public List<HistoricTaskVo> getHistoricTaskVos(HttpServletRequest req, String name, String categoryId, Integer priority) {
        List<HistoricTaskVo> list = new ArrayList<>();
        LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        String userId = loginUser.getUsername();
        HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery().or().taskCandidateUser(userId).
                taskAssignee(userId).endOr().finished();

        // 多条件搜索
        query.orderByTaskCreateTime().desc();
        if (StrUtil.isNotBlank(name)) {
            query.taskNameLike("%" + name + "%");
        }
        if (StrUtil.isNotBlank(categoryId)) {
            query.taskCategory(categoryId);
        }
        if (priority != null) {
            query.taskPriority(priority);
        }
        String searchVal = req.getParameter("searchVal");
        if (StrUtil.isNotBlank(searchVal)) {
            //搜索标题、申请人
            List<LoginUser> usersByName = getBaseMapper().getUsersByName(searchVal);
            List<String> uNames;
            if (usersByName.size() == 0) {
                uNames = Lists.newArrayList("");
            } else {
                uNames = usersByName.stream().map(LoginUser::getUsername).collect(Collectors.toList());
            }
            //标题查询
            List<ActBusiness> businessList = this.list(new LambdaQueryWrapper<ActBusiness>()
                    .like(ActBusiness::getTitle, searchVal)
                    .or().in(ActBusiness::getUserId, uNames)
            );
            if (businessList.size() > 0) {
                // 定义id
                List<String> pids = businessList.stream().map(ActBusiness::getProcInstId).filter(Objects::nonNull).collect(Collectors.toList());
                query.processInstanceIdIn(pids);
            } else {
                query.processInstanceIdIn(Lists.newArrayList(""));
            }
        }
        String type = req.getParameter("type");
        if (StrUtil.isNotBlank(type)) {
            List<String> deploymentIdList = this.getBaseMapper().deployment_idListByType(type);
            if (deploymentIdList.size() == 0) {
                query.deploymentIdIn(Lists.newArrayList(""));
            } else {
                query.deploymentIdIn(deploymentIdList);
            }
        }
        String createTimeEnd = req.getParameter("createTime_end");
        if (StrUtil.isNotBlank(createTimeEnd)) {
            Date end = DateUtil.parse(createTimeEnd);
            query.taskCreatedBefore(DateUtil.endOfDay(end));
        }
        // 流程定义key
        String procDefKey = req.getParameter("procDefKey");
        if (StrUtil.isNotBlank(procDefKey)) {
            query.processDefinitionId(procDefKey);
        }

        List<HistoricTaskInstance> taskList = query.list();
        // 是否需要业务数据
        String needData = req.getParameter("needData");
        // 转换vo
        List<ComboModel> allUser = sysBaseApi.queryAllUserBackCombo();
        Map<String, String> userMap = allUser.stream().collect(Collectors.toMap(ComboModel::getUsername, ComboModel::getTitle));
        taskList.forEach(e -> {
            HistoricTaskVo htv = new HistoricTaskVo(e);
            // 关联委托人
            if (StrUtil.isNotBlank(htv.getOwner())) {
                htv.setOwner(userMap.get(htv.getOwner()));
            }
            List<HistoricIdentityLink> identityLinks = historyService.getHistoricIdentityLinksForProcessInstance(htv.getProcInstId());
            for (HistoricIdentityLink hik : identityLinks) {
                // 关联发起人
                if ("starter".equals(hik.getType()) && StrUtil.isNotBlank(hik.getUserId())) {
                    htv.setApplyer(userMap.get(hik.getUserId()));
                }
            }
            // 关联审批意见
            List<Comment> comments = taskService.getTaskComments(htv.getId(), "comment");
            if (comments != null && comments.size() > 0) {
                htv.setComment(comments.get(0).getFullMessage());
            }
            // 关联流程信息
            ActZprocess actProcess = actZprocessService.getById(htv.getProcDefId());
            if (actProcess != null) {
                htv.setProcessName(actProcess.getName());
                htv.setRouteName(actProcess.getRouteName());
            }
            // 关联业务key
            HistoricProcessInstance hpi = historyService.createHistoricProcessInstanceQuery().processInstanceId(htv.getProcInstId()).singleResult();
            htv.setBusinessKey(hpi.getBusinessKey());
            ActBusiness actBusiness = this.getById(hpi.getBusinessKey());
            if (actBusiness != null) {
                htv.setTableId(actBusiness.getTableId());
                htv.setTableName(actBusiness.getTableName());
                htv.setTitle(actBusiness.getTitle());
                htv.setStatus(actBusiness.getStatus());
                htv.setResult(actBusiness.getResult());
                htv.setApplyTime(actBusiness.getApplyTime());
                // 需要业务数据
                if (StrUtil.equals(needData, TRUE)) {
                    Map<String, Object> applyForm = this.getApplyForm(actBusiness.getTableId(), actBusiness.getTableName());
                    htv.setDataMap(applyForm);
                }
            }

            list.add(htv);
        });
        return list;
    }

    public ActBusiness getActBusinessByTableInfo(String tableName, String tableId) {
        Map<String, Object> map = new HashMap<>();
        map.put("table_name", tableName);
        map.put("table_id", tableId);
        return baseMapper.selectByMap(map).stream().findFirst().orElse(null);
    }

    public void deleteBusinessSub(String subTableName, String fkId,String tableId) {
        this.baseMapper.deleteBusiSubData(tableId, subTableName, fkId);
    }

    @Override
    public List<String> getTableNameList(String schemaName) {
        String sql = SqlUtils.getAllTableSql(DATABSE_TYPE_MYSQL, schemaName);
        return this.baseMapper.getTableNameList(sql);
    }

    @Override
    public List<String> getColumnNameList(String tableName, String schemaName) {
        String sql = SqlUtils.getAllColumnSQL(DATABSE_TYPE_MYSQL, tableName, schemaName);
        List<String> columNames = this.baseMapper.getTableNameList(sql);
        return columNames.stream().map( columName -> oConvertUtils.camelName(columName)).collect(Collectors.toList());
    }
}
