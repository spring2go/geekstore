/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.e2e;

import io.geekstore.*;
import io.geekstore.config.TestConfig;
import io.geekstore.types.asset.*;
import io.geekstore.types.common.DeletionResponse;
import io.geekstore.types.common.DeletionResult;
import io.geekstore.types.common.SortOrder;
import io.geekstore.types.product.Product;
import io.geekstore.utils.TestHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.graphql.spring.boot.test.GraphQLResponse;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.annotation.DirtiesContext;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created on Dec, 2020 by @author bobo
 */
@GeekStoreGraphQLTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext
public class AssetTest {

    public static final String ASSET_GRAPHQL_RESOURCE_TEMPLATE = "graphql/admin/asset/%s.graphqls";
    public static final String GET_ASSET =
            String.format(ASSET_GRAPHQL_RESOURCE_TEMPLATE, "get_asset");
    public static final String CREATE_ASSETS =
            String.format(ASSET_GRAPHQL_RESOURCE_TEMPLATE, "create_assets");

    public static final String SHARED_GRAPHQL_RESOURCE_TEMPLATE = "graphql/shared/%s.graphqls";
    public static final String PRODUCT_VARIANT_FRAGMENT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "product_variant_fragment");
    public static final String PRODUCT_WITH_VARIANTS_FRAGMENT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "product_with_variants_fragment");
    public static final String GET_PRODUCT_WITH_VARIANTS =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "get_product_with_variants");
    public static final String DELETE_ASSET =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "delete_asset");
    public static final String UPDATE_ASSET =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "update_asset");
    public static final String ASSET_FRAGMENT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "asset_fragment");
    public static final String GET_ASSET_LIST =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "get_asset_list");

    @Autowired
    TestHelper testHelper;

    @Autowired
    @Qualifier(TestConfig.ADMIN_CLIENT_BEAN)
    ApiClient adminClient;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    MockDataService mockDataService;

    Long firstAssetId;
    Long createdAssetId;

    @BeforeAll
    void beforeAll() throws IOException {
        PopulateOptions populateOptions = PopulateOptions.builder().customerCount(1).build();
        populateOptions.setInitialData(testHelper.getInitialData());
        populateOptions.setProductCsvPath(testHelper.getTestFixture("e2e-products-full.csv"));

        mockDataService.populate(populateOptions);
        adminClient.asSuperAdmin();
    }

    @Test
    @Order(1)
    public void assets() throws IOException {
        AssetListOptions options = new AssetListOptions();
        AssetSortParameter sortParameter = new AssetSortParameter();
        sortParameter.setName(SortOrder.ASC);
        options.setSort(sortParameter);
        JsonNode optionsNode = objectMapper.valueToTree(options);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("options", optionsNode);

        GraphQLResponse graphQLResponse =
                this.adminClient.perform(GET_ASSET_LIST, variables, Arrays.asList(ASSET_FRAGMENT));
        AssetList assetList = graphQLResponse.get("$.data.assets", AssetList.class);
        assertThat(assetList.getTotalItems()).isEqualTo(4);

        Asset asset1 = assetList.getItems().get(0);
        assertThat(asset1.getFileSize()).isEqualTo(1680);
        assertThat(asset1.getMimeType()).isEqualTo("image/jpeg");
        assertThat(asset1.getName()).isEqualTo("alexandru-acea-686569-unsplash.jpg");
        assertThat(asset1.getPreview()).isEqualTo("target/test-assets/alexandru-acea-686569-unsplash__preview.jpg");
        assertThat(asset1.getSource()).isEqualTo("target/test-assets/alexandru-acea-686569-unsplash.jpg");
        assertThat(asset1.getType()).isEqualTo(AssetType.IMAGE);

        Asset asset2 = assetList.getItems().get(1);
        assertThat(asset2.getFileSize()).isEqualTo(1680);
        assertThat(asset2.getMimeType()).isEqualTo("image/jpeg");
        assertThat(asset2.getName()).isEqualTo("derick-david-409858-unsplash.jpg");
        assertThat(asset2.getPreview()).isEqualTo("target/test-assets/derick-david-409858-unsplash__preview.jpg");
        assertThat(asset2.getSource()).isEqualTo("target/test-assets/derick-david-409858-unsplash.jpg");
        assertThat(asset2.getType()).isEqualTo(AssetType.IMAGE);

        Asset asset3 = assetList.getItems().get(2);
        assertThat(asset3.getFileSize()).isEqualTo(1680);
        assertThat(asset3.getMimeType()).isEqualTo("image/jpeg");
        assertThat(asset3.getName()).isEqualTo("florian-olivo-1166419-unsplash.jpg");
        assertThat(asset3.getPreview()).isEqualTo("target/test-assets/florian-olivo-1166419-unsplash__preview.jpg");
        assertThat(asset3.getSource()).isEqualTo("target/test-assets/florian-olivo-1166419-unsplash.jpg");
        assertThat(asset3.getType()).isEqualTo(AssetType.IMAGE);

        Asset asset4 = assetList.getItems().get(3);
        assertThat(asset4.getFileSize()).isEqualTo(1680);
        assertThat(asset4.getMimeType()).isEqualTo("image/jpeg");
        assertThat(asset4.getName()).isEqualTo("vincent-botta-736919-unsplash.jpg");
        assertThat(asset4.getPreview()).isEqualTo("target/test-assets/vincent-botta-736919-unsplash__preview.jpg");
        assertThat(asset4.getSource()).isEqualTo("target/test-assets/vincent-botta-736919-unsplash.jpg");
        assertThat(asset4.getType()).isEqualTo(AssetType.IMAGE);

        firstAssetId = assetList.getItems().get(0).getId();
    }

    @Test
    @Order(2)
    public void asset() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", firstAssetId);
        GraphQLResponse graphQLResponse =
                this.adminClient.perform(GET_ASSET, variables, Arrays.asList(ASSET_FRAGMENT));

        Asset asset = graphQLResponse.get("$.data.asset", Asset.class);
        assertThat(asset.getFileSize()).isEqualTo(1680);
        assertThat(asset.getHeight()).isEqualTo(48);
        assertThat(asset.getMimeType()).isEqualTo("image/jpeg");
        assertThat(asset.getName()).isEqualTo("alexandru-acea-686569-unsplash.jpg");
        assertThat(asset.getPreview()).isEqualTo("target/test-assets/alexandru-acea-686569-unsplash__preview.jpg");
        assertThat(asset.getSource()).isEqualTo("target/test-assets/alexandru-acea-686569-unsplash.jpg");
        assertThat(asset.getType()).isEqualTo(AssetType.IMAGE);
        assertThat(asset.getWidth()).isEqualTo(48);
    }

    /**
     * createAssets
     */
    @Test
    @Order(3)
    public void permitted_types_mime_type() throws IOException {
        GraphQLResponse graphQLResponse = adminClient.uploadMultipleFiles(
                CREATE_ASSETS,
                Arrays.asList(
                        "src/test/resources/fixtures/assets/pps1.jpg",
                        "src/test/resources/fixtures/assets/pps2.jpg"
                )
        );
        List<Asset> assetList = graphQLResponse.getList("$.data.createAssets", Asset.class);
        assertThat(assetList.size()).isEqualTo(2);
        List<Asset> sortedAssetList = assetList.stream().sorted(Comparator.comparing(Asset::getName))
                .collect(Collectors.toList());
        Asset asset1 = sortedAssetList.get(0);
        assertThat(asset1.getFileSize()).isEqualTo(1680);
        assertThat(asset1.getFocalPoint()).isNull();
        assertThat(asset1.getMimeType()).isEqualTo("image/jpeg");
        assertThat(asset1.getName()).isEqualTo("pps1.jpg");
        assertThat(asset1.getPreview()).isEqualTo("target/test-assets/pps1__preview.jpg");
        assertThat(asset1.getSource()).isEqualTo("target/test-assets/pps1.jpg");
        assertThat(asset1.getType()).isEqualTo(AssetType.IMAGE);

        Asset asset2 = sortedAssetList.get(1);
        assertThat(asset2.getFileSize()).isEqualTo(1680);
        assertThat(asset2.getFocalPoint()).isNull();
        assertThat(asset2.getMimeType()).isEqualTo("image/jpeg");
        assertThat(asset2.getName()).isEqualTo("pps2.jpg");
        assertThat(asset2.getPreview()).isEqualTo("target/test-assets/pps2__preview.jpg");
        assertThat(asset2.getSource()).isEqualTo("target/test-assets/pps2.jpg");
        assertThat(asset2.getType()).isEqualTo(AssetType.IMAGE);

        createdAssetId = assetList.get(0).getId();
    }

    @Test
    @Order(4)
    public void permitted_type_by_file_extension() throws IOException {
        GraphQLResponse graphQLResponse = adminClient.uploadMultipleFiles(
                CREATE_ASSETS,
                Arrays.asList(
                        "src/test/resources/fixtures/assets/dummy.pdf"
                )
        );
        List<Asset> assetList = graphQLResponse.getList("$.data.createAssets", Asset.class);
        assertThat(assetList.size()).isEqualTo(1);

        Asset asset1 = assetList.get(0);
        assertThat(asset1.getFileSize()).isEqualTo(1680);
        assertThat(asset1.getFocalPoint()).isNull();
        assertThat(asset1.getMimeType()).isEqualTo("application/pdf");
        assertThat(asset1.getName()).isEqualTo("dummy.pdf");
        assertThat(asset1.getPreview()).isEqualTo("target/test-assets/dummy__preview.pdf.png");
        assertThat(asset1.getSource()).isEqualTo("target/test-assets/dummy.pdf");
        assertThat(asset1.getType()).isEqualTo(AssetType.BINARY);
    }

    @Test
    @Order(5)
    public void not_permitted_type() throws IOException {
        try {
            adminClient.uploadMultipleFiles(
                    CREATE_ASSETS,
                    Arrays.asList(
                            "src/test/resources/fixtures/assets/dummy.txt"
                    )
            );
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo("The MIME type 'text/plain' is not permitted.");
        }
    }

    /**
     * updateAsset
     */

    @Test
    @Order(6)
    public void update_name() throws IOException {
        UpdateAssetInput input = new UpdateAssetInput();
        input.setId(firstAssetId);
        input.setName("new name");

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse =
                this.adminClient.perform(UPDATE_ASSET, variables, Arrays.asList(ASSET_FRAGMENT));
        Asset updatedAsset = graphQLResponse.get("$.data.updateAsset", Asset.class);
        assertThat(updatedAsset.getName()).isEqualTo("new name");
    }

    @Test
    @Order(7)
    public void update_focal_point() throws IOException {
        UpdateAssetInput input = new UpdateAssetInput();
        input.setId(firstAssetId);
        CoordinateInput coordinateInput = new CoordinateInput();
        coordinateInput.setX(0.3F);
        coordinateInput.setY(0.9F);
        input.setFocalPoint(coordinateInput);

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse =
                this.adminClient.perform(UPDATE_ASSET, variables, Arrays.asList(ASSET_FRAGMENT));
        Asset updatedAsset = graphQLResponse.get("$.data.updateAsset", Asset.class);
        assertThat(updatedAsset.getFocalPoint().getX()).isEqualTo(0.3F);
        assertThat(updatedAsset.getFocalPoint().getY()).isEqualTo(0.9F);
    }

    @Test
    @Order(8)
    public void unset_focal_point() throws IOException {
        UpdateAssetInput input = new UpdateAssetInput();
        input.setId(firstAssetId);

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse =
                this.adminClient.perform(UPDATE_ASSET, variables, Arrays.asList(ASSET_FRAGMENT));
        Asset updatedAsset = graphQLResponse.get("$.data.updateAsset", Asset.class);
        assertThat(updatedAsset.getFocalPoint()).isNull();
    }

    /**
     * deleteAsset
     */

    Product firstProduct;

    void beforeTest9() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", 1L);

        GraphQLResponse graphQLResponse = this.adminClient.perform(GET_PRODUCT_WITH_VARIANTS, variables, Arrays.asList(
                PRODUCT_VARIANT_FRAGMENT, PRODUCT_WITH_VARIANTS_FRAGMENT, ASSET_FRAGMENT
        ));

        firstProduct = graphQLResponse.get("$.data.adminProduct", Product.class);
    }

    @Test
    @Order(9)
    public void non_featured_asset() throws IOException {
        beforeTest9();

        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", createdAssetId);

        GraphQLResponse graphQLResponse = this.adminClient.perform(DELETE_ASSET, variables);
        DeletionResponse deletionResponse = graphQLResponse.get("$.data.deleteAsset", DeletionResponse.class);
        assertThat(deletionResponse.getResult()).isEqualTo(DeletionResult.DELETED);

        variables = objectMapper.createObjectNode();
        variables.put("id", createdAssetId);
        graphQLResponse =
                this.adminClient.perform(GET_ASSET, variables, Arrays.asList(ASSET_FRAGMENT));

        Asset asset = graphQLResponse.get("$.data.asset", Asset.class);
        assertThat(asset).isNull();
    }

    @Test
    @Order(10)
    public void featured_asset_not_deleted() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", firstProduct.getFeaturedAsset().getId());

        GraphQLResponse graphQLResponse = this.adminClient.perform(DELETE_ASSET, variables);
        DeletionResponse deletionResponse = graphQLResponse.get("$.data.deleteAsset", DeletionResponse.class);
        assertThat(deletionResponse.getResult()).isEqualTo(DeletionResult.NOT_DELETED);
        assertThat(deletionResponse.getMessage())
                .isEqualTo("The selected {1} asset(s) is featured by {1} product(s) " +
                                "and {0} variant(s) and {0} collection(s)");

        variables = objectMapper.createObjectNode();
        variables.put("id", firstAssetId);
        graphQLResponse =
                this.adminClient.perform(GET_ASSET, variables, Arrays.asList(ASSET_FRAGMENT));

        Asset asset = graphQLResponse.get("$.data.asset", Asset.class);
        assertThat(asset).isNotNull();
    }
}
