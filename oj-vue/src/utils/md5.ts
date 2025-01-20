import SparkMD5 from 'spark-md5'

const DEFAULT_SIZE = 10 * 1024 * 1024
export const md5 = (file: File, chunkSize = DEFAULT_SIZE): Promise<string> => {
    return new Promise((resolve, reject) => {
        
        // 这里是为了兼容性吧
        let blobSlice =
            File.prototype.slice ||
            // @ts-ignore 
            File.prototype.mozSlice ||
            // @ts-ignore 
            File.prototype.webkitSlice;
        let chunks = Math.ceil(file.size / chunkSize);
        let currentChunk = 0;
        let spark = new SparkMD5.ArrayBuffer(); //追加数组缓冲区。
        let fileReader = new FileReader(); //读取文件
        fileReader.onload = function (e) {
            // @ts-ignore
            spark.append(e?.target?.result);
            currentChunk++;
            if (currentChunk < chunks) {
                loadNext();
            } else {
                const md5 = spark.end(); //完成md5的计算，返回十六进制结果。
                resolve(md5);
            }
        };
        fileReader.onerror = function (e) {
            reject(e);
        };

        function loadNext() {
            let start = currentChunk * chunkSize;
            let end = start + chunkSize;
            (end > file.size) && (end = file.size);
            fileReader.readAsArrayBuffer(blobSlice.call(file, start, end));
        }
        loadNext();
    });
}