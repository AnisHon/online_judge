// 根据文件名后缀区分 文件类型

export enum MatchType {
    OTHER = 'other',
    IMAGE = 'image',
    TEXT = 'txt',
    EXCEL = 'excel',
    WORD = 'word',
    PDF = 'pdf',
    PPT = 'ppt',
    VIDEO = 'video',
    RADIO = 'radio',
    ZIP = 'zip',
    FOLDER = 'folder',
}

/*
 * @param: fileName - 文件名称
 * @param: 数据返回 1) 无后缀匹配 - other
 * @param: 数据返回 2) 匹配图片 - image
 * @param: 数据返回 3) 匹配 txt - txt
 * @param: 数据返回 4) 匹配 excel - excel
 * @param: 数据返回 5) 匹配 word - word
 * @param: 数据返回 6) 匹配 pdf - pdf
 * @param: 数据返回 7) 匹配 ppt - ppt
 * @param: 数据返回 8) 匹配 视频 - video
 * @param: 数据返回 9) 匹配 音频 - radio
 * @param: 数据返回 10) 匹配 压缩包 - zip
 * @param: 数据返回 11) 其他匹配项 - other
 */
export const matchType = (fileName: string): MatchType => {
    // 后缀获取
    let suffix = '';
    // 获取类型结果
    let result: MatchType;
    let matched: boolean = false;
    try {
        let flieArr = fileName.split('.');
        suffix = flieArr[flieArr.length - 1];
    } catch (err) {
        suffix = '';
    }
    // fileName无后缀返回 false
    if (!suffix) {
        result = MatchType.OTHER;
        return result;
    }
    // 图片格式
    let imgList = ['png', 'jpg', 'jpeg', 'bmp', 'gif'];
    // 进行图片匹配
    matched = imgList.some(function (item) {
        return item == suffix;
    });
    if (matched) {
        result = MatchType.IMAGE;
        return result;
    }
    // 匹配txt
    let txtList = ['txt', 'json', 'c', 'cpp', 'h', 'hpp', 'java', 'python'];
    matched = txtList.some(function (item) {
        return item == suffix;
    });
    if (matched) {
        result = MatchType.TEXT;
        return result;
    }
    // 匹配 excel
    let excelList = ['xls', 'xlsx'];
    matched = excelList.some(function (item) {
        return item == suffix;
    });
    if (matched) {
        result = MatchType.EXCEL;
        return result;
    }
    // 匹配 word
    let wordList = ['doc', 'docx'];
    matched = wordList.some(function (item) {
        return item == suffix;
    });
    if (matched) {
        result = MatchType.WORD;
        return result;
    }
    // 匹配 pdf
    let pdfList = ['pdf'];
    matched = pdfList.some(function (item) {
        return item == suffix;
    });
    if (matched) {
        result = MatchType.PDF;
        return result;
    }
    // 匹配 ppt
    let pptList = ['ppt', 'pps'];
    matched = pptList.some(function (item) {
        return item == suffix;
    });
    if (matched) {
        result = MatchType.PPT;
        return result;
    }
    // 匹配 视频
    let videoList = ['mp4', 'm2v', 'mkv'];
    matched = videoList.some(function (item) {
        return item == suffix;
    });
    if (matched) {
        result = MatchType.VIDEO;
        return result;
    }
    // 匹配 音频
    let radioList = ['mp3', 'wav', 'wmv'];
    matched = radioList.some(function (item) {
        return item == suffix;
    });
    if (matched) {
        result = MatchType.RADIO;
        return result;
    }

    // 匹配 压缩包
    let packageList = ['zip', 'rar', 'tar', '7z'];
    matched = packageList.some(function (item) {
        return item == suffix;
    });
    if (matched) {
        result = MatchType.ZIP;
        return result;
    }

    // 其他 文件类型
    result = MatchType.OTHER;
    return result;
}

