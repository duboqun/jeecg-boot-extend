package org.jeecg.modules.business.vo;

import java.util.List;
import org.jeecg.modules.business.entity.CesOrderMain;
import org.jeecg.modules.business.entity.CesOrderGoods;
import org.jeecg.modules.business.entity.CesOrderCustomer;
import lombok.Data;
import org.jeecgframework.poi.excel.annotation.Excel;
import org.jeecgframework.poi.excel.annotation.ExcelEntity;
import org.jeecgframework.poi.excel.annotation.ExcelCollection;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;
import java.util.Date;
import org.jeecg.common.aspect.annotation.Dict;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * @Description: 订单主表
 * @Author: jeecg-boot
 * @Date:   2021-10-21
 * @Version: V1.0
 */
@Data
@ApiModel(value="ces_order_mainPage对象", description="订单主表")
public class CesOrderMainPage {

	/**id*/
	@ApiModelProperty(value = "id")
    private java.lang.String id;
	/**创建人*/
	@ApiModelProperty(value = "创建人")
    private java.lang.String createBy;
	/**创建日期*/
	@JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern="yyyy-MM-dd")
	@ApiModelProperty(value = "创建日期")
    private java.util.Date createTime;
	/**更新人*/
	@ApiModelProperty(value = "更新人")
    private java.lang.String updateBy;
	/**更新日期*/
	@JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern="yyyy-MM-dd")
	@ApiModelProperty(value = "更新日期")
    private java.util.Date updateTime;
	/**所属部门*/
	@ApiModelProperty(value = "所属部门")
    private java.lang.String sysOrgCode;
	/**订单编码*/
	@Excel(name = "订单编码", width = 15)
	@ApiModelProperty(value = "订单编码")
    private java.lang.String orderCode;
	/**下单时间*/
	@Excel(name = "下单时间", width = 15, format = "yyyy-MM-dd")
	@JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern="yyyy-MM-dd")
	@ApiModelProperty(value = "下单时间")
    private java.util.Date xdDate;
	/**订单总额*/
	@Excel(name = "订单总额", width = 15)
	@ApiModelProperty(value = "订单总额")
    private java.lang.Double money;
	/**备注*/
	@Excel(name = "备注", width = 15)
	@ApiModelProperty(value = "备注")
    private java.lang.String remark;
	/**流程状态*/
	@Excel(name = "流程状态", width = 15)
	@ApiModelProperty(value = "流程状态")
    private java.lang.String bpmStatus;
	/**流程标识*/
	@Excel(name = "流程标识", width = 15)
	@ApiModelProperty(value = "流程标识")
    private java.lang.String bpmId;

	@ExcelCollection(name="订单商品")
	@ApiModelProperty(value = "订单商品")
	private List<CesOrderGoods> cesOrderGoodsList;
	@ExcelCollection(name="订单客户")
	@ApiModelProperty(value = "订单客户")
	private List<CesOrderCustomer> cesOrderCustomerList;

}
