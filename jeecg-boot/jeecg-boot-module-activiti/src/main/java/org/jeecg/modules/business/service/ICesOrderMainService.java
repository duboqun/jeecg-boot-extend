package org.jeecg.modules.business.service;

import org.jeecg.modules.business.entity.CesOrderGoods;
import org.jeecg.modules.business.entity.CesOrderCustomer;
import org.jeecg.modules.business.entity.CesOrderMain;
import com.baomidou.mybatisplus.extension.service.IService;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * @Description: 订单主表
 * @Author: jeecg-boot
 * @Date:   2021-10-21
 * @Version: V1.0
 */
public interface ICesOrderMainService extends IService<CesOrderMain> {

	/**
	 * 添加一对多
	 * 
	 */
	public void saveMain(CesOrderMain cesOrderMain,List<CesOrderGoods> cesOrderGoodsList,List<CesOrderCustomer> cesOrderCustomerList) ;
	
	/**
	 * 修改一对多
	 * 
	 */
	public void updateMain(CesOrderMain cesOrderMain,List<CesOrderGoods> cesOrderGoodsList,List<CesOrderCustomer> cesOrderCustomerList);
	
	/**
	 * 删除一对多
	 */
	public void delMain (String id);
	
	/**
	 * 批量删除一对多
	 */
	public void delBatchMain (Collection<? extends Serializable> idList);
	
}
