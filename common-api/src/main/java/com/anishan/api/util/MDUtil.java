package com.anishan.api.util;
import org.commonmark.node.*;
import org.commonmark.parser.Parser;

import java.util.HashMap;
import java.util.Map;

public class MDUtil {

    private static final Parser parser;

    static {
        // 创建 Markdown 解析器
        parser = Parser.builder().build();
    }

    // 递归处理节点树，提取图片链接
    private static void extractImagesFromNode(Node node, Map<String, Long> imageLinks) {

        if (node instanceof Image) {
            // 如果是图片节点，获取图片链接（destination）
            Image image = (Image) node;
            String url = image.getDestination();
            imageLinks.put(url, imageLinks.getOrDefault(url, 0L) + 1);
        }


        Node child = node.getFirstChild();
        while (child != null) {
            child = child.getNext();
            extractImagesFromNode(child, imageLinks);
        }
    }

    public static Map<String, Long> getLinkReferenceMap(String contents) {
        HashMap<String, Long> map = new HashMap<>();
        getLinkReferenceMap(contents, map);
        return map;
    }
    public static void getLinkReferenceMap(String contents, Map<String, Long> map) {

        // 解析 Markdown 内容，生成节点树
        Node document  = parser.parse(contents);

        extractImagesFromNode(document, map);
    }
    public static Map<String, Long> getLinkReferenceMap(String ... contents) {
        HashMap<String, Long> map = new HashMap<>();
        for (String content : contents) {
            getLinkReferenceMap(content, map);
        }
        return map;
    }

}
