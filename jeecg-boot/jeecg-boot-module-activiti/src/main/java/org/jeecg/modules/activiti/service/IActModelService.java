package org.jeecg.modules.activiti.service;

import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.activiti.vo.ProcessDeploymentVo;

import java.io.InputStreamReader;

/**
 * activiti 模型服务
 *
 * @author dongjb
 * @date 2021/11/15
 */
public interface IActModelService {
    /**
     * 创建模型
     *
     * @param reader 输入文件读取
     */
    Result<Object> createModel(InputStreamReader reader, ProcessDeploymentVo deployment);

    /**
     * 发布流程
     *
     * @param modelId    模型标识
     * @param deployment 发布内容
     */
    Result<Object> deployProcess(Result<Object> modelId, ProcessDeploymentVo deployment);
}
