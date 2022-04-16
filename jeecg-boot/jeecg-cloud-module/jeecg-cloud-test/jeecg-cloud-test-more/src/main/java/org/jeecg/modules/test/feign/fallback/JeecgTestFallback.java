package org.jeecg.modules.test.feign.fallback;

import org.jeecg.common.api.vo.Result;

import lombok.Setter;
import org.jeecg.modules.test.feign.client.JeecgTestClient;


/**
* 接口fallback实现
* 
* @author: scott
* @date: 2022/4/11 19:41
*/
public class JeecgTestFallback implements JeecgTestClient {

    @Setter
    private Throwable cause;


    @Override
    public Result<Object> getMessage(String name) {
        return Result.OK("访问超时, 自定义FallbackFactory");
    }
}
