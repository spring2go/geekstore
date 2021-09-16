/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.common;

/**
 * Created on Nov, 2020 by @author bobo
 */
public abstract class Constant {
    public static final String DB_NAME_H2 = "h2";
    public static final String DB_NAME_MYSQL = "mysql";

    public static final String USERNAME_ANONYMOUS = "anonymous";
    public static final String USERNAME_SYSTEM = "system";

    public static final String PEBBLE_TEMPLATE_PREFIX = "templates/email/";
    public static final String PEBBLE_TEMPLATE_SUFFIX = ".pebble";

    public static final String REQUEST_ATTRIBUTE_CURRENT_USER = "GEEKSTORE_REQUEST_CURRENT_USER";

    public static final String COOKIE_NAME_TOKEN = "token";

    public static final String HTTP_HEADER_AUTHORIZATION = "Authorization";

    public static final String DEFAULT_ANONYMOUS_SESSION_DURATION = "1y";
    public static final String DEFAULT_REMEMBER_ME_DURATION = "1y";

    public static final int PASSWORD_LENGTH_MIN = 8;
    public static final int PASSWORD_LENGTH_MAX = 20;

    public static final int DEFAULT_CURRENT_PAGE = 1;
    public static final int DEFAULT_PAGE_SIZE = 100;

    public static final int DEFAULT_IN_MEMORY_CACHE_SIZE = 1000;

    public static final String NATIVE_AUTH_STRATEGY_NAME = "native";

    public static final String ROOT_COLLECTION_NAME = "__root_collection__";

    public static final String DEFAULT_AUTH_TOKEN_HEADER_KEY = "geekstore-shared-token";
    public static final String SUPER_ADMIN_USER_IDENTIFIER = "superadmin";
    public static final String SUPER_ADMIN_USER_PASSWORD = "superadmin123";

    public static final String DATA_LOADER_NAME_ADMINISTRATOR_USER = "administratorUserDataLoader";
    public static final String DATA_LOADER_NAME_HISTORY_ENTRY_ADMINISTRATOR = "historyEntryAdministrator";
    public static final String DATA_LOADER_NAME_CUSTOMER_USER = "customerUserDataLoader";
    public static final String DATA_LOADER_NAME_CUSTOMER_ADDRESSES = "customerAddressesDataLoader";
    public static final String DATA_LOADER_NAME_CUSTOMER_GROUPS = "customerGroupsDataLoader";
    public static final String DATA_LOADER_NAME_USER_ROLES = "userRolesDataLoader";
    public static final String DATA_LOADER_NAME_USER_AUTHENTICATION_METHODS = "userAuthenticationMethodsDataLoader";

    public static final String DATA_LOADER_NAME_FACET_VALUE_FACET = "facetValueFacetDataLoader";
    public static final String DATA_LOADER_NAME_FACET_VALUES = "facetValuesDataLoader";

    public static final String DATA_LOADER_NAME_PRODUCT_OPTIONS = "productOptionsDataLoader";
    public static final String DATA_LOADER_NAME_PRODUCT_OPTION_GROUP = "productOptionGroupDataLoader";
    public static final String DATA_LOADER_NAME_PRODUCT_FEATURED_ASSET = "productFeaturedAssetDataLoader";
    public static final String DATA_LOADER_NAME_PRODUCT_ASSETS = "productAssetsDataLoader";
    public static final String DATA_LOADER_NAME_PRODUCT_VARIANTS = "productVariantsDataLoader";
    public static final String DATA_LOADER_NAME_PRODUCT_OPTION_GROUPS = "productOptionGroupsDataLoader";
    public static final String DATA_LOADER_NAME_PRODUCT_FACET_VALUES = "productFacetValuesDataLoader";

    public static final String DATA_LOADER_NAME_PRODUCT_VARIANT_FEATURED_ASSET =
            "productVariantFeaturedAssetDataLoader";
    public static final String DATA_LOADER_NAME_PRODUCT_VARIANT_PRODUCT = "productVariantProductDataLoader";
    public static final String DATA_LOADER_NAME_PRODUCT_VARIANT_ASSETS = "productVariantAssetsDataLoader";
    public static final String DATA_LOADER_NAME_PRODUCT_VARIANT_OPTIONS = "productVariantOptionsDataLoader";
    public static final String DATA_LOADER_NAME_PRODUCT_VARIANT_FACET_VALUES = "productVariantFacetValuesDataLoader";

    public static final String DATA_LOADER_NAME_COLLECTION_FEATURED_ASSET =
            "collectionFeaturedAssetDataLoader";
    public static final String DATA_LOADER_NAME_COLLECTION_ASSETS = "collectionAssetsDataLoader";
    public static final String DATA_LOADER_NAME_COLLECTION_PARENT = "collectionParentDataLoader";
    public static final String DATA_LOADER_NAME_COLLECTION_CHILDREN = "collectionChildrenDataLoader";
    public static final String DATA_LOADER_NAME_ORDER_LINE_PRODUCT_VARIANT = "orderLineProductVariantDataLoader";
    public static final String DATA_LOADER_NAME_ORDER_LINE_FEATURED_ASSET = "orderLineFeaturedAssetDataLoader";
    public static final String DATA_LOADER_NAME_ORDER_LINE_ORDER = "orderLineOrderDataLoader";
    public static final String DATA_LOADER_NAME_ORDER_ITEM_FULFILLMENT = "orderItemFulfillmentDataLoader";
    public static final String DATA_LOADER_NAME_FULFILLMENT_ORDER_ITEMS = "fulfillmentOrderItemsDataLoader";
    public static final String DATA_LOADER_NAME_ORDER_CUSTOMER = "orderCustomerDataLoader";
    public static final String DATA_LOADER_NAME_ORDER_PROMOTIONS = "orderPromotionsDataLoader";
    public static final String DATA_LOADER_NAME_ORDER_PAYMENTS = "orderPaymentsDataLoader";
    public static final String DATA_LOADER_NAME_ORDER_SHIPPING_METHOD = "orderShippingMethodDataLoader";
    public static final String DATA_LOADER_NAME_ORDER_LINE_ITEMS = "orderLineItemsDataLoader";
    public static final String DATA_LOADER_NAME_ORDER_LINES = "orderLinesDataLoader";
    public static final String DATA_LOADER_NAME_PAYMENT_REFUNDS = "paymentRefundsDataLoader";
    public static final String DATA_LOADER_NAME_REFUND_ORDER_ITEMS = "refundOrderItemsDataLoader";
}
