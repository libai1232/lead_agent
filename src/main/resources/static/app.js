// 页面脚本只调用聊天 API，不复制后端状态机、限流或安全规则。
const chatForm = document.querySelector("#chat-form");
const customerIdInput = document.querySelector("#customer-id");
const messageInput = document.querySelector("#message");
const sendButton = document.querySelector("#send-button");
const chatHistory = document.querySelector("#chat-history");
const leadProduct = document.querySelector("#lead-product");
const leadNeed = document.querySelector("#lead-need");
const leadBudget = document.querySelector("#lead-budget");
const leadTimeline = document.querySelector("#lead-timeline");
const leadRole = document.querySelector("#lead-role");
const leadContact = document.querySelector("#lead-contact");
const handoffSection = document.querySelector("#handoff-section");
const handoffSummary = document.querySelector("#handoff-summary");

/** 使用 textContent 写入内容，避免把客户消息或模型回复当作 HTML 执行。 */
function appendMessage(label, text, className) {
    const emptyTip = chatHistory.querySelector(".empty-tip");
    if (emptyTip) {
        emptyTip.remove();
    }

    const messageElement = document.createElement("p");
    messageElement.className = `chat-message ${className}`;
    messageElement.textContent = `${label}：${text}`;
    chatHistory.appendChild(messageElement);
    chatHistory.scrollTop = chatHistory.scrollHeight;
}

/** 在每轮结果后显示只读 Demo 诊断字段，不允许这些值从前端回写。 */
function appendDiagnostics(result) {
    const displayValue = (value) => value === null || value === undefined ? "-" : String(value);
    const details = [
        `intent=${displayValue(result.intent)}`,
        `dissatisfied=${displayValue(result.dissatisfied)}`,
        `action=${displayValue(result.action)}`,
        `sessionStatus=${displayValue(result.sessionStatus)}`,
        `abnormalCount=${displayValue(result.abnormalCount)}`,
        `lastSentAt=${displayValue(result.lastSentAt)}`,
        `historySize=${displayValue(result.conversationHistorySize)}`
    ].join(" | ");

    appendMessage("调试状态", details, "debug-message");
}

/**
 * 在聊天框外更新累计初筛资料和人工摘要；所有模型文本都通过 textContent 安全展示。
 */
function renderLeadInformation(result) {
    const profile = result.leadProfile || {};
    const displayLeadValue = (value) =>
        typeof value === "string" && value.trim().length > 0 ? value : "未确认";

    leadProduct.textContent = displayLeadValue(profile.interestedProduct);
    leadNeed.textContent = displayLeadValue(profile.customerNeed);
    leadBudget.textContent = displayLeadValue(profile.budget);
    leadTimeline.textContent = displayLeadValue(profile.purchaseTimeline);
    leadRole.textContent = displayLeadValue(profile.decisionRole);
    leadContact.textContent = displayLeadValue(profile.contactPreference);

    const hasHandoffSummary = result.sessionStatus === "HUMAN_ESCALATED"
        && typeof result.handoffSummary === "string"
        && result.handoffSummary.length > 0;
    handoffSection.hidden = !hasHandoffSummary;
    handoffSummary.textContent = hasHandoffSummary ? result.handoffSummary : "";
}

/** 提交客户消息，并根据 ChatResponse 决定是否展示 Agent 回复。 */
async function sendMessage(event) {
    event.preventDefault();

    const customerId = customerIdInput.value.trim();
    const message = messageInput.value.trim();
    if (!customerId || !message) {
        appendMessage("系统", "请填写 customerId 和消息。", "system-message");
        return;
    }

    appendMessage("客户", message, "user-message");
    messageInput.value = "";
    sendButton.disabled = true;

    try {
        const response = await fetch("/api/chat", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({ customerId, message })
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }

        const result = await response.json();
        if (result.silent === true) {
            appendMessage("系统", "本轮 Agent 未自动回复", "system-message");
        } else if (typeof result.message === "string" && result.message.length > 0) {
            appendMessage("Agent", result.message, "agent-message");
        } else {
            appendMessage("系统", "Agent 未返回可显示的回复。", "system-message");
        }
        renderLeadInformation(result);
        appendDiagnostics(result);
    } catch (error) {
        // 页面只展示通用错误，不把后端异常细节暴露到聊天记录。
        appendMessage("系统", "请求失败，请确认服务是否已启动。", "system-message");
    } finally {
        sendButton.disabled = false;
        messageInput.focus();
    }
}

chatForm.addEventListener("submit", sendMessage);
