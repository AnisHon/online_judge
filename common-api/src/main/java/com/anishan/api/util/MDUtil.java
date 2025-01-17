package com.anishan.api.util;
import cn.hutool.core.collection.CollectionUtil;
import lombok.extern.slf4j.Slf4j;
import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class MDUtil {

    private static final Parser parser;

    private static final HtmlRenderer renderer;

    static {
        // 创建 Markdown 解析器
        parser = Parser.builder().build();
        renderer = HtmlRenderer.builder().build();
    }

    // 递归处理节点树，提取图片链接
    private static void extractImagesFromNode(Node node, Map<String, Long> imageLinks) {

        if (node instanceof Image) {
            // 如果是图片节点，获取图片链接（destination）
            Image image = (Image) node;
            String url = image.getDestination();
            imageLinks.put(url, imageLinks.getOrDefault(url, 0L) + 1);
        }

        System.out.println(node.getClass().getName());


        Node child = node.getFirstChild();
        while (child != null) {
            extractImagesFromNode(child, imageLinks);
            child = child.getNext();

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

        String render = renderer.render(document);


        extractImagesFromNode(document, map);
    }
    public static Map<String, Long> getLinkReferenceMap(String ... contents) {
        return getLinkReferenceMap(CollectionUtil.toList(contents));
    }

    public static Map<String, Long> getLinkReferenceMap(List<String> contents) {
        HashMap<String, Long> map = new HashMap<>();
        for (String content : contents) {
            getLinkReferenceMap(content, map);
        }
        return map;
    }

}
