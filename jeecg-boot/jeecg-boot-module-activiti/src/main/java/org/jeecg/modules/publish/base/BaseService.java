package org.jeecg.modules.publish.base;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.modules.publish.exception.MyException;
import org.springframework.core.GenericTypeResolver;

import java.math.BigDecimal;
import java.util.Collection;

/**
 * 基础服务
 *
 * @author: dongjb
 * @date: 2021/6/7
 */
public class BaseService<T> extends ServiceImpl<BaseMapper<T>, T> implements IService<T> {

    /**
     * 判断非空
     */
    protected static boolean isNotEmpty(Integer value) {
        return value != null;
    }

    /**
     * 判断非空
     */
    protected static boolean isNotEmpty(BigDecimal value) {
        return value != null;
    }

    /**
     * 字符串判断非空
     */
    protected static boolean isNotEmpty(String str) {
        return StringUtils.isNotEmpty(str);
    }

    /**
     * 集合判断是否非空
     */
    protected static boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }

    /**
     * 集合判断是否空
     */
    protected static boolean isEmpty(Collection<?> collection) {
        return CollectionUtils.isEmpty(collection);
    }

    /**
     * 判断某个字段是否唯一，不唯一抛出异常
     *
     * @param fn        要检查的字段
     * @param value     要检查的字段值
     * @param excludeId 要排除的id
     * @param errorMsg  错误提示文字
     */
    public void checkUnique(SFunction<T, Object> fn, Object value, Integer excludeId, String errorMsg) {
        QueryWrapper<T> queryWrapper = new QueryWrapper<>();
        if (excludeId != null) {
            queryWrapper.ne("id", excludeId);
        }
        LambdaQueryWrapper<T> lambdaQueryWrapper = queryWrapper.lambda();
        lambdaQueryWrapper.eq(fn, value);
        int count = this.count(lambdaQueryWrapper);
        if (count > 0) {
            throw new MyException(errorMsg);
        }
    }

    /**
     * 覆写mybatis-plus获取泛型class方式，防止多重继承时无法获取class
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Class<T> currentModelClass() {
        return (Class<T>) GenericTypeResolver.resolveTypeArgument(getClass(), BaseService.class);
    }
}
