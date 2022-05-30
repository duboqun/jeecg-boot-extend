package org.jeecg.modules.business.service;

import org.jeecg.modules.business.entity.CesOrderGoods;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

/**
 * @Description: 订单商品
 * @Author: jeecg-boot
 * @Date:   2021-10-21
 * @Version: V1.0
 */
public interface ICesOrderGoodsService extends IService<CesOrderGoods> {

	public List<CesOrderGoods> selectByMainId(String mainId);
}
