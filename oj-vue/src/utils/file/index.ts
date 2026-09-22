/**
 * 最大图片大小，单位MB
 */
export const MAX_IMAGE_SIZE = 5;

/**
 * 最大上传图片数量
 */
export const MAX_IMAGE_COUNT = 5;

/** 图片前端限制对应的字节数，避免在不同调用方之间混用 MB/GB。 */
export const MAX_IMAGE_SIZE_BYTES = MAX_IMAGE_SIZE * 1024 * 1024;

/**
 * 返回MB大小
 * @param byte 字节数
 */
export const bytesToMegabytes = (byte: number): number => byte / (1024 * 1024);

/** @deprecated 使用 bytesToMegabytes，保留导出以兼容旧调用方。 */
export const fileSize = bytesToMegabytes;
