package com.anishan.api.util;

import cn.hutool.json.JSONUtil;
import com.anishan.commons.domain.R;
import feign.FeignException;
import feign.Response;
import feign.Util;
import feign.codec.DecodeException;
import feign.codec.Decoder;

import java.io.IOException;
import java.lang.reflect.Type;

public class FeignResultDecoder implements Decoder {

    @Override
    public Object decode(Response response, Type type) throws IOException, FeignException {
        if (response.body() == null) {
            throw new DecodeException(response.status(), "没有返回有效的数据", response.request());
        }
        String bodyStr = Util.toString(response.body().asReader(Util.UTF_8));
        //对结果进行转换
        R<?> bean = JSONUtil.toBean(bodyStr, type, false);
        //如果返回错误，且为内部错误，则直接抛出异常
        if (bean.getCode() != 200) {

            throw new DecodeException(response.status(), "接口返回错误：" + bean.getMessage(), response.request());

        }
        return bean;
    }

}