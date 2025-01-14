package com.anishan.api.client.content.domain;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class LinkReferenceCount {
    private String path;
    private Long count;
}
