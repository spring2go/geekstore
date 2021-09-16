/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */
drop all objects delete files;

create table tb_user (
    id bigint not null auto_increment,
    deleted_at datetime,
    identifier varchar(50),
    verified boolean not null default false,
    last_login datetime,
    created_by varchar(50),
    created_at datetime,
    updated_by varchar(50),
    updated_at datetime,
    primary key (id),
    unique key (identifier)
);

create index idx_user_identifier on tb_user(identifier);

create table tb_auth_method (
    id bigint not null auto_increment,
    user_id bigint not null,
    external boolean not null default false,
    /* begin for native shared */
    identifier varchar(50),
    password_hash varchar(255),
    verification_token varchar(100),
    password_reset_token varchar(100),
    identifier_change_token varchar(100),
    pending_identifier varchar(50),
    /* end for native shared */

    /* begin for external shared */
    strategy varchar(50),
    external_identifier varchar(50),
    metadata text,
    /* end for external shared */
    created_by varchar(50),
    created_at datetime,
    updated_by varchar(50),
    updated_at datetime,
    primary key (id)
);

create index idx_auth_method_user_id on tb_auth_method(user_id);
create index idx_auth_method_verification_token on tb_auth_method(verification_token);
create index idx_auth_method_password_reset_token on tb_auth_method(password_reset_token);
create index idx_auth_method_identifier_change_token on tb_auth_method(identifier_change_token);
create index idx_auth_method_external_identifier on tb_auth_method(external_identifier);

create table tb_role (
    id bigint not null auto_increment,
    code varchar(50) not null,
    description varchar(255) not null,
    permissions text,
    created_by varchar(50),
    created_at datetime,
    updated_by varchar(50),
    updated_at datetime,
    primary key (id)
);

create index idx_role_code on tb_role(code);

create table tb_user_role_join (
     id bigint not null auto_increment,
     user_id bigint not null ,
     role_id bigint not null ,
     created_by varchar(50),
     created_at datetime,
     updated_by varchar(50),
     updated_at datetime,
     primary key (id),
     unique key (user_id, role_id),
     foreign key (role_id) references tb_role(id),
     foreign key (user_id) references tb_user(id)
);

create table tb_customer (
    id bigint not null auto_increment,
    deleted_at datetime,
    title varchar(50),
    first_name varchar(50),
    last_name varchar(50),
    phone_number varchar(50),
    email_address varchar(50),
    user_id bigint,
    created_by varchar(50),
    created_at datetime,
    updated_by varchar(50),
    updated_at datetime,
    primary key (id),
    foreign key (user_id) references tb_user(id)
);
create index idx_customer_user_id on tb_customer(user_id);

create table tb_administrator (
    id bigint not null auto_increment,
    first_name varchar(50),
    last_name varchar(50),
    email_address varchar(50),
    user_id bigint,
    deleted_at datetime,
    created_by varchar(50),
    created_at datetime,
    updated_by varchar(50),
    updated_at datetime,
    primary key (id),
    unique key (email_address),
    foreign key (user_id) references tb_user(id)
);
create index idx_administrator_user_id on tb_administrator(user_id);

create table tb_address (
     id bigint not null auto_increment,
     customer_id bigint not null,
     full_name varchar(50) not null default '',
     company varchar(50) not null default '',
     street_line1 varchar(100) not null,
     street_line2 varchar(100) not null default '',
     city varchar(50) not null default '',
     province varchar(50) not null default '',
     postal_code varchar(50) not null default '',
     phone_number varchar(50) not null default '',
     default_shipping_address boolean default false,
     default_billing_address boolean default false,
     created_by varchar(50),
     created_at datetime,
     updated_by varchar(50),
     updated_at datetime,
     primary key (id),
     foreign key (customer_id) references tb_customer(id)
);
create index idx_address_customer_id on tb_address(customer_id);

create table tb_customer_group (
     id bigint not null auto_increment,
     name varchar(50) not null,
     created_by varchar(50),
     created_at datetime,
     updated_by varchar(50),
     updated_at datetime,
     primary key (id)
);

create table tb_customer_group_join (
     id bigint not null auto_increment,
     customer_id bigint not null,
     group_id bigint not null,
     created_by varchar(50),
     created_at datetime,
     updated_by varchar(50),
     updated_at datetime,
     primary key (id),
     unique key (customer_id, group_id),
     foreign key (customer_id) references tb_customer(id),
     foreign key (group_id) references tb_customer_group(id)
);

create table tb_facet (
     id bigint not null auto_increment,
     name varchar(50) not null,
     code varchar(50) not null,
     private_only boolean,
     created_by varchar(50),
     created_at datetime,
     updated_by varchar(50),
     updated_at datetime,
     unique key (code),
     primary key (id)
);

create index idx_facet_code on tb_facet(code);

create table tb_facet_value (
     id bigint not null auto_increment,
     name varchar(50) not null,
     code varchar(50) not null,
     facet_id bigint not null,
     created_by varchar(50),
     created_at datetime,
     updated_by varchar(50),
     updated_at datetime,
     primary key (id),
     foreign key (facet_id) references tb_facet(id)
);

create index idx_facet_value_facet_id on tb_facet_value(facet_id);

create table tb_asset (
    id bigint not null auto_increment,
    name varchar(100),
    type varchar(50),
    file_size integer,
    width integer,
    height integer,
    mime_type varchar(50),
    source varchar(255),
    preview varchar(255),
    focal_point text,
    created_by varchar(50),
    created_at datetime,
    updated_by varchar(50),
    updated_at datetime,
    primary key (id)
);

create table tb_session (
    id bigint not null auto_increment,
    token varchar(100) not null,
    expires datetime not null,
    invalidated boolean not null,
    anonymous boolean not null default false,
    user_id bigint,
    authentication_strategy varchar(50),
    active_order_id bigint,
    created_by varchar(50),
    created_at datetime,
    updated_by varchar(50),
    updated_at datetime,
    primary key (id),
    unique key (token),
    foreign key (user_id) references tb_user(id)
    /* foreign key (active_order_id) references to_order(id) */
);

create index idx_session_user_id on tb_session(user_id);
create index idx_session_token on tb_session(token);
create index idx_session_active_order_id on tb_session(active_order_id);

create table tb_customer_history_entry (
    id bigint not null auto_increment,
    administrator_id bigint,
    type varchar(50) not null,
    private_only boolean,
    data text,
    customer_id bigint,
    created_by varchar(50),
    created_at datetime,
    updated_by varchar(50),
    updated_at datetime,
    primary key (id)
);

create index idx_customer_history_entry_type on tb_customer_history_entry(type);

create table tb_order_history_entry (
    id bigint not null auto_increment,
    administrator_id bigint,
    type varchar(50) not null,
    private_only boolean,
    data text,
    order_id bigint,
    created_by varchar(50),
    created_at datetime,
    updated_by varchar(50),
    updated_at datetime,
    primary key (id)
);

create table tb_product (
   id bigint not null auto_increment,
   name varchar(255) not null,
   slug varchar(255) not null,
   description text,
   enabled boolean default true,
   featured_asset_id bigint,
   deleted_at datetime default null,
   created_by varchar(30),
   created_at datetime,
   updated_by varchar(30),
   updated_at datetime,
   primary key (id),
   foreign key (featured_asset_id) references tb_asset(id)
);

create index idx_product_slug on tb_product(slug);

create table tb_product_option_group (
   id bigint not null auto_increment,
   name varchar(50) not null,
   code varchar(50) not null,
   product_id bigint,
   created_by varchar(30),
   created_at datetime,
   updated_by varchar(30),
   updated_at datetime,
   primary key (id),
   foreign key (product_id) references tb_product(id)
);

create table tb_product_option (
   id bigint not null auto_increment,
   name varchar(50) not null,
   code varchar(50) not null,
   group_id bigint not null,
   created_by varchar(50),
   created_at datetime,
   updated_by varchar(50),
   updated_at datetime,
   primary key (id),
   foreign key (group_id) references tb_product_option_group(id)
);

create table tb_product_option_group_join (
   id bigint not null auto_increment,
   product_id bigint not null,
   option_group_id bigint not null,
   created_by varchar(50),
   created_at datetime,
   updated_by varchar(50),
   updated_at datetime,
   primary key (id),
   unique key (product_id, option_group_id),
   foreign key (product_id) references tb_product(id),
   foreign key (option_group_id) references tb_product_option_group(id)
);

create table tb_product_asset_join (
   id bigint not null auto_increment,
   product_id bigint not null,
   asset_id bigint not null,
   position bigint,
   created_by varchar(50),
   created_at datetime,
   updated_by varchar(50),
   updated_at datetime,
   primary key (id),
   unique key (product_id, asset_id),
   foreign key (product_id) references tb_product(id),
   foreign key (asset_id) references tb_asset(id)
);

create table tb_product_facet_value_join (
   id bigint not null auto_increment,
   created_by varchar(50),
   created_at datetime,
   updated_by varchar(50),
   updated_at datetime,
   product_id bigint not null,
   facet_value_id bigint not null,
   primary key (id),
   unique key(product_id, facet_value_id),
   foreign key (product_id) references tb_product(id),
   foreign key (facet_value_id) references tb_facet_value(id)
);

create table tb_product_variant (
   id bigint not null auto_increment,
   name varchar(255) not null,
   sku varchar(100) not null,
   price integer not null,
   featured_asset_id bigint,
   product_id bigint not null,
   stock_on_hand bigint default 0,
   track_inventory boolean,
   deleted_at datetime default null,
   enabled boolean default true,
   created_by varchar(30),
   created_at datetime,
   updated_by varchar(30),
   updated_at datetime,
   primary key (id),
   foreign key (featured_asset_id) references tb_asset(id),
   foreign key (product_id) references tb_product(id)
);

create index idx_product_variant_product_id on tb_product_variant(product_id);

create table tb_product_variant_asset_join (
   id bigint not null auto_increment,
   product_variant_id bigint not null,
   asset_id bigint not null,
   position bigint,
   created_by varchar(50),
   created_at datetime,
   updated_by varchar(50),
   updated_at datetime,
   primary key (id),
   unique key (product_variant_id, asset_id),
   foreign key (product_variant_id) references tb_product_variant(id),
   foreign key (asset_id) references tb_asset(id)
);

create table tb_product_variant_product_option_join (
   id bigint not null auto_increment,
   product_variant_id bigint not null,
   product_option_id bigint not null,
   created_by varchar(50),
   created_at datetime,
   updated_by varchar(50),
   updated_at datetime,
   primary key (id),
   unique key (product_variant_id, product_option_id),
   foreign key (product_variant_id) references tb_product_variant(id),
   foreign key (product_option_id) references tb_product_option(id)
);

create table tb_product_variant_facet_value_join (
   id bigint not null auto_increment,
   product_variant_id bigint not null,
   facet_value_id bigint not null,
   created_by varchar(50),
   created_at datetime,
   updated_by varchar(50),
   updated_at datetime,
   primary key (id),
   unique key (product_variant_id, facet_value_id),
   foreign key (product_variant_id) references tb_product_variant(id),
   foreign key (facet_value_id) references tb_facet_value(id)
);

create table tb_stock_movement (
   id bigint not null auto_increment,
   type varchar(50) not null,
   product_variant_id bigint not null,
   quantity bigint,
   order_line_id bigint,
   order_item_id bigint,
   created_by varchar(50),
   created_at datetime,
   updated_by varchar(50),
   updated_at datetime,
   primary key (id),
   foreign key (product_variant_id) references tb_product_variant(id)
);

create table tb_global_settings (
   id bigint not null auto_increment,
   track_inventory boolean default false,
   custom_fields text,
   created_by varchar(50),
   created_at datetime,
   updated_by varchar(50),
   updated_at datetime,
   primary key (id)
);

create table tb_collection (
   id bigint not null auto_increment,
   root boolean default false,
   private_only boolean default false,
   position integer,
   name varchar(255),
   slug varchar(255),
   description text,
   featured_asset_id bigint,
   parent_id bigint,
   filters text,
   created_by varchar(50),
   created_at datetime,
   updated_by varchar(50),
   updated_at datetime,
   primary key (id),
   foreign key (featured_asset_id) references tb_asset(id),
   foreign key (parent_id) references tb_collection(id)
);

create index idx_collection_parent_id on tb_collection(parent_id);

create table tb_product_variant_collection_join (
   id bigint not null auto_increment,
   product_variant_id bigint not null,
   collection_id bigint not null,
   created_by varchar(50),
   created_at datetime,
   updated_by varchar(50),
   updated_at datetime,
   primary key (id),
   unique key (product_variant_id, collection_id),
   foreign key (product_variant_id) references tb_product_variant(id),
   foreign key (collection_id) references tb_collection(id)
);

create table tb_collection_asset_join (
   id bigint not null auto_increment,
   collection_id bigint not null,
   asset_id bigint not null,
   position bigint,
   created_by varchar(50),
   created_at datetime,
   updated_by varchar(50),
   updated_at datetime,
   primary key (id),
   unique key (collection_id, asset_id),
   foreign key (collection_id) references tb_collection(id),
   foreign key (asset_id) references tb_asset(id)
);

create table tb_shipping_method (
   id bigint not null auto_increment,
   code varchar(50),
   description text,
   deleted_at datetime,
   checker text,
   calculator text,
   created_by varchar(50),
   created_at datetime,
   updated_by varchar(50),
   updated_at datetime,
   primary key (id)
);

create table tb_order (
   id bigint not null auto_increment,
   code varchar(50),
   state varchar(30),
   active boolean default true,
   order_placed_at datetime default null,
   customer_id bigint,
   coupon_codes text,
   pending_adjustments text,
   shipping_address text,
   billing_address text,
   sub_total integer,
   shipping_method_id bigint,
   shipping integer default 0,
   created_by varchar(50),
   created_at datetime,
   updated_by varchar(50),
   updated_at datetime,
   primary key (id),
   foreign key (customer_id) references tb_customer(id),
   foreign key (shipping_method_id) references tb_shipping_method(id)
);

create index order_code_index on tb_order(code);


create table tb_order_line (
    id bigint not null auto_increment,
    product_variant_id bigint not null,
    featured_asset_id bigint,
    order_id bigint not null,
    created_by varchar(50),
    created_at datetime,
    updated_by varchar(50),
    updated_at datetime,
    foreign key (order_id) references tb_order(id),
    foreign key (product_variant_id) references tb_product_variant(id),
    foreign key (featured_asset_id) references tb_asset(id),
    primary key (id)
);

create index idx_order_line_product_variant_id on tb_order_line(product_variant_id);
create index idx_order_line_order_id on tb_order_line(order_id);

create table tb_promotion (
   id bigint not null auto_increment,
   name varchar(100),
   enabled boolean,
   deleted_at datetime default null,
   starts_at datetime default null,
   ends_at datetime default null,
   coupon_code varchar(32) default null,
   per_customer_usage_limit integer default null,
   conditions text,
   actions text,
   priority_score integer,
   created_by varchar(50),
   created_at datetime,
   updated_by varchar(50),
   updated_at datetime,
   primary key (id)
);

create table tb_order_promotion_join (
    id bigint not null auto_increment,
    order_id bigint not null,
    promotion_id bigint not null,
    created_by varchar(50),
    created_at datetime,
    updated_by varchar(50),
    updated_at datetime,
    primary key (id),
    unique key (order_id, promotion_id),
    foreign key (order_id) references tb_order(id),
    foreign key (promotion_id) references tb_promotion(id)
);

create table tb_refund (
    id bigint not null auto_increment,
    items integer,
    shipping integer,
    adjustment integer,
    total integer,
    method varchar(30),
    reason varchar(255),
    state varchar(30),
    transaction_id varchar(128),
    payment_id bigint,
    metadata text,
    created_by varchar(50),
    created_at datetime,
    updated_by varchar(50),
    updated_at datetime,
    primary key (id)
);

create table tb_fulfillment (
    id bigint not null auto_increment,
    tracking_code varchar(128),
    method varchar(30),
    created_by varchar(50),
    created_at datetime,
    updated_by varchar(50),
    updated_at datetime,
    primary key (id)
);

create table tb_order_item (
   id bigint not null auto_increment,
   order_line_id bigint,
   unit_price integer,
   pending_adjustments text,
   fulfillment_id bigint,
   refund_id bigint,
   cancellation_id bigint,
   cancelled boolean default false,
   created_by varchar(50),
   created_at datetime,
   updated_by varchar(50),
   updated_at datetime,
   primary key (id),
   foreign key (order_line_id) references tb_order_line(id)
);

create table tb_payment (
   id bigint not null auto_increment,
   method varchar(50),
   amount integer,
   state varchar(30),
   error_message varchar(255),
   transaction_id varchar(128),
   metadata text,
   order_id bigint not null,
   created_by varchar(50),
   created_at datetime,
   updated_by varchar(50),
   updated_at datetime,
   primary key (id),
   foreign key (order_id) references tb_order(id)
);

create table tb_payment_method (
    id bigint not null auto_increment,
    code varchar(50),
    enabled boolean,
    config_args text,
    created_by varchar(50),
    created_at datetime,
    updated_by varchar(50),
    updated_at datetime,
    primary key (id)
);

create table tb_search_index_item (
    product_variant_id bigint not null,
    product_id bigint,
    enabled boolean,
    product_name varchar(255),
    product_variant_name varchar(255),
    description text,
    slug varchar(255),
    sku varchar(100),
    price integer,
    facet_ids text,
    facet_value_ids text,
    collection_ids text,
    collection_slugs text,
    product_preview varchar(255),
    product_preview_focal_point tinytext,
    product_variant_preview varchar(255),
    product_variant_preview_focal_point tinytext,
    product_asset_id bigint,
    product_variant_asset_id bigint,
    primary key (product_variant_id)
);

create index idx_product_asset_id on tb_search_index_item(product_asset_id);
create index idx_product_variant_asset_id on tb_search_index_item(product_variant_asset_id);