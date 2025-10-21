package com.dhbw.broker.graphql;

import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
public class QueryResolver {

    @QueryMapping
    public String ping() {
        return "pong";
    }

    @QueryMapping
    public Map<String, Object> me() {
        return Map.of(
                "id", "demo-user-1",
                "email", "demo@example.com"
        );
    }
}