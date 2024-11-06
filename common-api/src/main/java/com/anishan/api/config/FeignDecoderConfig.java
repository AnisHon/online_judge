package com.anishan.api.config;

import com.anishan.api.util.FeignResultDecoder;
import feign.codec.Decoder;
import org.springframework.context.annotation.Bean;

public class FeignDecoderConfig {

    @Bean
    public Decoder feignDecoder() {
        return new FeignResultDecoder();
    }
}
