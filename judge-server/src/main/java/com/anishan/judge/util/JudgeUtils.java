package com.anishan.judge.util;

import com.anishan.commons.e.JudgeResult;

import java.util.*;

/**
 * @Author Himit_ZH
 * @Date 2021/11/24 19:16
 * @Description
 */
public class JudgeUtils {


    public static List<String> translateCommandline(String toProcess) {
        if (toProcess != null && !toProcess.isEmpty()) {
            int state = 0;
            StringTokenizer tok = new StringTokenizer(toProcess, "\"' ", true);
            List<String> result = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            boolean lastTokenHasBeenQuoted = false;

            while (true) {
                while (tok.hasMoreTokens()) {
                    String nextTok = tok.nextToken();
                    switch (state) {
                        case 1:
                            if ("'".equals(nextTok)) {
                                lastTokenHasBeenQuoted = true;
                                state = 0;
                            } else {
                                current.append(nextTok);
                            }
                            continue;
                        case 2:
                            if ("\"".equals(nextTok)) {
                                lastTokenHasBeenQuoted = true;
                                state = 0;
                            } else {
                                current.append(nextTok);
                            }
                            continue;
                    }

                    if ("'".equals(nextTok)) {
                        state = 1;
                    } else if ("\"".equals(nextTok)) {
                        state = 2;
                    } else if (" ".equals(nextTok)) {
                        if (lastTokenHasBeenQuoted || current.length() > 0) {
                            result.add(current.toString());
                            current.setLength(0);
                        }
                    } else {
                        current.append(nextTok);
                    }

                    lastTokenHasBeenQuoted = false;
                }

                if (lastTokenHasBeenQuoted || current.length() > 0) {
                    result.add(current.toString());
                }

                if (state != 1 && state != 2) {
                    return result;
                }

                throw new RuntimeException("unbalanced quotes in " + toProcess);
            }
        } else {
            return new ArrayList<>();
        }
    }
    public static JudgeResult judgeToStatus(Integer judge) {
        JudgeResult result;
        switch (judge) {

            case 0: // AC
                result = JudgeResult.Accept;
                break;
            case -1:
                result = JudgeResult.WrongAnswer;
                break;
            case 1:
                result = JudgeResult.TimeLimitExceeded;
                break;
            case 2:
                result = JudgeResult.MemoryLimitExceeded;
                break;
            case 3:
            default:
                result = JudgeResult.RuntimeError;
        }
        return result;
    }

}