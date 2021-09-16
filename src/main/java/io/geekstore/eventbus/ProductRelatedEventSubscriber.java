/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.eventbus;

import io.geekstore.common.RequestContext;
import io.geekstore.entity.CollectionEntity;
import io.geekstore.eventbus.events.ApplyCollectionFilterEvent;
import io.geekstore.eventbus.events.ProductEvent;
import io.geekstore.eventbus.events.ProductVariantEvent;
import io.geekstore.mapper.CollectionEntityMapper;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ProductRelatedEventSubscriber {

    private final CollectionEntityMapper collectionEntityMapper;
    private final EventBus eventBus;

    @PostConstruct
    void init() {
        eventBus.register(this);
    }

    @Subscribe
    public void onEvent(ProductEvent productEvent) {
        this.handle(productEvent.getCtx());
    }

    @Subscribe
    public void onEvent(ProductVariantEvent productVariantEvent) {
        this.handle(productVariantEvent.getCtx());
    }

    private void handle(RequestContext ctx) {
        List<Long> collectionIds = collectionEntityMapper.selectList(null)
                .stream().map(CollectionEntity::getId).collect(Collectors.toList());

        ApplyCollectionFilterEvent applyCollectionFilterEvent =
                new ApplyCollectionFilterEvent(ctx, collectionIds);
        this.eventBus.post(applyCollectionFilterEvent);
    }
}
