package org.jeecg.modules.activiti.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.activiti.entity.ActBusiness;

import java.util.List;

/**
 * 流程业务扩展表
 *
 * @author: dongjb
 * @date: 2021/5/27
 */
public interface IActBusinessService extends IService<ActBusiness> {
    List<String> getTableNameList(String schemaName);
    List<String> getColumnNameList(String tableName, String schemaName);
}
