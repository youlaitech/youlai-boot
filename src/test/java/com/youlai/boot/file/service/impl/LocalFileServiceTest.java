package com.youlai.boot.file.service.impl;

import com.youlai.boot.common.exception.BusinessException;
import com.youlai.boot.file.config.LocalFileProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalFileServiceTest {

    @TempDir
    Path storagePath;

    @Test
    void should_reject_path_outside_storage_root() {
        LocalFileProperties properties = new LocalFileProperties();
        properties.setStoragePath(storagePath.toString());
        LocalFileService fileService = new LocalFileService(properties);

        assertThatThrownBy(() -> fileService.deleteFile("../secret.txt"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("文件路径不属于当前本地存储配置");
    }
}
