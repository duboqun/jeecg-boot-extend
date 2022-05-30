package org.jeecg.modules.activiti.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 节点派发详细信息
 *
 * @author dongjb
 * @date 2021/11/22
 */

@Getter
@Setter
@ToString
public class ProcessNodeSpryVo {
    /**
     * 1用户
     */
    private String userIds;
    /**
     * 0角色
     */
    private String roleIds;
    /**
     * 5部门负责人
     */
    private String departmentIds;
    /**
     * 4发起人的部门负责人
     */
    private String departmentManageIds;
    /**
     * 6窗口变量
     */
    private String formVariables;
    /**
     * 3是否选中发起人
     */
    private Boolean chooseSponsor;
    /**
     * 4是否选中发起人的部门领导
     */
    private Boolean chooseDepHeader;

}
