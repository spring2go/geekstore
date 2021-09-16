/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.custom.graphql;

import io.geekstore.common.Constant;
import io.geekstore.mapper.*;
import io.geekstore.resolver.dataloader.*;
import io.geekstore.types.address.Address;
import io.geekstore.types.administrator.Administrator;
import io.geekstore.types.asset.Asset;
import io.geekstore.types.collection.Collection;
import io.geekstore.types.customer.Customer;
import io.geekstore.types.customer.CustomerGroup;
import io.geekstore.types.facet.Facet;
import io.geekstore.types.facet.FacetValue;
import io.geekstore.types.order.Fulfillment;
import io.geekstore.types.order.Order;
import io.geekstore.types.order.OrderItem;
import io.geekstore.types.order.OrderLine;
import io.geekstore.types.payment.Payment;
import io.geekstore.types.payment.Refund;
import io.geekstore.types.product.Product;
import io.geekstore.types.product.ProductOption;
import io.geekstore.types.product.ProductOptionGroup;
import io.geekstore.types.product.ProductVariant;
import io.geekstore.types.promotion.Promotion;
import io.geekstore.types.role.Role;
import io.geekstore.types.shipping.ShippingMethod;
import io.geekstore.types.user.AuthenticationMethod;
import io.geekstore.types.user.User;
import graphql.kickstart.execution.context.DefaultGraphQLContext;
import graphql.kickstart.execution.context.GraphQLContext;
import graphql.kickstart.servlet.context.DefaultGraphQLServletContext;
import graphql.kickstart.servlet.context.DefaultGraphQLWebSocketContext;
import graphql.kickstart.servlet.context.GraphQLServletContextBuilder;
import lombok.RequiredArgsConstructor;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderRegistry;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;
import java.util.List;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Component
@RequiredArgsConstructor
public class CustomGraphQLContextBuilder implements GraphQLServletContextBuilder {
    private final UserEntityMapper userEntityMapper;
    private final AdministratorEntityMapper administratorEntityMapper;
    private final CustomerEntityMapper customerEntityMapper;
    private final AddressEntityMapper addressEntityMapper;
    private final CustomerGroupJoinEntityMapper customerGroupJoinEntityMapper;
    private final CustomerGroupEntityMapper customerGroupEntityMapper;
    private final UserRoleJoinEntityMapper userRoleJoinEntityMapper;
    private final RoleEntityMapper roleEntityMapper;
    private final AuthenticationMethodEntityMapper authenticationMethodEntityMapper;
    private final FacetEntityMapper facetEntityMapper;
    private final FacetValueEntityMapper facetValueEntityMapper;
    private final ProductOptionEntityMapper productOptionEntityMapper;
    private final ProductOptionGroupEntityMapper productOptionGroupEntityMapper;
    private final AssetEntityMapper assetEntityMapper;
    private final ProductAssetJoinEntityMapper productAssetJoinEntityMapper;
    private final ProductVariantEntityMapper productVariantEntityMapper;
    private final ProductOptionGroupJoinEntityMapper productOptionGroupJoinEntityMapper;
    private final ProductFacetValueJoinEntityMapper productFacetValueJoinEntityMapper;
    private final ProductEntityMapper productEntityMapper;
    private final ProductVariantAssetJoinEntityMapper productVariantAssetJoinEntityMapper;
    private final ProductVariantProductOptionJoinEntityMapper productVariantProductOptionJoinEntityMapper;
    private final ProductVariantFacetValueJoinEntityMapper productVariantFacetValueJoinEntityMapper;
    private final CollectionAssetJoinEntityMapper collectionAssetJoinEntityMapper;
    private final CollectionEntityMapper collectionEntityMapper;
    private final OrderEntityMapper orderEntityMapper;
    private final FulfillmentEntityMapper fulfillmentEntityMapper;
    private final OrderItemEntityMapper orderItemEntityMapper;
    private final OrderPromotionJoinEntityMapper orderPromotionJoinEntityMapper;
    private final PromotionEntityMapper promotionEntityMapper;
    private final PaymentEntityMapper paymentEntityMapper;
    private final ShippingMethodEntityMapper shippingMethodEntityMapper;
    private final OrderLineEntityMapper orderLineEntityMapper;
    private final RefundEntityMapper refundEntityMapper;

    @Override
    public GraphQLContext build(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        DefaultGraphQLServletContext defaultGraphQLServletContext =
                DefaultGraphQLServletContext.createServletContext(buildDataLoaderRegistry(), null)
                        .with(httpServletRequest).with(httpServletResponse).build();
        return new CustomGraphQLServletContext(defaultGraphQLServletContext);
    }

    @Override
    public GraphQLContext build(Session session, HandshakeRequest handshakeRequest) {
        return DefaultGraphQLWebSocketContext.createWebSocketContext(buildDataLoaderRegistry(), null)
                .with(session).with(handshakeRequest).build();
    }

    @Override
    public GraphQLContext build() {
        return new DefaultGraphQLContext(buildDataLoaderRegistry(), null);
    }

    private DataLoaderRegistry buildDataLoaderRegistry() {
        DataLoaderRegistry dataLoaderRegistry = new DataLoaderRegistry();

        DataLoader<Long, User> administratorUserDataLoader = DataLoader.newMappedDataLoader(
                new AdministratorUserDataLoader(this.userEntityMapper, this.administratorEntityMapper)
        );
        dataLoaderRegistry.register(Constant.DATA_LOADER_NAME_ADMINISTRATOR_USER, administratorUserDataLoader);

        DataLoader<Long, Administrator> historyEntryAdministratorDataLoader = DataLoader.newMappedDataLoader(
                new HistoryEntryAdministratorDataLoader(this.administratorEntityMapper)
        );
        dataLoaderRegistry.register(
                Constant.DATA_LOADER_NAME_HISTORY_ENTRY_ADMINISTRATOR,
                historyEntryAdministratorDataLoader);

        DataLoader<Long, User> customerUserDataLoader = DataLoader.newMappedDataLoader(
                new CustomerUserDataLoader(this.userEntityMapper, this.customerEntityMapper)
        );
        dataLoaderRegistry.register(Constant.DATA_LOADER_NAME_CUSTOMER_USER, customerUserDataLoader);

        DataLoader<Long, List<Address>> customerAddressesDataLoader = DataLoader.newMappedDataLoader(
                new CustomerAddressesDataLoader(this.addressEntityMapper)
        );
        dataLoaderRegistry.register(Constant.DATA_LOADER_NAME_CUSTOMER_ADDRESSES, customerAddressesDataLoader);

        DataLoader<Long, List<CustomerGroup>> customerGroupDataLoader = DataLoader.newMappedDataLoader(
                new CustomerGroupsDataLoader(this.customerGroupJoinEntityMapper, this.customerGroupEntityMapper)
        );
        dataLoaderRegistry.register(Constant.DATA_LOADER_NAME_CUSTOMER_GROUPS, customerGroupDataLoader);

        DataLoader<Long, List<Role>> userRolesDataLoader = DataLoader.newMappedDataLoader(
                new UserRolesDataLoader(this.userRoleJoinEntityMapper, this.roleEntityMapper)
        );
        dataLoaderRegistry.register(Constant.DATA_LOADER_NAME_USER_ROLES, userRolesDataLoader);

        DataLoader<Long, List<AuthenticationMethod>> userAuthenticationMethodsDataLoader =
                DataLoader.newMappedDataLoader(
                        new UserAuthenticationMethodsDataLoader(this.authenticationMethodEntityMapper));
        dataLoaderRegistry.register(
                Constant.DATA_LOADER_NAME_USER_AUTHENTICATION_METHODS, userAuthenticationMethodsDataLoader);

        DataLoader<Long, Facet> facetValueFacetDataLoader = DataLoader.newMappedDataLoader(
                new FacetValueFacetDataLoader((this.facetEntityMapper))
        );
        dataLoaderRegistry.register(Constant.DATA_LOADER_NAME_FACET_VALUE_FACET, facetValueFacetDataLoader);

        DataLoader<Long, List<FacetValue>> facetValuesDataLoader = DataLoader.newMappedDataLoader(
                new FacetValuesDataLoader(this.facetValueEntityMapper)
        );
        dataLoaderRegistry.register(Constant.DATA_LOADER_NAME_FACET_VALUES, facetValuesDataLoader);

        DataLoader<Long, List<ProductOption>> productOptionsDataLoader = DataLoader.newMappedDataLoader(
                new ProductOptionsDataLoader(this.productOptionEntityMapper)
        );
        dataLoaderRegistry.register(Constant.DATA_LOADER_NAME_PRODUCT_OPTIONS, productOptionsDataLoader);

        DataLoader<Long, ProductOptionGroup> productOptionGroupDataLoader = DataLoader.newMappedDataLoader(
                new ProductOptionGroupDataLoader(this.productOptionGroupEntityMapper)
        );
        dataLoaderRegistry.register(Constant.DATA_LOADER_NAME_PRODUCT_OPTION_GROUP, productOptionGroupDataLoader);

        DataLoader<Long, Asset> productFeaturedAssetDataLoader = DataLoader.newMappedDataLoader(
                new FeaturedAssetDataLoader(this.assetEntityMapper)
        ) ;
        dataLoaderRegistry.register(
                Constant.DATA_LOADER_NAME_PRODUCT_FEATURED_ASSET, productFeaturedAssetDataLoader
        );

        DataLoader<Long, List<Asset>> productAssetsDataLoader = DataLoader.newMappedDataLoader(
                new ProductAssetsDataLoader(this.productAssetJoinEntityMapper, this.assetEntityMapper)
        );
        dataLoaderRegistry.register(
                Constant.DATA_LOADER_NAME_PRODUCT_ASSETS, productAssetsDataLoader
        );

        DataLoader<Long, List<ProductVariant>> productVariantsDataLoader = DataLoader.newMappedDataLoader(
                new ProductVariantsDataLoader(this.productVariantEntityMapper)
        );
        dataLoaderRegistry.register(
                Constant.DATA_LOADER_NAME_PRODUCT_VARIANTS, productVariantsDataLoader
        );

        DataLoader<Long, List<ProductOptionGroup>> productOptionGroupsDataLoader = DataLoader.newMappedDataLoader(
                new ProductOptionGroupsDataLoader(
                        this.productOptionGroupJoinEntityMapper, this.productOptionGroupEntityMapper)
        );
        dataLoaderRegistry.register(
                Constant.DATA_LOADER_NAME_PRODUCT_OPTION_GROUPS, productOptionGroupsDataLoader
        );

        DataLoader<Long, List<FacetValue>> productFacetValuesDataLoader = DataLoader.newMappedDataLoader(
                new ProductFacetValuesDataLoader(
                        this.productFacetValueJoinEntityMapper,
                        this.facetValueEntityMapper,
                        this.facetEntityMapper)
        );
        dataLoaderRegistry.register(
                Constant.DATA_LOADER_NAME_PRODUCT_FACET_VALUES, productFacetValuesDataLoader
        );

        DataLoader<Long, Asset> productVariantFeaturedAssetDataLoader = DataLoader.newMappedDataLoader(
                new FeaturedAssetDataLoader(this.assetEntityMapper)
        ) ;
        dataLoaderRegistry.register(
                Constant.DATA_LOADER_NAME_PRODUCT_VARIANT_FEATURED_ASSET, productVariantFeaturedAssetDataLoader
        );

        DataLoader<Long, Product> productVariantProductDataLoader = DataLoader.newMappedDataLoader(
                new ProductVariantProductDataLoader(this.productEntityMapper)
        );
        dataLoaderRegistry.register(
                Constant.DATA_LOADER_NAME_PRODUCT_VARIANT_PRODUCT, productVariantProductDataLoader
        );

        DataLoader<Long, List<Asset>> productVariantAssetsDataLoader = DataLoader.newMappedDataLoader(
                new ProductVariantAssetsDataLoader(this.productVariantAssetJoinEntityMapper, this.assetEntityMapper)
        );
        dataLoaderRegistry.register(
                Constant.DATA_LOADER_NAME_PRODUCT_VARIANT_ASSETS, productVariantAssetsDataLoader
        );

        DataLoader<Long, List<ProductOption>> productVariantOptionsDataLoader = DataLoader.newMappedDataLoader(
                new ProductVariantOptionsDataLoader(
                        this.productVariantProductOptionJoinEntityMapper,this.productOptionEntityMapper)
        );
        dataLoaderRegistry.register(
                Constant.DATA_LOADER_NAME_PRODUCT_VARIANT_OPTIONS, productVariantOptionsDataLoader
        );

        DataLoader<Long, List<FacetValue>> productVariantFacetValuesDataLoader = DataLoader.newMappedDataLoader(
                new ProductVariantFacetValuesDataLoader(
                        this.productVariantFacetValueJoinEntityMapper,
                        this.facetValueEntityMapper,
                        this.facetEntityMapper
                )
        );
        dataLoaderRegistry.register(
                Constant.DATA_LOADER_NAME_PRODUCT_VARIANT_FACET_VALUES, productVariantFacetValuesDataLoader
        );

        DataLoader<Long, Asset> collectionFeaturedAssetDataLoader = DataLoader.newMappedDataLoader(
                new FeaturedAssetDataLoader(this.assetEntityMapper)
        ) ;
        dataLoaderRegistry.register(
                Constant.DATA_LOADER_NAME_COLLECTION_FEATURED_ASSET, collectionFeaturedAssetDataLoader
        );

        DataLoader<Long, List<Asset>> collectionAssetsDataLoader = DataLoader.newMappedDataLoader(
                new CollectionAssetsDataLoader(this.collectionAssetJoinEntityMapper, this.assetEntityMapper)
        );
        dataLoaderRegistry.register(
                Constant.DATA_LOADER_NAME_COLLECTION_ASSETS, collectionAssetsDataLoader
        );

        DataLoader<Long, Collection> collectionParentDataLoader = DataLoader.newMappedDataLoader(
                new CollectionParentDataLoader(this.collectionEntityMapper)
        );
        dataLoaderRegistry.register(
                Constant.DATA_LOADER_NAME_COLLECTION_PARENT, collectionParentDataLoader
        );

        DataLoader<Long, List<Collection>> collectionChildrenDataLoader = DataLoader.newMappedDataLoader(
                new CollectionChildrenDataLoader(this.collectionEntityMapper)
        );
        dataLoaderRegistry.register(
                Constant.DATA_LOADER_NAME_COLLECTION_CHILDREN, collectionChildrenDataLoader
        );

        DataLoader<Long, ProductVariant> orderLineProductVariantDataLoader = DataLoader.newMappedDataLoader(
                new ProductVariantDataLoader(this.productVariantEntityMapper)
        );
        dataLoaderRegistry.register(
                Constant.DATA_LOADER_NAME_ORDER_LINE_PRODUCT_VARIANT, orderLineProductVariantDataLoader
        );

        DataLoader<Long, Asset> orderLineFeaturedAssetDataLoader = DataLoader.newMappedDataLoader(
                new FeaturedAssetDataLoader(this.assetEntityMapper)
        ) ;
        dataLoaderRegistry.register(
                Constant.DATA_LOADER_NAME_ORDER_LINE_FEATURED_ASSET, orderLineFeaturedAssetDataLoader
        );

        DataLoader<Long, Order> orderLineOrderDataLoader = DataLoader.newMappedDataLoader(
                new OrderDataLoader(this.orderEntityMapper)
        );
        dataLoaderRegistry.register(
                Constant.DATA_LOADER_NAME_ORDER_LINE_ORDER, orderLineOrderDataLoader
        );

        DataLoader<Long, Fulfillment> orderItemFulfillmentDataLoader = DataLoader.newMappedDataLoader(
                new FulfillmentDataLoader(this.fulfillmentEntityMapper)
        );
        dataLoaderRegistry.register(
                Constant.DATA_LOADER_NAME_ORDER_ITEM_FULFILLMENT, orderItemFulfillmentDataLoader
        );

        DataLoader<Long, List<OrderItem>> fulfillmentOrderItemsDataLoader = DataLoader.newMappedDataLoader(
                new FulfillmentOrderItemsDataLoder(this.orderItemEntityMapper)
        );
        dataLoaderRegistry.register(
                Constant.DATA_LOADER_NAME_FULFILLMENT_ORDER_ITEMS, fulfillmentOrderItemsDataLoader
        );

        DataLoader<Long, Customer> orderCustomerDataLoader = DataLoader.newMappedDataLoader(
                new CustomerDataLoader(this.customerEntityMapper)
        );
        dataLoaderRegistry.register(
                Constant.DATA_LOADER_NAME_ORDER_CUSTOMER, orderCustomerDataLoader
        );

        DataLoader<Long, List<Promotion>> orderPromotionsDataLoader = DataLoader.newMappedDataLoader(
                new OrderPromotionsDataLoader(
                        this.orderPromotionJoinEntityMapper,
                        this.promotionEntityMapper
                )
        );
        dataLoaderRegistry.register(
                Constant.DATA_LOADER_NAME_ORDER_PROMOTIONS, orderPromotionsDataLoader
        );

        DataLoader<Long, List<Payment>> orderPaymentsDataLoader = DataLoader.newMappedDataLoader(
                new OrderPaymentsDataLoader(this.paymentEntityMapper)
        );
        dataLoaderRegistry.register(
                Constant.DATA_LOADER_NAME_ORDER_PAYMENTS, orderPaymentsDataLoader
        );

        DataLoader<Long, ShippingMethod> orderShippingMethodDataLoader = DataLoader.newMappedDataLoader(
                new ShippingMethodDataLoader(this.shippingMethodEntityMapper)
        );
        dataLoaderRegistry.register(
                Constant.DATA_LOADER_NAME_ORDER_SHIPPING_METHOD, orderShippingMethodDataLoader
        );

        DataLoader<Long, List<OrderItem>> orderLineItemsDataLoader = DataLoader.newMappedDataLoader(
                new OrderLineItemsDataLoader(this.orderItemEntityMapper)
        );
        dataLoaderRegistry.register(
                Constant.DATA_LOADER_NAME_ORDER_LINE_ITEMS, orderLineItemsDataLoader
        );

        DataLoader<Long, List<OrderLine>> orderLinesDataLoader = DataLoader.newMappedDataLoader(
                new OrderLinesDataLoader(this.orderLineEntityMapper, this.orderItemEntityMapper)
        );
        dataLoaderRegistry.register(
                Constant.DATA_LOADER_NAME_ORDER_LINES, orderLinesDataLoader
        );

        DataLoader<Long, List<Refund>> paymentRefundsDataLoader = DataLoader.newMappedDataLoader(
                new PaymentRefundsDataLoader(this.refundEntityMapper)
        );
        dataLoaderRegistry.register(
                Constant.DATA_LOADER_NAME_PAYMENT_REFUNDS, paymentRefundsDataLoader
        );

        DataLoader<Long, List<OrderItem>> refundOrderItemsDataLoader = DataLoader.newMappedDataLoader(
                new RefundOrderItemsDataLoader(this.orderItemEntityMapper)
        );
        dataLoaderRegistry.register(
                Constant.DATA_LOADER_NAME_REFUND_ORDER_ITEMS, refundOrderItemsDataLoader
        );

        return dataLoaderRegistry;
    }
}
