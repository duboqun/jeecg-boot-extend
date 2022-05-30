package org.jeecg.modules.activiti.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 节点派发信息
 *
 * @author dongjb
 * @date 2021/11/22
 */

@Getter
@Setter
@ToString
public class ProcessAsignNodeVo {
    /**
     * 流程节点标识
     */
    private String nodeId;

    /**
     * 节点派发详细信息
     */
    private ProcessNodeSpryVo spry;

}