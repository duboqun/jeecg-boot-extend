package org.jeecg.modules.business.service;

import org.jeecg.modules.business.entity.CesOrderCustomer;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

/**
 * @Description: 订单客户
 * @Author: jeecg-boot
 * @Date:   2021-10-21
 * @Version: V1.0
 */
public interface ICesOrderCustomerService extends IService<CesOrderCustomer> {

	public List<CesOrderCustomer> selectByMainId(String mainId);
}
