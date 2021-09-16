/*
 * Copyright (c) 2021 GeekStore.
 * All rights reserved.
 */

package io.geekstore.eventbus;

import io.geekstore.entity.ProductVariantEntity;
import io.geekstore.eventbus.events.*;
import io.geekstore.service.SearchIndexService;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created on Jan, 2021 by @author bobo
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SearchIndexRelatedSubscriber {
    private final EventBus eventBus;
    private final SearchIndexService searchIndexService;

    @PostConstruct
    void init() {
        eventBus.register(this);
    }

    @Subscribe
    public void onEvent(ProductEvent event) {
        if (Objects.equals(event.getType(), "deleted")) {
            this.searchIndexService.deleteProduct(event.getProduct().getId());
        } else {
            this.searchIndexService.updateProduct(event.getProduct().getId());
        }
    }

    @Subscribe
    public void onEvent(ProductVariantEvent event) {
        List<Long> variantIds = event.getVariants().stream()
                .map(ProductVariantEntity::getId).collect(Collectors.toList());
        if (Objects.equals(event.getType(), "deleted")) {
            this.searchIndexService.deleteVariants(variantIds);
        } else {
            this.searchIndexService.updateVariants(variantIds);
        }
    }

    @Subscribe
    public void onEvent(AssetEvent event) {
        if (Objects.equals(event.getType(), "updated")) {
            this.searchIndexService.updateAsset(event.getAsset());
        }
        if (Objects.equals(event.getType(), "deleted")) {
            this.searchIndexService.deleteAsset(event.getAsset().getId());
        }
    }

    @Subscribe
    public void onEvent(CollectionModificationEvent event) {
        this.searchIndexService.updateVariants(new ArrayList<>(event.getProductVariantIds()));
    }

    @Subscribe
    public void onEvent(ReIndexEvent event) {
        this.searchIndexService.reindex();
    }
}
