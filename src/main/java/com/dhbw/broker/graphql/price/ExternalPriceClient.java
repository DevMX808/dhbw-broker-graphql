package com.dhbw.broker.graphql.price;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class ExternalPriceClient {

  private final WebClient.Builder builder;

  @Value("${price.api.base-url:https://api.gold-api.com}")
  private String baseUrl;

  public String fetchPrice(String symbol) {
    return builder.baseUrl(baseUrl).build()
      .get().uri("/price/{symbol}", symbol)
      .retrieve()
      .bodyToMono(String.class)
      .block();
  }
}