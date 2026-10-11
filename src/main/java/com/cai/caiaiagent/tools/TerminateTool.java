package com.cai.caiaiagent.tools;

import org.springframework.ai.tool.annotation.Tool;

/**
 * 任务终止工具：智能体完成任务后调用它主动结束 Agent Loop
 *
 * 作用：没有它，智能体只能靠"达到最大步数"被动停止，无法自主判断"任务已完成"
 * 注意：工具名默认为方法名 doTerminate，ToolCallAgent 靠它把状态置为 FINISHED
 */
public class TerminateTool {

    // 用 @Tool 描述清楚"什么时候该调用我"，模型靠这句话决定何时结束任务
    @Tool(description = """
            Terminate the interaction when the request is met OR if the assistant cannot proceed further with the task.
            When you have finished all the tasks, call this tool to end the work.
            """)
    public String doTerminate() {
        return "任务结束";
    }
}
