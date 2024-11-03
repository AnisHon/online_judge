package com.anishan.api.client.problem;

import com.anishan.api.domain.LoginUser;
import com.anishan.commons.domain.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient(value = "user-service")
public interface AuthClient {

    @RequestMapping("/me")
    R<LoginUser> me();
}
