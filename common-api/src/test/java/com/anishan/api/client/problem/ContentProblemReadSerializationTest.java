package com.anishan.api.client.problem;

import com.anishan.api.client.problem.domain.dto.ContentProblemReadRequest;
import com.anishan.api.client.problem.domain.vo.ContentProblemReadVo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentProblemReadSerializationTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void requestContainsOnlyStringProblemIdsAndFlagsCallerSuppliedIdentity() throws Exception {
        ContentProblemReadRequest request = objectMapper.readValue(
                "{\"problemIds\":[\"9007199254740993\"]}", ContentProblemReadRequest.class);
        assertEquals("9007199254740993", request.getProblemIds().get(0));

        ContentProblemReadRequest forged = objectMapper.readValue(
                "{\"problemIds\":[\"1\"],\"userId\":\"99\",\"permissions\":[\"problem:problem:list\"]}",
                ContentProblemReadRequest.class);
        assertTrue(forged.hasUnsupportedProperties());
        assertEquals("{\"problemIds\":[\"1\"]}", objectMapper.writeValueAsString(forged));
    }

    @Test
    void responseSerializesLongIdentifiersAsExactStringsAndContainsNoProblemBody() throws Exception {
        ContentProblemReadVo response = new ContentProblemReadVo(
                "9007199254740993", true, false, 1, true, true, "题目标题");
        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(response));

        assertTrue(json.get("problemId").isTextual());
        assertEquals("9007199254740993", json.get("problemId").textValue());
        assertTrue(json.get("publicReadable").booleanValue());
        assertTrue(json.get("privateWritable").booleanValue());
        assertTrue(json.get("title").isTextual());
        assertTrue(json.get("description") == null);
        assertTrue(json.get("answer") == null);
    }
}
