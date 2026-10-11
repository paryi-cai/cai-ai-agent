package com.cai.caiaiagent.agent;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 超级智能体测试
 *
 * 注意：自主智能体会连续多轮调用大模型和工具，消耗 token 较多，建议按需运行；
 * 智能体实例是有状态的（运行一次后状态变为 FINISHED），每个用例单独创建实例
 */
@SpringBootTest
class YuManusTest {

    /** Spring 容器管理的智能体实例 */
    @Resource
    private YuManus yuManus;

    @Resource
    private ToolCallback[] allTools;

    @Resource
    private ChatModel dashscopeChatModel;

    /**
     * 冒烟测试（低成本）：验证 Agent Loop 和工具调用链路是否正常
     * 故意限制最大步数为 4：查时间 → 终止，两三个回合就能验证"自主停止"生效
     */
    @Test
    void runSimple() {
        // 新建独立实例（有状态对象，避免和完整任务测试互相影响）
        YuManus agent = new YuManus(allTools, dashscopeChatModel);
        agent.setMaxSteps(4);
        String answer = agent.run("请调用时间工具查询当前时间，然后调用终止工具结束任务");
        System.out.println("===== 冒烟测试执行结果 =====");
        System.out.println(answer);
        Assertions.assertNotNull(answer);
    }

    /**
     * 完整任务测试（教程原题）：验证智能体自主规划完成复杂多步任务
     * 任务链路（AI 自主决定）：联网搜索约会地点 → 处理图片 → 生成 PDF
     */
    @Test
    void run() {
        String userPrompt = """
                我的另一半居住在上海静安区，请帮我找到 5 公里内合适的约会地点，
                并结合一些网络图片，制定一份详细的约会计划，
                并以 PDF 格式输出""";
        String answer = yuManus.run(userPrompt);
        System.out.println("===== 完整任务执行结果 =====");
        System.out.println(answer);
        Assertions.assertNotNull(answer);
    }
}
