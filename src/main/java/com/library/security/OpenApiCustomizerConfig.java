package com.library.security;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiCustomizerConfig {

    @Bean
    public OpenApiCustomizer globalResponsesCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) return;
            for (PathItem pathItem : openApi.getPaths().values()) {
                for (Operation op : pathItem.readOperations()) {
                    ApiResponses responses = op.getResponses();
                    if (responses == null) {
                        responses = new ApiResponses();
                    }
                    if (!responses.containsKey("401")) {
                        responses.addApiResponse("401", new ApiResponse().description("Unauthorized"));
                    }
                    if (!responses.containsKey("403")) {
                        responses.addApiResponse("403", new ApiResponse().description("Forbidden"));
                    }
                    op.setResponses(responses);
                }
            }
        };
    }
}
