package com.housedesign.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.housedesign.Service.KbDocumentService;
import com.housedesign.common.BusinessException;
import com.housedesign.dto.response.KbDocumentCreateResponse;
import com.housedesign.entity.KbDocumentStatus;

/**
 * 语料导入器单元测试：只验证「遍历、过滤、跳过、失败隔离」这些导入器自身逻辑，
 * 不覆盖 createKbDocument 内部的校验与落库（那属于 KbDocumentService 的测试范围）。
 */
@ExtendWith(MockitoExtension.class)
class KbCorpusImporterTest {

    @Mock
    private KbDocumentService kbDocumentService;

    @TempDir
    Path tempDir;

    private KbCorpusImporter importer;

    @BeforeEach
    void setUp() {
        importer = new KbCorpusImporter(kbDocumentService);
        ReflectionTestUtils.setField(importer, "corpusPath", tempDir.toString());
    }

    // ==================== 目录与文件筛选 ====================

    @Test
    void skipsWhenDirectoryMissing() throws Exception {
        ReflectionTestUtils.setField(importer, "corpusPath", tempDir.resolve("missing").toString());

        importer.run(null);

        verifyNoInteractions(kbDocumentService);
    }

    @Test
    void importsMarkdownRecursivelyAndIgnoresOtherFiles() throws Exception {
        write("style/01-北欧风.md", "# 北欧风格\n浅色打底");
        write("standard/住宅设计尺寸与规范.md", "# 规范\n最小面积");
        write("note.txt", "不是 Markdown，应被忽略");

        importer.run(null);

        Map<String, String> imported = captureImported();
        assertEquals(2, imported.size());
        assertEquals("# 北欧风格\n浅色打底", imported.get("01-北欧风"));
        assertEquals("# 规范\n最小面积", imported.get("住宅设计尺寸与规范"));
    }

    // ==================== 内容长度与空内容 ====================

    @Test
    void skipsBlankAndOversizedFilesButKeepsOthers() throws Exception {
        write("ok.md", "# 正常内容\n正文");
        write("blank.md", " \n\t ");
        write("huge.md", "x".repeat(100_001));

        importer.run(null);

        Map<String, String> imported = captureImported();
        assertEquals(1, imported.size());
        assertEquals("# 正常内容\n正文", imported.get("ok"));
    }

    @Test
    void importsTextAtExactMaxLength() throws Exception {
        String text = "a".repeat(100_000);
        write("limit.md", text);

        importer.run(null);

        Map<String, String> imported = captureImported();
        assertEquals(1, imported.size());
        assertEquals(text, imported.get("limit"));
    }

    // ==================== 异常分类与失败隔离 ====================

    @Test
    void duplicate409IsSkippedAndLoopContinues() throws Exception {
        write("a.md", "# A");
        write("b.md", "# B");
        when(kbDocumentService.createKbDocument(isNull(), anyString(), anyString(), eq("PUBLIC")))
                .thenThrow(new BusinessException(409, "已存在内容相同的文档"))
                .thenReturn(new KbDocumentCreateResponse(1L, KbDocumentStatus.PENDING));

        assertDoesNotThrow(() -> importer.run(null));

        // 第一篇抛 409 后循环不能中断，第二篇仍要提交
        verify(kbDocumentService, times(2)).createKbDocument(isNull(), anyString(), anyString(), eq("PUBLIC"));
    }

    @Test
    void otherBusinessFailureDoesNotStopRemainingFiles() throws Exception {
        write("a.md", "# A");
        write("b.md", "# B");
        when(kbDocumentService.createKbDocument(isNull(), anyString(), anyString(), eq("PUBLIC")))
                .thenThrow(new BusinessException(400, "文本内容过长"))
                .thenReturn(new KbDocumentCreateResponse(2L, KbDocumentStatus.PENDING));

        assertDoesNotThrow(() -> importer.run(null));

        verify(kbDocumentService, times(2)).createKbDocument(isNull(), anyString(), anyString(), eq("PUBLIC"));
    }

    // ==================== 辅助方法 ====================

    /** 抓取全部 createKbDocument 调用，返回「标题 → 正文」映射；条数由调用方断言 */
    private Map<String, String> captureImported() {
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> titleCaptor = ArgumentCaptor.forClass(String.class);
        verify(kbDocumentService, atLeastOnce())
                .createKbDocument(isNull(), textCaptor.capture(), titleCaptor.capture(), eq("PUBLIC"));

        List<String> texts = textCaptor.getAllValues();
        List<String> titles = titleCaptor.getAllValues();
        Map<String, String> result = new HashMap<>();
        for (int i = 0; i < titles.size(); i++) {
            result.put(titles.get(i), texts.get(i));
        }
        return result;
    }

    private void write(String relativePath, String content) throws IOException {
        Path file = tempDir.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }
}
