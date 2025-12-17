package com.nadvolod.order.domain;

import java.util.List;

public record AgentAdvice(String summary, List<String> recommendedActions, String customerMessage) {}
