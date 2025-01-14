package com.anishan.api.client.content.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Accessors(chain = true)
public class OSSFileInfo {

    /**
     * hash值
     */
    private String hash;

    /**
     * 文件名
     */
    private String filename;

    /**
     * 所有者ID
     */
    private String ownerId;

    /**
     * 所有者名
     */
    private String ownerName;

    /**
     * 是否是文件夹
     */
    private boolean dir;

    /**
     * 最后修改时间
     */
    private LocalDateTime lastModified;

    /**
     * 文件大小
     */
    private long size;

    /**
     * 存储级别，标准存储，低频访问存储，归档存储
     */
    private String storageClass;

    /**
     * 是否为最新版本
     */
    private boolean latest; // except ListObjects V1

    /**
     * 版本ID
     */
    private String versionId; // except ListObjects V1

    /**
     * 用户meta信息
     */
    private Map<String, String> meta;

}
