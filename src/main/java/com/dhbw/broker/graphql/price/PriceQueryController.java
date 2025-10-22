package com.dhbw.broker.graphql.price;

import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class PriceQueryController {

  private final AssetPriceRepository repository;

  @QueryMapping
  public AssetPriceRepository.PriceTick latestPrice(@Argument String assetSymbol) {
    return repository.findLatest(assetSymbol);
  }

  @QueryMapping
  public List<AssetPriceRepository.PriceTick> priceHistory24h(@Argument String assetSymbol) {
    return repository.find24hHistory(assetSymbol);
  }
}