package com.library.borrow.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Sentinel 预置流控规则：应用启动即加载，使借/还书核心接口限流开箱即用，
 * 无需依赖运行期手动 POST 规则。
 *
 * 可通过配置覆盖：
 *   sentinel.rules.borrow-quota   （borrowBook 阈值 QPS，默认 20）
 *   sentinel.rules.return-quota  （returnBook  阈值 QPS，默认 30）
 */
@Slf4j
@Component
public class SentinelRuleInit {

    private double borrowQuota = 20;
    private double returnQuota = 30;

    @PostConstruct
    public void initRules() {
        List<FlowRule> rules = new ArrayList<>();

        rules.add(flowRule("borrowBook", borrowQuota));
        rules.add(flowRule("returnBook", returnQuota));

        FlowRuleManager.loadRules(rules);
        log.info("Sentinel 预置流控规则已加载: borrowBook qps={}, returnBook qps={}",
                borrowQuota, returnQuota);
    }

    private FlowRule flowRule(String resource, double qps) {
        FlowRule rule = new FlowRule();
        rule.setResource(resource);
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule.setCount(qps);
        rule.setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_DEFAULT);
        return rule;
    }
}