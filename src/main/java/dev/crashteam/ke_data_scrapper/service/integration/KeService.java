package dev.crashteam.ke_data_scrapper.service.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.crashteam.ke_data_scrapper.exception.CategoryRequestException;
import dev.crashteam.ke_data_scrapper.model.ProxyRequestParams;
import dev.crashteam.ke_data_scrapper.model.StyxProxyResult;
import dev.crashteam.ke_data_scrapper.model.ke.*;
import dev.crashteam.ke_data_scrapper.util.RandomUserAgent;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeService {

    private final StyxProxyService proxyService;
    private final ThreadPoolTaskExecutor taskExecutor;

    @Value("${app.integration.kazan.token}")
    private String authToken;

    @Value("${app.integration.timeout}")
    private Long timeout;

    private static final String ROOT_URL = "https://api.kazanexpress.ru/api";

    public List<KeCategory.Data> getRootCategories() {
        ProxyRequestParams.ContextValue headers = ProxyRequestParams.ContextValue.builder()
                .key("headers")
                .value(Map.of("Authorization", authToken,
                        "User-Agent", RandomUserAgent.getRandomUserAgent())).build();
        Random randomTimeout = new Random();
        ProxyRequestParams requestParams = ProxyRequestParams.builder()
                .timeout(randomTimeout.nextLong(50L, timeout))
                .url(ROOT_URL + "/main/root-categories")
                .httpMethod(HttpMethod.GET.name())
                .context(Collections.singletonList(headers))
                .build();
        StyxProxyResult<KeCategory> proxyResult = proxyService.getProxyResult(requestParams, new ParameterizedTypeReference<>() {
        });
        return proxyResult.getBody().getPayload();
    }

    public KeProduct getProduct(Long id) {
        ProxyRequestParams.ContextValue headers = ProxyRequestParams.ContextValue.builder()
                .key("headers")
                .value(Map.of("Authorization", authToken,
                        "x-iid", UUID.randomUUID().toString(),
                        "User-Agent", RandomUserAgent.getRandomUserAgent())).build();
        Random randomTimeout = new Random();
        ProxyRequestParams requestParams = ProxyRequestParams.builder()
                .timeout(randomTimeout.nextLong(50L, timeout))
                .url(ROOT_URL + "/v2/product/%s".formatted(id))
                .httpMethod(HttpMethod.GET.name())
                .context(Collections.singletonList(headers))
                .build();
        return proxyService.getProxyResult(requestParams, new ParameterizedTypeReference<StyxProxyResult<KeProduct>>() {
        }).getBody();
    }

    public KeCategoryChild getCategoryData(Long id) {
        ProxyRequestParams.ContextValue headers = ProxyRequestParams.ContextValue.builder()
                .key("headers")
                .value(Map.of("Authorization", authToken,
                        "x-iid", UUID.randomUUID().toString(),
                        "User-Agent", RandomUserAgent.getRandomUserAgent())).build();
        Random randomTimeout = new Random();
        ProxyRequestParams requestParams = ProxyRequestParams.builder()
                .timeout(randomTimeout.nextLong(50L, timeout))
                .url(ROOT_URL + "/category/v2/%s".formatted(id))
                .httpMethod(HttpMethod.GET.name())
                .context(Collections.singletonList(headers))
                .build();
        return proxyService.getProxyResult(requestParams, new ParameterizedTypeReference<StyxProxyResult<KeCategoryChild>>() {
        }).getBody();
    }

    @SneakyThrows
    public Set<Long> getIds() {
        log.info("Collecting category id's...");
        Set<Long> ids = new CopyOnWriteArraySet<>();
        List<Callable<Void>> callables = new ArrayList<>();
        for (KeCategory.Data data : getRootCategories()) {
            callables.add(extractIdsAsync(data, ids));
        }
        List<Future<Void>> futures = callables.stream()
                .map(taskExecutor::submit)
                .toList();
        for (Future<Void> future : futures) {
            try {
                future.get();
            } catch (ExecutionException e) {
                throw Optional.ofNullable(e.getCause()).orElse(e);
            }
        }
        log.info("Collected id's size - {}", ids.size());
        return ids;
    }

    @SneakyThrows
    public Set<Long> getIdsByMainCategory(boolean all) {
        log.info("Collecting category id's...");
        Set<Long> ids = new HashSet<>();
        KeCategoryChild categoryData = getCategoryData(1L);
        if (categoryData.getPayload() != null && categoryData.getPayload().getCategory() != null) {
            for (KeCategory.Data category : categoryData.getPayload().getCategory().getChildren()) {
                if (all) {
                    extractAllIds(category, ids);
                } else {
                    extractIds(category, ids);
                }
            }
        } else {
            throw new CategoryRequestException("Id collecting failed");
        }
        log.info("Collected id's size - {}", ids.size());
        return ids;
    }

    private void extractALlIds(KeCategory.Data data, Set<Long> ids) {
        ids.add(data.getId());
        extractAllIds(data, ids);
    }

    private void extractIds(KeCategory.Data data, Set<Long> ids) {
        ids.add(data.getId());
        for (KeCategory.Data child : data.getChildren()) {
            ids.add(child.getId());
        }
    }


    private Callable<Void> extractIdsAsync(KeCategory.Data data, Set<Long> ids) {
        return () -> {
            ids.add(data.getId());
            extractAllIds(getCategoryData(data.getId()).getPayload().getCategory(), ids);
            return null;
        };
    }

    @SneakyThrows
    public KeGQLResponse getGQLSearchResponse(String categoryId, long offset, long limit) {
        log.info("Starting gql catalog search with values: [categoryId - {}] , [offset - {}], [limit - {}]"
                , categoryId, offset, limit);
        String query = "query getMakeSearch($queryInput: MakeSearchQueryInput!) {\n  makeSearch(query: $queryInput) {\n    id\n    queryId\n    queryText\n    category {\n      ...CategoryShortFragment\n      __typename\n    }\n    categoryTree {\n      category {\n        ...CategoryFragment\n        __typename\n      }\n      total\n      __typename\n    }\n    items {\n      catalogCard {\n        __typename\n        ...SkuGroupCardFragment\n      }\n      __typename\n    }\n    facets {\n      ...FacetFragment\n      __typename\n    }\n    total\n    mayHaveAdultContent\n    categoryFullMatch\n    __typename\n  }\n}\n\nfragment FacetFragment on Facet {\n  filter {\n    id\n    title\n    type\n    measurementUnit\n    description\n    __typename\n  }\n  buckets {\n    filterValue {\n      id\n      description\n      image\n      name\n      __typename\n    }\n    total\n    __typename\n  }\n  range {\n    min\n    max\n    __typename\n  }\n  __typename\n}\n\nfragment CategoryFragment on Category {\n  id\n  icon\n  parent {\n    id\n    __typename\n  }\n  seo {\n    header\n    metaTag\n    __typename\n  }\n  title\n  adult\n  __typename\n}\n\nfragment CategoryShortFragment on Category {\n  id\n  parent {\n    id\n    title\n    __typename\n  }\n  title\n  __typename\n}\n\nfragment SkuGroupCardFragment on SkuGroupCard {\n  ...DefaultCardFragment\n  photos {\n    key\n    link(trans: PRODUCT_540) {\n      high\n      low\n      __typename\n    }\n    previewLink: link(trans: PRODUCT_240) {\n      high\n      low\n      __typename\n    }\n    __typename\n  }\n  badges {\n    ... on BottomTextBadge {\n      backgroundColor\n      description\n      id\n      link\n      text\n      textColor\n      __typename\n    }\n    __typename\n  }\n  characteristicValues {\n    id\n    value\n    title\n    characteristic {\n      values {\n        id\n        title\n        value\n        __typename\n      }\n      title\n      id\n      __typename\n    }\n    __typename\n  }\n  __typename\n}\n\nfragment DefaultCardFragment on CatalogCard {\n  adult\n  favorite\n  feedbackQuantity\n  id\n  minFullPrice\n  minSellPrice\n  offer {\n    due\n    icon\n    text\n    textColor\n    __typename\n  }\n  badges {\n    backgroundColor\n    text\n    textColor\n    __typename\n  }\n  ordersQuantity\n  productId\n  rating\n  title\n  __typename\n}";
        KeSearchQuery.Variables variables = KeSearchQuery.Variables.builder()
                .queryInput(KeSearchQuery.QueryInput.builder()
                        .categoryId(categoryId)
                        .filters(Collections.emptyList())
                        .showAdultContent("TRUE")
                        .sort("BY_RELEVANCE_DESC")
                        .pagination(KeSearchQuery.Pagination.builder()
                                .limit(limit).offset(offset).build()).build()
                )
                .build();
        KeSearchQuery searchQuery = KeSearchQuery.builder()
                .variables(variables)
                .query(query)
                .build();
        ObjectMapper objectMapper = new ObjectMapper();
        byte[] bytes = objectMapper.writeValueAsBytes(searchQuery);
        String base64Body = Base64.getEncoder().encodeToString(bytes);
        ProxyRequestParams.ContextValue headers = ProxyRequestParams.ContextValue.builder()
                .key("headers")
                .value(Map.of("Authorization", authToken,
                        "x-iid", UUID.randomUUID().toString(),
                        "Content-Type", "application/json",
                        "User-Agent", RandomUserAgent.getRandomUserAgent())).build();
        ProxyRequestParams.ContextValue content = ProxyRequestParams.ContextValue.builder()
                .key("content")
                .value(base64Body)
                .build();
        Random randomTimeout = new Random();
        ProxyRequestParams requestParams = ProxyRequestParams.builder()
                .timeout(randomTimeout.nextLong(50L, timeout))
                .url("https://dshop.kznexpress.ru/")
                .httpMethod(HttpMethod.POST.name())
                .context(List.of(headers, content))
                .build();
        return proxyService
                .getProxyResult(requestParams, new ParameterizedTypeReference<StyxProxyResult<KeGQLResponse>>() {
                }).getBody();
    }

    private void extractAllIds(KeCategory.Data data, Set<Long> ids) {
        ids.add(data.getId());
        for (KeCategory.Data child : data.getChildren()) {
            extractAllIds(child, ids);
        }
    }
}
