package com.anishan.api.client.content;

import lombok.Data;
import lombok.Getter;
import okhttp3.Headers;

import java.io.FilterInputStream;
import java.io.InputStream;

@Getter
public class OssFileInputStream extends FilterInputStream {

    private final Headers headers;

    private final String bucket;

    private final String region;

    private final String filename;

    public OssFileInputStream(InputStream in, Headers headers, String bucket, String region, String filename) {
        super(in);
        this.bucket = bucket;
        this.region = region;
        this.filename = filename;
        this.headers = headers;
    }

}
