package org.jeecg.modules.test.feign.controller;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.boot.starter.rabbitmq.client.RabbitMqClient;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.test.feign.client.JeecgTestClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;


@Slf4j
@RestController
@RequestMapping("/sys/test")
@Api(tags = "【微服务】单元测试")
public class JeecgTestFeignController {

    @Autowired
    private JeecgTestClient jeecgTestClient;
//    @Autowired
//    private RabbitMqClient rabbitMqClient;
//     @Autowired
//    private JeecgFeignService jeecgFeignService;

    @GetMapping("/getMessage")
    @ApiOperation(value = "测试feign", notes = "测试feign")
    @SentinelResource(value = "test_more_getMessage", fallback = "getDefaultUser")
    public Result<Object> getMessage(@RequestParam(value = "name", required = false) String name) {
        log.info("---------Feign fallbackFactory优先级高于@SentinelResource-----------------");
        return jeecgTestClient.getMessage("fegin——jeecg-boot1");
    }

    /**
     * 测试方法：关闭demo服务，访问请求 http://127.0.0.1:9999/sys/test/getMessage
     *
     * @param name
     * @return
     */
    @GetMapping("/getMessage2")
    @ApiOperation(value = "测试feign2", notes = "测试feign2")
    public Result<Object> getMessage2(@RequestParam(value = "name", required = false) String name) {
        log.info("---------测试 Feign fallbackFactory-----------------");
        return jeecgTestClient.getMessage("fegin——jeecg-boot2");
    }


//    @GetMapping("getMessage2")
//    @ApiOperation(value = "测试动态feign", notes = "测试动态feign")
//    public Result<String> getMessage2() {
//        JeecgTestClientDyn myClientDyn = jeecgFeignService.newInstance(JeecgTestClientDyn.class, CloudConstant.SERVER_NAME_JEECGDEMO);
//        return myClientDyn.getMessage("动态fegin——jeecg-boot2");
//    }

    @GetMapping("/fallback")
    @ApiOperation(value = "测试熔断", notes = "测试熔断")
    @SentinelResource(value = "test_more_fallback", fallback = "getDefaultUser")
    public Result<Object> test(@RequestParam(value = "name", required = false) String name) {
        if (StringUtils.isEmpty(name)) {
            throw new IllegalArgumentException("name param is empty");
        }
        return Result.OK();
    }

    /**
     * 熔断，默认回调函数
     *
     * @param name
     * @return
     */
    public Result<Object> getDefaultUser(String name) {
        log.info("熔断，默认回调函数");
        return Result.OK("访问超时, 自定义 @SentinelResource Fallback");
    }
}
