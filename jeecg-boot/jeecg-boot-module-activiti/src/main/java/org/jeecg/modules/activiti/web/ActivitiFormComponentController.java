package org.jeecg.modules.activiti.web;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.activiti.entity.ActFormComponent;
import org.jeecg.modules.activiti.service.Impl.ActFormComponentServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 流程关联表单组件信息
 *
 * @author dongjb
 * @date 2021/10/13
 */
@RestController
@RequestMapping("/activiti/formComponent")
@Slf4j
@Api(tags = "工作流-表单组件信息")
public class ActivitiFormComponentController {

    @Autowired
    private ActFormComponentServiceImpl actFormComponentService;

    @RequestMapping(value = "/queryDynamicForm", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "表单组件查询", notes = "表单组件查询")
    public Result<List<ActFormComponent>> queryDynamicForm(HttpServletRequest request) {
        Map<String, Object> map = new HashMap<>(8);
        map.put("business_table", request.getParameter("businessTable"));
        map.put("table_type", "4");
        List<ActFormComponent> models = actFormComponentService.listByMap(map);
        return Result.OK(models);
    }

    @RequestMapping(value = "/query", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "表单组件查询", notes = "表单组件查询")
    public Result<List<ActFormComponent>> query(HttpServletRequest request) {
        Map<String, Object> map = new HashMap<>(8);
        map.put("text", request.getParameter("keyWord"));
        List<ActFormComponent> models = actFormComponentService.listByMap(map);
        return Result.OK(models);
    }

    @RequestMapping(value = "/list", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "表单组件列表", notes = "表单组件列表")
    public Result<List<ActFormComponent>> list() {
        List<ActFormComponent> list = actFormComponentService.list();
        return Result.OK(list);
    }

    @PostMapping(value = "/add")
    @ApiOperation(value = "表单组件添加", notes = "表单组件添加")
    public Result<String> add(@RequestBody ActFormComponent actFormComponent) {
        actFormComponentService.save(actFormComponent);
        return Result.OK("添加成功！");
    }

    @PutMapping(value = "/edit")
    @ApiOperation(value = "表单组件编辑", notes = "表单组件编辑")
    public Result<?> edit(@RequestBody ActFormComponent actFormComponent) {
        actFormComponentService.updateById(actFormComponent);
        return Result.OK("编辑成功!");
    }

    @DeleteMapping(value = "/delete")
    @ApiOperation(value = "表单组件删除", notes = "表单组件删除")
    public Result<?> delete(@RequestParam(name = "id") String id) {
        actFormComponentService.removeById(id);
        return Result.OK("删除成功!");
    }
}
