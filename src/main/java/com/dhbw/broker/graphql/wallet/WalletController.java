package com.dhbw.broker.graphql.wallet;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Controller
public class WalletController {

    private final WebClient webClient;

    public WalletController(WebClient webClient) {
        this.webClient = webClient;
    }

    @QueryMapping
    public Mono<Map<String, Object>> walletBalance() {
        return webClient.get()
                .uri("/api/wallet/balance")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .map(response -> Map.of(
                    "currentBalance", response.get("balance"),
                    "currency", "USD"
                ));
    }

    @QueryMapping
    public Mono<List<Map<String, Object>>> walletTransactions() {
        return webClient.get()
                .uri("/api/wallet/transactions")
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .collectList();
    }
}