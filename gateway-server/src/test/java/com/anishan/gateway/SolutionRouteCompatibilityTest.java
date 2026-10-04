package com.anishan.gateway;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SolutionRouteCompatibilityTest {
    @Test
    void oldSolutionPrefixIsAFirstClassGatewayProxyToContentService() throws Exception {
        String yaml = Files.readString(Paths.get("src/main/resources/application.yml"));
        String route = "(?s)- id: legacy-solution\\s+order: -10\\s+uri: lb://content-service\\s+"
                + "predicates:\\s+- Path=/problem-api/solution/\\*\\*\\s+"
                + "filters:\\s+- StripPrefix=1";

        assertTrue(Pattern.compile(route).matcher(yaml).find(),
                "legacy solution requests must be proxied without a browser redirect before the problem route");
        assertTrue(yaml.contains("- id: content") && yaml.contains("Path=/content-api/**"),
                "the canonical content API route must remain configured");
        assertTrue(yaml.contains("- id: problem") && yaml.contains("Path=/problem-api/**"),
                "other problem-service APIs must keep their original route");
    }
}
