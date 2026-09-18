package com.library.borrow.controller;

import com.library.common.result.Result;
import com.library.common.result.ResultCode;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/sentinel")
public class SentinelDemoController {

    @GetMapping("/borrow-demo")
    @SentinelResource(value = "borrowBook", blockHandler = "borrowBookBlockHandler")
    public Result<Map<String, Object>> borrowBookDemo() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("resource", "borrowBook");
        data.put("time", LocalDateTime.now().toString());
        data.put("message", "request passed sentinel");
        return Result.success(data);
    }

    public Result<Map<String, Object>> borrowBookBlockHandler(BlockException ex) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("resource", "borrowBook");
        data.put("blockedBy", ex.getClass().getSimpleName());
        data.put("message", "request blocked by sentinel");
        return Result.error(ResultCode.SYSTEM_BUSY.getCode(), "Sentinel blocked borrowBook");
    }

    @PostMapping("/borrow-demo/rule")
    public Result<Map<String, Object>> configureBorrowBookRule(
            @RequestParam(defaultValue = "1") double qps) {
        FlowRule rule = new FlowRule();
        rule.setResource("borrowBook");
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule.setCount(qps);
        rule.setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_DEFAULT);

        FlowRuleManager.loadRules(Collections.singletonList(rule));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("resource", "borrowBook");
        data.put("qps", qps);
        data.put("message", "flow rule loaded");
        return Result.success(data);
    }

    @DeleteMapping("/borrow-demo/rule")
    public Result<Map<String, Object>> clearBorrowBookRule() {
        FlowRuleManager.loadRules(Collections.emptyList());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("resource", "borrowBook");
        data.put("message", "flow rule cleared");
        return Result.success(data);
    }
}
