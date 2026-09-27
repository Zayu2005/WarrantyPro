package com.warrantypro.common.event;

/**
 * 工单受理事件：客服受理后发布，由派单模块监听并执行智能体直派。
 * 以事件解耦 order → dispatch 依赖（dispatch 依赖 order 的 Mapper，反向只能走事件）。
 */
public class OrderAcceptedEvent {

    private final Long orderId;

    public OrderAcceptedEvent(Long orderId) {
        this.orderId = orderId;
    }

    public Long getOrderId() {
        return orderId;
    }
}
