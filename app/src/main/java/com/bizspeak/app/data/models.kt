package com.bizspeak.app.data

/**
 * 对话消息模型。
 * sender = "user" 表示学习者，"assistant" 表示 AI 导师。
 */
data class ChatMessage(
    val id: String = "",
    val sender: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    // AI 回复里解析出的纠错提示（可选）
    val correction: String? = null,
    // AI 回复里解析出的词汇点拨（可选）
    val vocabulary: String? = null,
)

/**
 * 英语水平分级。默认 ELEMENTARY（初级），贴合《剑桥商务标准英语教程·初级》。
 */
enum class Level(val key: String, val label: String) {
    BEGINNER("beginner", "入门 A1"),
    ELEMENTARY("elementary", "初级 A2"),
    INTERMEDIATE("intermediate", "中级 B1"),
    UPPER("upper", "中高级 B2"),
    ADVANCED("advanced", "高级 C1+");

    companion object {
        fun fromKey(key: String): Level = entries.firstOrNull { it.key == key } ?: ELEMENTARY
    }
}

/**
 * 商务对话场景。
 */
enum class Scenario(
    val key: String,
    val title: String,
    val description: String,
    val starter: String,
) {
    PHONE(
        "phone",
        "商务电话",
        "模拟接打商务电话（预约、确认、留言）",
        "Hello, this is [Your Name] from [Company]. May I speak to [Client], please?",
    ),
    MEETING(
        "meeting",
        "会议讨论",
        "模拟会议发言、表态、总结",
        "Thanks for joining. Let's start with a quick round of introductions, shall we?",
    ),
    RECEPTION(
        "reception",
        "客户接待",
        "模拟接待来访客户、带路、寒暄",
        "Welcome to our office! Did you have any trouble finding us?",
    ),
    INTERVIEW(
        "interview",
        "商务面试",
        "模拟英文面试问答",
        "Thanks for coming in today. Could you start by telling me a little about yourself?",
    ),
    SOCIAL(
        "social",
        "商务社交",
        "模拟展会、午餐、酒会等非正式交流",
        "Hi, I don't think we've met. I'm [Your Name] from the sales team.",
    ),
    NEGOTIATION(
        "negotiation",
        "价格谈判",
        "模拟报价、还价、让步",
        "We're interested in your proposal. Let's talk about the pricing first.",
    );

    companion object {
        fun fromKey(key: String): Scenario = entries.firstOrNull { it.key == key } ?: PHONE
    }
}
