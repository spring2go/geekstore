/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.service;

import io.geekstore.common.Constant;
import io.geekstore.common.RequestContext;
import io.geekstore.common.utils.BeanMapper;
import io.geekstore.common.utils.NormalizeUtil;
import io.geekstore.config.collection.CollectionFilter;
import io.geekstore.entity.*;
import io.geekstore.eventbus.events.ApplyCollectionFilterEvent;
import io.geekstore.eventbus.events.CollectionModificationEvent;
import io.geekstore.exception.EntityNotFoundException;
import io.geekstore.exception.IllegalOperationException;
import io.geekstore.exception.UserInputException;
import io.geekstore.mapper.*;
import io.geekstore.service.helpers.PageInfo;
import io.geekstore.service.helpers.QueryHelper;
import io.geekstore.service.helpers.ServiceHelper;
import io.geekstore.types.collection.*;
import io.geekstore.types.collection.Collection;
import io.geekstore.types.common.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.eventbus.EventBus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Service
@RequiredArgsConstructor
@SuppressWarnings("Duplicates")
public class CollectionService {

    static final Integer MAX_DESCENDANT_DEPTH = 32; // 防止程序错误引发的无限递归

    private final CollectionEntityMapper collectionEntityMapper;
    private final ConfigService configService;
    private final ProductVariantCollectionJoinEntityMapper productVariantCollectionJoinEntityMapper;
    private final ProductVariantEntityMapper productVariantEntityMapper;
    private final AssetEntityMapper assetEntityMapper;
    private final CollectionAssetJoinEntityMapper collectionAssetJoinEntityMapper;

    private final EventBus eventBus;

    private CollectionEntity rootCollection;

    public CollectionList findAll(CollectionListOptions options) {
        PageInfo pageInfo = ServiceHelper.getListOptions(options);
        IPage<CollectionEntity> page = new Page<>(pageInfo.current, pageInfo.size);
        QueryWrapper<CollectionEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(CollectionEntity::isRoot, false).orderByAsc(CollectionEntity::getPosition);
        if (options != null) {
            buildFilter(queryWrapper, options.getFilter());
            buildSortOrder(queryWrapper, options.getSort());
        }
        IPage<CollectionEntity> collectionEntityPage =
                this.collectionEntityMapper.selectPage(page, queryWrapper);

        CollectionList collectionList = new CollectionList();
        collectionList.setTotalItems((int) collectionEntityPage.getTotal()); // 设置满足条件总记录数

        if (CollectionUtils.isEmpty(collectionEntityPage.getRecords()))
            return collectionList; // 返回空

        // 将持久化实体类型转换成GraphQL传输类型
        collectionEntityPage.getRecords().forEach(collectionEntity -> {
            Collection collection = BeanMapper.map(collectionEntity, Collection.class);
            collectionList.getItems().add(collection);
        });

        return collectionList;
    }

    private void buildFilter(QueryWrapper queryWrapper, CollectionFilterParameter filterParameter) {
        if (filterParameter == null) return;
        QueryHelper.buildOneStringOperatorFilter(queryWrapper, filterParameter.getName(), "name");
        QueryHelper.buildOneStringOperatorFilter(queryWrapper, filterParameter.getSlug(), "slug");
        QueryHelper.buildOneStringOperatorFilter(queryWrapper, filterParameter.getDescription(), "description");
        QueryHelper.buildOneBooleanOperatorFilter(
                queryWrapper, filterParameter.getPrivateOnly(), "private_only");
        QueryHelper.buildOneNumberOperatorFilter(queryWrapper, filterParameter.getPosition(), "position");
        QueryHelper.buildOneDateOperatorFilter(queryWrapper, filterParameter.getCreatedAt(), "created_at");
        QueryHelper.buildOneDateOperatorFilter(queryWrapper, filterParameter.getUpdatedAt(), "updated_at");
    }


    private void buildSortOrder(QueryWrapper queryWrapper, CollectionSortParameter sortParameter) {
        if (sortParameter == null) return ;
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getId(), "id");
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getName(), "name");
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getSlug(), "slug");
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getDescription(), "description");
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getPosition(), "position");
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getCreatedAt(), "created_at");
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getUpdatedAt(), "updated_at");
    }

    public CollectionEntity findOne(Long collectionId) {
        return this.collectionEntityMapper.selectById(collectionId);
    }

    public CollectionEntity findOneBySlug(String slug) {
        QueryWrapper<CollectionEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(CollectionEntity::getSlug, slug);
        return this.collectionEntityMapper.selectOne(queryWrapper);
    }

    public List<ConfigurableOperationDefinition> getAvailableFilters() {
        return this.configService.getCatalogConfig().getCollectionFilters()
                .stream().map(f -> f.toGraphQLType()).collect(Collectors.toList());
    }

    public CollectionEntity getParent(Long collectionId) {
        CollectionEntity collectionEntity =
                ServiceHelper.getEntityOrThrow(this.collectionEntityMapper, CollectionEntity.class, collectionId);
        Long parentId = collectionEntity.getParentId();
        if (parentId == null) return null;
        return this.collectionEntityMapper.selectById(parentId);
    }

    public List<CollectionBreadcrumb> getBreadcrumbs(Long collectionId) {
        CollectionEntity collectionEntity =
                ServiceHelper.getEntityOrThrow(this.collectionEntityMapper, CollectionEntity.class, collectionId);
        List<CollectionBreadcrumb> breadcrumbs = new ArrayList<>();
        CollectionEntity root = this.getRootCollection();
        breadcrumbs.add(new CollectionBreadcrumb(root.getId(), root.getName(), root.getSlug()));
        if (Objects.equals(collectionId, root.getId())) {
            return breadcrumbs;
        }
        List<CollectionEntity> ancestors = this.getAncestors(collectionId);
        ancestors.forEach(ancestor ->
            breadcrumbs.add(new CollectionBreadcrumb(ancestor.getId(), ancestor.getName(), ancestor.getSlug()))
        );
        breadcrumbs.add(new CollectionBreadcrumb(collectionId, collectionEntity.getName(), collectionEntity.getSlug()));
        return breadcrumbs;
    }

    public List<CollectionEntity> getChildren(Long collectionId) {
        return this.getDescendants(collectionId, 1);
    }

    public List<CollectionEntity> getCollectionsByProductId(Long productId, boolean publicOnly) {
        QueryWrapper<ProductVariantEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ProductVariantEntity::getProductId, productId).select(ProductVariantEntity::getId);
        List<ProductVariantEntity> productVariantEntities = this.productVariantEntityMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(productVariantEntities)) return new ArrayList<>();
        List<Long> variantIds = productVariantEntities.stream()
                .map(ProductVariantEntity::getId).distinct().collect(Collectors.toList());

        QueryWrapper<ProductVariantCollectionJoinEntity> productVariantCollectionJoinEntityQueryWrapper =
                new QueryWrapper<>();
        productVariantCollectionJoinEntityQueryWrapper.lambda()
                .in(ProductVariantCollectionJoinEntity::getProductVariantId, variantIds);
        List<Long> collectionIds = this.productVariantCollectionJoinEntityMapper
                .selectList(productVariantCollectionJoinEntityQueryWrapper).stream()
                .map(ProductVariantCollectionJoinEntity::getCollectionId).distinct().collect(Collectors.toList());
        if (CollectionUtils.isEmpty(collectionIds)) return new ArrayList<>();

        QueryWrapper<CollectionEntity> collectionEntityQueryWrapper = new QueryWrapper<>();
        collectionEntityQueryWrapper.lambda()
                .in(CollectionEntity::getId, collectionIds).orderByAsc(CollectionEntity::getId);
        if (publicOnly) {
            collectionEntityQueryWrapper.lambda().eq(CollectionEntity::isPrivateOnly, false);
        }
        return this.collectionEntityMapper.selectList(collectionEntityQueryWrapper);
    }

    public List<CollectionEntity> getAncestors(Long collectionId) {
        List<CollectionEntity> collectionEntities = new ArrayList<>();
        CollectionEntity collectionEntity = this.collectionEntityMapper.selectById(collectionId);
        while(collectionEntity != null && !collectionEntity.isRoot()) {
            Long parentId = collectionEntity.getParentId();
            if (parentId != null) {
                collectionEntity = this.collectionEntityMapper.selectById(parentId);
                if (collectionEntity != null && !collectionEntity.isRoot()) {
                    collectionEntities.add(collectionEntity);
                }
            }
        }
        return collectionEntities.stream()
                .sorted(Comparator.comparing(CollectionEntity::getId)).collect(Collectors.toList());
    }

    public List<CollectionEntity> getDescendants(Long rootId) {
        return this.getDescendants(rootId, MAX_DESCENDANT_DEPTH);
    }

    /**
     * Returns the descendants of a Collection as a flat array. The depth of the traversal can be limited
     * with the maxDepth argument. So to get only the immediate children, set maxDepth = 1.
     */
    public List<CollectionEntity> getDescendants(Long rootId, Integer maxDepth) {
        List<CollectionEntity> descendents = new ArrayList<>();
        getChildren(rootId, descendents, 1, maxDepth);
        return descendents;
    }

    @Transactional
    public CollectionEntity create(RequestContext ctx, CreateCollectionInput input) {
        if (input.getSlug() != null) {
            String validatedSlug = this.validateSlug(input.getSlug());
            input.setSlug(validatedSlug);
        }

        if (input.getFeaturedAssetId() != null) {
            // 确保Asset存在
            ServiceHelper.getEntityOrThrow(this.assetEntityMapper, AssetEntity.class, input.getFeaturedAssetId());
        }

        CollectionEntity parent = this.getParentCollection(input.getParentId());
        if (parent == null) throw new EntityNotFoundException("Parent collection", input.getParentId());

        CollectionEntity collectionEntity = BeanMapper.patch(input, CollectionEntity.class);
        collectionEntity.setParentId(parent.getId());
        collectionEntity.setPosition(this.getNextPositionInParent(input.getParentId()));
        if (!CollectionUtils.isEmpty(input.getFilters())) {
            collectionEntity.setFilters(this.getCollectionFiltersFromInput(input.getFilters()));
        }

        this.collectionEntityMapper.insert(collectionEntity);

        if (!CollectionUtils.isEmpty(input.getAssetIds())) {
            this.joinCollectionWithAssets(collectionEntity.getId(), input.getAssetIds());
        }

        ApplyCollectionFilterEvent event =
                new ApplyCollectionFilterEvent(ctx, Arrays.asList(collectionEntity.getId()));
        this.eventBus.post(event);

        return collectionEntity;
    }

    @Transactional
    public CollectionEntity update(RequestContext ctx, UpdateCollectionInput input) {
        if (input.getSlug() != null) {
            String validatedSlug = this.validateSlug(input.getSlug());
            input.setSlug(validatedSlug);
        }

        if (input.getFeaturedAssetId() != null) {
            // 确保Asset存在
            ServiceHelper.getEntityOrThrow(this.assetEntityMapper, AssetEntity.class, input.getFeaturedAssetId());
        }

        CollectionEntity collectionEntity =
                ServiceHelper.getEntityOrThrow(this.collectionEntityMapper, CollectionEntity.class, input.getId());
        BeanMapper.patch(input, collectionEntity);

        if (!CollectionUtils.isEmpty(input.getFilters())) {
            collectionEntity.setFilters(this.getCollectionFiltersFromInput(input.getFilters()));
        }

        if (input.getFeaturedAssetId() == null || (input.getAssetIds() != null && input.getAssetIds().size() == 0)) {
            collectionEntity.setFeaturedAssetId(null);
        }

        this.collectionEntityMapper.updateById(collectionEntity);

        if (input.getAssetIds() != null) {
            // 先删除现有关联关系
            QueryWrapper<CollectionAssetJoinEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().eq(CollectionAssetJoinEntity::getCollectionId, collectionEntity.getId());
            this.collectionAssetJoinEntityMapper.delete(queryWrapper);
            // 再添加关联关系
            if (input.getAssetIds().size() > 0) {
                this.joinCollectionWithAssets(collectionEntity.getId(), input.getAssetIds());
            }
        }

        if (!CollectionUtils.isEmpty(input.getFilters())) {
            ApplyCollectionFilterEvent event =
                    new ApplyCollectionFilterEvent(ctx, Arrays.asList(collectionEntity.getId()));
            this.eventBus.post(event);
        }

        return collectionEntity;
    }

    @Transactional
    public DeletionResponse delete(RequestContext ctx, Long id) {
        CollectionEntity collectionEntity = ServiceHelper.getEntityOrThrow(
                this.collectionEntityMapper, CollectionEntity.class, id);
        List<CollectionEntity> descendents = this.getDescendants(collectionEntity.getId());
        Collections.reverse(descendents);

        List<CollectionEntity> collections = new ArrayList(descendents);
        collections.add(collectionEntity);
        for(CollectionEntity coll : collections) {
            // 先删除关联关系
            QueryWrapper<ProductVariantCollectionJoinEntity> productVariantCollectionJoinEntityQueryWrapper =
                    new QueryWrapper<>();
            productVariantCollectionJoinEntityQueryWrapper.lambda()
                    .eq(ProductVariantCollectionJoinEntity::getCollectionId, coll.getId());
            this.productVariantCollectionJoinEntityMapper.delete(productVariantCollectionJoinEntityQueryWrapper);

            QueryWrapper<CollectionAssetJoinEntity> collectionAssetJoinEntityQueryWrapper =
                    new QueryWrapper<>();
            collectionAssetJoinEntityQueryWrapper.lambda()
                    .eq(CollectionAssetJoinEntity::getCollectionId, coll.getId());
            this.collectionAssetJoinEntityMapper.delete(collectionAssetJoinEntityQueryWrapper);

            // 再删除Collection实体
            this.collectionEntityMapper.deleteById(coll.getId());
            Set<Long> affectedVariantIds = this.getCollectionProductVariantIds(coll.getId());
            CollectionModificationEvent event =
                    new CollectionModificationEvent(ctx, collectionEntity, affectedVariantIds);
            this.eventBus.post(event);
        }

        DeletionResponse deletionResponse = new DeletionResponse();
        deletionResponse.setResult(DeletionResult.DELETED);

        return deletionResponse;
    }

    public CollectionEntity move(RequestContext ctx, MoveCollectionInput input) {
        CollectionEntity target = ServiceHelper.getEntityOrThrow(
                this.collectionEntityMapper, CollectionEntity.class, input.getCollectionId());
        List<CollectionEntity> descendants = this.getDescendants(input.getCollectionId());

        if (Objects.equals(input.getParentId(), target.getId()) ||
                descendants.stream().anyMatch(cat -> Objects.equals(input.getParentId(), cat.getId()))) {
            throw new IllegalOperationException("Cannot move a Collection into itself");
        }

        QueryWrapper<CollectionEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(CollectionEntity::getParentId, input.getParentId());
        List<CollectionEntity> siblings = this.collectionEntityMapper.selectList(queryWrapper);

        if (!Objects.equals(target.getParentId(), input.getParentId())) {
            target.setParentId(input.getParentId());
        }

        siblings = moveToIndex(input.getIndex(), target, siblings);
        for(CollectionEntity sibling : siblings) {
            this.collectionEntityMapper.updateById(sibling);
        }

        ApplyCollectionFilterEvent event =
                new ApplyCollectionFilterEvent(ctx, Arrays.asList(target.getId()));
        this.eventBus.post(event);

        return target;
    }

    /**
     * Moves the target Collection entity to the given index amongst its siblings.
     * Returns the siblings (including the target) which should then be persisted to the database.
     */
    private List<CollectionEntity> moveToIndex(
            int index, CollectionEntity target, List<CollectionEntity> siblings) {
        int normalizdIndex = Math.max(Math.min(index, siblings.size() - 1), 0);
        int currentIndex = IntStream.range(0, siblings.size())
                .filter(i -> Objects.equals(siblings.get(i).getId(), target.getId())).findFirst().orElse(-1);
        Collections.sort(siblings, (a, b) -> a.getPosition() > b.getPosition() ? 1 : -1);
        List<CollectionEntity> siblingsWithTarget = siblings;
        if (currentIndex < 0) {
            siblingsWithTarget.add(target);
        }
        currentIndex = IntStream.range(0, siblingsWithTarget.size())
                .filter(i -> Objects.equals(siblingsWithTarget.get(i).getId(), target.getId())).findFirst().getAsInt();
        if (currentIndex != normalizdIndex) {
            siblingsWithTarget.add(normalizdIndex, siblingsWithTarget.remove(currentIndex));
            for(int position = 0; position < siblingsWithTarget.size(); position++) {
                CollectionEntity sibling = siblingsWithTarget.get(position);
                sibling.setPosition(position);
                if (Objects.equals(target.getId(), sibling.getId())) {
                    target.setPosition(position);
                }
            }
        }
        return siblingsWithTarget;
    }

    private void joinCollectionWithAssets(Long collectionId, List<Long> assetIds) {
        int pos = 0;
        for(Long assetId : assetIds) {
            CollectionAssetJoinEntity joinEntity = new CollectionAssetJoinEntity();
            joinEntity.setCollectionId(collectionId);
            joinEntity.setAssetId(assetId);
            joinEntity.setPosition(pos);
            this.collectionAssetJoinEntityMapper.insert(joinEntity);
            pos++;
        }
    }

    /**
     * Returns the IDs of the Collection's ProductVariants
     */
    public Set<Long> getCollectionProductVariantIds(Long collectionId) {
        QueryWrapper<ProductVariantCollectionJoinEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ProductVariantCollectionJoinEntity::getCollectionId, collectionId)
                .select(ProductVariantCollectionJoinEntity::getProductVariantId);
        return this.productVariantCollectionJoinEntityMapper.selectList(queryWrapper)
                .stream().map(ProductVariantCollectionJoinEntity::getProductVariantId)
                .collect(Collectors.toSet());

    }


    /**
     * Returns the next position value in the given parent collection.
     */
    private Integer getNextPositionInParent(Long maybeParentId) {
        Long parentId = maybeParentId;
        if (parentId == null) {
            parentId = this.getRootCollection().getId();
        }
        QueryWrapper<CollectionEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(CollectionEntity::getParentId, parentId).select(CollectionEntity::getPosition);
        List<Integer> positions = this.collectionEntityMapper.selectList(queryWrapper)
                .stream().map(CollectionEntity::getPosition).collect(Collectors.toList());
        Integer index = 0;
        for(Integer position : positions) {
            if (position > index) index = position;
        }
        return index + 1;
    }

    private List<ConfigurableOperation> getCollectionFiltersFromInput(List<ConfigurableOperationInput> inputFilters) {
        if (CollectionUtils.isEmpty(inputFilters)) return new ArrayList<>();

        List<ConfigurableOperation> filters = new ArrayList<>();
        for(ConfigurableOperationInput filter : inputFilters) {
//            CollectionFilter match = this.getFilterByCode(filter.getCode());
            ConfigurableOperation output = new ConfigurableOperation();
            output.setCode(filter.getCode());
            List<ConfigArg> args = filter.getArguments().stream().map(arg -> {
                ConfigArg configArg = new ConfigArg();
                configArg.setName(arg.getName());
                configArg.setValue(arg.getValue());
                return configArg;
            }).collect(Collectors.toList());
            output.getArgs().addAll(args);
            filters.add(output);
        }
        return filters;
    }

    private CollectionFilter getFilterByCode(String code) {
        Optional<CollectionFilter> filter = this.configService.getCatalogConfig().getCollectionFilters().stream()
                .filter(a -> a.getCode().equals(code)).findFirst();
        if (!filter.isPresent()) {
            throw new UserInputException("CollectionFilter with code '" + code + "' not found");
        }
        return filter.get();
    }

    private CollectionEntity getParentCollection(Long parentId) {
        if (parentId != null) {
            return this.collectionEntityMapper.selectById(parentId);
        } else {
            return this.getRootCollection();
        }
    }

    private void getChildren(Long id, List<CollectionEntity> descendants, Integer depth, Integer maxDepth) {
        QueryWrapper<CollectionEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(CollectionEntity::getParentId, id);
        List<CollectionEntity> children = this.collectionEntityMapper.selectList(queryWrapper);
        for(CollectionEntity child : children) {
            descendants.add(child);
            if (depth < maxDepth) {
                getChildren(child.getId(), descendants, depth++, maxDepth);
            }
        }
    }


    private CollectionEntity getRootCollection() {
        final CollectionEntity cachedRoot = this.rootCollection;

        if (cachedRoot != null) return cachedRoot;

        QueryWrapper<CollectionEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(CollectionEntity::isRoot, true);
        CollectionEntity existingRoot = this.collectionEntityMapper.selectOne(queryWrapper);
        if (existingRoot != null) {
            this.rootCollection = existingRoot;
            return this.rootCollection;
        }

        CollectionEntity newRoot = new CollectionEntity();
        newRoot.setRoot(true);
        newRoot.setPosition(0);
        newRoot.setName(Constant.ROOT_COLLECTION_NAME);
        newRoot.setDescription("The root of the Collection tree.");
        newRoot.setSlug(Constant.ROOT_COLLECTION_NAME);

        this.collectionEntityMapper.insert(newRoot);
        this.rootCollection = newRoot;
        return newRoot;
    }

    private static Pattern alreadySuffixed = Pattern.compile("-\\d+$");

    /**
     * Normalizes the slug to be URL-safe, and ensure it is unique for the product.
     * Mutates the input.
     */
    private String validateSlug(String inputStug) {
        String slug = NormalizeUtil.normalizeString(inputStug, "-");
        int suffix = 1;
        boolean match = false;
        do {
            QueryWrapper<CollectionEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().eq(CollectionEntity::getSlug, slug);
            match = this.collectionEntityMapper.selectCount(queryWrapper) > 0;
            if (match) {
                suffix++;
                if (alreadySuffixed.matcher(slug).find()) {
                    slug = slug.replaceFirst("-\\d+$", "-" + suffix);
                } else {
                    slug = slug + "-" + suffix;
                }
            }
        } while (match);
        return slug;
    }
}
