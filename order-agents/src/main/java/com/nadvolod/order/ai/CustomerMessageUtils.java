package com.nadvolod.order.ai;

import com.nadvolod.order.domain.AgentAdvice;

/**
 * Utility class for customer message generation logic shared across implementations.
 */
final class CustomerMessageUtils {

    private CustomerMessageUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Determines the message type based on keywords in the advice summary.
     */
    enum MessageType {
        SUCCESS,
        FAILURE,
        NEUTRAL
    }

    /**
     * Analyzes the advice summary to determine the appropriate message type.
     *
     * @param advice the agent advice containing the summary
     * @return the message type based on keywords found
     */
    static MessageType determineMessageType(AgentAdvice advice) {
        String summaryLower = advice.summary().toLowerCase();

        if (summaryLower.contains("accept") || summaryLower.contains("success")) {
            return MessageType.SUCCESS;
        } else if (summaryLower.contains("reject") || summaryLower.contains("fail")) {
            return MessageType.FAILURE;
        } else {
            return MessageType.NEUTRAL;
        }
    }
}
