package com.youlai.boot.support.file.service;

import com.youlai.boot.support.file.model.FileInfo;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储服务接口
 *
 * @author haoxr
 * @since 2022/11/19
 */
public interface FileService {

    /**
     * 上传文件
     *
     * @param file 表单文件对象
     * @return 文件信息
     */
    FileInfo uploadFile(MultipartFile file);

    /**
     * 删除文件
     *
     * @param filePath 文件完整URL
     * @return 删除结果
     */
    boolean deleteFile(String filePath);

}
