package org.jeecg.modules.activiti.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ProcessDeploymentVo {
    /**
     * 流程类别
     */
    private String category;

    /**
     * 流程定义key
     */
    private String processKey;

    /**
     * 流程定义名字
     */
    private String processName;

    /**
     * 流程描述
     */
    private String processDescription;

    /**
     * 权限
     */
    private String initiator;

    /**
     * 流程定义内容
     */
    private String xml;

    /**
     * 流程定义svg
     */
    private String svg;

    /**
     * 节点指派人列表
     */
    private ProcessAsignNodeVo[]  asignNodeList;

    /**
     * 关联业务表名
     */
    private String businessTable;

    /**
     * 关联业务表类型
     */
    private String tableType;

    /**
     * 关联前端表单路由名
     */
    private String routeName;
}
