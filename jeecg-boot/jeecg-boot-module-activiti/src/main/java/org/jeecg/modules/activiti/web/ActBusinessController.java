package org.jeecg.modules.activiti.web;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.task.Task;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.common.util.UUIDGenerator;
import org.jeecg.modules.activiti.entity.*;
import org.jeecg.modules.activiti.service.Impl.ActBusinessServiceImpl;
import org.jeecg.modules.activiti.service.Impl.ActZprocessServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * 业务办理+我的申请
 *
 * @author: dongjb
 * @date: 2021/5/27
 */
@RestController
@RequestMapping("/actBusiness")
@Slf4j
@Transactional(rollbackFor = Exception.class)
@Api(tags = "工作流-业务列表+我的申请")
public class ActBusinessController {
    private final ActBusinessServiceImpl actBusinessService;
    private final ActZprocessServiceImpl actZprocessService;
    private final TaskService taskService;
    private final RuntimeService runtimeService;

    @Autowired
    public ActBusinessController(ActBusinessServiceImpl actBusinessService,
                                 ActZprocessServiceImpl actZprocessService,
                                 TaskService taskService,
                                 RuntimeService runtimeService) {
        this.actBusinessService = actBusinessService;
        this.actZprocessService = actZprocessService;
        this.taskService = taskService;
        this.runtimeService = runtimeService;
    }

    public static final String SPLIT_FLAG = ",";

    @AutoLog(value = "流程-添加申请草稿状态")
    @ApiOperation(value = "流程-流程申请", notes = "添加申请草稿状态,业务表单参数数据一并传过来！")
    @PostMapping(value = "/add")
    public Result<ActBusiness> add(@ApiParam(value = "流程定义Id", required = true) String procDefId,
                                   @ApiParam(value = "申请标题", required = true) String procDeTitle,
                                   @ApiParam(value = "数据表名", required = true) String tableName,
                                   HttpServletRequest request) {
        /*保存业务表单数据到数据库表*/
        String tableId = IdUtil.simpleUUID();
        //如果前端上传了id
        String id = request.getParameter("id");
        if (id != null && !"".equals(id)) {
            tableId = id;
        }
        boolean isNew = actBusinessService.saveApplyForm(tableId, request);
        ActBusiness actBusiness = new ActBusiness();
        if (isNew) {
            // 新增数据 保存至我的申请业务
            LoginUser sysUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
            String username = sysUser.getUsername();
            actBusiness.setId(UUIDGenerator.generate());
            actBusiness.setUserId(username);
            actBusiness.setTableId(tableId);
            actBusiness.setProcDefId(procDefId);
            String title = request.getParameter(ActivitiConstant.TITLE_KEY);
            if (StrUtil.isNotBlank(title)) {
                actBusiness.setTitle(title);
            } else {
                actBusiness.setTitle(procDeTitle);
            }
            actBusiness.setTableName(tableName);
            actBusinessService.save(actBusiness);
        } else {
            actBusiness = actBusinessService.getOne(new LambdaQueryWrapper<ActBusiness>().eq(ActBusiness::getTableId, tableId).last("limit 1"));
        }
        actBusinessService.updateBusinessStatus(actBusiness.getTableName(), actBusiness.getTableId(), "2");
        return Result.OK(actBusiness);
    }

    @AutoLog(value = "流程-添加申请online")
    @ApiOperation(value = "流程-添加申请online", notes = "添加申请草稿状态,业务表单数据已经具备,只需要操作actBusiness！")
    @PostMapping(value = "/addOnline")
    public Result<ActBusiness> addOnline(@ApiParam(value = "流程定义Id", required = true) String procDefId,
                                         @ApiParam(value = "申请标题", required = true) String procDeTitle,
                                         @ApiParam(value = "数据表名", required = true) String tableName,
                                         @ApiParam(value = "数据标识", required = true) String tableId) {
        // 新增数据 保存至我的申请业务
        String uuid = UUIDGenerator.generate();
        ActBusiness actBusiness = new ActBusiness();
        LoginUser sysUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        String username = sysUser.getUsername();
        actBusiness.setId(uuid);
        actBusiness.setUserId(username);
        actBusiness.setTableId(tableId);
        actBusiness.setProcDefId(procDefId);
        actBusiness.setTitle(procDeTitle);
        actBusiness.setTableName(tableName);
        actBusinessService.save(actBusiness);
        for (String id : tableId.split(SPLIT_FLAG)) {
            actBusinessService.updateBusinessStatusAndId(actBusiness.getTableName(), id, "2", uuid);
        }
        return Result.OK(actBusiness);
    }

    @AutoLog(value = "流程-获取业务表单信息")
    @ApiOperation(value = "流程-获取业务表单信息", notes = "获取业务表单信息")
    @RequestMapping(value = "/getForm", method = RequestMethod.GET)
    public Result<Map<String, Object>> getForm(@ApiParam(value = "业务表数据id", required = true) String tableId,
                                               @ApiParam(value = "业务表名", required = true) String tableName) {
        if (StrUtil.isBlank(tableName)) {
            return Result.error("参数缺省！", null);
        }
        Map<String, Object> applyForm = actBusinessService.getApplyForm(tableId, tableName);
        return Result.OK(applyForm);
    }

    @AutoLog(value = "流程-修改业务表单信息")
    @ApiOperation(value = "流程-修改业务表单信息", notes = "业务表单参数数据一并传过来!")
    @RequestMapping(value = "/editForm", method = RequestMethod.POST)
    public Result<Map<String, String>> editForm(@ApiParam(value = "业务表数据id", required = true) String id,
                                                HttpServletRequest request) {
        /*保存业务表单数据到数据库表*/
        actBusinessService.saveApplyForm(id, request);

        LambdaQueryWrapper<ActBusiness> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ActBusiness::getTableId, id);
        queryWrapper.eq(ActBusiness::getTableName, request.getParameter("tableName"));
        ActBusiness actBusiness = actBusinessService.getOne(queryWrapper);

        Map<String, String> map = new HashMap<>(8);
        map.put("id", actBusiness.getId());

        return Result.OK(map);
    }

    @AutoLog(value = "流程-通过id删除草稿状态申请")
    @ApiOperation(value = "流程-通过id删除草稿状态申请", notes = "通过id删除草稿状态申请")
    @RequestMapping(value = "/delByIds", method = RequestMethod.POST)
    public Result<String> delByIds(@ApiParam(value = "流程扩展表id，多个,号相连", required = true) String ids) {
        for (String id : ids.split(SPLIT_FLAG)) {
            ActBusiness actBusiness = actBusinessService.getById(id);
            if (!actBusiness.getStatus().equals(ActivitiConstant.STATUS_TO_APPLY)) {
                return Result.error("删除失败, 仅能删除草稿状态的申请", null);
            }
            // 删除关联业务表
            actBusinessService.deleteBusiness(actBusiness.getTableName(), actBusiness.getTableId());
            // 删除关联业务子表
            ActZprocess actZprocess = actZprocessService.getActZprocessByTableName(actBusiness.getTableName());
            if (StringUtils.equals("3", actZprocess.getTableType()) && StringUtils.isNotBlank(actZprocess.getOtherInfo())) {
                for (String subTableAndFk : StringUtils.split(actZprocess.getOtherInfo(), ",")) {
                    String[] s = StringUtils.split(subTableAndFk, ":");
                    actBusinessService.deleteBusinessSub(s[0], s[1], actBusiness.getTableId());
                }
            }
            actBusinessService.removeById(id);
        }
        return Result.OK("删除成功", null);
    }

    @AutoLog(value = "流程-提交申请 启动流程")
    @ApiOperation(value = "流程-提交申请 启动流程", notes = "提交申请 启动流程。")
    @RequestMapping(value = "/apply", method = RequestMethod.POST)
    public Result<String> apply(ActBusiness act) {
        ActBusiness actBusiness = actBusinessService.getById(act.getId());
        if (actBusiness == null) {
            return Result.error("actBusiness表中该id不存在", null);
        }
        String tableId = actBusiness.getTableId();
        String tableName = actBusiness.getTableName();
        act.setTableId(tableId);
        String tid = Arrays.stream(tableId.split(SPLIT_FLAG)).findFirst().get();
        Map<String, Object> busiData = actBusinessService.getBusiData(tid, tableName);

        if (MapUtil.isNotEmpty(busiData) && busiData.get(ActivitiConstant.TITLE_KEY) != null) {
            //如果表单里有 标题  更新一下
            actBusiness.setTitle(busiData.get(ActivitiConstant.TITLE_KEY) + "");
        }
        String processInstanceId = actZprocessService.startProcess(act);
        actBusiness.setProcInstId(processInstanceId);
        actBusiness.setStatus(ActivitiConstant.STATUS_DEALING);
        actBusiness.setResult(ActivitiConstant.RESULT_DEALING);
        actBusiness.setApplyTime(new Date());
        actBusinessService.updateById(actBusiness);
        //修改业务表的流程字段
        for (String id : actBusiness.getTableId().split(SPLIT_FLAG)) {
            actBusinessService.updateBusinessStatus(actBusiness.getTableName(), id, "3");
        }
        return Result.OK("操作成功", null);
    }

    @AutoLog(value = "流程-撤回申请")
    @ApiOperation(value = "流程-撤回申请", notes = "撤回申请")
    @RequestMapping(value = "/cancel", method = RequestMethod.POST)
    public Result<Object> cancel(@ApiParam(value = "流程扩展表id", required = true) @RequestParam String id,
                                 @ApiParam(value = "流程实例id", required = true) @RequestParam String procInstId,
                                 @ApiParam(value = "撤销理由原因说明") @RequestParam(required = false) String reason) {

        if (StrUtil.isBlank(reason)) {
            reason = "";
        }
        runtimeService.deleteProcessInstance(procInstId, "canceled-" + reason);
        ActBusiness actBusiness = actBusinessService.getById(id);
        actBusiness.setStatus(ActivitiConstant.STATUS_CANCELED);
        actBusiness.setResult(ActivitiConstant.RESULT_TO_SUBMIT);
        actBusinessService.updateById(actBusiness);
        //修改业务表的流程字段
        for (String tableId : actBusiness.getTableId().split(SPLIT_FLAG)) {
            actBusinessService.updateBusinessStatus(actBusiness.getTableName(), tableId, "4");
        }
        return Result.OK("操作成功");
    }

    @AutoLog(value = "流程-我申请的流程列表")
    @ApiOperation(value = "流程-我申请的流程列表", notes = "我申请的流程列表")
    @RequestMapping(value = "/listData", method = RequestMethod.GET)
    public Result<List<ActBusiness>> listData(ActBusiness param, HttpServletRequest request) {
        return Result.OK(actBusinessService.approveList(request, param));
    }

    @AutoLog(value = "流程-流程定义列表")
    @ApiOperation(value = "流程-流程定义列表", notes = "流程定义列表")
    @RequestMapping(value = "/actZProcess", method = RequestMethod.GET)
    public Result<List<ActZprocess>> listData() {
        List<ActZprocess> list = actZprocessService.list();
        return Result.OK(list);
    }

    @AutoLog(value = "流程-查询申请列表 与 已办列表的合集")
    @ApiOperation(value = "流程-查询申请列表 与 已办列表的合集", notes = "查询申请列表 与 已办列表的合集")
    @RequestMapping(value = "/doAndApplyList", method = RequestMethod.GET)
    public Result<List<ActDoAndApplyVo>> doAndApplyList(ActBusiness param,
                                                        String name,
                                                        String categoryId,
                                                        Integer priority,
                                                        HttpServletRequest request) {
        List<ActDoAndApplyVo> list = new ArrayList<>();

        // 查询审批列表
        List<ActBusiness> actBusinesses = actBusinessService.approveList(request, param);

        // 查询已办列表
        List<HistoricTaskVo> doneList = actBusinessService.getHistoricTaskVos(request, name, categoryId, priority);

        // 复制
        try {
            // 我的申请列表
            for (ActBusiness actBusiness : actBusinesses) {
                ActDoAndApplyVo actDoAndApplyVo = new ActDoAndApplyVo();
                BeanUtils.copyProperties(actDoAndApplyVo, actBusiness);
                actDoAndApplyVo.setType("1");
                list.add(actDoAndApplyVo);
            }

            // 我的已办流程
            for (HistoricTaskVo historicTaskVo : doneList) {
                ActDoAndApplyVo actDoAndApplyVo = new ActDoAndApplyVo();
                actDoAndApplyVo.setType("2");
                BeanUtils.copyProperties(actDoAndApplyVo, historicTaskVo);

                // 关联当前任务 查询当前待办
                List<Task> runTask = taskService.createTaskQuery().processInstanceId(historicTaskVo.getProcInstId()).list();
                if (runTask != null && runTask.size() == 1) {
                    actDoAndApplyVo.setCurrTaskName(runTask.get(0).getName());
                } else if (runTask != null && runTask.size() > 1) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < runTask.size() - 1; i++) {
                        sb.append(runTask.get(i).getName()).append("、");
                    }
                    sb.append(runTask.get(runTask.size() - 1).getName());
                    actDoAndApplyVo.setCurrTaskName(sb.toString());
                }
                list.add(actDoAndApplyVo);
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

        return Result.OK(list);
    }

    @AutoLog(value = "流程-获取指定业务表信息")
    @ApiOperation(value = "流程-获取指定业务表信息", notes = "获取指定业务表信息")
    @RequestMapping(value = "/getActBusinessByTableInfo", method = RequestMethod.GET)
    public Result<ActBusiness> getActBusinessByTableInfo(@RequestParam(value = "tableName", defaultValue = "") String tableName, @RequestParam(value = "tableId", defaultValue = "") String tableId) {
        return Result.OK(actBusinessService.getActBusinessByTableInfo(tableName, tableId));
    }

    @AutoLog(value = "流程-表单自动生成获取数据库的表名列表")
    @ApiOperation(value = "流程-获取数据库表名列表", notes = "表单自动生成获取数据库的表名列表")
    @RequestMapping(value = "/getTableNameList", method = RequestMethod.GET)
    public Result<List<String>> getTableNameList(@RequestParam(value = "schemaName", defaultValue = "'jeecg-boot'") String schemaName) {
        return Result.OK(actBusinessService.getTableNameList(schemaName));
    }

    @AutoLog(value = "流程-表单自动生成获取表的字段列表")
    @ApiOperation(value = "流程-获取表的字段列表", notes = "获取表的字段列表")
    @RequestMapping(value = "/getColumnNameList", method = RequestMethod.POST)
    public Result<List<String>> getColumnNameList(@RequestParam(value = "tableName", defaultValue = "'act_b_leave'") String tableName,
                                                    @RequestParam(value = "schemaName",defaultValue = "'jeecg-boot'") String schemaName) {
        log.info("入参测试tableName:{tableName}");
        log.info("入参测试schemaName:{schemaName}");
        return Result.OK(actBusinessService.getColumnNameList(tableName, schemaName));
    }

}
