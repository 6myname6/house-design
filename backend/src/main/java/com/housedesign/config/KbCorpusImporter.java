package com.housedesign.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.housedesign.Service.KbDocumentService;
import com.housedesign.common.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 开发期语料导入器：应用启动后把 docs/kb-corpus 下的 Markdown 批量写入知识库。
 *
 * <p>刻意不绕过业务逻辑：每篇都调用 {@link KbDocumentService#createKbDocument}，
 * 与接口上传走同一条校验 / 查重 / 落库路径，因此导入本身就是入库链路的回归用例。</p>
 *
 * <p>只在 {@code app.rag.corpus-import.enabled=true} 时装配，生产保持关闭；
 * 重复启动不会产生重复文档（内容哈希查重返回 409，按“已存在跳过”处理）。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.rag.corpus-import", name = "enabled", havingValue = "true")
public class KbCorpusImporter implements ApplicationRunner {
    /** 与接口的文本长度上限保持一致，避免绕过 Controller 写入超长正文 */
    private static final int MAX_TEXT_CHARS = 100000;

    /** 文档服务：复用正式的创建路径（校验、查重、落库、触发异步入库） */
    private final KbDocumentService kbDocumentService;

    /** 语料根目录，相对进程工作目录；可用环境变量 KB_CORPUS_PATH 覆盖 */
    @Value("${app.rag.corpus-import.path}")
    private String corpusPath;

    /**
     * 启动后执行导入。单篇失败不影响后续文件，最后打印汇总统计。
     *
     * @param args 启动参数（本类未使用）
     * @throws Exception 仅在目录遍历等不可恢复场景抛出
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 相对路径转绝对路径，日志里能直接看出实际扫描目录
        Path root = Paths.get(corpusPath).toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) {
            log.warn("语料目录不存在，跳过导入 path={}", root);
            return;
        }
        log.info("语料目录就绪 root={}", root);
        List<Path> files = listMarkdownFiles(root);
        int created = 0, skipped = 0, failed = 0;   // 新建 / 跳过 / 失败计数
        for (Path file : files) {
            String fileName = file.getFileName().toString();
            String title = stripExtension(fileName);    // 标题默认取文件名（去扩展名）
            try {
                String text = Files.readString(file, StandardCharsets.UTF_8);
                if (text.isBlank() || text.length() > MAX_TEXT_CHARS) {
                    log.warn("语料内容为空或超长，跳过 file = {},chars={}", fileName, text.length());
                    skipped++;
                    continue;
                }
                kbDocumentService.createKbDocument(null, text, title, "PUBLIC");
                created++;
                log.info("语料导入成功title={}", title);
            } catch (BusinessException e) {
                // 409 = 内容哈希重复，属于幂等场景；其他业务错误只记失败，不中断循环
                if (e.getCode() == 409) {
                    skipped++;
                    log.info("语料已存在，跳过title={}", title);
                } else {
                    failed++;
                    log.warn("语料导入失败title={}，reason={}", title, e);
                }
            }
        }
        log.info("语料导入结束root={}共{}篇，新建{}，跳过{}，失败{}", root, files.size(), created, skipped, failed);

    }

    /**
     * 递归收集目录下所有 Markdown 文件，并按路径排序保证每次顺序一致。
     *
     * @param root 语料根目录
     * @return 排序后的 .md 文件列表；遍历失败返回空列表
     */
    private List<Path> listMarkdownFiles(Path root) {
        try (Stream<Path> stream = Files.walk(root)) {
            return stream.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".md"))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            log.error("遍历语料目录失败，root={}", root, e);
            return List.of();
        }
    }

    /**
     * 去掉文件名扩展名，得到文档标题。
     *
     * @param fileName 原始文件名，如 01-北欧风.md
     * @return 不含扩展名的标题；无扩展名时原样返回
     */
    private String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }
}
