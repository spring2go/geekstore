/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.service;

import io.geekstore.common.RequestContext;
import io.geekstore.common.utils.AssetUtil;
import io.geekstore.common.utils.BeanMapper;
import io.geekstore.config.asset.AssetNamingStrategy;
import io.geekstore.config.asset.AssetPreviewStrategy;
import io.geekstore.config.asset.AssetStorageStrategy;
import io.geekstore.entity.AssetEntity;
import io.geekstore.entity.CollectionEntity;
import io.geekstore.entity.ProductEntity;
import io.geekstore.entity.ProductVariantEntity;
import io.geekstore.eventbus.events.AssetEvent;
import io.geekstore.exception.InternalServerError;
import io.geekstore.exception.UserInputException;
import io.geekstore.mapper.AssetEntityMapper;
import io.geekstore.mapper.CollectionEntityMapper;
import io.geekstore.mapper.ProductEntityMapper;
import io.geekstore.mapper.ProductVariantEntityMapper;
import io.geekstore.service.helpers.PageInfo;
import io.geekstore.service.helpers.QueryHelper;
import io.geekstore.service.helpers.ServiceHelper;
import io.geekstore.types.asset.*;
import io.geekstore.types.common.DeletionResponse;
import io.geekstore.types.common.DeletionResult;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.eventbus.EventBus;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MimeType;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import javax.servlet.http.Part;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Service
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("Duplicates")
public class AssetService {
    private final ConfigService configService;
    private final EventBus eventBus;
    private final AssetEntityMapper assetEntityMapper;
    private final ProductEntityMapper productEntityMapper;
    private final ProductVariantEntityMapper productVariantEntityMapper;
    private final CollectionEntityMapper collectionEntityMapper;

    private List<MimeType> permittedMimeTypes = new ArrayList<>();

    private AssetNamingStrategy assetNamingStrategy;
    private AssetStorageStrategy assetStorageStrategy;
    private AssetPreviewStrategy assetPreviewStrategy;

    @PostConstruct
    void init() {
        List<String> permittedFileTypes = configService.getAssetOptions().getPermittedFileTypes();
        if (!StringUtils.isEmpty(permittedFileTypes)) {
            permittedFileTypes.forEach(type -> {
                try {
                    MimeType mimeType = MimeType.valueOf(type);
                    permittedMimeTypes.add(mimeType);
                } catch (Exception ex) {
                    log.warn("Invalid mime type '" + type + "'", ex);
                }
            });
        }
        assetNamingStrategy = configService.getAssetConfig().getAssetNamingStrategy();
        assetStorageStrategy = configService.getAssetConfig().getAssetStorageStrategy();
        assetPreviewStrategy = configService.getAssetConfig().getAssetPreviewStrategy();
    }

    public AssetEntity findOne(Long id) {
        return assetEntityMapper.selectById(id);
    }

    public AssetList findAll(AssetListOptions options) {
        PageInfo pageInfo = ServiceHelper.getListOptions(options);
        IPage<AssetEntity> page = new Page<>(pageInfo.current, pageInfo.size);
        QueryWrapper<AssetEntity> queryWrapper = new QueryWrapper<>();
        if (options != null) {
            buildFilter(queryWrapper, options.getFilter());
            buildSortOrder(queryWrapper, options.getSort());
        }
        IPage<AssetEntity> assetEntityPage =
                this.assetEntityMapper.selectPage(page, queryWrapper);

        AssetList assetList = new AssetList();
        assetList.setTotalItems((int) assetEntityPage.getTotal()); // 设置满足条件总记录数

        if (CollectionUtils.isEmpty(assetEntityPage.getRecords()))
            return assetList; // 返回空

        // 将持久化实体类型转换成GraphQL传输类型
        assetEntityPage.getRecords().forEach(assetEntity -> {
            Asset asset = BeanMapper.map(assetEntity, Asset.class);
            assetList.getItems().add(asset);
        });

        return assetList;
    }

    private void buildFilter(QueryWrapper queryWrapper, AssetFilterParameter filterParameter) {
        if (filterParameter == null) return;
        QueryHelper.buildOneStringOperatorFilter(queryWrapper, filterParameter.getName(), "name");
        QueryHelper.buildOneStringOperatorFilter(queryWrapper, filterParameter.getType(), "type");
        QueryHelper.buildOneDateOperatorFilter(queryWrapper, filterParameter.getCreatedAt(), "created_at");
        QueryHelper.buildOneDateOperatorFilter(queryWrapper, filterParameter.getUpdatedAt(), "updated_at");
    }


    private void buildSortOrder(QueryWrapper queryWrapper, AssetSortParameter sortParameter) {
        if (sortParameter == null) return ;
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getId(), "id");
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getName(), "name");
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getCreatedAt(), "created_at");
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getUpdatedAt(), "updated_at");
    }

    public void validateInputMimeTypes(List<Part> parts) {
        parts.forEach(part -> {
            String mimeType = part.getContentType();
            if (!this.validateMimeType(mimeType)) {
                throw new UserInputException("The MIME type '{ "+ mimeType + " }' is not permitted.");
            }
        });
    }

    /**
     * Create an Asset based on a file uploaded via the GraphQL API.
     */
    public AssetEntity create(RequestContext ctx, Part part) {
        try {
            InputStream stream = part.getInputStream();
            String fileName = part.getSubmittedFileName();
            String mimeType = part.getContentType();

            AssetEntity assetEntity = this.createAssetInternal(stream, fileName, mimeType);

            AssetEvent assetEvent = new AssetEvent(ctx, assetEntity, "create");
            this.eventBus.post(assetEvent);
            return assetEntity;
        } catch (IOException ioEx) {
            log.error("IO exception during asset creation", ioEx);
            throw new InternalServerError("IO exception during asset creation");
        }
    }


    public AssetEntity update(RequestContext ctx, UpdateAssetInput input) {
        AssetEntity assetEntity =
                ServiceHelper.getEntityOrThrow(this.assetEntityMapper, AssetEntity.class, input.getId());

        BeanMapper.patch(input, assetEntity);

        if (input.getFocalPoint() != null) {
            CoordinateInput fp = input.getFocalPoint();
            Coordinate coordinate = new Coordinate();
            coordinate.setX(to3dp(fp.getX()));
            coordinate.setY(to3dp(fp.getY()));
            assetEntity.setFocalPoint(coordinate);
        } else {
            assetEntity.setFocalPoint(null);
        }

        this.assetEntityMapper.updateById(assetEntity);
        this.eventBus.post(new AssetEvent(ctx, assetEntity, "updated"));
        return assetEntity;
    }

    /**
     * Create an Asset from a file stream created during data import.
     */
    public AssetEntity createFromFileStream(InputStream stream, String filename) {
        try {
            String mimeType = URLConnection.guessContentTypeFromName(filename);
            AssetEntity assetEntity = createAssetInternal(stream, filename, mimeType);
            return assetEntity;
        } catch (IOException ex) {
            log.error("Fail to create asset from file steam", ex);
            throw new InternalServerError("Fail to create asset");
        }
    }

    public DeletionResponse delete(RequestContext ctx, List<Long> ids) {
        List<AssetEntity> assetEntityList = this.assetEntityMapper.selectBatchIds(ids);
        UsageCount usageCount = UsageCount.builder().build();

        assetEntityList.forEach(assetEntity -> {
            UsageCount usages = this.findAssetUsages(assetEntity);
            usageCount.products += usages.products;
            usageCount.variants += usages.variants;
            usageCount.collections += usages.collections;
        });

        boolean hasUsages = usageCount.products + usageCount.variants + usageCount.collections > 0;

        DeletionResponse deletionResponse = new DeletionResponse();
        if (hasUsages) {
            deletionResponse.setResult(DeletionResult.NOT_DELETED);
            String message = "The selected {%d} asset(s) is featured by {%d} product(s) " +
                    "and {%d} variant(s) and {%d} collection(s)";
            message = String.format(message, assetEntityList.size(),
                    usageCount.products, usageCount.variants, usageCount.collections);
            deletionResponse.setMessage(message);
            return deletionResponse;
        }

        for (AssetEntity assetEntity : assetEntityList) {
            this.assetEntityMapper.deleteById(assetEntity.getId());
            this.assetStorageStrategy.deleteFile(assetEntity.getSource());
            this.assetStorageStrategy.deleteFile(assetEntity.getPreview());
            this.eventBus.post(new AssetEvent(ctx, assetEntity, "deleted"));
        }

        deletionResponse.setResult(DeletionResult.DELETED);
        return deletionResponse;
    }

    private float to3dp(float number) {
        BigDecimal numberBigDecimal = new BigDecimal(number);
        numberBigDecimal  = numberBigDecimal .setScale(3, BigDecimal.ROUND_HALF_UP);
        return numberBigDecimal.floatValue();
    }

    private boolean validateMimeType(String mimeTypeInString) {
        try {
            MimeType mType = MimeType.valueOf(mimeTypeInString);
            MimeType typeMatch = this.permittedMimeTypes.stream()
                    .filter(pType -> pType.getType().equals(mType.getType())).findFirst().orElse(null);
            if (typeMatch != null) {
                return typeMatch.getSubtype().equals(mType.getSubtype()) || "*".equals(typeMatch.getSubtype());
            }
            return false;
        } catch (Exception ex) {
            log.warn("Invalid mime type '" + mimeTypeInString + "'", ex);
        }
        return false;
    }

    private AssetEntity createAssetInternal(InputStream stream, String fileName, String mimeType) throws IOException {
        if (!this.validateMimeType(mimeType)) {
            throw new UserInputException("The MIME type '{ " + mimeType + " }' is not permitted.");
        }

        String sourceFileName = this.generateUniqueSourceName(fileName);
        String previewFileName = this.generateUniquePreviewName(sourceFileName);

        String sourceFileIdentifier = assetStorageStrategy.writeFileFromStream(sourceFileName, stream);
        byte[] sourceFile = assetStorageStrategy.readFileToBuffer(sourceFileIdentifier);
        byte[] preview = assetPreviewStrategy.generatePreviewImage(mimeType, sourceFile);
        String previewFileIdentifier = assetStorageStrategy.writeFileFromBuffer(previewFileName, preview);

        AssetType type = AssetUtil.getAssetType(mimeType);
        Dimensions dimensions = this.getDimensions(AssetType.IMAGE.equals(type) ? sourceFile : preview);

        AssetEntity assetEntity = new AssetEntity();
        assetEntity.setType(type);
        assetEntity.setWidth(dimensions.width);
        assetEntity.setHeight(dimensions.height);
        assetEntity.setName(FilenameUtils.getName(sourceFileName));
        assetEntity.setFileSize(sourceFile.length);
        assetEntity.setMimeType(mimeType);
        assetEntity.setSource(sourceFileIdentifier);
        assetEntity.setPreview(previewFileIdentifier);
        assetEntity.setFocalPoint(null);
        this.assetEntityMapper.insert(assetEntity);
        return assetEntity;
    }

    private String generateUniquePreviewName(String inputFileName) {
        String outputFileName = null;
        do {
            outputFileName = assetNamingStrategy.generatePreviewFileName(inputFileName, outputFileName);
        } while (assetStorageStrategy.fileExists(outputFileName));
        return outputFileName;
    }

    private String generateUniqueSourceName(String inputFileName) {
        String outputFileName = null;
        do {
            outputFileName = assetNamingStrategy.generateSourceFileName(inputFileName, outputFileName);
        } while (assetStorageStrategy.fileExists(outputFileName));
        return outputFileName;
    }

    private Dimensions getDimensions(byte[] buffer) {
        try {
            BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(buffer));
            int width = bufferedImage.getWidth();
            int height = bufferedImage.getHeight();
            return Dimensions.builder().width(width).height(height).build();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Dimensions.builder().build();
    }

    /**
     * Find the entities which reference the given Asset as a featuredAsset.
     */
    private UsageCount findAssetUsages(AssetEntity assetEntity) {
        QueryWrapper<ProductEntity> productEntityQueryWrapper = new QueryWrapper<>();
        productEntityQueryWrapper.lambda().eq(ProductEntity::getFeaturedAssetId, assetEntity.getId())
                .isNull(ProductEntity::getDeletedAt);
        int productUsageCount = this.productEntityMapper.selectCount(productEntityQueryWrapper);

        QueryWrapper<ProductVariantEntity> productVariantEntityQueryWrapper = new QueryWrapper<>();
        productVariantEntityQueryWrapper.lambda().eq(ProductVariantEntity::getFeaturedAssetId, assetEntity.getId())
                .isNull(ProductVariantEntity::getDeletedAt);
        int productVariantUsageCount = this.productVariantEntityMapper.selectCount(productVariantEntityQueryWrapper);

        QueryWrapper<CollectionEntity> collectionEntityQueryWrapper = new QueryWrapper<>();
        collectionEntityQueryWrapper.lambda().eq(CollectionEntity::getFeaturedAssetId, assetEntity.getId());
        int collectionUsageCount = this.collectionEntityMapper.selectCount(collectionEntityQueryWrapper);

        return UsageCount.builder()
                .products(productUsageCount)
                .variants(productVariantUsageCount)
                .collections(collectionUsageCount)
                .build();
    }

    @Builder
    static class Dimensions {
        int width;
        int height;
    }

    @Builder
    static class UsageCount {
        int products;
        int variants;
        int collections;
    }

}
