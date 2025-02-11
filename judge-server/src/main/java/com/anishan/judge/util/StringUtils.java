package com.anishan.judge.util;

import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Objects;

@Slf4j
public class StringUtils {
    public static boolean contentEqualsIgnoreBlankAndEOL(Reader input1, Reader input2) throws IORuntimeException {
        final BufferedReader br1 = IoUtil.getReader(input1);
        final BufferedReader br2 = IoUtil.getReader(input2);



        try {
            String line1 = StrUtil.trimEnd(br1.readLine());
            String line2 = StrUtil.trimEnd(br2.readLine());

            while (line1 != null && line1.equals(line2)) {
                line1 = StrUtil.trimEnd(br1.readLine());
                line2 = StrUtil.trimEnd(br2.readLine());
            }
            return Objects.equals(line1, line2);
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
    }
}
