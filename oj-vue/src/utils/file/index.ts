/**
 * 最大图片大小，单位MB
 */
export const MAX_IMAGE_SIZE = 5;

/**
 * 最大上传图片数量
 */
export const MAX_IMAGE_COUNT = 5;

/**
 * 返回MB大小
 * @param byte 字节数
 */
export const fileSize = (byte: number): number => {
    //             Byte   kb   * mb
    return byte / (1024 * 1024 * 1024);
}