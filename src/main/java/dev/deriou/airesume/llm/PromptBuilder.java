package dev.deriou.airesume.llm;

import dev.deriou.airesume.entity.Job;
import dev.deriou.airesume.entity.Resume;
import org.springframework.stereotype.Component;

@Component
public class PromptBuilder {

    public String buildResumeScorePrompt(Resume resume, String targetDirection) {
        return """
                你是面向大学生求职场景的简历评估助手。
                请根据目标方向对简历进行评分。
                只返回合法 JSON, 不要 Markdown 代码块, 不要解释性前后缀。
                所有分数必须是 0 到 100 的整数, 语言使用中文。

                目标方向:
                %s

                简历内容:
                %s

                返回格式:
                {
                  "overallScore": 82,
                  "dimensions": [
                    {
                      "name": "项目匹配度",
                      "score": 85,
                      "comment": "项目经历与目标岗位较匹配"
                    }
                  ],
                  "suggestions": [
                    "补充 Prometheus/Grafana 监控指标截图",
                    "将 Redis 使用场景写得更具体"
                  ]
                }
                """.formatted(targetDirection, resume.getContentMd());
    }

    public String buildResumeOptimizePrompt(Resume resume, String targetDirection) {
        return """
                你是面向大学生求职场景的简历优化助手。
                请根据目标方向给出简历优化建议, 不要改写成虚假经历。
                只返回合法 JSON, 不要 Markdown 代码块, 不要解释性前后缀。
                语言使用中文。

                目标方向:
                %s

                简历内容:
                %s

                返回格式:
                {
                  "summary": "整体方向正确, 但需要强化指标化表达。",
                  "optimizedBullets": [
                    "使用 Docker Compose 搭建 MySQL/Redis 本地环境, 支撑登录态与业务数据调试。"
                  ],
                  "rewriteSuggestions": [
                    "将'了解 Redis'改为'使用 Redis 承载验证码、登录态和热点缓存'。"
                  ]
                }
                """.formatted(targetDirection, resume.getContentMd());
    }

    public String buildJobMatchPrompt(Resume resume, Job job) {
        return """
                你是招聘匹配助手。
                请比较简历和岗位 JD, 输出匹配分数、优势、短板和改进建议。
                只返回合法 JSON, 不要 Markdown 代码块, 不要解释性前后缀。
                匹配分数必须是 0 到 100 的整数, 语言使用中文。

                简历内容:
                %s

                岗位标题:
                %s

                岗位 JD:
                %s

                技术栈:
                %s

                返回格式:
                {
                  "matchScore": 78,
                  "strengths": [
                    "简历中有 Spring Boot 和 Redis 项目经历"
                  ],
                  "gaps": [
                    "缺少实际线上故障处理案例"
                  ],
                  "suggestions": [
                    "补充 Jenkins 或 GitOps 发布流程"
                  ]
                }
                """.formatted(
                resume.getContentMd(),
                job.getTitle(),
                job.getJdContent(),
                job.getTechStack() != null ? job.getTechStack() : ""
        );
    }
}
