package org.jeecg.modules.publish.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.jeecg.modules.publish.base.BaseEntity;

import java.util.Date;

/**
 * 应用版本实体类
 *
 * @author: dongjb
 * @date: 2021/6/7
 */
@ApiModel("应用版本列表")
@Getter
@Setter
@TableName("pub_app_version")
public class AppVersion extends BaseEntity {

    @ApiModelProperty("所属应用id")
    private Integer appId;

    @ApiModelProperty("应用名称")
    private String name;

    @ApiModelProperty("版本")
    private String versionName;

    @ApiModelProperty("构建版本")
    private Integer versionCode;

    @ApiModelProperty("大小，kb")
    private Long size;

    @ApiModelProperty("图标")
    private String icon;

    @ApiModelProperty("下载链接")
    private String downloadUrl;

    @ApiModelProperty("下载次数")
    private int downloadCount;

    @ApiModelProperty("更新日志")
    private String changeLog;

    @ApiModelProperty("上传时间")
    @TableField(fill = FieldFill.INSERT, updateStrategy = FieldStrategy.NEVER)
    private Date createTime;
}
