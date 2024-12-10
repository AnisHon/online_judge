package com.anishan.api.client.gojudge.enumeration;

import com.anishan.commons.enumeration.Enumerator;
import lombok.Getter;

@Getter
public enum FileErrorType implements Enumerator<String> {
    CopyInOpenFile("CopyInOpenFile"),
    CopyInCreateFile("CopyInCreateFile"),
    CopyInCopyContent("CopyInCopyContent"),
    CopyOutOpen("CopyOutOpen"),
    CopyOutNotRegularFile("CopyOutNotRegularFile"),
    CopyOutSizeExceeded("CopyOutSizeExceeded"),
    CopyOutCreateFile("CopyOutCreateFile"),
    CopyOutCopyContent("CopyOutCopyContent"),
    CollectSizeExceeded("CollectSizeExceeded"),
    ;
    private final String value;

    FileErrorType(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }
}
