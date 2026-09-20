package com.anishan.judge.judge;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.anishan.judge.exception.SystemError;

import java.util.HashMap;
import java.util.List;

public interface SandboxRun {
    JSONArray run(JSONObject param) throws SystemError;

    void delFile(String fileId);

    JSONArray compile(Long maxCpuTime,
                      Long maxRealTime,
                      Long maxMemory,
                      Long maxStack,
                      String srcName,
                      String exeName,
                      List<String> args,
                      List<String> envs,
                      String code,
                      HashMap<String, String> extraFiles,
                      Boolean needCopyOutCached,
                      Boolean needCopyOutExe,
                      String copyOutDir) throws SystemError;

    /**
     * @param args            普通评测运行cmd的命令参数
     * @param envs            普通评测运行的环境变量
     * @param testCasePath    题目数据的输入文件路径
     * @param testCaseContent 题目数据的输入数据（与testCasePath二者选一）
     * @param maxTime         评测的最大限制时间 ms
     * @param maxOutputSize   评测的最大输出大小 kb
     * @param maxStack        评测的最大限制栈空间 mb
     * @param exeName         评测的用户程序名称
     * @param fileId          评测的用户程序文件id
     * @param fileContent     评测的用户程序文件内容，如果userFileId存在则为null
     * @param isFileIO        是否为文件IO
     * @param ioReadFileName  题目指定的io输入文件的名称
     * @param ioWriteFileName 题目指定的io输出文件的名称
     * @MethodName testCase
     * @Description 普通评测
     * @Return JSONArray
     * @Since 2022/1/3
     */
    JSONArray testCase(List<String> args,
                       List<String> envs,
                       String testCasePath,
                       String testCaseContent,
                       Long maxTime,
                       Long maxMemory,
                       Long maxOutputSize,
                       Integer maxStack,
                       String exeName,
                       String fileId,
                       String fileContent,
                       Boolean isFileIO,
                       String ioReadFileName,
                       String ioWriteFileName) throws SystemError;

    /**
     * 使用显式资源限制运行用户自定义测试输入。
     * 正式题目判题继续使用 {@link #testCase}，避免两条链路相互影响。
     *
     * @param maxCpuTime      CPU 时间上限，单位 ms
     * @param maxWallTime     墙钟时间上限，单位 ms
     * @param maxMemory       内存上限，单位 KB
     * @param maxOutputSize   标准输出上限，单位 byte
     * @param maxProcessLimit 进程数上限
     * @param maxStack        栈空间上限，单位 MB
     */
    JSONArray testCaseWithLimits(List<String> args,
                                 List<String> envs,
                                 String testCasePath,
                                 String testCaseContent,
                                 Long maxCpuTime,
                                 Long maxWallTime,
                                 Long maxMemory,
                                 Long maxOutputSize,
                                 Integer maxProcessLimit,
                                 Integer maxStack,
                                 String exeName,
                                 String fileId,
                                 String fileContent,
                                 Boolean isFileIO,
                                 String ioReadFileName,
                                 String ioWriteFileName) throws SystemError;

    /**
     * @param args                   特殊判题的运行cmd命令参数
     * @param envs                   特殊判题的运行环境变量
     * @param userOutputFilePath     用户程序输出文件的路径
     * @param userOutputFileName     用户程序输出文件的名字
     * @param testCaseInputFilePath  题目数据的输入文件的路径
     * @param testCaseInputFileName  题目数据的输入文件的名字
     * @param testCaseOutputFilePath 题目数据的输出文件的路径
     * @param testCaseOutputFileName 题目数据的输出文件的路径
     * @param spjExeSrc              特殊判题的exe文件的路径
     * @param spjExeName             特殊判题的exe文件的名字
     * @MethodName spjCheckResult
     * @Description 特殊判题的评测
     * @Return JSONArray
     * @Since 2022/1/3
     */
    JSONArray spjCheckResult(List<String> args,
                             List<String> envs,
                             String userOutputFilePath,
                             String userOutputFileName,
                             String testCaseInputFilePath,
                             String testCaseInputFileName,
                             String testCaseOutputFilePath,
                             String testCaseOutputFileName,
                             String spjExeSrc,
                             String spjExeName) throws SystemError;

    JSONArray interactTestCase(List<String> args,
                               List<String> envs,
                               String userExeName,
                               String userFileId,
                               String userFileContent,
                               Long userMaxTime,
                               Long userMaxMemory,
                               Integer userMaxStack,
                               String testCaseInputPath,
                               String testCaseInputFileName,
                               String testCaseOutputFilePath,
                               String testCaseOutputFileName,
                               String userOutputFileName,
                               List<String> interactArgs,
                               List<String> interactEnvs,
                               String interactExeSrc,
                               String interactExeName) throws SystemError;
}
