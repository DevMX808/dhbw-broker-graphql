package com.dhbw.broker.graphql.api;

import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
public class QueryController {

    @QueryMapping
    public String ping() {
        return "pong";
    }
}