package org.jeecg.modules.activiti.web;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricIdentityLink;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricProcessInstanceQuery;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.runtime.ProcessInstanceQuery;
import org.activiti.engine.task.Task;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.common.system.api.ISysBaseAPI;
import org.jeecg.common.system.vo.ComboModel;
import org.jeecg.modules.activiti.entity.*;
import org.jeecg.modules.activiti.service.Impl.ActBusinessServiceImpl;
import org.jeecg.modules.activiti.service.Impl.ActZprocessServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 流程实例控制器
 *
 * @author: dongjb
 * @date: 2021/5/31
 */
@Slf4j
@RestController
@RequestMapping("/actProcessIns")
@Transactional(rollbackFor = Exception.class)
@Api(tags = "工作流-进行中流程,已结束流程,我的申请(第一节点)")
public class ActProcessInsController {

    private final ActZprocessServiceImpl actZprocessService;

    private final RuntimeService runtimeService;

    private final HistoryService historyService;

    private final TaskService taskService;

    private final ActBusinessServiceImpl actBusinessService;

    private final ISysBaseAPI sysBaseApi;

    @Autowired
    public ActProcessInsController(ActZprocessServiceImpl actZprocessService,
                                   RuntimeService runtimeService,
                                   HistoryService historyService,
                                   TaskService taskService,
                                   ActBusinessServiceImpl actBusinessService,
                                   ISysBaseAPI sysBaseApi) {
        this.actZprocessService = actZprocessService;
        this.runtimeService = runtimeService;
        this.historyService = historyService;
        this.taskService = taskService;
        this.actBusinessService = actBusinessService;
        this.sysBaseApi = sysBaseApi;
    }

    public static final String SPLIT_FLAG = ",";

    @AutoLog(value = "流程-通过流程定义id获取第一个任务节点")
    @ApiOperation(value = "流程-通过流程定义id获取第一个任务节点", notes = "通过流程定义id获取第一个任务节点，包含可供选择的审批人、网关信息等")
    @RequestMapping(value = "/getFirstNode", method = RequestMethod.GET)
    public Result<Object> getFirstNode(@ApiParam(value = "流程定义Id", required = true) String procDefId,
                               @ApiParam(value = "表名", required = true) String tableName,
                               @ApiParam(value = "表id", required = true) String tableId) {
        ProcessNodeVo node = actZprocessService.getFirstNode(procDefId, tableName, tableId);
        return Result.OK(node);
    }

    @AutoLog(value = "流程-获取运行中的流程实例")
    @ApiOperation(value = "流程-获取运行中的流程实例", notes = "获取运行中的流程实例")
    @RequestMapping(value = "/getRunningProcess", method = RequestMethod.GET)
    public Result<Object> getRunningProcess(@RequestParam(required = false) String name,
                                            @RequestParam(required = false) String categoryId,
                                            @RequestParam(required = false) String key
    ) {

        List<ProcessInsVo> list = new ArrayList<>();

        ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery()
                .orderByProcessInstanceId().desc();

        if (StrUtil.isNotBlank(name)) {
            query.processInstanceNameLike("%" + name + "%");
        }
        if (StrUtil.isNotBlank(categoryId)) {
            query.processDefinitionCategory(categoryId);
        }
        if (StrUtil.isNotBlank(key)) {
            query.processDefinitionKey(key);
        }

        List<ProcessInstance> processInstanceList = query.list();
        processInstanceList.forEach(e -> list.add(new ProcessInsVo(e)));
        List<ComboModel> allUser = sysBaseApi.queryAllUserBackCombo();
        Map<String, String> userMap = allUser.stream().collect(Collectors.toMap(ComboModel::getUsername, ComboModel::getTitle));
        list.forEach(e -> {
            List<HistoricIdentityLink> identityLinks = historyService.getHistoricIdentityLinksForProcessInstance(e.getId());
            for (HistoricIdentityLink hik : identityLinks) {
                // 关联发起人
                if ("starter".equals(hik.getType()) && StrUtil.isNotBlank(hik.getUserId())) {
                    e.setApplyer(userMap.get(hik.getUserId()));
                }
            }
            // 关联当前任务
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
            // 关联流程表单路由
            ActZprocess actProcess = actZprocessService.getById(e.getProcDefId());
            if (actProcess != null) {
                e.setRouteName(actProcess.getRouteName());
            }
            // 关联业务表id
            ActBusiness actBusiness = actBusinessService.getById(e.getBusinessKey());
            if (actBusiness != null) {
                e.setTableId(actBusiness.getTableId());
                e.setTableName(actBusiness.getTableName());
            }
        });
        return Result.OK(list);
    }

    @AutoLog(value = "通过id删除运行中的实例")
    @ApiOperation(value = "通过id删除运行中的实例", notes = "通过id删除运行中的实例")
    @RequestMapping(value = "/delInsByIds/{ids}", method = RequestMethod.POST)
    public Result<Object> delInsByIds(@PathVariable String ids,
                                      @RequestParam(required = false) String reason) {

        if (StrUtil.isBlank(reason)) {
            reason = "";
        }
        for (String id : ids.split(SPLIT_FLAG)) {
            // 关联业务状态结束
            ProcessInstance pi = runtimeService.createProcessInstanceQuery().processInstanceId(id).singleResult();
            ActBusiness actBusiness = actBusinessService.getById(pi.getBusinessKey());
            if (actBusiness != null) {
                actBusiness.setStatus(ActivitiConstant.STATUS_TO_APPLY);
                actBusiness.setResult(ActivitiConstant.RESULT_TO_SUBMIT);
                actBusinessService.updateById(actBusiness);
            }
            runtimeService.deleteProcessInstance(id, ActivitiConstant.DELETE_PRE + reason);
        }
        return Result.OK("删除成功");
    }

    @AutoLog(value = "激活或挂起流程实例")
    @ApiOperation(value = "激活或挂起流程实例", notes = "激活或挂起流程实例")
    @RequestMapping(value = "/updateInsStatus", method = RequestMethod.POST)
    public Result<Object> updateStatus(@RequestParam String id,
                                       @RequestParam Integer status) {

        if (ActivitiConstant.PROCESS_STATUS_ACTIVE.equals(status)) {
            runtimeService.activateProcessInstanceById(id);
        } else if (ActivitiConstant.PROCESS_STATUS_SUSPEND.equals(status)) {
            runtimeService.suspendProcessInstanceById(id);
        }

        return Result.OK("修改成功");
    }

    @AutoLog(value = "获取结束的的流程实例")
    @ApiOperation(value = "获取结束的的流程实例", notes = "获取结束的的流程实例")
    @RequestMapping(value = "/getFinishedProcess", method = RequestMethod.GET)
    public Result<Object> getFinishedProcess(@RequestParam(required = false) String name,
                                             @RequestParam(required = false) String categoryId,
                                             @RequestParam(required = false) String key, String startDate, String endDate) {

        List<HistoricProcessInsVo> list = new ArrayList<>();

        HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery().finished().
                orderByProcessInstanceEndTime().desc();

        if (StrUtil.isNotBlank(name)) {
            query.processInstanceNameLike("%" + name + "%");
        }
        if (StrUtil.isNotBlank(categoryId)) {
            query.processDefinitionCategory(categoryId);
        }
        if (StrUtil.isNotBlank(key)) {
            query.processDefinitionKey(key);
        }

        if (StrUtil.isNotBlank(startDate) && StrUtil.isNotBlank(endDate)) {
            Date start = DateUtil.parse(startDate);
            Date end = DateUtil.parse(endDate);
            query.finishedAfter(start);
            query.finishedBefore(DateUtil.endOfDay(end));
        }

        List<HistoricProcessInstance> processInstanceList = query.list();
        processInstanceList.forEach(e -> list.add(new HistoricProcessInsVo(e)));
        List<ComboModel> allUser = sysBaseApi.queryAllUserBackCombo();
        Map<String, String> userMap = allUser.stream().collect(Collectors.toMap(ComboModel::getUsername, ComboModel::getTitle));
        list.forEach(e -> {
            List<HistoricIdentityLink> identityLinks = historyService.getHistoricIdentityLinksForProcessInstance(e.getId());
            for (HistoricIdentityLink hik : identityLinks) {
                // 关联发起人
                if ("starter".equals(hik.getType()) && StrUtil.isNotBlank(hik.getUserId())) {
                    e.setApplyer(userMap.get(hik.getUserId()));
                }
            }
            // 关联流程表单路由
            ActZprocess actProcess = actZprocessService.getById(e.getProcDefId());
            if (actProcess != null) {
                e.setRouteName(actProcess.getRouteName());
            }
            // 关联业务表id和结果
            ActBusiness actBusiness = actBusinessService.getById(e.getBusinessKey());
            if (actBusiness != null) {
                e.setTableId(actBusiness.getTableId());
                e.setTableName(actBusiness.getTableName());
                String reason = e.getDeleteReason();
                if (reason == null) {
                    e.setResult(ActivitiConstant.RESULT_PASS);
                } else if (reason.contains(ActivitiConstant.CANCEL_PRE)) {
                    e.setResult(ActivitiConstant.RESULT_CANCEL);
                    if (reason.length() > ActivitiConstant.CANCEL_PRE.length()) {
                        e.setDeleteReason(reason.substring(ActivitiConstant.CANCEL_PRE.length()));
                    } else {
                        e.setDeleteReason("");
                    }
                } else if (ActivitiConstant.BACKED_FLAG.equals(reason)) {
                    e.setResult(ActivitiConstant.RESULT_FAIL);
                    e.setDeleteReason("");
                } else if (reason.contains(ActivitiConstant.DELETE_PRE)) {
                    e.setResult(ActivitiConstant.RESULT_DELETED);
                    if (reason.length() > ActivitiConstant.DELETE_PRE.length()) {
                        e.setDeleteReason(reason.substring(ActivitiConstant.DELETE_PRE.length()));
                    } else {
                        e.setDeleteReason("");
                    }
                } else {
                    e.setResult(ActivitiConstant.RESULT_PASS);
                }
            }
        });
        return Result.OK(list);
    }

    @RequestMapping(value = "/delHistoricInsByIds/{ids}")
    public Result<Object> delHistoricInsByIds(@PathVariable String ids) {

        for (String id : ids.split(SPLIT_FLAG)) {
            historyService.deleteHistoricProcessInstance(id);
        }
        return Result.OK("删除成功");
    }

}
