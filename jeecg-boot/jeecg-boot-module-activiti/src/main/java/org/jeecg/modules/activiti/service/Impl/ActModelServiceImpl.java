package org.jeecg.modules.activiti.service.Impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.activiti.bpmn.converter.BpmnXMLConverter;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.editor.constants.ModelDataJsonConstants;
import org.activiti.editor.language.json.converter.BpmnJsonConverter;
import org.activiti.engine.*;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.Model;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.explorer.util.XmlUtil;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.activiti.entity.ActNode;
import org.jeecg.modules.activiti.entity.ActZprocess;
import org.jeecg.modules.activiti.service.IActModelService;
import org.jeecg.modules.activiti.vo.ProcessAsignNodeVo;
import org.jeecg.modules.activiti.vo.ProcessDeploymentVo;
import org.jeecg.modules.activiti.vo.ProcessNodeSpryVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * activiti 模型服务
 *
 * @author dongjb
 * @date 2021/11/15
 */
@Slf4j
@Service
public class ActModelServiceImpl implements IActModelService {
    private final RepositoryService repositoryService;
    private final ActZprocessServiceImpl actZprocessService;
    private final ActNodeServiceImpl actNodeService;

    @Autowired
    public ActModelServiceImpl(RepositoryService repositoryService,
                               ActZprocessServiceImpl actZprocessService,
                               ActNodeServiceImpl actNodeService) {
        this.repositoryService = repositoryService;
        this.actZprocessService = actZprocessService;
        this.actNodeService = actNodeService;
    }

    public static final String BPMN_XML_SUFFIX = ".bpmn20.xml";
    public static final String NC_NAME = "NCName";
    public static final String PRIMARY = "PRIMARY";
    public static final String SPLIT_FLAG = ",";

    @Override
    public Result<Object> createModel(InputStreamReader reader, ProcessDeploymentVo deployment) {
        String processName = null;
        String processKey = null;
        String processDescription = null;

        if (deployment != null) {
            if (StringUtils.isNotEmpty(deployment.getProcessName())) {
                processName = deployment.getProcessName();
            }
            if (StringUtils.isNotEmpty(deployment.getProcessKey())) {
                processKey = deployment.getProcessKey();
            }
            if (StringUtils.isNotEmpty(deployment.getProcessDescription())) {
                processDescription = deployment.getProcessDescription();
            }
        }

        try {
            XMLInputFactory xif = XmlUtil.createSafeXmlInputFactory();
            XMLStreamReader xtr = xif.createXMLStreamReader(reader);
            BpmnModel bpmnModel = new BpmnXMLConverter().convertToBpmnModel(xtr);

            if (bpmnModel.getMainProcess() == null || bpmnModel.getMainProcess().getId() == null) {
                System.out.println("err1");
                return Result.error("审批至少要配置一个主流程！");
            } else {
                if (bpmnModel.getLocationMap().isEmpty()) {
                    System.out.println("err2");
                    Result.error("审批至少要配置一个节点!");
                } else {
                    if (StringUtils.isEmpty(processName)) {
                        if (StringUtils.isNotEmpty(bpmnModel.getMainProcess().getName())) {
                            processName = bpmnModel.getMainProcess().getName();
                        } else {
                            processName = bpmnModel.getMainProcess().getId();
                        }
                    }
                    Model modelData;
                    modelData = repositoryService.newModel();
                    ObjectNode modelObjectNode = new ObjectMapper().createObjectNode();
                    modelObjectNode.put(ModelDataJsonConstants.MODEL_NAME, processName);
                    modelObjectNode.put(ModelDataJsonConstants.MODEL_REVISION, 1);
                    modelObjectNode.put(ModelDataJsonConstants.MODEL_DESCRIPTION, StringUtils.isNotEmpty(processDescription) ? processDescription : processName);
                    modelData.setMetaInfo(modelObjectNode.toString());
                    modelData.setName(processName);
                    modelData.setKey(StringUtils.isNotEmpty(processKey) ? processKey : processName);

                    repositoryService.saveModel(modelData);

                    BpmnJsonConverter jsonConverter = new BpmnJsonConverter();
                    ObjectNode editorNode = jsonConverter.convertToJson(bpmnModel);

                    repositoryService.addModelEditorSource(modelData.getId(), editorNode.toString().getBytes(StandardCharsets.UTF_8));
                    return Result.OK(modelData.getId());
                }
            }
        } catch (Exception e) {
            return Result.error("解析流程xml内部错误");
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Result.error("关闭流资源错");
                }
            }
        }
        return Result.error("创建模型错误");
    }

    @Override
    public Result<Object> deployProcess(Result<Object> modelId, ProcessDeploymentVo deploymentVo) {
        log.info("流程发布详细信息{}", deploymentVo);
        // 获取模型
        Model modelData = repositoryService.getModel((String) modelId.getResult());
        byte[] bytes = repositoryService.getModelEditorSource(modelData.getId());

        if (bytes == null) {
            return Result.error("模型数据为空，请先成功设计流程并保存。然后再发布");
        }

        try {
            JsonNode modelNode = new ObjectMapper().readTree(bytes);

            BpmnModel model = new BpmnJsonConverter().convertToBpmnModel(modelNode);
            if (model.getProcesses().size() == 0) {
                return Result.error("模型不符要求，请至少设计一条主线流程");
            }
            byte[] bpmnBytes = new BpmnXMLConverter().convertToXML(model);

            // 部署发布模型流程
            String processName = modelData.getName() + BPMN_XML_SUFFIX;
            Deployment deployment = repositoryService.createDeployment()
                    .name(modelData.getName())
                    .addString(processName, new String(bpmnBytes, StandardCharsets.UTF_8))
                    .deploy();

            //可以更新 model 中的 deployment id 字段 （看看后续有无必要）
            modelData.setDeploymentId(deployment.getId());
            repositoryService.saveModel(modelData);

            String metaInfo = modelData.getMetaInfo();
            JSONObject metaInfoMap = JSON.parseObject(metaInfo);

            // 设置流程分类 保存扩展流程至数据库
            List<ProcessDefinition> list = repositoryService.createProcessDefinitionQuery().deploymentId(deployment.getId()).list();
            for (ProcessDefinition pd : list) {
                //更新流程类别
                if (StrUtil.isNotBlank(deploymentVo.getCategory())) {
                    repositoryService.setProcessDefinitionCategory(pd.getId(), deploymentVo.getCategory());
                    repositoryService.setDeploymentCategory(pd.getDeploymentId(), deploymentVo.getCategory());
                }
                //激活流程,默认就是激活状态，不需要激活
//                repositoryService.activateProcessDefinitionById(pd.getId(), true, new Date());

                //流程业务信息
                ActZprocess actZprocess = new ActZprocess();
                actZprocess.setId(pd.getId());
                actZprocess.setName(StringUtils.isNotEmpty(deploymentVo.getProcessName()) ? deploymentVo.getProcessName() : modelData.getName());
                actZprocess.setProcessKey(StringUtils.isNotEmpty(deploymentVo.getProcessKey()) ? deploymentVo.getProcessKey() : modelData.getKey());
                actZprocess.setDeploymentId((deployment.getId()));
                actZprocess.setDescription(StringUtils.isNotEmpty(deploymentVo.getProcessDescription()) ? deploymentVo.getProcessDescription() : metaInfoMap.getString(ModelDataJsonConstants.MODEL_DESCRIPTION));
                actZprocess.setVersion(pd.getVersion());
                actZprocess.setDiagramName(pd.getDiagramResourceName());
                actZprocess.setCategoryId(deploymentVo.getCategory());
                actZprocess.setRoles(deploymentVo.getInitiator());
                actZprocess.setStatus(1);
                actZprocess.setBusinessTable(deploymentVo.getBusinessTable());
                actZprocess.setTableType("4");
                actZprocess.setRouteName("@/views/activiti/form/ZBParser");
                actZprocessService.setAllOldByProcessKey(modelData.getKey());
                actZprocess.setLatest(true);
                actZprocessService.save(actZprocess);

                // 节点派送设置
                for (ProcessAsignNodeVo asignNode : deploymentVo.getAsignNodeList()) {
                    String procDefId = pd.getId();
                    String nodeId = asignNode.getNodeId();
                    ProcessNodeSpryVo nodeSpry = asignNode.getSpry();
                    String userIds = nodeSpry.getUserIds();
                    String roleIds = nodeSpry.getRoleIds();
                    String departmentIds = nodeSpry.getDepartmentIds();
                    String departmentManageIds = nodeSpry.getDepartmentManageIds();
                    String formVariables = nodeSpry.getFormVariables();
                    Boolean chooseDepHeader = nodeSpry.getChooseDepHeader();
                    Boolean chooseSponsor = nodeSpry.getChooseSponsor();

                    //删除节点指派
                    actNodeService.deleteByNodeId(nodeId, procDefId);

                    // 分配新用户
                    for (String userId : userIds.split(SPLIT_FLAG)) {
                        ActNode actNode = new ActNode();
                        actNode.setProcDefId(procDefId);
                        actNode.setNodeId(nodeId);
                        actNode.setRelateId(userId);
                        actNode.setType(1);
                        actNodeService.save(actNode);
                    }
                    // 分配新角色
                    for (String roleId : roleIds.split(SPLIT_FLAG)) {
                        ActNode actNode = new ActNode();
                        actNode.setProcDefId(procDefId);
                        actNode.setNodeId(nodeId);
                        actNode.setRelateId(roleId);
                        actNode.setType(0);
                        actNodeService.save(actNode);
                    }
                    // 分配新部门
                    for (String departmentId : departmentIds.split(SPLIT_FLAG)) {
                        ActNode actNode = new ActNode();
                        actNode.setProcDefId(procDefId);
                        actNode.setNodeId(nodeId);
                        actNode.setRelateId(departmentId);
                        actNode.setType(2);
                        actNodeService.save(actNode);
                    }
                    // 分配新部门负责人
                    for (String departmentId : departmentManageIds.split(SPLIT_FLAG)) {
                        ActNode actNode = new ActNode();
                        actNode.setProcDefId(procDefId);
                        actNode.setNodeId(nodeId);
                        actNode.setRelateId(departmentId);
                        actNode.setType(5);
                        actNodeService.save(actNode);
                    }

                    // 表单变量
                    for (String formVariable : formVariables.split(SPLIT_FLAG)) {
                        ActNode actNode = new ActNode();
                        actNode.setProcDefId(procDefId);
                        actNode.setNodeId(nodeId);
                        actNode.setRelateId(formVariable);
                        actNode.setType(6);
                        actNodeService.save(actNode);
                    }

                    if (chooseDepHeader != null && chooseDepHeader) {
                        ActNode actNode = new ActNode();
                        actNode.setProcDefId(procDefId);
                        actNode.setNodeId(nodeId);
                        actNode.setType(4);
                        actNodeService.save(actNode);
                    }
                    if (chooseSponsor != null && chooseSponsor) {
                        ActNode actNode = new ActNode();
                        actNode.setProcDefId(procDefId);
                        actNode.setNodeId(nodeId);
                        actNode.setType(3);
                        actNodeService.save(actNode);
                    }
                }
            }
        } catch (Exception e) {
            String err = e.toString();
            log.error(e.getMessage(), e);
            if (err.contains(NC_NAME)) {
                return Result.error("部署失败：流程设计中的流程名称不能为空，不能为数字以及特殊字符开头！");
            }
            if (err.contains(PRIMARY)) {
                return Result.error("部署失败：该模型已发布，key唯一！");
            }
            return Result.error("部署失败！");
        }
        return Result.OK("部署成功");
    }

}
