package com.anishan.judge.util;

import com.anishan.commons.enumeration.JudgeResult;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
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
                result = JudgeResult.ACCEPT;
                break;
            case -1:
                result = JudgeResult.WRONG_ANSWER;
                break;
            case 1:
                result = JudgeResult.TIME_LIMIT_EXCEEDED;
                break;
            case 2:
                result = JudgeResult.MEMORY_LIMIT_EXCEEDED;
                break;
            case 3:
            default:
                result = JudgeResult.RUNTIME_ERROR;
        }
        return result;
    }


    public static boolean equals(InputStream is, String out) throws IOException {
        StringReader reader = new StringReader(out);
        InputStreamReader inputStreamReader = new InputStreamReader(is, StandardCharsets.UTF_8);
        return IOUtils.contentEquals(reader, inputStreamReader);

    }

}