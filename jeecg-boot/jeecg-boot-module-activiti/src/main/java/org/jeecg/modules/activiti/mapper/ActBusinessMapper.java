package org.jeecg.modules.activiti.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.activiti.entity.ActBusiness;

import java.util.List;
import java.util.Map;

/**
 * 流程业务扩展表
 *
 * @author: dongjb
 * @date: 2021/5/28
 */
public interface ActBusinessMapper extends BaseMapper<ActBusiness> {
    @Select("select * from ${tableName} where id = #{tableId}")
    Map<String, Object> getBusiData(@Param("tableId") String tableId, @Param("tableName") String tableName);

    @Select("select * from ${tableName} where id in (${tableIdsSql})")
    List<Map<String, Object>> getBusiDatas(@Param("tableIdsSql") String tableIdsSql, @Param("tableName") String tableName);

    @Insert("${sql}")
    int insertBusiData(@Param("sql") String sql);

    @Update("${sql}")
    int updateBusiData(@Param("sql") String sql);

    @Select("${sql}")
    List<String> getTableNameList(@Param("sql") String sql);

    @Select("${sql}")
    List<String> getColumnNameList(@Param("sql") String sql);

    @Delete("delete from ${tableName} where id = #{tableId}")
    int deleteBusiData(@Param("tableId") String tableId, @Param("tableName") String tableName);

    @Delete("delete from ${subTableName} where ${fkId} = #{tableId}")
    int deleteBusiSubData(@Param("tableId") String tableId, @Param("subTableName") String subTableName, @Param("fkId") String fkId);

    @Select("SELECT ahi.USER_ID_ FROM ACT_HI_IDENTITYLINK ahi\n" +
            "      WHERE TYPE_ = #{type} AND TASK_ID_ = #{taskId}\n" +
            "      LIMIT 1")
    List<String> findUserIdByTypeAndTaskId(@Param("type") String type, @Param("taskId") String taskId);

    @Insert("INSERT INTO ACT_HI_IDENTITYLINK (ID_, TYPE_, USER_ID_, TASK_ID_, PROC_INST_ID_)\n" +
            "      VALUES (#{id}, #{type}, #{userId}, #{taskId}, #{procInstId})")
    int insertHI_IDENTITYLINK(@Param("id") String id, @Param("type") String type, @Param("userId") String userId, @Param("taskId") String taskId, @Param("procInstId") String procInstId);

    @Select("SELECT ari.USER_ID_ FROM ACT_RU_IDENTITYLINK ari\n" +
            "      WHERE TYPE_ = #{type} AND TASK_ID_ = #{taskId}")
    List<String> selectIRunIdentity(@Param("taskId") String taskId, @Param("type") String type);

    @Select("SELECT ari.ID_ FROM ACT_RU_IDENTITYLINK ari\n" +
            "      WHERE TYPE_ = #{type} AND TASK_ID_ = #{taskId} AND USER_ID_ = #{userName}")
    List<String> selectRunIdentityByUser(@Param("taskId") String taskId, @Param("type") String type, @Param("userName") String userName);

    @Update("update ${tableName} set bpm_status = #{actStatus} where id = #{tableId}")
    int updateBusinessStatus(@Param("tableName") String tableName, @Param("tableId") String tableId, @Param("actStatus") String actStatus);

    @Update("update ${tableName} set bpm_status = #{actStatus}, bpm_id = #{actId} where id = #{tableId}")
    int updateBusinessStatusAndId(@Param("tableName") String tableName, @Param("tableId") String tableId, @Param("actStatus") String actStatus,  @Param("actId") String actId);

    @Select("select id from act_z_business where proc_def_id in " +
            "(select id from act_z_process where type_id in" +
            "(select id from sys_category where code like '${type}%')" +
            ")")
    List<String> listByTypeApp(@Param("type") String type);

    @Select(
            "select id from act_z_process where type_id in" +
                    "(select id from sys_category where code like '${type}%')")
    List<String> proc_def_idListByType(@Param("type") String type);

    @Select(
            "select deployment_id from act_z_process where type_id in" +
                    "(select id from sys_category where code like '${type}%')")
    List<String> deployment_idListByType(@Param("type") String type);

    @Select("select * from  sys_user  where (realname like '%${searchVal}%' or username like '%${searchVal}%') and del_flag = 0")
    List<LoginUser> getUsersByName(@Param("searchVal") String searchVal);
}
