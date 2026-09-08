-- Flyway baseline migration for genuinely empty PostgreSQL databases.
--
-- Generated from the schema-only pg_dump of an exact e3b88b7 application
-- instance after V1-V134 had completed with the former local compatibility
-- callback and Hibernate validation had passed. No application rows, secrets,
-- fixed-ID placeholder companies, or local-staging seed data are included.
-- Existing databases with versioned history do not execute baseline migrations;
-- they receive the guarded V140 forward repair instead.
--
-- PostgreSQL database dump
--


-- Dumped from database version 16.14
-- Dumped by pg_dump version 16.14

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: pg_trgm; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS pg_trgm WITH SCHEMA public;


--
-- Name: EXTENSION pg_trgm; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON EXTENSION pg_trgm IS 'text similarity measurement and index searching based on trigrams';


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: app_content_page; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.app_content_page (
    id bigint NOT NULL,
    slug character varying(100) NOT NULL,
    title character varying(150) NOT NULL,
    content text NOT NULL,
    content_type character varying(30) DEFAULT 'PLAIN_TEXT'::character varying NOT NULL,
    active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: app_content_page_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.app_content_page_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: app_content_page_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.app_content_page_id_seq OWNED BY public.app_content_page.id;


--
-- Name: app_screen_content; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.app_screen_content (
    id bigint NOT NULL,
    screen_key character varying(50) NOT NULL,
    placement character varying(50) NOT NULL,
    media_type character varying(30) NOT NULL,
    media_url text NOT NULL,
    enabled boolean DEFAULT true NOT NULL,
    background_color character varying(20) DEFAULT '#000000'::character varying NOT NULL,
    aspect_ratio double precision,
    start_at timestamp with time zone,
    end_at timestamp with time zone,
    min_app_version character varying(40),
    sort_order integer DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT chk_app_screen_content_aspect_ratio CHECK (((aspect_ratio IS NULL) OR ((aspect_ratio > ((0)::numeric)::double precision) AND (aspect_ratio <= ((10)::numeric)::double precision)))),
    CONSTRAINT chk_app_screen_content_background_color CHECK (((background_color)::text ~ '^#([0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$'::text)),
    CONSTRAINT chk_app_screen_content_media_type CHECK (((media_type)::text = ANY ((ARRAY['LOTTIE_JSON'::character varying, 'VIDEO'::character varying])::text[]))),
    CONSTRAINT chk_app_screen_content_media_url CHECK ((media_url ~* '^https?://.+'::text)),
    CONSTRAINT chk_app_screen_content_placement CHECK (((placement)::text = 'TOP_BANNER'::text)),
    CONSTRAINT chk_app_screen_content_screen_key CHECK (((screen_key)::text = 'FAVORITES'::text)),
    CONSTRAINT chk_app_screen_content_time_window CHECK (((end_at IS NULL) OR (start_at IS NULL) OR (end_at >= start_at)))
);


--
-- Name: app_screen_content_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.app_screen_content_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: app_screen_content_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.app_screen_content_id_seq OWNED BY public.app_screen_content.id;


--
-- Name: app_setting; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.app_setting (
    id bigint NOT NULL,
    setting_key character varying(120) NOT NULL,
    setting_value text,
    description character varying(255),
    active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: app_setting_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.app_setting_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: app_setting_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.app_setting_id_seq OWNED BY public.app_setting.id;


--
-- Name: brand; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.brand (
    id bigint NOT NULL,
    name character varying(150) NOT NULL,
    logo_url text,
    description text,
    active boolean DEFAULT true NOT NULL,
    published boolean DEFAULT false NOT NULL,
    priority integer DEFAULT 0 NOT NULL,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    deleted boolean DEFAULT false NOT NULL,
    promo_media_type character varying(20),
    promo_media_url text,
    promo_enabled boolean DEFAULT false NOT NULL,
    category_id bigint,
    slug character varying(180) NOT NULL,
    hero_image_url text,
    short_description character varying(255),
    founded_year integer,
    customer_rating numeric(2,1),
    customer_rating_count integer,
    CONSTRAINT chk_brand_customer_rating_count_non_negative CHECK (((customer_rating_count IS NULL) OR (customer_rating_count >= 0))),
    CONSTRAINT chk_brand_customer_rating_range CHECK (((customer_rating IS NULL) OR ((customer_rating >= (0)::numeric) AND (customer_rating <= (5)::numeric)))),
    CONSTRAINT chk_brand_founded_year_range CHECK (((founded_year IS NULL) OR ((founded_year >= 1800) AND (founded_year <= 2200))))
);


--
-- Name: brand_category_link; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.brand_category_link (
    id bigint NOT NULL,
    brand_id bigint NOT NULL,
    category_id bigint NOT NULL,
    sort_order integer DEFAULT 0 NOT NULL,
    active boolean DEFAULT true NOT NULL,
    deleted boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: brand_category_link_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.brand_category_link_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: brand_category_link_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.brand_category_link_id_seq OWNED BY public.brand_category_link.id;


--
-- Name: brand_certificate; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.brand_certificate (
    id bigint NOT NULL,
    brand_id bigint NOT NULL,
    title character varying(180) NOT NULL,
    issuer character varying(180),
    certificate_url text,
    sort_order integer DEFAULT 0 NOT NULL,
    active boolean DEFAULT true NOT NULL,
    deleted boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: brand_certificate_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.brand_certificate_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: brand_certificate_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.brand_certificate_id_seq OWNED BY public.brand_certificate.id;


--
-- Name: brand_collaboration; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.brand_collaboration (
    id bigint NOT NULL,
    brand_id bigint NOT NULL,
    target_type character varying(20) NOT NULL,
    project_id bigint,
    builder_id bigint,
    company_id bigint,
    business_id bigint,
    role character varying(50),
    sort_order integer DEFAULT 0 NOT NULL,
    active boolean DEFAULT true NOT NULL,
    priority integer DEFAULT 0 NOT NULL,
    deleted boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    relation_type character varying(50),
    source_type character varying(50),
    usage_category character varying(150),
    title character varying(255),
    description text,
    verified boolean DEFAULT false NOT NULL,
    public_visible boolean DEFAULT false NOT NULL,
    featured boolean DEFAULT false NOT NULL,
    company_project_id bigint,
    CONSTRAINT chk_brand_collaboration_single_target CHECK (((((target_type)::text = 'PROJECT'::text) AND (project_id IS NOT NULL) AND (builder_id IS NULL) AND (company_id IS NULL) AND (business_id IS NULL) AND (company_project_id IS NULL)) OR (((target_type)::text = 'BUILDER'::text) AND (builder_id IS NOT NULL) AND (project_id IS NULL) AND (company_id IS NULL) AND (business_id IS NULL) AND (company_project_id IS NULL)) OR (((target_type)::text = 'COMPANY'::text) AND (company_id IS NOT NULL) AND (project_id IS NULL) AND (builder_id IS NULL) AND (business_id IS NULL) AND (company_project_id IS NULL)) OR (((target_type)::text = 'BUSINESS'::text) AND (business_id IS NOT NULL) AND (project_id IS NULL) AND (builder_id IS NULL) AND (company_id IS NULL) AND (company_project_id IS NULL)) OR (((target_type)::text = 'COMPANY_PROJECT'::text) AND (company_project_id IS NOT NULL) AND (project_id IS NULL) AND (builder_id IS NULL) AND (company_id IS NULL) AND (business_id IS NULL)))),
    CONSTRAINT chk_brand_collaboration_target_type CHECK (((target_type)::text = ANY ((ARRAY['PROJECT'::character varying, 'BUILDER'::character varying, 'COMPANY'::character varying, 'BUSINESS'::character varying, 'COMPANY_PROJECT'::character varying])::text[])))
);


--
-- Name: brand_collaboration_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.brand_collaboration_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: brand_collaboration_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.brand_collaboration_id_seq OWNED BY public.brand_collaboration.id;


--
-- Name: brand_distributor; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.brand_distributor (
    id bigint NOT NULL,
    brand_id bigint NOT NULL,
    distributor_id bigint NOT NULL,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    priority integer DEFAULT 0 NOT NULL,
    offer_title character varying(150),
    offer_description text,
    offer_banner_url text,
    valid_till timestamp with time zone,
    active boolean DEFAULT true NOT NULL,
    deleted boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: brand_distributor_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.brand_distributor_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: brand_distributor_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.brand_distributor_id_seq OWNED BY public.brand_distributor.id;


--
-- Name: brand_faq; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.brand_faq (
    id bigint NOT NULL,
    brand_id bigint NOT NULL,
    question character varying(300) NOT NULL,
    answer text NOT NULL,
    sort_order integer DEFAULT 0 NOT NULL,
    active boolean DEFAULT true NOT NULL,
    deleted boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: brand_faq_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.brand_faq_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: brand_faq_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.brand_faq_id_seq OWNED BY public.brand_faq.id;


--
-- Name: brand_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.brand_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: brand_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.brand_id_seq OWNED BY public.brand.id;


--
-- Name: brand_media; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.brand_media (
    id bigint NOT NULL,
    brand_id bigint NOT NULL,
    media_type character varying(20) NOT NULL,
    placement character varying(20) NOT NULL,
    url text NOT NULL,
    caption character varying(255),
    sort_order integer DEFAULT 0 NOT NULL,
    action_type character varying(30),
    action_value text,
    active boolean DEFAULT true NOT NULL,
    deleted boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone,
    updated_at timestamp with time zone,
    brand_sku_id bigint
);


--
-- Name: brand_media_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.brand_media_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: brand_media_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.brand_media_id_seq OWNED BY public.brand_media.id;


--
-- Name: brand_product_category; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.brand_product_category (
    id bigint NOT NULL,
    brand_id bigint NOT NULL,
    name character varying(150) NOT NULL,
    slug character varying(180) NOT NULL,
    description text,
    image_url text,
    external_url text,
    active boolean DEFAULT true NOT NULL,
    public_visible boolean DEFAULT true NOT NULL,
    sort_order integer DEFAULT 0 NOT NULL,
    deleted boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: brand_product_category_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.brand_product_category_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: brand_product_category_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.brand_product_category_id_seq OWNED BY public.brand_product_category.id;


--
-- Name: brand_sku; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.brand_sku (
    id bigint NOT NULL,
    brand_id bigint NOT NULL,
    category_id bigint,
    name character varying(150) NOT NULL,
    short_description character varying(255),
    image_url text,
    price_label character varying(60),
    sort_order integer DEFAULT 0 NOT NULL,
    active boolean DEFAULT true NOT NULL,
    published boolean DEFAULT false NOT NULL,
    priority integer DEFAULT 0 NOT NULL,
    deleted boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    slug character varying(180) NOT NULL,
    sku_code character varying(80),
    description text,
    featured boolean DEFAULT false NOT NULL,
    latest boolean DEFAULT false NOT NULL,
    external_url text,
    product_category_id bigint
);


--
-- Name: brand_sku_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.brand_sku_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: brand_sku_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.brand_sku_id_seq OWNED BY public.brand_sku.id;


--
-- Name: builder; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.builder (
    id bigint NOT NULL,
    name character varying(150) NOT NULL,
    logo_url character varying(255),
    description text,
    phone character varying(20),
    whatsapp character varying(20),
    email character varying(150),
    address_line text,
    city_id bigint,
    latitude double precision,
    longitude double precision,
    active boolean DEFAULT true NOT NULL,
    published boolean DEFAULT false NOT NULL,
    priority integer DEFAULT 0 NOT NULL,
    deleted boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: builder_after_sales_upgrade; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.builder_after_sales_upgrade (
    id bigint NOT NULL,
    profile_id bigint NOT NULL,
    title character varying(180) NOT NULL,
    short_summary text,
    legacy_issue_title character varying(180),
    legacy_issue_description text,
    solution_title character varying(180),
    solution_description text,
    result_title character varying(180),
    result_description text,
    metric_value character varying(80),
    metric_label character varying(120),
    implemented_at date,
    impact_level character varying(30) DEFAULT 'MEDIUM'::character varying NOT NULL,
    evidence_status character varying(40) DEFAULT 'EVIDENCE_PENDING'::character varying NOT NULL,
    display_order integer DEFAULT 0 NOT NULL,
    active boolean DEFAULT true NOT NULL,
    published boolean DEFAULT true NOT NULL,
    deleted boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    icon_key character varying(80),
    CONSTRAINT chk_builder_after_sales_upgrade_evidence CHECK (((evidence_status)::text = ANY ((ARRAY['EVIDENCE_PENDING'::character varying, 'BUILDER_SUBMITTED'::character varying, 'SFS_REVIEWED'::character varying, 'SFS_VERIFIED'::character varying, 'PUBLIC_RECORD_VERIFIED'::character varying, 'NOT_APPLICABLE'::character varying])::text[]))),
    CONSTRAINT chk_builder_after_sales_upgrade_impact CHECK (((impact_level)::text = ANY ((ARRAY['LOW'::character varying, 'MEDIUM'::character varying, 'HIGH'::character varying])::text[])))
);


--
-- Name: builder_after_sales_upgrade_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.builder_after_sales_upgrade_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: builder_after_sales_upgrade_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.builder_after_sales_upgrade_id_seq OWNED BY public.builder_after_sales_upgrade.id;


--
-- Name: builder_highlight_item; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.builder_highlight_item (
    id bigint NOT NULL,
    builder_id bigint NOT NULL,
    project_id bigint,
    city_id bigint,
    highlight_type character varying(40) NOT NULL,
    source_type character varying(40) NOT NULL,
    media_type character varying(40) NOT NULL,
    title character varying(180) NOT NULL,
    subtitle character varying(255),
    summary text,
    body text,
    tag_label character varying(80),
    tag_type character varying(60),
    thumbnail_url text,
    image_url text,
    video_url text,
    youtube_video_id character varying(80),
    external_url text,
    webview_enabled boolean DEFAULT false NOT NULL,
    publisher_name character varying(180),
    author_label character varying(150),
    read_time_minutes integer DEFAULT 0 NOT NULL,
    published_at timestamp with time zone,
    featured boolean DEFAULT false NOT NULL,
    verified boolean DEFAULT false NOT NULL,
    public_visible boolean DEFAULT false NOT NULL,
    active boolean DEFAULT true NOT NULL,
    sort_order integer DEFAULT 0 NOT NULL,
    status character varying(40) DEFAULT 'DRAFT'::character varying NOT NULL,
    created_by bigint,
    updated_by bigint,
    approved_by bigint,
    approved_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    deleted_at timestamp with time zone,
    CONSTRAINT chk_builder_highlight_external_news_publisher CHECK ((((highlight_type)::text <> 'NEWS_ARTICLE'::text) OR ((source_type)::text <> 'EXTERNAL_NEWS'::text) OR (publisher_name IS NOT NULL))),
    CONSTRAINT chk_builder_highlight_media_type CHECK (((media_type)::text = ANY ((ARRAY['IMAGE'::character varying, 'VIDEO'::character varying, 'YOUTUBE'::character varying, 'WEBVIEW'::character varying, 'NONE'::character varying])::text[]))),
    CONSTRAINT chk_builder_highlight_read_time CHECK ((read_time_minutes >= 0)),
    CONSTRAINT chk_builder_highlight_sort_order CHECK ((sort_order >= 0)),
    CONSTRAINT chk_builder_highlight_source_type CHECK (((source_type)::text = ANY ((ARRAY['BUILDER_OFFICIAL'::character varying, 'SFS_EDITORIAL'::character varying, 'EXTERNAL_NEWS'::character varying, 'SOCIAL_MEDIA'::character varying, 'OTHER'::character varying])::text[]))),
    CONSTRAINT chk_builder_highlight_status CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'PENDING_REVIEW'::character varying, 'PUBLISHED'::character varying, 'ARCHIVED'::character varying])::text[]))),
    CONSTRAINT chk_builder_highlight_type CHECK (((highlight_type)::text = ANY ((ARRAY['BUILDER_UPDATE'::character varying, 'SOCIAL_IMPACT'::character varying, 'NEWS_ARTICLE'::character varying, 'SFS_ANALYSIS'::character varying])::text[]))),
    CONSTRAINT chk_builder_highlight_webview_url CHECK ((((media_type)::text <> 'WEBVIEW'::text) OR (external_url IS NOT NULL))),
    CONSTRAINT chk_builder_highlight_youtube_ref CHECK ((((media_type)::text <> 'YOUTUBE'::text) OR (youtube_video_id IS NOT NULL) OR (video_url IS NOT NULL)))
);


--
-- Name: builder_highlight_item_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.builder_highlight_item_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: builder_highlight_item_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.builder_highlight_item_id_seq OWNED BY public.builder_highlight_item.id;


--
-- Name: builder_highlight_point; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.builder_highlight_point (
    id bigint NOT NULL,
    highlight_item_id bigint NOT NULL,
    point_type character varying(40) NOT NULL,
    title character varying(180),
    text text,
    icon_key character varying(80),
    display_order integer DEFAULT 0 NOT NULL,
    active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT chk_builder_highlight_point_display_order CHECK ((display_order >= 0)),
    CONSTRAINT chk_builder_highlight_point_type CHECK (((point_type)::text = ANY ((ARRAY['ADVANTAGE'::character varying, 'DISADVANTAGE'::character varying, 'SUMMARY'::character varying, 'IMPACT_METRIC'::character varying, 'KEY_TAKEAWAY'::character varying])::text[])))
);


--
-- Name: builder_highlight_point_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.builder_highlight_point_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: builder_highlight_point_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.builder_highlight_point_id_seq OWNED BY public.builder_highlight_point.id;


--
-- Name: builder_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.builder_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: builder_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.builder_id_seq OWNED BY public.builder.id;


--
-- Name: builder_improvement_action; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.builder_improvement_action (
    id bigint NOT NULL,
    profile_id bigint NOT NULL,
    action_type character varying(60) NOT NULL,
    title character varying(180) NOT NULL,
    subtitle character varying(255),
    context_text text,
    action_text text,
    result_text text,
    icon_key character varying(80),
    impact_level character varying(30) DEFAULT 'MEDIUM'::character varying NOT NULL,
    evidence_status character varying(40) DEFAULT 'EVIDENCE_PENDING'::character varying NOT NULL,
    display_order integer DEFAULT 0 NOT NULL,
    active boolean DEFAULT true NOT NULL,
    published boolean DEFAULT true NOT NULL,
    deleted boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT chk_builder_improvement_action_evidence CHECK (((evidence_status)::text = ANY ((ARRAY['EVIDENCE_PENDING'::character varying, 'BUILDER_SUBMITTED'::character varying, 'SFS_REVIEWED'::character varying, 'SFS_VERIFIED'::character varying, 'PUBLIC_RECORD_VERIFIED'::character varying, 'NOT_APPLICABLE'::character varying])::text[]))),
    CONSTRAINT chk_builder_improvement_action_impact CHECK (((impact_level)::text = ANY ((ARRAY['LOW'::character varying, 'MEDIUM'::character varying, 'HIGH'::character varying])::text[]))),
    CONSTRAINT chk_builder_improvement_action_type CHECK (((action_type)::text = ANY ((ARRAY['TIMELINE_OPTIMIZATION'::character varying, 'CUSTOMER_SUPPORT_HUB'::character varying, 'DOCUMENTATION_TRANSPARENCY'::character varying, 'PROJECT_MONITORING'::character varying, 'CONTRACTOR_CHANGED'::character varying, 'SITE_MANPOWER_INCREASED'::character varying, 'APPROVAL_CLEARED'::character varying, 'MONTHLY_BUYER_UPDATES'::character varying, 'PENDING_COMPLAINTS_REDUCED'::character varying, 'SITE_PROGRESS_TRACKING'::character varying, 'COMMUNITY_CONTRIBUTION'::character varying, 'OTHER'::character varying])::text[])))
);


--
-- Name: builder_improvement_action_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.builder_improvement_action_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: builder_improvement_action_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.builder_improvement_action_id_seq OWNED BY public.builder_improvement_action.id;


--
-- Name: builder_improvement_issue; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.builder_improvement_issue (
    id bigint NOT NULL,
    profile_id bigint NOT NULL,
    issue_type character varying(60) NOT NULL,
    title character varying(180) NOT NULL,
    description text,
    status character varying(40) DEFAULT 'PENDING'::character varying NOT NULL,
    reported_at timestamp with time zone,
    resolved_at timestamp with time zone,
    target_resolution_date date,
    resolution_summary text,
    evidence_status character varying(40) DEFAULT 'EVIDENCE_PENDING'::character varying NOT NULL,
    display_order integer DEFAULT 0 NOT NULL,
    active boolean DEFAULT true NOT NULL,
    published boolean DEFAULT true NOT NULL,
    deleted boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    delay_reason text,
    CONSTRAINT chk_builder_improvement_issue_evidence CHECK (((evidence_status)::text = ANY ((ARRAY['EVIDENCE_PENDING'::character varying, 'BUILDER_SUBMITTED'::character varying, 'SFS_REVIEWED'::character varying, 'SFS_VERIFIED'::character varying, 'PUBLIC_RECORD_VERIFIED'::character varying, 'NOT_APPLICABLE'::character varying])::text[]))),
    CONSTRAINT chk_builder_improvement_issue_status CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'IN_PROGRESS'::character varying, 'RESOLVED'::character varying, 'REOPENED'::character varying, 'CANCELLED'::character varying])::text[]))),
    CONSTRAINT chk_builder_improvement_issue_type CHECK (((issue_type)::text = ANY ((ARRAY['BUYER_COMPLAINT'::character varying, 'HANDOVER_DEFECT'::character varying, 'DOCUMENTATION_GAP'::character varying, 'AMENITY_DELAY'::character varying, 'CONSTRUCTION_IMPACT'::character varying, 'MAINTENANCE_SUPPORT'::character varying, 'COMMUNICATION_DELAY'::character varying, 'OTHER'::character varying])::text[])))
);


--
-- Name: builder_improvement_issue_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.builder_improvement_issue_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: builder_improvement_issue_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.builder_improvement_issue_id_seq OWNED BY public.builder_improvement_issue.id;


--
-- Name: builder_improvement_profile; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.builder_improvement_profile (
    id bigint NOT NULL,
    builder_id bigint NOT NULL,
    project_id bigint,
    badge_text character varying(120) DEFAULT 'Commitment 2026'::character varying NOT NULL,
    hero_title text,
    hero_subtitle text,
    hero_image_url text,
    verified_by_text character varying(180) DEFAULT 'Verified by SFS Trust Engine'::character varying,
    summary text,
    builder_response_title character varying(180),
    builder_response_person_name character varying(150),
    builder_response_person_designation character varying(150),
    builder_response_text text,
    overall_status character varying(40) DEFAULT 'UNDER_REVIEW'::character varying NOT NULL,
    evidence_status character varying(40) DEFAULT 'EVIDENCE_PENDING'::character varying NOT NULL,
    review_status character varying(40) DEFAULT 'DRAFT'::character varying NOT NULL,
    active boolean DEFAULT true NOT NULL,
    published boolean DEFAULT false NOT NULL,
    deleted boolean DEFAULT false NOT NULL,
    display_order integer DEFAULT 0 NOT NULL,
    created_by_dashboard_user_id bigint,
    updated_by_dashboard_user_id bigint,
    submitted_by_dashboard_user_id bigint,
    reviewed_by_dashboard_user_id bigint,
    published_by_dashboard_user_id bigint,
    submitted_at timestamp with time zone,
    reviewed_at timestamp with time zone,
    published_at timestamp with time zone,
    last_reviewed_at timestamp with time zone,
    review_remarks text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT chk_builder_improvement_evidence_status CHECK (((evidence_status)::text = ANY ((ARRAY['EVIDENCE_PENDING'::character varying, 'BUILDER_SUBMITTED'::character varying, 'SFS_REVIEWED'::character varying, 'SFS_VERIFIED'::character varying, 'PUBLIC_RECORD_VERIFIED'::character varying, 'NOT_APPLICABLE'::character varying])::text[]))),
    CONSTRAINT chk_builder_improvement_overall_status CHECK (((overall_status)::text = ANY ((ARRAY['STRONG_IMPROVEMENT'::character varying, 'IMPROVING'::character varying, 'STABLE'::character varying, 'UNDER_REVIEW'::character varying, 'PAUSED'::character varying])::text[]))),
    CONSTRAINT chk_builder_improvement_review_status CHECK (((review_status)::text = ANY ((ARRAY['DRAFT'::character varying, 'PENDING_REVIEW'::character varying, 'RECHECK'::character varying, 'APPROVED'::character varying, 'REJECTED'::character varying])::text[])))
);


--
-- Name: builder_improvement_profile_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.builder_improvement_profile_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: builder_improvement_profile_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.builder_improvement_profile_id_seq OWNED BY public.builder_improvement_profile.id;


--
-- Name: builder_improvement_timeline; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.builder_improvement_timeline (
    id bigint NOT NULL,
    profile_id bigint NOT NULL,
    timeline_type character varying(60) DEFAULT 'OTHER'::character varying NOT NULL,
    event_date date,
    event_month_label character varying(40),
    title character varying(180) NOT NULL,
    short_description text,
    problem_observed text,
    action_taken text,
    current_status text,
    evidence_status character varying(40) DEFAULT 'EVIDENCE_PENDING'::character varying NOT NULL,
    display_order integer DEFAULT 0 NOT NULL,
    active boolean DEFAULT true NOT NULL,
    published boolean DEFAULT true NOT NULL,
    deleted boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    icon_key character varying(80),
    CONSTRAINT chk_builder_improvement_timeline_evidence CHECK (((evidence_status)::text = ANY ((ARRAY['EVIDENCE_PENDING'::character varying, 'BUILDER_SUBMITTED'::character varying, 'SFS_REVIEWED'::character varying, 'SFS_VERIFIED'::character varying, 'PUBLIC_RECORD_VERIFIED'::character varying, 'NOT_APPLICABLE'::character varying])::text[]))),
    CONSTRAINT chk_builder_improvement_timeline_type CHECK (((timeline_type)::text = ANY ((ARRAY['DELAY_OBSERVATION'::character varying, 'AUDIT_INITIATED'::character varying, 'TRANSPARENCY_PROTOCOL'::character varying, 'MILESTONE_VERIFICATION'::character varying, 'DELIVERY_CERTIFICATION'::character varying, 'CUSTOMER_SUPPORT_UPDATE'::character varying, 'DOCUMENTATION_UPDATE'::character varying, 'ISSUE_RESOLUTION'::character varying, 'CONSTRUCTION_PROGRESS'::character varying, 'TOWER_COMPLETION'::character varying, 'FLOOR_COMPLETION'::character varying, 'AMENITY_PROGRESS'::character varying, 'APPROVAL_PROGRESS'::character varying, 'QUALITY_CHECK'::character varying, 'HANDOVER_PROGRESS'::character varying, 'POST_DELIVERY_SUPPORT'::character varying, 'COMMUNITY_CONTRIBUTION'::character varying, 'OTHER'::character varying])::text[])))
);


--
-- Name: builder_improvement_timeline_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.builder_improvement_timeline_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: builder_improvement_timeline_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.builder_improvement_timeline_id_seq OWNED BY public.builder_improvement_timeline.id;


--
-- Name: business; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.business (
    id bigint NOT NULL,
    name character varying(255) NOT NULL,
    primary_phone character varying(20),
    secondary_phone character varying(20),
    email character varying(255),
    website character varying(255),
    address_line1 character varying(255),
    address_line2 character varying(255),
    landmark character varying(255),
    pincode character varying(10),
    city_id bigint NOT NULL,
    latitude double precision,
    longitude double precision,
    category_id bigint NOT NULL,
    avg_rating double precision DEFAULT 0 NOT NULL,
    total_ratings integer DEFAULT 0 NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    whatsapp_phone character varying(20),
    open_time time without time zone,
    close_time time without time zone,
    established_year integer,
    highlight_badge character varying(150),
    is_top_rated boolean DEFAULT false NOT NULL,
    is_near_and_fast boolean DEFAULT false NOT NULL,
    sponsored boolean DEFAULT false NOT NULL,
    sponsored_priority integer DEFAULT 0 NOT NULL,
    sponsored_start timestamp with time zone,
    sponsored_end timestamp with time zone,
    owner_user_id bigint
);


--
-- Name: business_event; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.business_event (
    id bigint NOT NULL,
    business_id bigint NOT NULL,
    city_id bigint,
    category_id bigint,
    event_type character varying(50) NOT NULL,
    source character varying(50),
    listing_position integer,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: business_event_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.business_event_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: business_event_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.business_event_id_seq OWNED BY public.business_event.id;


--
-- Name: business_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.business_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: business_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.business_id_seq OWNED BY public.business.id;


--
-- Name: category; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.category (
    id bigint NOT NULL,
    name character varying(150) NOT NULL,
    slug character varying(180) NOT NULL,
    parent_id bigint,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    priority integer DEFAULT 100 NOT NULL,
    active boolean DEFAULT true NOT NULL
);


--
-- Name: category_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.category_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: category_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.category_id_seq OWNED BY public.category.id;


--
-- Name: circle_rate_rule; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.circle_rate_rule (
    id bigint NOT NULL,
    state_name character varying(100) NOT NULL,
    city_name character varying(100) NOT NULL,
    locality_name character varying(150) NOT NULL,
    property_type character varying(40) NOT NULL,
    unit_type character varying(20) NOT NULL,
    formula_type character varying(40) NOT NULL,
    rate_per_unit numeric(18,2) NOT NULL,
    effective_from date NOT NULL,
    effective_to date,
    active boolean DEFAULT true NOT NULL,
    source_note character varying(500),
    CONSTRAINT chk_circle_rate_rule_date_range CHECK (((effective_to IS NULL) OR (effective_to >= effective_from))),
    CONSTRAINT chk_circle_rate_rule_rate_positive CHECK ((rate_per_unit > (0)::numeric))
);


--
-- Name: circle_rate_rule_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.circle_rate_rule_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: circle_rate_rule_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.circle_rate_rule_id_seq OWNED BY public.circle_rate_rule.id;


--
-- Name: city; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.city (
    id bigint NOT NULL,
    name character varying(150) NOT NULL,
    state character varying(150),
    country_code character varying(10) DEFAULT 'IN'::character varying NOT NULL,
    latitude double precision,
    longitude double precision,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    slug character varying(180) NOT NULL,
    cover_image_url text,
    active boolean DEFAULT true NOT NULL,
    homepage_featured boolean DEFAULT false NOT NULL,
    display_order integer DEFAULT 0 NOT NULL,
    growth_percent double precision
);


--
-- Name: city_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.city_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: city_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.city_id_seq OWNED BY public.city.id;


--
-- Name: company; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.company (
    id bigint NOT NULL,
    name character varying(150) NOT NULL,
    slug character varying(180) NOT NULL,
    company_type character varying(40) NOT NULL,
    logo_url text,
    cover_image_url text,
    description text,
    specialization_text character varying(255),
    info_line_1 text,
    info_line_2 text,
    city_id bigint,
    address_line text,
    services_offered text,
    phone character varying(20),
    whatsapp character varying(20),
    email character varying(150),
    active boolean DEFAULT true NOT NULL,
    published boolean DEFAULT true NOT NULL,
    priority integer DEFAULT 0 NOT NULL,
    deleted boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: company_award; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.company_award (
    id bigint NOT NULL,
    company_id bigint NOT NULL,
    title character varying(180) NOT NULL,
    subtitle character varying(180),
    description text,
    display_order integer DEFAULT 0 NOT NULL,
    active boolean DEFAULT true NOT NULL,
    deleted boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: company_award_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.company_award_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: company_award_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.company_award_id_seq OWNED BY public.company_award.id;


--
-- Name: company_brand_link; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.company_brand_link (
    id bigint NOT NULL,
    company_id bigint NOT NULL,
    brand_id bigint NOT NULL,
    sort_order integer DEFAULT 0 NOT NULL,
    active boolean DEFAULT true NOT NULL,
    deleted boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone,
    updated_at timestamp with time zone
);


--
-- Name: company_brand_link_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.company_brand_link_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: company_brand_link_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.company_brand_link_id_seq OWNED BY public.company_brand_link.id;


--
-- Name: company_certificate; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.company_certificate (
    id bigint NOT NULL,
    company_id bigint NOT NULL,
    title character varying(180) NOT NULL,
    issuer character varying(180),
    certificate_url text,
    display_order integer DEFAULT 0 NOT NULL,
    active boolean DEFAULT true NOT NULL,
    deleted boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    description text,
    certificate_file_url text,
    year integer,
    verified boolean DEFAULT false NOT NULL,
    public_visible boolean DEFAULT true NOT NULL
);


--
-- Name: company_certificate_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.company_certificate_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: company_certificate_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.company_certificate_id_seq OWNED BY public.company_certificate.id;


--
-- Name: company_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.company_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: company_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.company_id_seq OWNED BY public.company.id;


--
-- Name: company_media; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.company_media (
    id bigint NOT NULL,
    company_id bigint NOT NULL,
    media_url text NOT NULL,
    media_type character varying(20) DEFAULT 'IMAGE'::character varying NOT NULL,
    usage_type character varying(20) NOT NULL,
    title character varying(180),
    alt_text character varying(255),
    sort_order integer DEFAULT 0 NOT NULL,
    public_visible boolean DEFAULT true NOT NULL,
    active boolean DEFAULT true NOT NULL,
    deleted boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT chk_company_media_media_type CHECK (((media_type)::text = 'IMAGE'::text)),
    CONSTRAINT chk_company_media_usage_type CHECK (((usage_type)::text = ANY ((ARRAY['HERO'::character varying, 'GALLERY'::character varying, 'CARD'::character varying])::text[])))
);


--
-- Name: company_media_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.company_media_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: company_media_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.company_media_id_seq OWNED BY public.company_media.id;


--
-- Name: company_pricing_plan; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.company_pricing_plan (
    id bigint NOT NULL,
    company_id bigint NOT NULL,
    pricing_type character varying(20) NOT NULL,
    plan_name character varying(100) NOT NULL,
    price_amount numeric(12,2),
    currency character varying(10) DEFAULT 'INR'::character varying NOT NULL,
    billing_unit character varying(30),
    description text,
    features_json jsonb DEFAULT '[]'::jsonb NOT NULL,
    sort_order integer DEFAULT 0 NOT NULL,
    public_visible boolean DEFAULT true NOT NULL,
    active boolean DEFAULT true NOT NULL,
    deleted boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT chk_company_pricing_plan_price CHECK (((price_amount IS NULL) OR (price_amount >= (0)::numeric))),
    CONSTRAINT chk_company_pricing_plan_type CHECK (((pricing_type)::text = ANY ((ARRAY['SUBSCRIPTION'::character varying, 'PROJECT_BASED'::character varying])::text[])))
);


--
-- Name: company_pricing_plan_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.company_pricing_plan_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: company_pricing_plan_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.company_pricing_plan_id_seq OWNED BY public.company_pricing_plan.id;


--
-- Name: company_project; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.company_project (
    id bigint NOT NULL,
    company_id bigint NOT NULL,
    name character varying(180) NOT NULL,
    slug character varying(220),
    description text,
    city_id bigint,
    address_line text,
    cover_media_url text,
    cover_media_type character varying(20),
    active boolean DEFAULT true NOT NULL,
    published boolean DEFAULT false NOT NULL,
    priority integer DEFAULT 0 NOT NULL,
    deleted boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone,
    updated_at timestamp with time zone,
    client_name character varying(180),
    project_area character varying(100),
    detail3 character varying(180),
    tags text
);


--
-- Name: company_project_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.company_project_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: company_project_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.company_project_id_seq OWNED BY public.company_project.id;


--
-- Name: company_stat; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.company_stat (
    id bigint NOT NULL,
    company_id bigint NOT NULL,
    label character varying(120) NOT NULL,
    value character varying(180) NOT NULL,
    display_order integer DEFAULT 0 NOT NULL,
    active boolean DEFAULT true NOT NULL,
    deleted boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    icon_key character varying(60),
    public_visible boolean DEFAULT true NOT NULL
);


--
-- Name: company_stat_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.company_stat_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: company_stat_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.company_stat_id_seq OWNED BY public.company_stat.id;


--
-- Name: content_version; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.content_version (
    key character varying(50) NOT NULL,
    version bigint NOT NULL
);


--
-- Name: dashboard_action_audit; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.dashboard_action_audit (
    id bigint NOT NULL,
    dashboard_user_id bigint,
    dashboard_user_name character varying(150),
    dashboard_user_role character varying(50),
    action character varying(80) NOT NULL,
    entity_type character varying(80) NOT NULL,
    entity_id bigint NOT NULL,
    project_id bigint,
    summary text,
    ip_address character varying(100),
    user_agent text,
    created_at timestamp with time zone NOT NULL
);


--
-- Name: dashboard_action_audit_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.dashboard_action_audit_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: dashboard_action_audit_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.dashboard_action_audit_id_seq OWNED BY public.dashboard_action_audit.id;


--
-- Name: dashboard_content_review_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.dashboard_content_review_history (
    id bigint NOT NULL,
    entity_type character varying(80) NOT NULL,
    entity_id bigint NOT NULL,
    old_status character varying(50),
    new_status character varying(50),
    action_type character varying(80) NOT NULL,
    remarks text,
    action_by bigint,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT chk_dashboard_review_history_action_type CHECK (((action_type)::text = ANY ((ARRAY['CREATED'::character varying, 'UPDATED'::character varying, 'SUBMITTED_FOR_REVIEW'::character varying, 'FIELD_MARKED_RECHECK'::character varying, 'FIELD_MARKED_WRONG'::character varying, 'FIELD_MARKED_FIXED'::character varying, 'APPROVED'::character varying, 'APPROVAL_ROLLED_BACK'::character varying, 'REJECTED'::character varying, 'REOPENED'::character varying, 'MOVED_TO_RECHECK'::character varying, 'PUBLISHED'::character varying, 'UNPUBLISHED'::character varying, 'ARCHIVED'::character varying, 'RESTORED'::character varying, 'FIELD_ISSUE_DELETED'::character varying])::text[]))),
    CONSTRAINT chk_dashboard_review_history_entity_type CHECK (((entity_type)::text = ANY ((ARRAY['PROJECT'::character varying, 'PROJECT_MEDIA'::character varying, 'PROJECT_FLOOR_PLAN'::character varying, 'PROJECT_HIGHLIGHT'::character varying, 'PROJECT_CONNECTIVITY'::character varying, 'PROJECT_CONNECTIVITY_PLACE'::character varying, 'PROJECT_METER'::character varying, 'PROJECT_METER_SNAPSHOT'::character varying, 'BUILDER'::character varying, 'BUILDER_IMPROVEMENT_PROFILE'::character varying, 'BUILDER_IMPROVEMENT_ACTION'::character varying, 'BUILDER_IMPROVEMENT_ISSUE'::character varying, 'BUILDER_AFTER_SALES_UPGRADE'::character varying, 'BUILDER_IMPROVEMENT_TIMELINE'::character varying, 'COMPANY'::character varying, 'PROMO_BANNER'::character varying, 'APP_CONTENT'::character varying, 'CITY'::character varying, 'CATEGORY'::character varying])::text[])))
);


--
-- Name: dashboard_content_review_history_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.dashboard_content_review_history_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: dashboard_content_review_history_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.dashboard_content_review_history_id_seq OWNED BY public.dashboard_content_review_history.id;


--
-- Name: dashboard_field_help; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.dashboard_field_help (
    id bigint NOT NULL,
    module character varying(100) NOT NULL,
    field_key character varying(150) NOT NULL,
    field_label character varying(180) NOT NULL,
    short_help text NOT NULL,
    detailed_help text,
    why_needed text,
    source_hint text,
    example_value text,
    validation_hint text,
    active boolean DEFAULT true NOT NULL,
    display_order integer DEFAULT 0 NOT NULL,
    created_by_dashboard_user_id bigint,
    updated_by_dashboard_user_id bigint,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: dashboard_field_help_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.dashboard_field_help_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: dashboard_field_help_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.dashboard_field_help_id_seq OWNED BY public.dashboard_field_help.id;


--
-- Name: dashboard_field_review_issues; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.dashboard_field_review_issues (
    id bigint NOT NULL,
    entity_type character varying(80) NOT NULL,
    entity_id bigint NOT NULL,
    field_key character varying(120) NOT NULL,
    field_label character varying(180),
    status character varying(40) NOT NULL,
    remarks text,
    marked_by bigint NOT NULL,
    fixed_by bigint,
    marked_at timestamp with time zone DEFAULT now() NOT NULL,
    fixed_at timestamp with time zone,
    active boolean DEFAULT true NOT NULL,
    CONSTRAINT chk_dashboard_field_review_entity_type CHECK (((entity_type)::text = ANY ((ARRAY['PROJECT'::character varying, 'PROJECT_MEDIA'::character varying, 'PROJECT_FLOOR_PLAN'::character varying, 'PROJECT_HIGHLIGHT'::character varying, 'PROJECT_CONNECTIVITY'::character varying, 'PROJECT_CONNECTIVITY_PLACE'::character varying, 'PROJECT_METER'::character varying, 'PROJECT_METER_SNAPSHOT'::character varying, 'BUILDER'::character varying, 'BUILDER_IMPROVEMENT_PROFILE'::character varying, 'BUILDER_IMPROVEMENT_ACTION'::character varying, 'BUILDER_IMPROVEMENT_ISSUE'::character varying, 'BUILDER_AFTER_SALES_UPGRADE'::character varying, 'BUILDER_IMPROVEMENT_TIMELINE'::character varying, 'COMPANY'::character varying, 'PROMO_BANNER'::character varying, 'APP_CONTENT'::character varying, 'CITY'::character varying, 'CATEGORY'::character varying])::text[]))),
    CONSTRAINT chk_dashboard_field_review_status CHECK (((status)::text = ANY ((ARRAY['RECHECK'::character varying, 'WRONG'::character varying, 'FIXED'::character varying])::text[])))
);


--
-- Name: dashboard_field_review_issues_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.dashboard_field_review_issues_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: dashboard_field_review_issues_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.dashboard_field_review_issues_id_seq OWNED BY public.dashboard_field_review_issues.id;


--
-- Name: dashboard_login_audit; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.dashboard_login_audit (
    id bigint NOT NULL,
    dashboard_user_id bigint,
    email character varying(180),
    success boolean NOT NULL,
    failure_reason character varying(255),
    ip_address character varying(100),
    user_agent text,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: dashboard_login_audit_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.dashboard_login_audit_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: dashboard_login_audit_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.dashboard_login_audit_id_seq OWNED BY public.dashboard_login_audit.id;


--
-- Name: dashboard_refresh_tokens; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.dashboard_refresh_tokens (
    id bigint NOT NULL,
    dashboard_user_id bigint NOT NULL,
    token_hash character varying(255) NOT NULL,
    revoked boolean DEFAULT false NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    revoked_at timestamp with time zone,
    replaced_by_token_hash character varying(255)
);


--
-- Name: dashboard_refresh_tokens_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.dashboard_refresh_tokens_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: dashboard_refresh_tokens_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.dashboard_refresh_tokens_id_seq OWNED BY public.dashboard_refresh_tokens.id;


--
-- Name: dashboard_scrape_candidate; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.dashboard_scrape_candidate (
    id bigint NOT NULL,
    source_code character varying(50) NOT NULL,
    rera_number character varying(150) NOT NULL,
    found boolean DEFAULT false NOT NULL,
    captcha_detected boolean DEFAULT false NOT NULL,
    source_search_url text,
    source_detail_url text,
    final_url text,
    page_title character varying(300),
    raw_html_path text,
    screenshot_path text,
    confidence_score integer,
    confidence_status character varying(50),
    total_expected_fields integer,
    found_fields integer,
    missing_fields integer,
    status character varying(50) NOT NULL,
    linked_builder_id bigint,
    linked_project_id bigint,
    applied_project_id bigint,
    created_by_dashboard_user_id bigint,
    applied_by_dashboard_user_id bigint,
    applied_at timestamp with time zone,
    remarks text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    captcha_session_id character varying(100)
);


--
-- Name: dashboard_scrape_candidate_builder; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.dashboard_scrape_candidate_builder (
    id bigint NOT NULL,
    candidate_id bigint NOT NULL,
    name character varying(255),
    phone character varying(80),
    email character varying(180),
    address_line text,
    city_name character varying(150),
    website text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: dashboard_scrape_candidate_builder_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.dashboard_scrape_candidate_builder_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: dashboard_scrape_candidate_builder_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.dashboard_scrape_candidate_builder_id_seq OWNED BY public.dashboard_scrape_candidate_builder.id;


--
-- Name: dashboard_scrape_candidate_compliance_item; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.dashboard_scrape_candidate_compliance_item (
    id bigint NOT NULL,
    candidate_id bigint NOT NULL,
    item_group character varying(80),
    item_key character varying(120),
    item_label character varying(180),
    status character varying(80),
    value_text text,
    document_url text,
    remarks text,
    display_order integer DEFAULT 0 NOT NULL,
    verified boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: dashboard_scrape_candidate_compliance_item_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.dashboard_scrape_candidate_compliance_item_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: dashboard_scrape_candidate_compliance_item_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.dashboard_scrape_candidate_compliance_item_id_seq OWNED BY public.dashboard_scrape_candidate_compliance_item.id;


--
-- Name: dashboard_scrape_candidate_cost_breakdown; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.dashboard_scrape_candidate_cost_breakdown (
    id bigint NOT NULL,
    candidate_id bigint NOT NULL,
    land_cost bigint,
    construction_cost bigint,
    infrastructure_cost bigint,
    other_cost bigint,
    total_cost bigint,
    source_label character varying(255),
    remarks text,
    verified boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: dashboard_scrape_candidate_cost_breakdown_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.dashboard_scrape_candidate_cost_breakdown_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: dashboard_scrape_candidate_cost_breakdown_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.dashboard_scrape_candidate_cost_breakdown_id_seq OWNED BY public.dashboard_scrape_candidate_cost_breakdown.id;


--
-- Name: dashboard_scrape_candidate_document; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.dashboard_scrape_candidate_document (
    id bigint NOT NULL,
    candidate_id bigint NOT NULL,
    document_type character varying(80) NOT NULL,
    title character varying(255),
    document_url text,
    source_label character varying(255),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: dashboard_scrape_candidate_document_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.dashboard_scrape_candidate_document_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: dashboard_scrape_candidate_document_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.dashboard_scrape_candidate_document_id_seq OWNED BY public.dashboard_scrape_candidate_document.id;


--
-- Name: dashboard_scrape_candidate_field_result; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.dashboard_scrape_candidate_field_result (
    id bigint NOT NULL,
    candidate_id bigint NOT NULL,
    section character varying(100),
    field_key character varying(150),
    field_label character varying(200),
    found boolean DEFAULT false NOT NULL,
    value_text text,
    source_label character varying(255),
    confidence integer,
    reason text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: dashboard_scrape_candidate_field_result_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.dashboard_scrape_candidate_field_result_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: dashboard_scrape_candidate_field_result_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.dashboard_scrape_candidate_field_result_id_seq OWNED BY public.dashboard_scrape_candidate_field_result.id;


--
-- Name: dashboard_scrape_candidate_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.dashboard_scrape_candidate_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: dashboard_scrape_candidate_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.dashboard_scrape_candidate_id_seq OWNED BY public.dashboard_scrape_candidate.id;


--
-- Name: dashboard_scrape_candidate_land_utilization; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.dashboard_scrape_candidate_land_utilization (
    id bigint NOT NULL,
    candidate_id bigint NOT NULL,
    total_land_area_sqm numeric(14,2),
    residential_area_sqm numeric(14,2),
    commercial_area_sqm numeric(14,2),
    parks_area_sqm numeric(14,2),
    open_area_sqm numeric(14,2),
    parking_area_sqm numeric(14,2),
    utility_area_sqm numeric(14,2),
    source_label character varying(255),
    remarks text,
    verified boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: dashboard_scrape_candidate_land_utilization_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.dashboard_scrape_candidate_land_utilization_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: dashboard_scrape_candidate_land_utilization_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.dashboard_scrape_candidate_land_utilization_id_seq OWNED BY public.dashboard_scrape_candidate_land_utilization.id;


--
-- Name: dashboard_scrape_candidate_project; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.dashboard_scrape_candidate_project (
    id bigint NOT NULL,
    candidate_id bigint NOT NULL,
    name character varying(255),
    description text,
    city_name character varying(150),
    address_line text,
    latitude numeric(10,7),
    longitude numeric(10,7),
    price_min bigint,
    price_max bigint,
    possession_date date,
    rera_number character varying(150),
    project_status character varying(80),
    property_types text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: dashboard_scrape_candidate_project_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.dashboard_scrape_candidate_project_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: dashboard_scrape_candidate_project_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.dashboard_scrape_candidate_project_id_seq OWNED BY public.dashboard_scrape_candidate_project.id;


--
-- Name: dashboard_scrape_candidate_raw_value; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.dashboard_scrape_candidate_raw_value (
    id bigint NOT NULL,
    candidate_id bigint NOT NULL,
    raw_key text NOT NULL,
    raw_value text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: dashboard_scrape_candidate_raw_value_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.dashboard_scrape_candidate_raw_value_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: dashboard_scrape_candidate_raw_value_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.dashboard_scrape_candidate_raw_value_id_seq OWNED BY public.dashboard_scrape_candidate_raw_value.id;


--
-- Name: dashboard_users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.dashboard_users (
    id bigint NOT NULL,
    name character varying(150) NOT NULL,
    email character varying(180) NOT NULL,
    password_hash character varying(255) NOT NULL,
    role character varying(50) NOT NULL,
    active boolean DEFAULT true NOT NULL,
    last_login_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT chk_dashboard_users_role CHECK (((role)::text = ANY ((ARRAY['ADMIN'::character varying, 'REVIEWER'::character varying, 'DATA_ENTRY'::character varying])::text[])))
);


--
-- Name: dashboard_users_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.dashboard_users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: dashboard_users_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.dashboard_users_id_seq OWNED BY public.dashboard_users.id;


--
-- Name: distributor; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.distributor (
    id bigint NOT NULL,
    name character varying(200) NOT NULL,
    phone character varying(30),
    whatsapp character varying(30),
    email character varying(200),
    address_line1 character varying(255),
    address_line2 character varying(255),
    pincode character varying(12),
    city_id bigint,
    latitude double precision,
    longitude double precision,
    active boolean DEFAULT true NOT NULL,
    deleted boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    logo_url text
);


--
-- Name: distributor_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.distributor_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: distributor_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.distributor_id_seq OWNED BY public.distributor.id;


--
-- Name: distributor_media; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.distributor_media (
    id bigint NOT NULL,
    distributor_id bigint NOT NULL,
    media_type character varying(20) NOT NULL,
    url text NOT NULL,
    caption character varying(255),
    sort_order integer DEFAULT 0 NOT NULL,
    active boolean DEFAULT true NOT NULL,
    deleted boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: distributor_media_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.distributor_media_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: distributor_media_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.distributor_media_id_seq OWNED BY public.distributor_media.id;


--
-- Name: favorites; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.favorites (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    business_id bigint NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: favorites_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.favorites_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: favorites_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.favorites_id_seq OWNED BY public.favorites.id;


--
-- Name: featured_carousel_config; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.featured_carousel_config (
    id bigint NOT NULL,
    city_id bigint,
    category_id bigint NOT NULL,
    variant character varying(20) NOT NULL,
    "position" integer NOT NULL,
    title character varying(180) NOT NULL,
    subtitle character varying(220),
    image_url text NOT NULL,
    logo_url text,
    entity_type character varying(20),
    entity_id bigint,
    target_url text,
    active boolean DEFAULT true NOT NULL,
    priority integer DEFAULT 0 NOT NULL,
    start_at timestamp with time zone,
    end_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    enabled boolean DEFAULT true NOT NULL,
    sort_order integer DEFAULT 0 NOT NULL,
    CONSTRAINT chk_featured_carousel_position CHECK ((("position" >= 1) AND ("position" <= 3))),
    CONSTRAINT chk_featured_carousel_variant CHECK (((variant)::text = ANY ((ARRAY['TALL'::character varying, 'SMALL_TOP'::character varying, 'SMALL_BOTTOM'::character varying])::text[])))
);


--
-- Name: featured_carousel_config_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.featured_carousel_config_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: featured_carousel_config_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.featured_carousel_config_id_seq OWNED BY public.featured_carousel_config.id;


--
-- Name: feed_section_config; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.feed_section_config (
    id bigint NOT NULL,
    screen character varying(30) NOT NULL,
    category_id bigint,
    entity_id bigint,
    city_id bigint,
    section_type character varying(80) NOT NULL,
    title character varying(150),
    sort_order integer DEFAULT 0 NOT NULL,
    max_items integer DEFAULT 10 NOT NULL,
    enabled boolean DEFAULT true NOT NULL,
    param1 jsonb,
    param2 character varying(200),
    param3 character varying(200),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: feed_section_config_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.feed_section_config_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: feed_section_config_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.feed_section_config_id_seq OWNED BY public.feed_section_config.id;


--
-- Name: feed_section_item; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.feed_section_item (
    id bigint NOT NULL,
    config_id bigint NOT NULL,
    item_type character varying(30) NOT NULL,
    ref_id bigint NOT NULL,
    title character varying(150),
    subtitle character varying(255),
    image_url text,
    logo_url text,
    group_key character varying(50),
    sort_order integer DEFAULT 0 NOT NULL,
    active boolean DEFAULT true NOT NULL,
    deleted boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: feed_section_item_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.feed_section_item_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: feed_section_item_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.feed_section_item_id_seq OWNED BY public.feed_section_item.id;


--
-- Name: guest_sessions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.guest_sessions (
    id bigint NOT NULL,
    installation_id character varying(120) NOT NULL,
    device_model character varying(100),
    device_name character varying(100),
    platform character varying(20),
    os_version character varying(30),
    app_version character varying(30),
    active boolean NOT NULL,
    linked_user_id bigint,
    linked_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    last_seen_at timestamp with time zone
);


--
-- Name: guest_sessions_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.guest_sessions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: guest_sessions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.guest_sessions_id_seq OWNED BY public.guest_sessions.id;


--
-- Name: home_project_analytics; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.home_project_analytics (
    id bigint NOT NULL,
    category_id bigint NOT NULL,
    builder_id bigint,
    deleted boolean DEFAULT false NOT NULL,
    priority integer DEFAULT 1 NOT NULL,
    caption character varying(255)
);


--
-- Name: home_project_analytics_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.home_project_analytics_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: home_project_analytics_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.home_project_analytics_id_seq OWNED BY public.home_project_analytics.id;


--
-- Name: home_section_config; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.home_section_config (
    id bigint NOT NULL,
    home_category_id bigint NOT NULL,
    section_type character varying(50) NOT NULL,
    title character varying(150),
    enabled boolean DEFAULT true NOT NULL,
    sort_order integer DEFAULT 0 NOT NULL,
    max_items integer DEFAULT 10 NOT NULL,
    param1 character varying(200),
    param2 character varying(200),
    param3 character varying(200),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    subtitle character varying(255)
);


--
-- Name: home_section_config_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.home_section_config_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: home_section_config_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.home_section_config_id_seq OWNED BY public.home_section_config.id;


--
-- Name: home_section_item; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.home_section_item (
    id bigint NOT NULL,
    home_category_id bigint NOT NULL,
    section_type character varying(50) NOT NULL,
    item_type character varying(30) NOT NULL,
    ref_id bigint NOT NULL,
    title character varying(150),
    subtitle character varying(255),
    image_url text,
    sort_order integer DEFAULT 0 NOT NULL,
    active boolean DEFAULT true NOT NULL,
    deleted boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    group_key character varying(50),
    config_id bigint
);


--
-- Name: home_section_item_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.home_section_item_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: home_section_item_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.home_section_item_id_seq OWNED BY public.home_section_item.id;


--
-- Name: instagram_reel; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.instagram_reel (
    id bigint NOT NULL,
    instagram_media_id character varying(100),
    caption text,
    title character varying(180),
    instagram_url character varying(500) NOT NULL,
    thumbnail_url character varying(1000),
    preview_video_url character varying(1000),
    media_type character varying(50),
    media_product_type character varying(50),
    published_at timestamp with time zone,
    view_count bigint DEFAULT 0 NOT NULL,
    like_count bigint DEFAULT 0 NOT NULL,
    comment_count bigint DEFAULT 0 NOT NULL,
    share_count bigint DEFAULT 0 NOT NULL,
    save_count bigint DEFAULT 0 NOT NULL,
    trending_score numeric(14,2) DEFAULT 0 NOT NULL,
    category_override character varying(50),
    display_order integer DEFAULT 0 NOT NULL,
    active boolean DEFAULT true NOT NULL,
    synced_from_meta boolean DEFAULT true NOT NULL,
    last_synced_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    deleted boolean DEFAULT false NOT NULL,
    source_thumbnail_url text,
    cached_thumbnail_url text,
    cached_thumbnail_storage_key character varying(500),
    thumbnail_cached_at timestamp with time zone,
    meta_fetched_at timestamp with time zone,
    last_sync_status character varying(40),
    last_sync_error text,
    CONSTRAINT chk_instagram_reel_category CHECK (((category_override IS NULL) OR ((category_override)::text = ANY ((ARRAY['LATEST'::character varying, 'TRENDING'::character varying, 'INTERVIEWS'::character varying, 'MOST_VIEWED'::character varying, 'MANUAL'::character varying])::text[]))))
);


--
-- Name: instagram_reel_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.instagram_reel_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: instagram_reel_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.instagram_reel_id_seq OWNED BY public.instagram_reel.id;


--
-- Name: interior_cost_addon_rule; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.interior_cost_addon_rule (
    id bigint NOT NULL,
    company_id bigint NOT NULL,
    city_name character varying(100) NOT NULL,
    package_type character varying(40) NOT NULL,
    addon_type character varying(40) NOT NULL,
    unit_price numeric(18,2) NOT NULL,
    active boolean DEFAULT true NOT NULL,
    effective_from date NOT NULL,
    effective_to date,
    source_note character varying(500),
    CONSTRAINT chk_interior_cost_addon_rule_date_range CHECK (((effective_to IS NULL) OR (effective_to >= effective_from))),
    CONSTRAINT chk_interior_cost_addon_rule_unit_price_non_negative CHECK ((unit_price >= (0)::numeric))
);


--
-- Name: interior_cost_addon_rule_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.interior_cost_addon_rule_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: interior_cost_addon_rule_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.interior_cost_addon_rule_id_seq OWNED BY public.interior_cost_addon_rule.id;


--
-- Name: interior_cost_rule; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.interior_cost_rule (
    id bigint NOT NULL,
    company_id bigint NOT NULL,
    city_name character varying(100) NOT NULL,
    property_type character varying(40) NOT NULL,
    area_unit character varying(20) NOT NULL,
    bhk_type character varying(40) NOT NULL,
    package_type character varying(40) NOT NULL,
    scope_type character varying(40) NOT NULL,
    min_area numeric(18,2) NOT NULL,
    max_area numeric(18,2) NOT NULL,
    base_rate_per_unit numeric(18,2) NOT NULL,
    minimum_project_cost numeric(18,2) NOT NULL,
    contingency_percent numeric(8,4) NOT NULL,
    tax_percent numeric(8,4) NOT NULL,
    active boolean DEFAULT true NOT NULL,
    effective_from date NOT NULL,
    effective_to date,
    source_note character varying(500),
    CONSTRAINT chk_interior_cost_rule_area_range CHECK ((max_area >= min_area)),
    CONSTRAINT chk_interior_cost_rule_base_rate_positive CHECK ((base_rate_per_unit > (0)::numeric)),
    CONSTRAINT chk_interior_cost_rule_contingency_max CHECK ((contingency_percent <= (100)::numeric)),
    CONSTRAINT chk_interior_cost_rule_contingency_non_negative CHECK ((contingency_percent >= (0)::numeric)),
    CONSTRAINT chk_interior_cost_rule_date_range CHECK (((effective_to IS NULL) OR (effective_to >= effective_from))),
    CONSTRAINT chk_interior_cost_rule_max_area_positive CHECK ((max_area > (0)::numeric)),
    CONSTRAINT chk_interior_cost_rule_min_area_positive CHECK ((min_area > (0)::numeric)),
    CONSTRAINT chk_interior_cost_rule_min_project_non_negative CHECK ((minimum_project_cost >= (0)::numeric)),
    CONSTRAINT chk_interior_cost_rule_tax_max CHECK ((tax_percent <= (100)::numeric)),
    CONSTRAINT chk_interior_cost_rule_tax_non_negative CHECK ((tax_percent >= (0)::numeric))
);


--
-- Name: interior_cost_rule_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.interior_cost_rule_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: interior_cost_rule_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.interior_cost_rule_id_seq OWNED BY public.interior_cost_rule.id;


--
-- Name: login_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.login_history (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    login_time timestamp with time zone DEFAULT now(),
    device_id text,
    fcm_token text,
    ip_address text,
    user_agent text,
    login_type text NOT NULL,
    success boolean DEFAULT true NOT NULL
);


--
-- Name: login_history_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.login_history_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: login_history_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.login_history_id_seq OWNED BY public.login_history.id;


--
-- Name: otp_request_tracker; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.otp_request_tracker (
    id bigint NOT NULL,
    phone_number character varying(20) NOT NULL,
    last_sent_at timestamp with time zone,
    cooldown_until timestamp with time zone,
    send_count_in_window integer DEFAULT 0 NOT NULL,
    send_window_start timestamp with time zone,
    failed_verify_count_in_window integer DEFAULT 0 NOT NULL,
    verify_window_start timestamp with time zone,
    blocked_until timestamp with time zone,
    last_verified_at timestamp with time zone
);


--
-- Name: otp_request_tracker_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.otp_request_tracker_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: otp_request_tracker_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.otp_request_tracker_id_seq OWNED BY public.otp_request_tracker.id;


--
-- Name: otps; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.otps (
    id bigint NOT NULL,
    code character varying(32) NOT NULL,
    created_at timestamp without time zone NOT NULL,
    expires_at timestamp without time zone NOT NULL,
    verified boolean DEFAULT false NOT NULL,
    type character varying(40) NOT NULL,
    user_id bigint NOT NULL
);


--
-- Name: TABLE otps; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.otps IS 'Legacy/local OTP table. Current production OTP flow uses Twilio Verify for generation, expiry, invalidation, and reuse prevention.';


--
-- Name: otps_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.otps_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: otps_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.otps_id_seq OWNED BY public.otps.id;


--
-- Name: project; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.project (
    id bigint NOT NULL,
    builder_id bigint NOT NULL,
    name character varying(180) NOT NULL,
    slug character varying(220),
    description text,
    city_id bigint,
    address_line text,
    latitude double precision,
    longitude double precision,
    price_min bigint,
    price_max bigint,
    monthly_emi_min bigint,
    monthly_emi_max bigint,
    average_price_per_sqft bigint,
    start_date date,
    possession_date date,
    rera_number character varying(60),
    status character varying(40),
    active boolean DEFAULT true NOT NULL,
    published boolean DEFAULT false NOT NULL,
    priority integer DEFAULT 0 NOT NULL,
    deleted boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    review_status character varying(40) DEFAULT 'DRAFT'::character varying NOT NULL,
    created_by_dashboard_user_id bigint,
    submitted_by_dashboard_user_id bigint,
    submitted_at timestamp with time zone,
    reviewed_by_dashboard_user_id bigint,
    reviewed_at timestamp with time zone,
    review_remarks text,
    CONSTRAINT chk_project_review_status CHECK (((review_status)::text = ANY ((ARRAY['DRAFT'::character varying, 'PENDING_REVIEW'::character varying, 'RECHECK'::character varying, 'APPROVED'::character varying, 'REJECTED'::character varying])::text[]))),
    CONSTRAINT project_status_check CHECK (((status IS NULL) OR ((status)::text = ANY ((ARRAY['UPCOMING'::character varying, 'UNDER_CONSTRUCTION'::character varying, 'READY_TO_MOVE'::character varying, 'COMPLETED'::character varying])::text[]))))
);


--
-- Name: project_amenity_progress; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.project_amenity_progress (
    id bigint NOT NULL,
    project_id bigint NOT NULL,
    amenity_code character varying(80) NOT NULL,
    amenity_label character varying(120) NOT NULL,
    status character varying(30) NOT NULL,
    progress_percent integer DEFAULT 0 NOT NULL,
    weight_percent integer DEFAULT 0 NOT NULL,
    display_order integer NOT NULL,
    remarks text,
    verified boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    category character varying(50),
    category_label character varying(100),
    icon_key character varying(80),
    rare boolean DEFAULT false NOT NULL,
    available boolean DEFAULT true NOT NULL,
    public_visible boolean DEFAULT true NOT NULL,
    active boolean DEFAULT true NOT NULL,
    category_display_order integer DEFAULT 0 NOT NULL
);


--
-- Name: project_amenity_progress_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.project_amenity_progress_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: project_amenity_progress_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.project_amenity_progress_id_seq OWNED BY public.project_amenity_progress.id;


--
-- Name: project_analytics; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.project_analytics (
    id bigint NOT NULL,
    category_id bigint NOT NULL,
    builder_id bigint,
    title character varying(200) NOT NULL,
    image_url text NOT NULL,
    caption character varying(255),
    priority integer DEFAULT 0 NOT NULL,
    active boolean DEFAULT true NOT NULL,
    deleted boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: project_analytics_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.project_analytics_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: project_analytics_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.project_analytics_id_seq OWNED BY public.project_analytics.id;


--
-- Name: project_compliance_item; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.project_compliance_item (
    id bigint NOT NULL,
    project_id bigint NOT NULL,
    item_group character varying(40) NOT NULL,
    item_key character varying(80) NOT NULL,
    item_label character varying(120) NOT NULL,
    status character varying(30) NOT NULL,
    value_text character varying(255),
    document_url text,
    remarks text,
    display_order integer NOT NULL,
    verified boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: project_compliance_item_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.project_compliance_item_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: project_compliance_item_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.project_compliance_item_id_seq OWNED BY public.project_compliance_item.id;


--
-- Name: project_connectivity; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.project_connectivity (
    id bigint NOT NULL,
    project_id bigint NOT NULL,
    title character varying(160),
    subtitle character varying(260),
    map_image_url text,
    active boolean DEFAULT true NOT NULL,
    deleted boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    summary text,
    default_radius_meters integer,
    search_enabled boolean DEFAULT true NOT NULL
);


--
-- Name: project_connectivity_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.project_connectivity_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: project_connectivity_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.project_connectivity_id_seq OWNED BY public.project_connectivity.id;


--
-- Name: project_connectivity_place; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.project_connectivity_place (
    id bigint NOT NULL,
    project_id bigint NOT NULL,
    connectivity_id bigint,
    place_name character varying(180) NOT NULL,
    place_type character varying(60) NOT NULL,
    distance_label character varying(80),
    image_url text,
    sort_order integer DEFAULT 0 NOT NULL,
    active boolean DEFAULT true NOT NULL,
    deleted boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    latitude double precision,
    longitude double precision,
    distance_meters integer,
    duration_seconds integer,
    duration_label character varying(80),
    external_place_id character varying(180),
    provider character varying(40),
    rating numeric(3,2),
    user_rating_count integer,
    address text,
    category character varying(40),
    verified boolean DEFAULT false NOT NULL,
    featured boolean DEFAULT false NOT NULL
);


--
-- Name: project_connectivity_place_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.project_connectivity_place_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: project_connectivity_place_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.project_connectivity_place_id_seq OWNED BY public.project_connectivity_place.id;


--
-- Name: project_construction_stage; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.project_construction_stage (
    id bigint NOT NULL,
    project_id bigint NOT NULL,
    stage_code character varying(50) NOT NULL,
    stage_label character varying(120) NOT NULL,
    display_order integer NOT NULL,
    weight_percent integer NOT NULL,
    progress_percent integer DEFAULT 0 NOT NULL,
    planned_start_date date,
    planned_end_date date,
    actual_start_date date,
    actual_end_date date,
    status character varying(30) NOT NULL,
    remarks text,
    evidence_count integer DEFAULT 0 NOT NULL,
    verified boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: project_construction_stage_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.project_construction_stage_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: project_construction_stage_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.project_construction_stage_id_seq OWNED BY public.project_construction_stage.id;


--
-- Name: project_cost_breakdown; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.project_cost_breakdown (
    id bigint NOT NULL,
    project_id bigint NOT NULL,
    land_cost bigint,
    construction_cost bigint,
    infrastructure_cost bigint,
    other_cost bigint,
    total_cost bigint,
    source_label character varying(80),
    remarks text,
    verified boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: project_cost_breakdown_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.project_cost_breakdown_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: project_cost_breakdown_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.project_cost_breakdown_id_seq OWNED BY public.project_cost_breakdown.id;


--
-- Name: project_floor_plan; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.project_floor_plan (
    id bigint NOT NULL,
    project_id bigint NOT NULL,
    title character varying(160) NOT NULL,
    floor_code character varying(80),
    image_url text NOT NULL,
    carpet_area character varying(100),
    exclusive_area character varying(100),
    super_area character varying(100),
    unit_label character varying(120),
    description text,
    sort_order integer DEFAULT 0 NOT NULL,
    active boolean DEFAULT true NOT NULL,
    deleted boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    unit_configuration_type character varying(30),
    price bigint,
    saleable_area_sqft numeric(10,2),
    carpet_area_sqft numeric(10,2),
    built_up_area_sqft numeric(10,2),
    super_area_sqft numeric(10,2),
    floor_height_meters numeric(5,2),
    carpet_efficiency_percent numeric(5,2),
    bedrooms integer,
    bathrooms integer,
    balconies integer,
    facing character varying(100),
    direction_summary character varying(255),
    tower_name character varying(100),
    floor_range character varying(100),
    key_plan_image_url text,
    featured boolean DEFAULT false NOT NULL,
    insights_available boolean DEFAULT false NOT NULL
);


--
-- Name: project_floor_plan_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.project_floor_plan_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: project_floor_plan_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.project_floor_plan_id_seq OWNED BY public.project_floor_plan.id;


--
-- Name: project_floor_plan_insight; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.project_floor_plan_insight (
    id bigint NOT NULL,
    floor_plan_id bigint NOT NULL,
    insight_type character varying(50) NOT NULL,
    title character varying(160) NOT NULL,
    summary character varying(500),
    detailed_text text,
    unit_value numeric(12,4),
    benchmark_value numeric(12,4),
    unit_label character varying(30),
    difference_value numeric(12,4),
    difference_percent numeric(8,4),
    chart_label_this_unit character varying(80),
    chart_label_average character varying(80),
    comparison_result character varying(30),
    score integer,
    positive boolean DEFAULT true NOT NULL,
    public_visible boolean DEFAULT true NOT NULL,
    verified boolean DEFAULT false NOT NULL,
    active boolean DEFAULT true NOT NULL,
    deleted boolean DEFAULT false NOT NULL,
    sort_order integer DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: project_floor_plan_insight_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.project_floor_plan_insight_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: project_floor_plan_insight_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.project_floor_plan_insight_id_seq OWNED BY public.project_floor_plan_insight.id;


--
-- Name: project_floor_plan_room_dimension; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.project_floor_plan_room_dimension (
    id bigint NOT NULL,
    floor_plan_id bigint NOT NULL,
    room_type character varying(50) NOT NULL,
    label character varying(100),
    length_ft numeric(8,2),
    width_ft numeric(8,2),
    area_sqft numeric(10,2),
    dimension_text character varying(100),
    icon_key character varying(60),
    notes character varying(255),
    sort_order integer DEFAULT 0 NOT NULL,
    active boolean DEFAULT true NOT NULL,
    deleted boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: project_floor_plan_room_dimension_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.project_floor_plan_room_dimension_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: project_floor_plan_room_dimension_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.project_floor_plan_room_dimension_id_seq OWNED BY public.project_floor_plan_room_dimension.id;


--
-- Name: project_highlight; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.project_highlight (
    id bigint NOT NULL,
    project_id bigint NOT NULL,
    title character varying(160) NOT NULL,
    subtitle character varying(260),
    icon_key character varying(80),
    sort_order integer DEFAULT 0 NOT NULL,
    active boolean DEFAULT true NOT NULL,
    deleted boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: project_highlight_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.project_highlight_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: project_highlight_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.project_highlight_id_seq OWNED BY public.project_highlight.id;


--
-- Name: project_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.project_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: project_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.project_id_seq OWNED BY public.project.id;


--
-- Name: project_land_utilization; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.project_land_utilization (
    id bigint NOT NULL,
    project_id bigint NOT NULL,
    total_land_area_sqm double precision,
    commercial_area_sqm double precision,
    parks_area_sqm double precision,
    open_area_sqm double precision,
    residential_area_sqm double precision,
    parking_area_sqm double precision,
    utility_area_sqm double precision,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: project_land_utilization_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.project_land_utilization_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: project_land_utilization_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.project_land_utilization_id_seq OWNED BY public.project_land_utilization.id;


--
-- Name: project_location_score; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.project_location_score (
    id bigint NOT NULL,
    project_id bigint NOT NULL,
    metro_score double precision,
    education_score double precision,
    healthcare_score double precision,
    retail_score double precision,
    job_score double precision,
    leisure_score double precision,
    current_strength_score double precision,
    future_growth_score double precision,
    final_score double precision,
    appreciation_percent_3y double precision,
    score_summary text,
    verified boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: project_location_score_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.project_location_score_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: project_location_score_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.project_location_score_id_seq OWNED BY public.project_location_score.id;


--
-- Name: project_master_plan; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.project_master_plan (
    id bigint NOT NULL,
    project_id bigint NOT NULL,
    title character varying(150),
    subtitle character varying(300),
    description text,
    master_plan_image_url text,
    image_caption character varying(300),
    image_alt_text character varying(300),
    total_units integer,
    total_towers integer,
    total_floors integer,
    park_area_value numeric(12,2),
    park_area_unit character varying(20),
    total_land_area_value numeric(12,2),
    total_land_area_unit character varying(20),
    open_space_area_value numeric(12,2),
    open_space_area_unit character varying(20),
    green_area_value numeric(12,2),
    green_area_unit character varying(20),
    clubhouse_area_value numeric(12,2),
    clubhouse_area_unit character varying(20),
    amenity_area_value numeric(12,2),
    amenity_area_unit character varying(20),
    road_width_value numeric(8,2),
    road_width_unit character varying(20),
    water_source character varying(120),
    parking_type character varying(30),
    total_parking_slots integer,
    visitor_parking_slots integer,
    basement_levels integer,
    entry_exit_gates integer,
    lift_count integer,
    phase_count integer,
    current_phase character varying(80),
    open_space_percent numeric(5,2),
    green_coverage_percent numeric(5,2),
    vastu_compliant boolean,
    gated_community boolean,
    boundary_wall boolean,
    fire_tender_movement boolean,
    sewage_treatment_plant boolean,
    rainwater_harvesting boolean,
    power_backup boolean,
    approval_status character varying(30),
    verified boolean DEFAULT false NOT NULL,
    source_label character varying(180),
    source_document_url character varying(500),
    last_verified_at timestamp with time zone,
    remarks text,
    active boolean DEFAULT true NOT NULL,
    deleted boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT chk_project_master_plan_amenity_area_unit CHECK (((amenity_area_unit IS NULL) OR ((amenity_area_unit)::text = ANY ((ARRAY['SQ_FT'::character varying, 'SQ_MT'::character varying, 'ACRE'::character varying, 'HECTARE'::character varying])::text[])))),
    CONSTRAINT chk_project_master_plan_approval_status CHECK (((approval_status IS NULL) OR ((approval_status)::text = ANY ((ARRAY['DRAFT'::character varying, 'SUBMITTED'::character varying, 'VERIFIED'::character varying, 'NEEDS_REVIEW'::character varying, 'NOT_AVAILABLE'::character varying])::text[])))),
    CONSTRAINT chk_project_master_plan_area_unit CHECK (((park_area_unit IS NULL) OR ((park_area_unit)::text = ANY ((ARRAY['SQ_FT'::character varying, 'SQ_MT'::character varying, 'ACRE'::character varying, 'HECTARE'::character varying])::text[])))),
    CONSTRAINT chk_project_master_plan_clubhouse_area_unit CHECK (((clubhouse_area_unit IS NULL) OR ((clubhouse_area_unit)::text = ANY ((ARRAY['SQ_FT'::character varying, 'SQ_MT'::character varying, 'ACRE'::character varying, 'HECTARE'::character varying])::text[])))),
    CONSTRAINT chk_project_master_plan_green_area_unit CHECK (((green_area_unit IS NULL) OR ((green_area_unit)::text = ANY ((ARRAY['SQ_FT'::character varying, 'SQ_MT'::character varying, 'ACRE'::character varying, 'HECTARE'::character varying])::text[])))),
    CONSTRAINT chk_project_master_plan_non_negative_areas CHECK ((((park_area_value IS NULL) OR (park_area_value >= (0)::numeric)) AND ((total_land_area_value IS NULL) OR (total_land_area_value >= (0)::numeric)) AND ((open_space_area_value IS NULL) OR (open_space_area_value >= (0)::numeric)) AND ((green_area_value IS NULL) OR (green_area_value >= (0)::numeric)) AND ((clubhouse_area_value IS NULL) OR (clubhouse_area_value >= (0)::numeric)) AND ((amenity_area_value IS NULL) OR (amenity_area_value >= (0)::numeric)) AND ((road_width_value IS NULL) OR (road_width_value >= (0)::numeric)))),
    CONSTRAINT chk_project_master_plan_non_negative_counts CHECK ((((total_units IS NULL) OR (total_units >= 0)) AND ((total_towers IS NULL) OR (total_towers >= 0)) AND ((total_floors IS NULL) OR (total_floors >= 0)) AND ((total_parking_slots IS NULL) OR (total_parking_slots >= 0)) AND ((visitor_parking_slots IS NULL) OR (visitor_parking_slots >= 0)) AND ((basement_levels IS NULL) OR (basement_levels >= 0)) AND ((entry_exit_gates IS NULL) OR (entry_exit_gates >= 0)) AND ((lift_count IS NULL) OR (lift_count >= 0)) AND ((phase_count IS NULL) OR (phase_count >= 0)))),
    CONSTRAINT chk_project_master_plan_open_space_area_unit CHECK (((open_space_area_unit IS NULL) OR ((open_space_area_unit)::text = ANY ((ARRAY['SQ_FT'::character varying, 'SQ_MT'::character varying, 'ACRE'::character varying, 'HECTARE'::character varying])::text[])))),
    CONSTRAINT chk_project_master_plan_parking_type CHECK (((parking_type IS NULL) OR ((parking_type)::text = ANY ((ARRAY['OPEN'::character varying, 'COVERED'::character varying, 'BASEMENT'::character varying, 'STILT'::character varying, 'MECHANICAL'::character varying, 'MIXED'::character varying, 'NOT_DISCLOSED'::character varying])::text[])))),
    CONSTRAINT chk_project_master_plan_percentages CHECK ((((open_space_percent IS NULL) OR ((open_space_percent >= (0)::numeric) AND (open_space_percent <= (100)::numeric))) AND ((green_coverage_percent IS NULL) OR ((green_coverage_percent >= (0)::numeric) AND (green_coverage_percent <= (100)::numeric))))),
    CONSTRAINT chk_project_master_plan_road_width_unit CHECK (((road_width_unit IS NULL) OR ((road_width_unit)::text = ANY ((ARRAY['SQ_FT'::character varying, 'SQ_MT'::character varying, 'ACRE'::character varying, 'HECTARE'::character varying])::text[])))),
    CONSTRAINT chk_project_master_plan_total_land_area_unit CHECK (((total_land_area_unit IS NULL) OR ((total_land_area_unit)::text = ANY ((ARRAY['SQ_FT'::character varying, 'SQ_MT'::character varying, 'ACRE'::character varying, 'HECTARE'::character varying])::text[]))))
);


--
-- Name: project_master_plan_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.project_master_plan_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: project_master_plan_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.project_master_plan_id_seq OWNED BY public.project_master_plan.id;


--
-- Name: project_media; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.project_media (
    id bigint NOT NULL,
    project_id bigint NOT NULL,
    media_type character varying(30) NOT NULL,
    url text NOT NULL,
    caption character varying(200),
    sort_order integer DEFAULT 0 NOT NULL,
    active boolean DEFAULT true NOT NULL,
    deleted boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: project_media_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.project_media_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: project_media_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.project_media_id_seq OWNED BY public.project_media.id;


--
-- Name: project_meter_snapshot; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.project_meter_snapshot (
    id bigint NOT NULL,
    project_id bigint NOT NULL,
    construction_progress_percent integer DEFAULT 0 NOT NULL,
    delay_days integer DEFAULT 0 NOT NULL,
    construction_start_date date,
    expected_completion_date date,
    revised_completion_date date,
    compliance_score integer,
    amenity_score integer,
    location_score double precision,
    location_appreciation_percent_3y double precision,
    launch_price bigint,
    current_price bigint,
    average_area_price bigint,
    price_appreciation_percent double precision,
    estimated_cost_total bigint,
    computed_at timestamp with time zone,
    last_verified_at timestamp with time zone,
    verified boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    original_completion_date date,
    latest_rera_completion_date date,
    actual_completion_date date,
    rera_extension_count integer DEFAULT 0 NOT NULL,
    delay_vs_original_days integer,
    delay_vs_latest_rera_days integer,
    timeline_status character varying(40),
    timeline_label text,
    timeline_hint text
);


--
-- Name: project_meter_snapshot_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.project_meter_snapshot_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: project_meter_snapshot_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.project_meter_snapshot_id_seq OWNED BY public.project_meter_snapshot.id;


--
-- Name: project_payment_milestone; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.project_payment_milestone (
    id bigint NOT NULL,
    project_id bigint NOT NULL,
    milestone_code character varying(80) NOT NULL,
    milestone_label character varying(120) NOT NULL,
    description character varying(255),
    percentage_value integer NOT NULL,
    display_order integer NOT NULL,
    linked_stage_code character varying(50),
    active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: project_payment_milestone_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.project_payment_milestone_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: project_payment_milestone_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.project_payment_milestone_id_seq OWNED BY public.project_payment_milestone.id;


--
-- Name: project_plan; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.project_plan (
    id bigint NOT NULL,
    category_id bigint NOT NULL,
    builder_id bigint,
    title character varying(200) NOT NULL,
    description text,
    image_url text NOT NULL,
    company_logo_url text,
    tags text,
    priority integer DEFAULT 0 NOT NULL,
    active boolean DEFAULT true NOT NULL,
    deleted boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: project_plan_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.project_plan_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: project_plan_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.project_plan_id_seq OWNED BY public.project_plan.id;


--
-- Name: project_price_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.project_price_history (
    id bigint NOT NULL,
    project_id bigint NOT NULL,
    year_label character varying(20) NOT NULL,
    project_price bigint NOT NULL,
    average_area_price bigint,
    display_order integer NOT NULL,
    verified boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: project_price_history_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.project_price_history_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: project_price_history_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.project_price_history_id_seq OWNED BY public.project_price_history.id;


--
-- Name: project_property_types; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.project_property_types (
    project_id bigint NOT NULL,
    property_type character varying(30) NOT NULL
);


--
-- Name: project_review; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.project_review (
    id bigint NOT NULL,
    project_id bigint NOT NULL,
    reviewer_name character varying(255),
    reviewer_phone_hash character varying(255),
    rating integer NOT NULL,
    headline character varying(300),
    review_text text,
    source_type character varying(60) DEFAULT 'USER_SUBMITTED'::character varying NOT NULL,
    verification_status character varying(40) DEFAULT 'PENDING'::character varying NOT NULL,
    category character varying(80),
    sentiment character varying(30),
    is_featured boolean DEFAULT false NOT NULL,
    display_status character varying(30) DEFAULT 'INTERNAL_ONLY'::character varying NOT NULL,
    reviewed_by_dashboard_user_id bigint,
    reviewed_at timestamp with time zone,
    internal_note text,
    deleted boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    user_id bigint,
    user_phone_hash character varying(255),
    submitted_by_user boolean DEFAULT false NOT NULL,
    CONSTRAINT project_review_rating_check CHECK (((rating >= 1) AND (rating <= 5)))
);


--
-- Name: project_review_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.project_review_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: project_review_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.project_review_id_seq OWNED BY public.project_review.id;


--
-- Name: promo_banner; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.promo_banner (
    id bigint NOT NULL,
    category_id bigint NOT NULL,
    title character varying(150) NOT NULL,
    subtitle character varying(255),
    image_url character varying(500),
    target_url character varying(500),
    priority integer DEFAULT 0 NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    start_at timestamp with time zone,
    end_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    slot_key character varying(20) DEFAULT 'HERO'::character varying NOT NULL,
    media_type character varying(30) DEFAULT 'IMAGE'::character varying NOT NULL,
    media_url text,
    display_duration_ms integer,
    CONSTRAINT chk_promo_banner_display_duration_ms CHECK (((display_duration_ms IS NULL) OR ((display_duration_ms >= 1000) AND (display_duration_ms <= 60000))))
);


--
-- Name: promo_banner_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.promo_banner_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: promo_banner_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.promo_banner_id_seq OWNED BY public.promo_banner.id;


--
-- Name: promo_banner_slot_config; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.promo_banner_slot_config (
    id bigint NOT NULL,
    screen character varying(20) NOT NULL,
    home_category_id bigint NOT NULL,
    city_id bigint,
    slot_key character varying(20) DEFAULT 'HERO'::character varying NOT NULL,
    insert_after_section_type character varying(50),
    position_index integer DEFAULT 0 NOT NULL,
    max_items integer DEFAULT 10 NOT NULL,
    priority integer DEFAULT 0 NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    start_at timestamp with time zone,
    end_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: promo_banner_slot_config_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.promo_banner_slot_config_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: promo_banner_slot_config_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.promo_banner_slot_config_id_seq OWNED BY public.promo_banner_slot_config.id;


--
-- Name: provider_media; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.provider_media (
    id bigint NOT NULL,
    provider_id bigint NOT NULL,
    media_type character varying(20) NOT NULL,
    url text NOT NULL,
    thumbnail_url text,
    sort_order integer DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    storage_key character varying(500)
);


--
-- Name: provider_media_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.provider_media_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: provider_media_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.provider_media_id_seq OWNED BY public.provider_media.id;


--
-- Name: provider_profile; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.provider_profile (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    business_id bigint,
    provider_type character varying(20) NOT NULL,
    display_name character varying(120) NOT NULL,
    headline character varying(180),
    bio text,
    primary_category_id bigint NOT NULL,
    experience_years integer,
    business_name character varying(150),
    gst_number character varying(30),
    verification_status character varying(20) DEFAULT 'UNVERIFIED'::character varying NOT NULL,
    is_featured boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: provider_profile_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.provider_profile_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: provider_profile_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.provider_profile_id_seq OWNED BY public.provider_profile.id;


--
-- Name: provider_project; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.provider_project (
    id bigint NOT NULL,
    provider_id bigint NOT NULL,
    title character varying(120) NOT NULL,
    description text,
    category_id bigint NOT NULL,
    city_id bigint,
    locality character varying(120),
    budget_min integer,
    budget_max integer,
    visibility character varying(20) DEFAULT 'PUBLIC'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: provider_project_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.provider_project_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: provider_project_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.provider_project_id_seq OWNED BY public.provider_project.id;


--
-- Name: provider_project_media; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.provider_project_media (
    id bigint NOT NULL,
    project_id bigint NOT NULL,
    media_type character varying(10) NOT NULL,
    url text NOT NULL,
    thumbnail_url text,
    sort_order integer DEFAULT 0 NOT NULL
);


--
-- Name: provider_project_media_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.provider_project_media_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: provider_project_media_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.provider_project_media_id_seq OWNED BY public.provider_project_media.id;


--
-- Name: provider_service_area; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.provider_service_area (
    id bigint NOT NULL,
    provider_id bigint NOT NULL,
    city_id bigint NOT NULL,
    locality character varying(120),
    pincode character varying(12),
    latitude double precision,
    longitude double precision
);


--
-- Name: provider_service_area_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.provider_service_area_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: provider_service_area_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.provider_service_area_id_seq OWNED BY public.provider_service_area.id;


--
-- Name: public_review_place; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.public_review_place (
    id bigint NOT NULL,
    target_type character varying(30) NOT NULL,
    target_id bigint NOT NULL,
    source_type character varying(40) DEFAULT 'GOOGLE_PLACES'::character varying NOT NULL,
    google_place_id character varying(255) NOT NULL,
    place_name character varying(255),
    formatted_address text,
    google_maps_uri text,
    place_category character varying(60) DEFAULT 'OTHER'::character varying NOT NULL,
    active boolean DEFAULT true NOT NULL,
    deleted boolean DEFAULT false NOT NULL,
    last_synced_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    one_time_fetched boolean DEFAULT false NOT NULL,
    fetch_status character varying(30) DEFAULT 'NOT_FETCHED'::character varying NOT NULL,
    content_expires_at timestamp with time zone,
    display_mode character varying(30) DEFAULT 'HIDDEN'::character varying NOT NULL,
    display_google_reviews boolean DEFAULT false NOT NULL,
    CONSTRAINT chk_public_review_place_category CHECK (((place_category)::text = ANY ((ARRAY['PROJECT_SITE'::character varying, 'REAL_ESTATE'::character varying, 'BUILDER_OFFICE'::character varying, 'SALES_OFFICE'::character varying, 'SOCIETY'::character varying, 'CLUBHOUSE'::character varying, 'OTHER'::character varying])::text[]))),
    CONSTRAINT chk_public_review_place_source_type CHECK (((source_type)::text = 'GOOGLE_PLACES'::text)),
    CONSTRAINT chk_public_review_place_target_type CHECK (((target_type)::text = ANY ((ARRAY['PROJECT'::character varying, 'BUILDER'::character varying, 'COMPANY'::character varying])::text[])))
);


--
-- Name: public_review_place_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.public_review_place_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: public_review_place_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.public_review_place_id_seq OWNED BY public.public_review_place.id;


--
-- Name: public_review_sample; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.public_review_sample (
    id bigint NOT NULL,
    review_place_id bigint NOT NULL,
    target_type character varying(30) NOT NULL,
    target_id bigint NOT NULL,
    source_type character varying(40) DEFAULT 'GOOGLE_PLACES'::character varying NOT NULL,
    reviewer_name character varying(255),
    reviewer_profile_url text,
    reviewer_photo_url text,
    rating integer,
    review_text text,
    original_review_text text,
    language_code character varying(20),
    relative_publish_time character varying(100),
    publish_time timestamp with time zone,
    sentiment character varying(30),
    category character varying(80),
    display_status character varying(30) DEFAULT 'INTERNAL_ONLY'::character varying NOT NULL,
    fetched_at timestamp with time zone DEFAULT now() NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT chk_public_review_sample_display_status CHECK (((display_status)::text = ANY ((ARRAY['INTERNAL_ONLY'::character varying, 'APPROVED_PUBLIC'::character varying, 'HIDDEN'::character varying])::text[]))),
    CONSTRAINT chk_public_review_sample_sentiment CHECK (((sentiment IS NULL) OR ((sentiment)::text = ANY ((ARRAY['POSITIVE'::character varying, 'NEGATIVE'::character varying, 'NEUTRAL'::character varying, 'MIXED'::character varying])::text[])))),
    CONSTRAINT chk_public_review_sample_source_type CHECK (((source_type)::text = 'GOOGLE_PLACES'::text)),
    CONSTRAINT chk_public_review_sample_target_type CHECK (((target_type)::text = ANY ((ARRAY['PROJECT'::character varying, 'BUILDER'::character varying, 'COMPANY'::character varying])::text[]))),
    CONSTRAINT ck_public_review_sample_rating CHECK (((rating IS NULL) OR ((rating >= 1) AND (rating <= 5))))
);


--
-- Name: public_review_sample_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.public_review_sample_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: public_review_sample_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.public_review_sample_id_seq OWNED BY public.public_review_sample.id;


--
-- Name: public_review_summary; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.public_review_summary (
    id bigint NOT NULL,
    review_place_id bigint NOT NULL,
    target_type character varying(30) NOT NULL,
    target_id bigint NOT NULL,
    source_type character varying(40) DEFAULT 'GOOGLE_PLACES'::character varying NOT NULL,
    rating numeric(2,1),
    user_rating_count integer,
    positive_sample_count integer DEFAULT 0 NOT NULL,
    negative_sample_count integer DEFAULT 0 NOT NULL,
    neutral_sample_count integer DEFAULT 0 NOT NULL,
    mixed_sample_count integer DEFAULT 0 NOT NULL,
    source_label character varying(100) DEFAULT 'Google Maps'::character varying NOT NULL,
    disclaimer text,
    last_synced_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT chk_public_review_summary_source_type CHECK (((source_type)::text = 'GOOGLE_PLACES'::text)),
    CONSTRAINT chk_public_review_summary_target_type CHECK (((target_type)::text = ANY ((ARRAY['PROJECT'::character varying, 'BUILDER'::character varying, 'COMPANY'::character varying])::text[]))),
    CONSTRAINT ck_public_review_summary_rating CHECK (((rating IS NULL) OR ((rating >= (0)::numeric) AND (rating <= (5)::numeric)))),
    CONSTRAINT ck_public_review_summary_user_rating_count CHECK (((user_rating_count IS NULL) OR (user_rating_count >= 0)))
);


--
-- Name: public_review_summary_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.public_review_summary_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: public_review_summary_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.public_review_summary_id_seq OWNED BY public.public_review_summary.id;


--
-- Name: refresh_tokens; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.refresh_tokens (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    token character varying(256) NOT NULL,
    device_id character varying(255),
    fcm_token text,
    expires_at timestamp with time zone NOT NULL,
    revoked boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: COLUMN refresh_tokens.token; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.refresh_tokens.token IS 'SHA-256 hex digest of the raw refresh token; raw refresh tokens are never persisted.';


--
-- Name: refresh_tokens_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.refresh_tokens_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: refresh_tokens_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.refresh_tokens_id_seq OWNED BY public.refresh_tokens.id;


--
-- Name: service_request; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.service_request (
    id bigint NOT NULL,
    customer_user_id bigint NOT NULL,
    status character varying(20) NOT NULL,
    city_id bigint NOT NULL,
    locality character varying(120),
    pincode character varying(12) NOT NULL,
    notes text,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL
);


--
-- Name: service_request_category; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.service_request_category (
    service_request_id bigint NOT NULL,
    category_id bigint NOT NULL
);


--
-- Name: service_request_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.service_request_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: service_request_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.service_request_id_seq OWNED BY public.service_request.id;


--
-- Name: service_request_interest; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.service_request_interest (
    id bigint NOT NULL,
    service_request_id bigint NOT NULL,
    provider_id bigint NOT NULL,
    status character varying(20) NOT NULL,
    message text,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL
);


--
-- Name: service_request_interest_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.service_request_interest_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: service_request_interest_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.service_request_interest_id_seq OWNED BY public.service_request_interest.id;


--
-- Name: stamp_duty_rule; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.stamp_duty_rule (
    id bigint NOT NULL,
    state_name character varying(100) NOT NULL,
    city_name character varying(100) NOT NULL,
    buyer_type character varying(30) NOT NULL,
    property_category character varying(30) NOT NULL,
    stamp_duty_percent numeric(8,4) NOT NULL,
    registration_percent numeric(8,4) NOT NULL,
    local_body_tax_percent numeric(8,4) NOT NULL,
    effective_from date NOT NULL,
    effective_to date,
    active boolean DEFAULT true NOT NULL,
    source_note character varying(500),
    CONSTRAINT chk_local_body_tax_percent_max CHECK ((local_body_tax_percent <= (100)::numeric)),
    CONSTRAINT chk_local_body_tax_percent_non_negative CHECK ((local_body_tax_percent >= (0)::numeric)),
    CONSTRAINT chk_registration_percent_max CHECK ((registration_percent <= (100)::numeric)),
    CONSTRAINT chk_registration_percent_non_negative CHECK ((registration_percent >= (0)::numeric)),
    CONSTRAINT chk_stamp_duty_percent_max CHECK ((stamp_duty_percent <= (100)::numeric)),
    CONSTRAINT chk_stamp_duty_percent_non_negative CHECK ((stamp_duty_percent >= (0)::numeric)),
    CONSTRAINT chk_stamp_duty_rule_date_range CHECK (((effective_to IS NULL) OR (effective_to >= effective_from)))
);


--
-- Name: stamp_duty_rule_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.stamp_duty_rule_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: stamp_duty_rule_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.stamp_duty_rule_id_seq OWNED BY public.stamp_duty_rule.id;


--
-- Name: user_favorite; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_favorite (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    target_type character varying(30) NOT NULL,
    target_id bigint NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: user_favorite_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.user_favorite_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: user_favorite_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.user_favorite_id_seq OWNED BY public.user_favorite.id;


--
-- Name: users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.users (
    id bigint NOT NULL,
    email character varying(255),
    name character varying(80),
    password character varying(255),
    phone_number character varying(20) NOT NULL,
    is_verified boolean DEFAULT false NOT NULL,
    role character varying(40) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    last_login_at timestamp with time zone,
    onboarding_status character varying(40) DEFAULT 'ROLE_PENDING'::character varying NOT NULL,
    role_selected_at timestamp with time zone,
    is_admin boolean DEFAULT false NOT NULL,
    profile_photo_url text,
    profile_photo_storage_key character varying(500),
    CONSTRAINT ck_users_phone_number_canonical_e164 CHECK (((phone_number)::text ~ '^\+91[6-9][0-9]{9}$'::text)),
    CONSTRAINT users_role_check CHECK (((role)::text = ANY ((ARRAY['CUSTOMER'::character varying, 'WORKER'::character varying, 'BRAND'::character varying, 'ROLE_ADMIN'::character varying])::text[])))
);


--
-- Name: users_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: users_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.users_id_seq OWNED BY public.users.id;


--
-- Name: app_content_page id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.app_content_page ALTER COLUMN id SET DEFAULT nextval('public.app_content_page_id_seq'::regclass);


--
-- Name: app_screen_content id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.app_screen_content ALTER COLUMN id SET DEFAULT nextval('public.app_screen_content_id_seq'::regclass);


--
-- Name: app_setting id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.app_setting ALTER COLUMN id SET DEFAULT nextval('public.app_setting_id_seq'::regclass);


--
-- Name: brand id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brand ALTER COLUMN id SET DEFAULT nextval('public.brand_id_seq'::regclass);


--
-- Name: brand_category_link id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brand_category_link ALTER COLUMN id SET DEFAULT nextval('public.brand_category_link_id_seq'::regclass);


--
-- Name: brand_certificate id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brand_certificate ALTER COLUMN id SET DEFAULT nextval('public.brand_certificate_id_seq'::regclass);


--
-- Name: brand_collaboration id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brand_collaboration ALTER COLUMN id SET DEFAULT nextval('public.brand_collaboration_id_seq'::regclass);


--
-- Name: brand_distributor id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brand_distributor ALTER COLUMN id SET DEFAULT nextval('public.brand_distributor_id_seq'::regclass);


--
-- Name: brand_faq id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brand_faq ALTER COLUMN id SET DEFAULT nextval('public.brand_faq_id_seq'::regclass);


--
-- Name: brand_media id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brand_media ALTER COLUMN id SET DEFAULT nextval('public.brand_media_id_seq'::regclass);


--
-- Name: brand_product_category id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brand_product_category ALTER COLUMN id SET DEFAULT nextval('public.brand_product_category_id_seq'::regclass);


--
-- Name: brand_sku id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brand_sku ALTER COLUMN id SET DEFAULT nextval('public.brand_sku_id_seq'::regclass);


--
-- Name: builder id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.builder ALTER COLUMN id SET DEFAULT nextval('public.builder_id_seq'::regclass);


--
-- Name: builder_after_sales_upgrade id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.builder_after_sales_upgrade ALTER COLUMN id SET DEFAULT nextval('public.builder_after_sales_upgrade_id_seq'::regclass);


--
-- Name: builder_highlight_item id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.builder_highlight_item ALTER COLUMN id SET DEFAULT nextval('public.builder_highlight_item_id_seq'::regclass);


--
-- Name: builder_highlight_point id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.builder_highlight_point ALTER COLUMN id SET DEFAULT nextval('public.builder_highlight_point_id_seq'::regclass);


--
-- Name: builder_improvement_action id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.builder_improvement_action ALTER COLUMN id SET DEFAULT nextval('public.builder_improvement_action_id_seq'::regclass);


--
-- Name: builder_improvement_issue id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.builder_improvement_issue ALTER COLUMN id SET DEFAULT nextval('public.builder_improvement_issue_id_seq'::regclass);


--
-- Name: builder_improvement_profile id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.builder_improvement_profile ALTER COLUMN id SET DEFAULT nextval('public.builder_improvement_profile_id_seq'::regclass);


--
-- Name: builder_improvement_timeline id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.builder_improvement_timeline ALTER COLUMN id SET DEFAULT nextval('public.builder_improvement_timeline_id_seq'::regclass);


--
-- Name: business id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.business ALTER COLUMN id SET DEFAULT nextval('public.business_id_seq'::regclass);


--
-- Name: business_event id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.business_event ALTER COLUMN id SET DEFAULT nextval('public.business_event_id_seq'::regclass);


--
-- Name: category id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.category ALTER COLUMN id SET DEFAULT nextval('public.category_id_seq'::regclass);


--
-- Name: circle_rate_rule id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.circle_rate_rule ALTER COLUMN id SET DEFAULT nextval('public.circle_rate_rule_id_seq'::regclass);


--
-- Name: city id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.city ALTER COLUMN id SET DEFAULT nextval('public.city_id_seq'::regclass);


--
-- Name: company id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company ALTER COLUMN id SET DEFAULT nextval('public.company_id_seq'::regclass);


--
-- Name: company_award id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_award ALTER COLUMN id SET DEFAULT nextval('public.company_award_id_seq'::regclass);


--
-- Name: company_brand_link id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_brand_link ALTER COLUMN id SET DEFAULT nextval('public.company_brand_link_id_seq'::regclass);


--
-- Name: company_certificate id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_certificate ALTER COLUMN id SET DEFAULT nextval('public.company_certificate_id_seq'::regclass);


--
-- Name: company_media id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_media ALTER COLUMN id SET DEFAULT nextval('public.company_media_id_seq'::regclass);


--
-- Name: company_pricing_plan id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_pricing_plan ALTER COLUMN id SET DEFAULT nextval('public.company_pricing_plan_id_seq'::regclass);


--
-- Name: company_project id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_project ALTER COLUMN id SET DEFAULT nextval('public.company_project_id_seq'::regclass);


--
-- Name: company_stat id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_stat ALTER COLUMN id SET DEFAULT nextval('public.company_stat_id_seq'::regclass);


--
-- Name: dashboard_action_audit id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dashboard_action_audit ALTER COLUMN id SET DEFAULT nextval('public.dashboard_action_audit_id_seq'::regclass);


--
-- Name: dashboard_content_review_history id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dashboard_content_review_history ALTER COLUMN id SET DEFAULT nextval('public.dashboard_content_review_history_id_seq'::regclass);


--
-- Name: dashboard_field_help id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dashboard_field_help ALTER COLUMN id SET DEFAULT nextval('public.dashboard_field_help_id_seq'::regclass);


--
-- Name: dashboard_field_review_issues id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dashboard_field_review_issues ALTER COLUMN id SET DEFAULT nextval('public.dashboard_field_review_issues_id_seq'::regclass);


--
-- Name: dashboard_login_audit id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dashboard_login_audit ALTER COLUMN id SET DEFAULT nextval('public.dashboard_login_audit_id_seq'::regclass);


--
-- Name: dashboard_refresh_tokens id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dashboard_refresh_tokens ALTER COLUMN id SET DEFAULT nextval('public.dashboard_refresh_tokens_id_seq'::regclass);


--
-- Name: dashboard_scrape_candidate id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dashboard_scrape_candidate ALTER COLUMN id SET DEFAULT nextval('public.dashboard_scrape_candidate_id_seq'::regclass);


--
-- Name: dashboard_scrape_candidate_builder id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dashboard_scrape_candidate_builder ALTER COLUMN id SET DEFAULT nextval('public.dashboard_scrape_candidate_builder_id_seq'::regclass);


--
-- Name: dashboard_scrape_candidate_compliance_item id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dashboard_scrape_candidate_compliance_item ALTER COLUMN id SET DEFAULT nextval('public.dashboard_scrape_candidate_compliance_item_id_seq'::regclass);


--
-- Name: dashboard_scrape_candidate_cost_breakdown id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dashboard_scrape_candidate_cost_breakdown ALTER COLUMN id SET DEFAULT nextval('public.dashboard_scrape_candidate_cost_breakdown_id_seq'::regclass);


--
-- Name: dashboard_scrape_candidate_document id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dashboard_scrape_candidate_document ALTER COLUMN id SET DEFAULT nextval('public.dashboard_scrape_candidate_document_id_seq'::regclass);


--
-- Name: dashboard_scrape_candidate_field_result id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dashboard_scrape_candidate_field_result ALTER COLUMN id SET DEFAULT nextval('public.dashboard_scrape_candidate_field_result_id_seq'::regclass);


--
-- Name: dashboard_scrape_candidate_land_utilization id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dashboard_scrape_candidate_land_utilization ALTER COLUMN id SET DEFAULT nextval('public.dashboard_scrape_candidate_land_utilization_id_seq'::regclass);


--
-- Name: dashboard_scrape_candidate_project id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dashboard_scrape_candidate_project ALTER COLUMN id SET DEFAULT nextval('public.dashboard_scrape_candidate_project_id_seq'::regclass);


--
-- Name: dashboard_scrape_candidate_raw_value id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dashboard_scrape_candidate_raw_value ALTER COLUMN id SET DEFAULT nextval('public.dashboard_scrape_candidate_raw_value_id_seq'::regclass);


--
-- Name: dashboard_users id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dashboard_users ALTER COLUMN id SET DEFAULT nextval('public.dashboard_users_id_seq'::regclass);


--
-- Name: distributor id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.distributor ALTER COLUMN id SET DEFAULT nextval('public.distributor_id_seq'::regclass);


--
-- Name: distributor_media id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.distributor_media ALTER COLUMN id SET DEFAULT nextval('public.distributor_media_id_seq'::regclass);


--
-- Name: favorites id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.favorites ALTER COLUMN id SET DEFAULT nextval('public.favorites_id_seq'::regclass);


--
-- Name: featured_carousel_config id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.featured_carousel_config ALTER COLUMN id SET DEFAULT nextval('public.featured_carousel_config_id_seq'::regclass);


--
-- Name: feed_section_config id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.feed_section_config ALTER COLUMN id SET DEFAULT nextval('public.feed_section_config_id_seq'::regclass);


--
-- Name: feed_section_item id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.feed_section_item ALTER COLUMN id SET DEFAULT nextval('public.feed_section_item_id_seq'::regclass);


--
-- Name: guest_sessions id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.guest_sessions ALTER COLUMN id SET DEFAULT nextval('public.guest_sessions_id_seq'::regclass);


--
-- Name: home_project_analytics id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.home_project_analytics ALTER COLUMN id SET DEFAULT nextval('public.home_project_analytics_id_seq'::regclass);


--
-- Name: home_section_config id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.home_section_config ALTER COLUMN id SET DEFAULT nextval('public.home_section_config_id_seq'::regclass);


--
-- Name: home_section_item id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.home_section_item ALTER COLUMN id SET DEFAULT nextval('public.home_section_item_id_seq'::regclass);


--
-- Name: instagram_reel id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.instagram_reel ALTER COLUMN id SET DEFAULT nextval('public.instagram_reel_id_seq'::regclass);


--
-- Name: interior_cost_addon_rule id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.interior_cost_addon_rule ALTER COLUMN id SET DEFAULT nextval('public.interior_cost_addon_rule_id_seq'::regclass);


--
-- Name: interior_cost_rule id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.interior_cost_rule ALTER COLUMN id SET DEFAULT nextval('public.interior_cost_rule_id_seq'::regclass);


--
-- Name: login_history id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.login_history ALTER COLUMN id SET DEFAULT nextval('public.login_history_id_seq'::regclass);


--
-- Name: otp_request_tracker id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.otp_request_tracker ALTER COLUMN id SET DEFAULT nextval('public.otp_request_tracker_id_seq'::regclass);


--
-- Name: otps id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.otps ALTER COLUMN id SET DEFAULT nextval('public.otps_id_seq'::regclass);


--
-- Name: project id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project ALTER COLUMN id SET DEFAULT nextval('public.project_id_seq'::regclass);


--
-- Name: project_amenity_progress id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_amenity_progress ALTER COLUMN id SET DEFAULT nextval('public.project_amenity_progress_id_seq'::regclass);


--
-- Name: project_analytics id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_analytics ALTER COLUMN id SET DEFAULT nextval('public.project_analytics_id_seq'::regclass);


--
-- Name: project_compliance_item id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_compliance_item ALTER COLUMN id SET DEFAULT nextval('public.project_compliance_item_id_seq'::regclass);


--
-- Name: project_connectivity id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_connectivity ALTER COLUMN id SET DEFAULT nextval('public.project_connectivity_id_seq'::regclass);


--
-- Name: project_connectivity_place id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_connectivity_place ALTER COLUMN id SET DEFAULT nextval('public.project_connectivity_place_id_seq'::regclass);


--
-- Name: project_construction_stage id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_construction_stage ALTER COLUMN id SET DEFAULT nextval('public.project_construction_stage_id_seq'::regclass);


--
-- Name: project_cost_breakdown id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_cost_breakdown ALTER COLUMN id SET DEFAULT nextval('public.project_cost_breakdown_id_seq'::regclass);


--
-- Name: project_floor_plan id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_floor_plan ALTER COLUMN id SET DEFAULT nextval('public.project_floor_plan_id_seq'::regclass);


--
-- Name: project_floor_plan_insight id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_floor_plan_insight ALTER COLUMN id SET DEFAULT nextval('public.project_floor_plan_insight_id_seq'::regclass);


--
-- Name: project_floor_plan_room_dimension id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_floor_plan_room_dimension ALTER COLUMN id SET DEFAULT nextval('public.project_floor_plan_room_dimension_id_seq'::regclass);


--
-- Name: project_highlight id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_highlight ALTER COLUMN id SET DEFAULT nextval('public.project_highlight_id_seq'::regclass);


--
-- Name: project_land_utilization id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_land_utilization ALTER COLUMN id SET DEFAULT nextval('public.project_land_utilization_id_seq'::regclass);


--
-- Name: project_location_score id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_location_score ALTER COLUMN id SET DEFAULT nextval('public.project_location_score_id_seq'::regclass);


--
-- Name: project_master_plan id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_master_plan ALTER COLUMN id SET DEFAULT nextval('public.project_master_plan_id_seq'::regclass);


--
-- Name: project_media id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_media ALTER COLUMN id SET DEFAULT nextval('public.project_media_id_seq'::regclass);


--
-- Name: project_meter_snapshot id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_meter_snapshot ALTER COLUMN id SET DEFAULT nextval('public.project_meter_snapshot_id_seq'::regclass);


--
-- Name: project_payment_milestone id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_payment_milestone ALTER COLUMN id SET DEFAULT nextval('public.project_payment_milestone_id_seq'::regclass);


--
-- Name: project_plan id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_plan ALTER COLUMN id SET DEFAULT nextval('public.project_plan_id_seq'::regclass);


--
-- Name: project_price_history id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_price_history ALTER COLUMN id SET DEFAULT nextval('public.project_price_history_id_seq'::regclass);


--
-- Name: project_review id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_review ALTER COLUMN id SET DEFAULT nextval('public.project_review_id_seq'::regclass);


--
-- Name: promo_banner id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.promo_banner ALTER COLUMN id SET DEFAULT nextval('public.promo_banner_id_seq'::regclass);


--
-- Name: promo_banner_slot_config id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.promo_banner_slot_config ALTER COLUMN id SET DEFAULT nextval('public.promo_banner_slot_config_id_seq'::regclass);


--
-- Name: provider_media id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.provider_media ALTER COLUMN id SET DEFAULT nextval('public.provider_media_id_seq'::regclass);


--
-- Name: provider_profile id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.provider_profile ALTER COLUMN id SET DEFAULT nextval('public.provider_profile_id_seq'::regclass);


--
-- Name: provider_project id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.provider_project ALTER COLUMN id SET DEFAULT nextval('public.provider_project_id_seq'::regclass);


--
-- Name: provider_project_media id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.provider_project_media ALTER COLUMN id SET DEFAULT nextval('public.provider_project_media_id_seq'::regclass);


--
-- Name: provider_service_area id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.provider_service_area ALTER COLUMN id SET DEFAULT nextval('public.provider_service_area_id_seq'::regclass);


--
-- Name: public_review_place id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.public_review_place ALTER COLUMN id SET DEFAULT nextval('public.public_review_place_id_seq'::regclass);


--
-- Name: public_review_sample id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.public_review_sample ALTER COLUMN id SET DEFAULT nextval('public.public_review_sample_id_seq'::regclass);


--
-- Name: public_review_summary id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.public_review_summary ALTER COLUMN id SET DEFAULT nextval('public.public_review_summary_id_seq'::regclass);


--
-- Name: refresh_tokens id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_tokens ALTER COLUMN id SET DEFAULT nextval('public.refresh_tokens_id_seq'::regclass);


--
-- Name: service_request id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.service_request ALTER COLUMN id SET DEFAULT nextval('public.service_request_id_seq'::regclass);


--
-- Name: service_request_interest id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.service_request_interest ALTER COLUMN id SET DEFAULT nextval('public.service_request_interest_id_seq'::regclass);


--
-- Name: stamp_duty_rule id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stamp_duty_rule ALTER COLUMN id SET DEFAULT nextval('public.stamp_duty_rule_id_seq'::regclass);


--
-- Name: user_favorite id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_favorite ALTER COLUMN id SET DEFAULT nextval('public.user_favorite_id_seq'::regclass);


--
-- Name: users id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users ALTER COLUMN id SET DEFAULT nextval('public.users_id_seq'::regclass);


--
-- Name: app_content_page app_content_page_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.app_content_page
    ADD CONSTRAINT app_content_page_pkey PRIMARY KEY (id);


--
-- Name: app_content_page app_content_page_slug_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.app_content_page
    ADD CONSTRAINT app_content_page_slug_key UNIQUE (slug);


--
-- Name: app_screen_content app_screen_content_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.app_screen_content
    ADD CONSTRAINT app_screen_content_pkey PRIMARY KEY (id);


--
-- Name: app_setting app_setting_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.app_setting
    ADD CONSTRAINT app_setting_pkey PRIMARY KEY (id);


--
-- Name: app_setting app_setting_setting_key_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.app_setting
    ADD CONSTRAINT app_setting_setting_key_key UNIQUE (setting_key);


--
-- Name: brand_category_link brand_category_link_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brand_category_link
    ADD CONSTRAINT brand_category_link_pkey PRIMARY KEY (id);


--
-- Name: brand_certificate brand_certificate_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brand_certificate
    ADD CONSTRAINT brand_certificate_pkey PRIMARY KEY (id);


--
-- Name: brand_collaboration brand_collaboration_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brand_collaboration
    ADD CONSTRAINT brand_collaboration_pkey PRIMARY KEY (id);


--
-- Name: brand_distributor brand_distributor_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brand_distributor
    ADD CONSTRAINT brand_distributor_pkey PRIMARY KEY (id);


--
-- Name: brand_faq brand_faq_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brand_faq
    ADD CONSTRAINT brand_faq_pkey PRIMARY KEY (id);


--
-- Name: brand_media brand_media_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brand_media
    ADD CONSTRAINT brand_media_pkey PRIMARY KEY (id);


--
-- Name: brand brand_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brand
    ADD CONSTRAINT brand_pkey PRIMARY KEY (id);


--
-- Name: brand_product_category brand_product_category_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brand_product_category
    ADD CONSTRAINT brand_product_category_pkey PRIMARY KEY (id);


--
-- Name: brand_sku brand_sku_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brand_sku
    ADD CONSTRAINT brand_sku_pkey PRIMARY KEY (id);


--
-- Name: builder_after_sales_upgrade builder_after_sales_upgrade_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.builder_after_sales_upgrade
    ADD CONSTRAINT builder_after_sales_upgrade_pkey PRIMARY KEY (id);


--
-- Name: builder_highlight_item builder_highlight_item_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.builder_highlight_item
    ADD CONSTRAINT builder_highlight_item_pkey PRIMARY KEY (id);


--
-- Name: builder_highlight_point builder_highlight_point_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.builder_highlight_point
    ADD CONSTRAINT builder_highlight_point_pkey PRIMARY KEY (id);


--
-- Name: builder_improvement_action builder_improvement_action_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.builder_improvement_action
    ADD CONSTRAINT builder_improvement_action_pkey PRIMARY KEY (id);


--
-- Name: builder_improvement_issue builder_improvement_issue_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.builder_improvement_issue
    ADD CONSTRAINT builder_improvement_issue_pkey PRIMARY KEY (id);


--
-- Name: builder_improvement_profile builder_improvement_profile_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.builder_improvement_profile
    ADD CONSTRAINT builder_improvement_profile_pkey PRIMARY KEY (id);


--
-- Name: builder_improvement_timeline builder_improvement_timeline_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.builder_improvement_timeline
    ADD CONSTRAINT builder_improvement_timeline_pkey PRIMARY KEY (id);


--
-- Name: builder builder_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.builder
    ADD CONSTRAINT builder_pkey PRIMARY KEY (id);


--
-- Name: business_event business_event_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.business_event
    ADD CONSTRAINT business_event_pkey PRIMARY KEY (id);


--
-- Name: business business_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.business
    ADD CONSTRAINT business_pkey PRIMARY KEY (id);


--
-- Name: category category_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.category
    ADD CONSTRAINT category_pkey PRIMARY KEY (id);


--
-- Name: circle_rate_rule circle_rate_rule_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.circle_rate_rule
    ADD CONSTRAINT circle_rate_rule_pkey PRIMARY KEY (id);


--
-- Name: city city_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.city
    ADD CONSTRAINT city_pkey PRIMARY KEY (id);


--
-- Name: company_award company_award_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_award
    ADD CONSTRAINT company_award_pkey PRIMARY KEY (id);


--
-- Name: company_brand_link company_brand_link_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_brand_link
    ADD CONSTRAINT company_brand_link_pkey PRIMARY KEY (id);


--
-- Name: company_certificate company_certificate_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_certificate
    ADD CONSTRAINT company_certificate_pkey PRIMARY KEY (id);


--
-- Name: company_media company_media_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_media
    ADD CONSTRAINT company_media_pkey PRIMARY KEY (id);


--
-- Name: company company_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company
    ADD CONSTRAINT company_pkey PRIMARY KEY (id);


--
-- Name: company_pricing_plan company_pricing_plan_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_pricing_plan
    ADD CONSTRAINT company_pricing_plan_pkey PRIMARY KEY (id);


--
-- Name: company_project company_project_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_project
    ADD CONSTRAINT company_project_pkey PRIMARY KEY (id);


--
-- Name: company_project company_project_slug_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_project
    ADD CONSTRAINT company_project_slug_key UNIQUE (slug);


--
-- Name: company company_slug_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company
    ADD CONSTRAINT company_slug_key UNIQUE (slug);


--
-- Name: company_stat company_stat_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_stat
    ADD CONSTRAINT company_stat_pkey PRIMARY KEY (id);


--
-- Name: content_version content_version_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.content_version
    ADD CONSTRAINT content_version_pkey PRIMARY KEY (key);


--
-- Name: dashboard_action_audit dashboard_action_audit_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dashboard_action_audit
    ADD CONSTRAINT dashboard_action_audit_pkey PRIMARY KEY (id);


--
-- Name: dashboard_content_review_history dashboard_content_review_history_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dashboard_content_review_history
    ADD CONSTRAINT dashboard_content_review_history_pkey PRIMARY KEY (id);


--
-- Name: dashboard_field_help dashboard_field_help_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dashboard_field_help
    ADD CONSTRAINT dashboard_field_help_pkey PRIMARY KEY (id);


--
-- Name: dashboard_field_review_issues dashboard_field_review_issues_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dashboard_field_review_issues
    ADD CONSTRAINT dashboard_field_review_issues_pkey PRIMARY KEY (id);


--
-- Name: dashboard_login_audit dashboard_login_audit_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dashboard_login_audit
    ADD CONSTRAINT dashboard_login_audit_pkey PRIMARY KEY (id);


--
-- Name: dashboard_refresh_tokens dashboard_refresh_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dashboard_refresh_tokens
    ADD CONSTRAINT dashboard_refresh_tokens_pkey PRIMARY KEY (id);


--
-- Name: dashboard_refresh_tokens dashboard_refresh_tokens_token_hash_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dashboard_refresh_tokens
    ADD CONSTRAINT dashboard_refresh_tokens_token_hash_key UNIQUE (token_hash);


--
-- Name: dashboard_scrape_candidate_builder dashboard_scrape_candidate_builder_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dashboard_scrape_candidate_builder
    ADD CONSTRAINT dashboard_scrape_candidate_builder_pkey PRIMARY KEY (id);


--
-- Name: dashboard_scrape_candidate_compliance_item dashboard_scrape_candidate_compliance_item_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dashboard_scrape_candidate_compliance_item
    ADD CONSTRAINT dashboard_scrape_candidate_compliance_item_pkey PRIMARY KEY (id);


--
-- Name: dashboard_scrape_candidate_cost_breakdown dashboard_scrape_candidate_cost_breakdown_candidate_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dashboard_scrape_candidate_cost_breakdown
    ADD CONSTRAINT dashboard_scrape_candidate_cost_breakdown_candidate_id_key UNIQUE (candidate_id);


--
-- Name: dashboard_scrape_candidate_cost_breakdown dashboard_scrape_candidate_cost_breakdown_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dashboard_scrape_candidate_cost_breakdown
    ADD CONSTRAINT dashboard_scrape_candidate_cost_breakdown_pkey PRIMARY KEY (id);


--
-- Name: dashboard_scrape_candidate_document dashboard_scrape_candidate_document_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dashboard_scrape_candidate_document
    ADD CONSTRAINT dashboard_scrape_candidate_document_pkey PRIMARY KEY (id);


--
-- Name: dashboard_scrape_candidate_field_result dashboard_scrape_candidate_field_result_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dashboard_scrape_candidate_field_result
    ADD CONSTRAINT dashboard_scrape_candidate_field_result_pkey PRIMARY KEY (id);


--
-- Name: dashboard_scrape_candidate_land_utilization dashboard_scrape_candidate_land_utilization_candidate_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dashboard_scrape_candidate_land_utilization
    ADD CONSTRAINT dashboard_scrape_candidate_land_utilization_candidate_id_key UNIQUE (candidate_id);


--
-- Name: dashboard_scrape_candidate_land_utilization dashboard_scrape_candidate_land_utilization_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dashboard_scrape_candidate_land_utilization
    ADD CONSTRAINT dashboard_scrape_candidate_land_utilization_pkey PRIMARY KEY (id);


--
-- Name: dashboard_scrape_candidate dashboard_scrape_candidate_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dashboard_scrape_candidate
    ADD CONSTRAINT dashboard_scrape_candidate_pkey PRIMARY KEY (id);


--
-- Name: dashboard_scrape_candidate_project dashboard_scrape_candidate_project_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dashboard_scrape_candidate_project
    ADD CONSTRAINT dashboard_scrape_candidate_project_pkey PRIMARY KEY (id);


--
-- Name: dashboard_scrape_candidate_raw_value dashboard_scrape_candidate_raw_value_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dashboard_scrape_candidate_raw_value
    ADD CONSTRAINT dashboard_scrape_candidate_raw_value_pkey PRIMARY KEY (id);


--
-- Name: dashboard_users dashboard_users_email_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dashboard_users
    ADD CONSTRAINT dashboard_users_email_key UNIQUE (email);


--
-- Name: dashboard_users dashboard_users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dashboard_users
    ADD CONSTRAINT dashboard_users_pkey PRIMARY KEY (id);


--
-- Name: distributor_media distributor_media_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.distributor_media
    ADD CONSTRAINT distributor_media_pkey PRIMARY KEY (id);


--
-- Name: distributor distributor_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.distributor
    ADD CONSTRAINT distributor_pkey PRIMARY KEY (id);


--
-- Name: favorites favorites_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.favorites
    ADD CONSTRAINT favorites_pkey PRIMARY KEY (id);


--
-- Name: featured_carousel_config featured_carousel_config_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.featured_carousel_config
    ADD CONSTRAINT featured_carousel_config_pkey PRIMARY KEY (id);


--
-- Name: feed_section_config feed_section_config_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.feed_section_config
    ADD CONSTRAINT feed_section_config_pkey PRIMARY KEY (id);


--
-- Name: feed_section_item feed_section_item_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.feed_section_item
    ADD CONSTRAINT feed_section_item_pkey PRIMARY KEY (id);


--
-- Name: guest_sessions guest_sessions_installation_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.guest_sessions
    ADD CONSTRAINT guest_sessions_installation_id_key UNIQUE (installation_id);


--
-- Name: guest_sessions guest_sessions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.guest_sessions
    ADD CONSTRAINT guest_sessions_pkey PRIMARY KEY (id);


--
-- Name: home_project_analytics home_project_analytics_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.home_project_analytics
    ADD CONSTRAINT home_project_analytics_pkey PRIMARY KEY (id);


--
-- Name: home_section_config home_section_config_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.home_section_config
    ADD CONSTRAINT home_section_config_pkey PRIMARY KEY (id);


--
-- Name: home_section_item home_section_item_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.home_section_item
    ADD CONSTRAINT home_section_item_pkey PRIMARY KEY (id);


--
-- Name: instagram_reel instagram_reel_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.instagram_reel
    ADD CONSTRAINT instagram_reel_pkey PRIMARY KEY (id);


--
-- Name: interior_cost_addon_rule interior_cost_addon_rule_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.interior_cost_addon_rule
    ADD CONSTRAINT interior_cost_addon_rule_pkey PRIMARY KEY (id);


--
-- Name: interior_cost_rule interior_cost_rule_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.interior_cost_rule
    ADD CONSTRAINT interior_cost_rule_pkey PRIMARY KEY (id);


--
-- Name: login_history login_history_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.login_history
    ADD CONSTRAINT login_history_pkey PRIMARY KEY (id);


--
-- Name: otp_request_tracker otp_request_tracker_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.otp_request_tracker
    ADD CONSTRAINT otp_request_tracker_pkey PRIMARY KEY (id);


--
-- Name: otps otps_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.otps
    ADD CONSTRAINT otps_pkey PRIMARY KEY (id);


--
-- Name: project_amenity_progress project_amenity_progress_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_amenity_progress
    ADD CONSTRAINT project_amenity_progress_pkey PRIMARY KEY (id);


--
-- Name: project_analytics project_analytics_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_analytics
    ADD CONSTRAINT project_analytics_pkey PRIMARY KEY (id);


--
-- Name: project_compliance_item project_compliance_item_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_compliance_item
    ADD CONSTRAINT project_compliance_item_pkey PRIMARY KEY (id);


--
-- Name: project_connectivity project_connectivity_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_connectivity
    ADD CONSTRAINT project_connectivity_pkey PRIMARY KEY (id);


--
-- Name: project_connectivity_place project_connectivity_place_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_connectivity_place
    ADD CONSTRAINT project_connectivity_place_pkey PRIMARY KEY (id);


--
-- Name: project_connectivity project_connectivity_project_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_connectivity
    ADD CONSTRAINT project_connectivity_project_id_key UNIQUE (project_id);


--
-- Name: project_construction_stage project_construction_stage_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_construction_stage
    ADD CONSTRAINT project_construction_stage_pkey PRIMARY KEY (id);


--
-- Name: project_cost_breakdown project_cost_breakdown_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_cost_breakdown
    ADD CONSTRAINT project_cost_breakdown_pkey PRIMARY KEY (id);


--
-- Name: project_cost_breakdown project_cost_breakdown_project_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_cost_breakdown
    ADD CONSTRAINT project_cost_breakdown_project_id_key UNIQUE (project_id);


--
-- Name: project_floor_plan_insight project_floor_plan_insight_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_floor_plan_insight
    ADD CONSTRAINT project_floor_plan_insight_pkey PRIMARY KEY (id);


--
-- Name: project_floor_plan project_floor_plan_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_floor_plan
    ADD CONSTRAINT project_floor_plan_pkey PRIMARY KEY (id);


--
-- Name: project_floor_plan_room_dimension project_floor_plan_room_dimension_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_floor_plan_room_dimension
    ADD CONSTRAINT project_floor_plan_room_dimension_pkey PRIMARY KEY (id);


--
-- Name: project_highlight project_highlight_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_highlight
    ADD CONSTRAINT project_highlight_pkey PRIMARY KEY (id);


--
-- Name: project_land_utilization project_land_utilization_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_land_utilization
    ADD CONSTRAINT project_land_utilization_pkey PRIMARY KEY (id);


--
-- Name: project_land_utilization project_land_utilization_project_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_land_utilization
    ADD CONSTRAINT project_land_utilization_project_id_key UNIQUE (project_id);


--
-- Name: project_location_score project_location_score_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_location_score
    ADD CONSTRAINT project_location_score_pkey PRIMARY KEY (id);


--
-- Name: project_location_score project_location_score_project_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_location_score
    ADD CONSTRAINT project_location_score_project_id_key UNIQUE (project_id);


--
-- Name: project_master_plan project_master_plan_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_master_plan
    ADD CONSTRAINT project_master_plan_pkey PRIMARY KEY (id);


--
-- Name: project_media project_media_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_media
    ADD CONSTRAINT project_media_pkey PRIMARY KEY (id);


--
-- Name: project_meter_snapshot project_meter_snapshot_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_meter_snapshot
    ADD CONSTRAINT project_meter_snapshot_pkey PRIMARY KEY (id);


--
-- Name: project_meter_snapshot project_meter_snapshot_project_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_meter_snapshot
    ADD CONSTRAINT project_meter_snapshot_project_id_key UNIQUE (project_id);


--
-- Name: project_payment_milestone project_payment_milestone_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_payment_milestone
    ADD CONSTRAINT project_payment_milestone_pkey PRIMARY KEY (id);


--
-- Name: project project_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project
    ADD CONSTRAINT project_pkey PRIMARY KEY (id);


--
-- Name: project_plan project_plan_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_plan
    ADD CONSTRAINT project_plan_pkey PRIMARY KEY (id);


--
-- Name: project_price_history project_price_history_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_price_history
    ADD CONSTRAINT project_price_history_pkey PRIMARY KEY (id);


--
-- Name: project_review project_review_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_review
    ADD CONSTRAINT project_review_pkey PRIMARY KEY (id);


--
-- Name: project project_slug_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project
    ADD CONSTRAINT project_slug_key UNIQUE (slug);


--
-- Name: promo_banner promo_banner_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.promo_banner
    ADD CONSTRAINT promo_banner_pkey PRIMARY KEY (id);


--
-- Name: promo_banner_slot_config promo_banner_slot_config_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.promo_banner_slot_config
    ADD CONSTRAINT promo_banner_slot_config_pkey PRIMARY KEY (id);


--
-- Name: provider_media provider_media_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.provider_media
    ADD CONSTRAINT provider_media_pkey PRIMARY KEY (id);


--
-- Name: provider_profile provider_profile_business_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.provider_profile
    ADD CONSTRAINT provider_profile_business_id_key UNIQUE (business_id);


--
-- Name: provider_profile provider_profile_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.provider_profile
    ADD CONSTRAINT provider_profile_pkey PRIMARY KEY (id);


--
-- Name: provider_profile provider_profile_user_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.provider_profile
    ADD CONSTRAINT provider_profile_user_id_key UNIQUE (user_id);


--
-- Name: provider_project_media provider_project_media_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.provider_project_media
    ADD CONSTRAINT provider_project_media_pkey PRIMARY KEY (id);


--
-- Name: provider_project provider_project_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.provider_project
    ADD CONSTRAINT provider_project_pkey PRIMARY KEY (id);


--
-- Name: provider_service_area provider_service_area_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.provider_service_area
    ADD CONSTRAINT provider_service_area_pkey PRIMARY KEY (id);


--
-- Name: public_review_place public_review_place_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.public_review_place
    ADD CONSTRAINT public_review_place_pkey PRIMARY KEY (id);


--
-- Name: public_review_sample public_review_sample_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.public_review_sample
    ADD CONSTRAINT public_review_sample_pkey PRIMARY KEY (id);


--
-- Name: public_review_summary public_review_summary_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.public_review_summary
    ADD CONSTRAINT public_review_summary_pkey PRIMARY KEY (id);


--
-- Name: refresh_tokens refresh_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_pkey PRIMARY KEY (id);


--
-- Name: service_request_category service_request_category_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.service_request_category
    ADD CONSTRAINT service_request_category_pkey PRIMARY KEY (service_request_id, category_id);


--
-- Name: service_request_interest service_request_interest_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.service_request_interest
    ADD CONSTRAINT service_request_interest_pkey PRIMARY KEY (id);


--
-- Name: service_request service_request_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.service_request
    ADD CONSTRAINT service_request_pkey PRIMARY KEY (id);


--
-- Name: stamp_duty_rule stamp_duty_rule_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stamp_duty_rule
    ADD CONSTRAINT stamp_duty_rule_pkey PRIMARY KEY (id);


--
-- Name: brand_category_link uk_brand_category_link_brand_category; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brand_category_link
    ADD CONSTRAINT uk_brand_category_link_brand_category UNIQUE (brand_id, category_id);


--
-- Name: brand_sku uk_brand_sku_brand_slug; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brand_sku
    ADD CONSTRAINT uk_brand_sku_brand_slug UNIQUE (brand_id, slug);


--
-- Name: brand uk_brand_slug; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brand
    ADD CONSTRAINT uk_brand_slug UNIQUE (slug);


--
-- Name: company_brand_link uk_company_brand_link_company_brand; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_brand_link
    ADD CONSTRAINT uk_company_brand_link_company_brand UNIQUE (company_id, brand_id);


--
-- Name: favorites uk_favorites_user_business; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.favorites
    ADD CONSTRAINT uk_favorites_user_business UNIQUE (user_id, business_id);


--
-- Name: otp_request_tracker uk_otp_tracker_phone; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.otp_request_tracker
    ADD CONSTRAINT uk_otp_tracker_phone UNIQUE (phone_number);


--
-- Name: project_construction_stage uk_project_stage_code; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_construction_stage
    ADD CONSTRAINT uk_project_stage_code UNIQUE (project_id, stage_code);


--
-- Name: service_request_interest uk_sri_req_provider; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.service_request_interest
    ADD CONSTRAINT uk_sri_req_provider UNIQUE (service_request_id, provider_id);


--
-- Name: user_favorite uk_user_favorite_user_target; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_favorite
    ADD CONSTRAINT uk_user_favorite_user_target UNIQUE (user_id, target_type, target_id);


--
-- Name: brand_distributor uq_brand_distributor; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brand_distributor
    ADD CONSTRAINT uq_brand_distributor UNIQUE (brand_id, distributor_id);


--
-- Name: dashboard_field_help uq_dashboard_field_help_module_field; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dashboard_field_help
    ADD CONSTRAINT uq_dashboard_field_help_module_field UNIQUE (module, field_key);


--
-- Name: featured_carousel_config uq_featured_carousel_unique; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.featured_carousel_config
    ADD CONSTRAINT uq_featured_carousel_unique UNIQUE (category_id, city_id, "position");


--
-- Name: public_review_place uq_public_review_place_target_google; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.public_review_place
    ADD CONSTRAINT uq_public_review_place_target_google UNIQUE (target_type, target_id, google_place_id);


--
-- Name: public_review_summary uq_public_review_summary_place; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.public_review_summary
    ADD CONSTRAINT uq_public_review_summary_place UNIQUE (review_place_id);


--
-- Name: user_favorite user_favorite_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_favorite
    ADD CONSTRAINT user_favorite_pkey PRIMARY KEY (id);


--
-- Name: users users_email_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_email_key UNIQUE (email);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: idx_app_screen_content_lookup; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_app_screen_content_lookup ON public.app_screen_content USING btree (screen_key, placement, enabled, sort_order, id DESC);


--
-- Name: idx_app_screen_content_window; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_app_screen_content_window ON public.app_screen_content USING btree (start_at, end_at);


--
-- Name: idx_area_city; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_area_city ON public.provider_service_area USING btree (city_id);


--
-- Name: idx_area_provider; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_area_provider ON public.provider_service_area USING btree (provider_id);


--
-- Name: idx_banner_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_banner_active ON public.promo_banner USING btree (is_active);


--
-- Name: idx_banner_category; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_banner_category ON public.promo_banner USING btree (category_id);


--
-- Name: idx_brand_category; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_brand_category ON public.brand USING btree (category_id);


--
-- Name: idx_brand_category_link_brand_sort; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_brand_category_link_brand_sort ON public.brand_category_link USING btree (brand_id, sort_order, id);


--
-- Name: idx_brand_category_link_category; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_brand_category_link_category ON public.brand_category_link USING btree (category_id);


--
-- Name: idx_brand_certificate_brand_sort; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_brand_certificate_brand_sort ON public.brand_certificate USING btree (brand_id, sort_order, id);


--
-- Name: idx_brand_collaboration_brand; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_brand_collaboration_brand ON public.brand_collaboration USING btree (brand_id, active, deleted, sort_order);


--
-- Name: idx_brand_collaboration_by_builder; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_brand_collaboration_by_builder ON public.brand_collaboration USING btree (builder_id, active, deleted, sort_order) WHERE (builder_id IS NOT NULL);


--
-- Name: idx_brand_collaboration_by_business; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_brand_collaboration_by_business ON public.brand_collaboration USING btree (business_id, active, deleted, sort_order) WHERE (business_id IS NOT NULL);


--
-- Name: idx_brand_collaboration_by_company; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_brand_collaboration_by_company ON public.brand_collaboration USING btree (company_id, active, deleted, sort_order) WHERE (company_id IS NOT NULL);


--
-- Name: idx_brand_collaboration_by_company_project; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_brand_collaboration_by_company_project ON public.brand_collaboration USING btree (company_project_id, active, deleted, sort_order) WHERE (company_project_id IS NOT NULL);


--
-- Name: idx_brand_collaboration_by_project; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_brand_collaboration_by_project ON public.brand_collaboration USING btree (project_id, active, deleted, sort_order) WHERE (project_id IS NOT NULL);


--
-- Name: idx_brand_distributor_brand; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_brand_distributor_brand ON public.brand_distributor USING btree (brand_id, active, deleted, priority);


--
-- Name: idx_brand_distributor_distributor; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_brand_distributor_distributor ON public.brand_distributor USING btree (distributor_id, active, deleted);


--
-- Name: idx_brand_faq_brand_sort; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_brand_faq_brand_sort ON public.brand_faq USING btree (brand_id, sort_order, id);


--
-- Name: idx_brand_media_sku; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_brand_media_sku ON public.brand_media USING btree (brand_sku_id);


--
-- Name: idx_brand_product_category_brand_sort; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_brand_product_category_brand_sort ON public.brand_product_category USING btree (brand_id, sort_order, id);


--
-- Name: idx_brand_sku_brand_public; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_brand_sku_brand_public ON public.brand_sku USING btree (brand_id, published, active, deleted, priority);


--
-- Name: idx_brand_sku_category; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_brand_sku_category ON public.brand_sku USING btree (category_id);


--
-- Name: idx_brand_sku_product_category; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_brand_sku_product_category ON public.brand_sku USING btree (product_category_id);


--
-- Name: idx_builder_after_sales_upgrade_profile; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_builder_after_sales_upgrade_profile ON public.builder_after_sales_upgrade USING btree (profile_id);


--
-- Name: idx_builder_after_sales_upgrade_public_sort; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_builder_after_sales_upgrade_public_sort ON public.builder_after_sales_upgrade USING btree (profile_id, published, active, deleted, display_order, id);


--
-- Name: idx_builder_city_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_builder_city_id ON public.builder USING btree (city_id);


--
-- Name: idx_builder_highlight_item_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_builder_highlight_item_active ON public.builder_highlight_item USING btree (active);


--
-- Name: idx_builder_highlight_item_builder; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_builder_highlight_item_builder ON public.builder_highlight_item USING btree (builder_id);


--
-- Name: idx_builder_highlight_item_deleted_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_builder_highlight_item_deleted_at ON public.builder_highlight_item USING btree (deleted_at);


--
-- Name: idx_builder_highlight_item_featured; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_builder_highlight_item_featured ON public.builder_highlight_item USING btree (featured);


--
-- Name: idx_builder_highlight_item_public_listing; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_builder_highlight_item_public_listing ON public.builder_highlight_item USING btree (builder_id, status, public_visible, active, deleted_at, highlight_type);


--
-- Name: idx_builder_highlight_item_public_sort; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_builder_highlight_item_public_sort ON public.builder_highlight_item USING btree (builder_id, highlight_type, status, public_visible, active, deleted_at, featured DESC, sort_order, published_at DESC, id DESC);


--
-- Name: idx_builder_highlight_item_public_visible; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_builder_highlight_item_public_visible ON public.builder_highlight_item USING btree (public_visible);


--
-- Name: idx_builder_highlight_item_published_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_builder_highlight_item_published_at ON public.builder_highlight_item USING btree (published_at);


--
-- Name: idx_builder_highlight_item_sort_order; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_builder_highlight_item_sort_order ON public.builder_highlight_item USING btree (sort_order);


--
-- Name: idx_builder_highlight_item_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_builder_highlight_item_status ON public.builder_highlight_item USING btree (status);


--
-- Name: idx_builder_highlight_item_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_builder_highlight_item_type ON public.builder_highlight_item USING btree (highlight_type);


--
-- Name: idx_builder_highlight_point_item_sort; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_builder_highlight_point_item_sort ON public.builder_highlight_point USING btree (highlight_item_id, active, display_order, id);


--
-- Name: idx_builder_improvement_action_profile; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_builder_improvement_action_profile ON public.builder_improvement_action USING btree (profile_id);


--
-- Name: idx_builder_improvement_action_public_sort; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_builder_improvement_action_public_sort ON public.builder_improvement_action USING btree (profile_id, published, active, deleted, display_order, id);


--
-- Name: idx_builder_improvement_issue_profile; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_builder_improvement_issue_profile ON public.builder_improvement_issue USING btree (profile_id);


--
-- Name: idx_builder_improvement_issue_public_sort; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_builder_improvement_issue_public_sort ON public.builder_improvement_issue USING btree (profile_id, published, active, deleted, display_order, id);


--
-- Name: idx_builder_improvement_issue_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_builder_improvement_issue_status ON public.builder_improvement_issue USING btree (profile_id, status);


--
-- Name: idx_builder_improvement_profile_builder; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_builder_improvement_profile_builder ON public.builder_improvement_profile USING btree (builder_id);


--
-- Name: idx_builder_improvement_profile_project; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_builder_improvement_profile_project ON public.builder_improvement_profile USING btree (project_id);


--
-- Name: idx_builder_improvement_profile_public; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_builder_improvement_profile_public ON public.builder_improvement_profile USING btree (builder_id, project_id, published, active, deleted);


--
-- Name: idx_builder_improvement_profile_review_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_builder_improvement_profile_review_status ON public.builder_improvement_profile USING btree (review_status);


--
-- Name: idx_builder_improvement_timeline_profile; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_builder_improvement_timeline_profile ON public.builder_improvement_timeline USING btree (profile_id);


--
-- Name: idx_builder_improvement_timeline_public_sort; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_builder_improvement_timeline_public_sort ON public.builder_improvement_timeline USING btree (profile_id, published, active, deleted, display_order, id);


--
-- Name: idx_builder_name_trgm; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_builder_name_trgm ON public.builder USING gin (lower((name)::text) public.gin_trgm_ops);


--
-- Name: idx_builder_public; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_builder_public ON public.builder USING btree (published, active, deleted);


--
-- Name: idx_business_category; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_business_category ON public.business USING btree (category_id);


--
-- Name: idx_business_city; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_business_city ON public.business USING btree (city_id);


--
-- Name: idx_business_event_business; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_business_event_business ON public.business_event USING btree (business_id);


--
-- Name: idx_business_event_business_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_business_event_business_created_at ON public.business_event USING btree (business_id, created_at DESC);


--
-- Name: idx_business_event_city_cat_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_business_event_city_cat_created_at ON public.business_event USING btree (city_id, category_id, created_at DESC);


--
-- Name: idx_business_event_created; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_business_event_created ON public.business_event USING btree (created_at);


--
-- Name: idx_business_event_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_business_event_type ON public.business_event USING btree (event_type);


--
-- Name: idx_business_event_type_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_business_event_type_created_at ON public.business_event USING btree (event_type, created_at DESC);


--
-- Name: idx_business_name_trgm; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_business_name_trgm ON public.business USING btree (lower((name)::text));


--
-- Name: idx_business_owner_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_business_owner_user_id ON public.business USING btree (owner_user_id);


--
-- Name: idx_business_sponsored_priority; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_business_sponsored_priority ON public.business USING btree (sponsored DESC, sponsored_priority DESC);


--
-- Name: idx_category_parent_priority; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_category_parent_priority ON public.category USING btree (parent_id, priority);


--
-- Name: idx_city_coordinates; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_city_coordinates ON public.city USING btree (latitude, longitude);


--
-- Name: idx_city_homepage_featured; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_city_homepage_featured ON public.city USING btree (active, homepage_featured, display_order, id);


--
-- Name: idx_company_award_company; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_company_award_company ON public.company_award USING btree (company_id);


--
-- Name: idx_company_brand_link_company_sort; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_company_brand_link_company_sort ON public.company_brand_link USING btree (company_id, sort_order, id);


--
-- Name: idx_company_certificate_company; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_company_certificate_company ON public.company_certificate USING btree (company_id);


--
-- Name: idx_company_city; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_company_city ON public.company USING btree (city_id);


--
-- Name: idx_company_media_company; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_company_media_company ON public.company_media USING btree (company_id);


--
-- Name: idx_company_media_lookup; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_company_media_lookup ON public.company_media USING btree (company_id, usage_type, deleted, active, public_visible);


--
-- Name: idx_company_name_trgm; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_company_name_trgm ON public.company USING gin (lower((name)::text) public.gin_trgm_ops);


--
-- Name: idx_company_pricing_plan_company; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_company_pricing_plan_company ON public.company_pricing_plan USING btree (company_id);


--
-- Name: idx_company_pricing_plan_lookup; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_company_pricing_plan_lookup ON public.company_pricing_plan USING btree (company_id, pricing_type, deleted, active, public_visible);


--
-- Name: idx_company_project_company; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_company_project_company ON public.company_project USING btree (company_id);


--
-- Name: idx_company_project_priority; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_company_project_priority ON public.company_project USING btree (priority, id);


--
-- Name: idx_company_project_pub_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_company_project_pub_active ON public.company_project USING btree (published, active, deleted);


--
-- Name: idx_company_stat_company; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_company_stat_company ON public.company_stat USING btree (company_id);


--
-- Name: idx_company_type_trgm; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_company_type_trgm ON public.company USING gin (lower((company_type)::text) public.gin_trgm_ops);


--
-- Name: idx_cr_city; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_cr_city ON public.circle_rate_rule USING btree (city_name);


--
-- Name: idx_cr_city_locality; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_cr_city_locality ON public.circle_rate_rule USING btree (city_name, locality_name);


--
-- Name: idx_cr_effective; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_cr_effective ON public.circle_rate_rule USING btree (effective_from, effective_to);


--
-- Name: idx_cr_lookup; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_cr_lookup ON public.circle_rate_rule USING btree (state_name, city_name, locality_name, property_type, active);


--
-- Name: idx_dashboard_action_audit_action; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_dashboard_action_audit_action ON public.dashboard_action_audit USING btree (action);


--
-- Name: idx_dashboard_action_audit_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_dashboard_action_audit_created_at ON public.dashboard_action_audit USING btree (created_at);


--
-- Name: idx_dashboard_action_audit_entity; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_dashboard_action_audit_entity ON public.dashboard_action_audit USING btree (entity_type, entity_id);


--
-- Name: idx_dashboard_action_audit_project_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_dashboard_action_audit_project_id ON public.dashboard_action_audit USING btree (project_id);


--
-- Name: idx_dashboard_action_audit_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_dashboard_action_audit_user_id ON public.dashboard_action_audit USING btree (dashboard_user_id);


--
-- Name: idx_dashboard_field_help_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_dashboard_field_help_active ON public.dashboard_field_help USING btree (active);


--
-- Name: idx_dashboard_field_help_module; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_dashboard_field_help_module ON public.dashboard_field_help USING btree (module);


--
-- Name: idx_dashboard_field_review_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_dashboard_field_review_active ON public.dashboard_field_review_issues USING btree (active);


--
-- Name: idx_dashboard_field_review_entity; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_dashboard_field_review_entity ON public.dashboard_field_review_issues USING btree (entity_type, entity_id);


--
-- Name: idx_dashboard_field_review_field_key; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_dashboard_field_review_field_key ON public.dashboard_field_review_issues USING btree (field_key);


--
-- Name: idx_dashboard_field_review_fixed_by; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_dashboard_field_review_fixed_by ON public.dashboard_field_review_issues USING btree (fixed_by);


--
-- Name: idx_dashboard_field_review_marked_by; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_dashboard_field_review_marked_by ON public.dashboard_field_review_issues USING btree (marked_by);


--
-- Name: idx_dashboard_field_review_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_dashboard_field_review_status ON public.dashboard_field_review_issues USING btree (status);


--
-- Name: idx_dashboard_login_audit_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_dashboard_login_audit_created_at ON public.dashboard_login_audit USING btree (created_at);


--
-- Name: idx_dashboard_login_audit_email; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_dashboard_login_audit_email ON public.dashboard_login_audit USING btree (email);


--
-- Name: idx_dashboard_login_audit_success; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_dashboard_login_audit_success ON public.dashboard_login_audit USING btree (success);


--
-- Name: idx_dashboard_login_audit_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_dashboard_login_audit_user_id ON public.dashboard_login_audit USING btree (dashboard_user_id);


--
-- Name: idx_dashboard_refresh_tokens_expires_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_dashboard_refresh_tokens_expires_at ON public.dashboard_refresh_tokens USING btree (expires_at);


--
-- Name: idx_dashboard_refresh_tokens_revoked; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_dashboard_refresh_tokens_revoked ON public.dashboard_refresh_tokens USING btree (revoked);


--
-- Name: idx_dashboard_refresh_tokens_token_hash; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_dashboard_refresh_tokens_token_hash ON public.dashboard_refresh_tokens USING btree (token_hash);


--
-- Name: idx_dashboard_refresh_tokens_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_dashboard_refresh_tokens_user_id ON public.dashboard_refresh_tokens USING btree (dashboard_user_id);


--
-- Name: idx_dashboard_review_history_action_by; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_dashboard_review_history_action_by ON public.dashboard_content_review_history USING btree (action_by);


--
-- Name: idx_dashboard_review_history_action_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_dashboard_review_history_action_type ON public.dashboard_content_review_history USING btree (action_type);


--
-- Name: idx_dashboard_review_history_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_dashboard_review_history_created_at ON public.dashboard_content_review_history USING btree (created_at);


--
-- Name: idx_dashboard_review_history_entity; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_dashboard_review_history_entity ON public.dashboard_content_review_history USING btree (entity_type, entity_id);


--
-- Name: idx_dashboard_users_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_dashboard_users_active ON public.dashboard_users USING btree (active);


--
-- Name: idx_dashboard_users_email; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_dashboard_users_email ON public.dashboard_users USING btree (email);


--
-- Name: idx_dashboard_users_role; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_dashboard_users_role ON public.dashboard_users USING btree (role);


--
-- Name: idx_distributor_city_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_distributor_city_active ON public.distributor USING btree (city_id, active, deleted);


--
-- Name: idx_distributor_media_distributor; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_distributor_media_distributor ON public.distributor_media USING btree (distributor_id, deleted, active, sort_order);


--
-- Name: idx_distributor_phone; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_distributor_phone ON public.distributor USING btree (phone);


--
-- Name: idx_distributor_whatsapp; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_distributor_whatsapp ON public.distributor USING btree (whatsapp);


--
-- Name: idx_favorites_business_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_favorites_business_id ON public.favorites USING btree (business_id);


--
-- Name: idx_favorites_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_favorites_user_id ON public.favorites USING btree (user_id);


--
-- Name: idx_feed_section_category_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_feed_section_category_id ON public.feed_section_config USING btree (category_id);


--
-- Name: idx_feed_section_city_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_feed_section_city_id ON public.feed_section_config USING btree (city_id);


--
-- Name: idx_feed_section_item_config; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_feed_section_item_config ON public.feed_section_item USING btree (config_id);


--
-- Name: idx_feed_section_screen; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_feed_section_screen ON public.feed_section_config USING btree (screen);


--
-- Name: idx_home_project_analytics_cat_builder; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_home_project_analytics_cat_builder ON public.home_project_analytics USING btree (category_id, builder_id);


--
-- Name: idx_home_section_item_config_sort; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_home_section_item_config_sort ON public.home_section_item USING btree (config_id, sort_order, id);


--
-- Name: idx_icar_city; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_icar_city ON public.interior_cost_addon_rule USING btree (city_name);


--
-- Name: idx_icar_company; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_icar_company ON public.interior_cost_addon_rule USING btree (company_id, active);


--
-- Name: idx_icar_effective; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_icar_effective ON public.interior_cost_addon_rule USING btree (effective_from, effective_to);


--
-- Name: idx_icar_lookup; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_icar_lookup ON public.interior_cost_addon_rule USING btree (city_name, package_type, addon_type, active);


--
-- Name: idx_icr_city; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_icr_city ON public.interior_cost_rule USING btree (city_name);


--
-- Name: idx_icr_company; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_icr_company ON public.interior_cost_rule USING btree (company_id, active);


--
-- Name: idx_icr_effective; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_icr_effective ON public.interior_cost_rule USING btree (effective_from, effective_to);


--
-- Name: idx_icr_lookup; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_icr_lookup ON public.interior_cost_rule USING btree (city_name, property_type, package_type, scope_type, bhk_type, active);


--
-- Name: idx_instagram_reel_active_published; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_instagram_reel_active_published ON public.instagram_reel USING btree (active, deleted, published_at DESC);


--
-- Name: idx_instagram_reel_active_trending; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_instagram_reel_active_trending ON public.instagram_reel USING btree (active, deleted, trending_score DESC);


--
-- Name: idx_instagram_reel_active_view_count; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_instagram_reel_active_view_count ON public.instagram_reel USING btree (active, deleted, view_count DESC);


--
-- Name: idx_instagram_reel_category_override; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_instagram_reel_category_override ON public.instagram_reel USING btree (category_override);


--
-- Name: idx_instagram_reel_display_order; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_instagram_reel_display_order ON public.instagram_reel USING btree (display_order);


--
-- Name: idx_instagram_reel_missing_cached_thumbnail; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_instagram_reel_missing_cached_thumbnail ON public.instagram_reel USING btree (active, deleted, cached_thumbnail_url) WHERE ((active = true) AND (deleted = false) AND (cached_thumbnail_url IS NULL));


--
-- Name: idx_media_project; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_media_project ON public.provider_project_media USING btree (project_id);


--
-- Name: idx_otp_tracker_blocked_until; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_otp_tracker_blocked_until ON public.otp_request_tracker USING btree (blocked_until);


--
-- Name: idx_otp_tracker_cooldown_until; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_otp_tracker_cooldown_until ON public.otp_request_tracker USING btree (cooldown_until);


--
-- Name: idx_otp_tracker_phone; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_otp_tracker_phone ON public.otp_request_tracker USING btree (phone_number);


--
-- Name: idx_otps_user_verified_expires; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_otps_user_verified_expires ON public.otps USING btree (user_id, verified, expires_at);


--
-- Name: idx_pfp_insight_floor_plan_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_pfp_insight_floor_plan_id ON public.project_floor_plan_insight USING btree (floor_plan_id);


--
-- Name: idx_pfp_insight_floor_plan_public; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_pfp_insight_floor_plan_public ON public.project_floor_plan_insight USING btree (floor_plan_id, public_visible, active, deleted);


--
-- Name: idx_pfp_room_dim_floor_plan_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_pfp_room_dim_floor_plan_active ON public.project_floor_plan_room_dimension USING btree (floor_plan_id, active, deleted);


--
-- Name: idx_pfp_room_dim_floor_plan_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_pfp_room_dim_floor_plan_id ON public.project_floor_plan_room_dimension USING btree (floor_plan_id);


--
-- Name: idx_proj_media_active_lookup; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_proj_media_active_lookup ON public.project_media USING btree (project_id, active, deleted, sort_order);


--
-- Name: idx_proj_media_project_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_proj_media_project_id ON public.project_media USING btree (project_id);


--
-- Name: idx_project_amenity_progress_project_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_project_amenity_progress_project_id ON public.project_amenity_progress USING btree (project_id);


--
-- Name: idx_project_amenity_project_category; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_project_amenity_project_category ON public.project_amenity_progress USING btree (project_id, category, active, public_visible, display_order);


--
-- Name: idx_project_amenity_public_lookup; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_project_amenity_public_lookup ON public.project_amenity_progress USING btree (project_id, public_visible, active);


--
-- Name: idx_project_builder_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_project_builder_id ON public.project USING btree (builder_id);


--
-- Name: idx_project_city_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_project_city_id ON public.project USING btree (city_id);


--
-- Name: idx_project_city_public; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_project_city_public ON public.project USING btree (city_id, published, active, deleted, priority, id);


--
-- Name: idx_project_compliance_project_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_project_compliance_project_id ON public.project_compliance_item USING btree (project_id);


--
-- Name: idx_project_connectivity_place_external; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_project_connectivity_place_external ON public.project_connectivity_place USING btree (provider, external_place_id);


--
-- Name: idx_project_connectivity_place_project_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_project_connectivity_place_project_active ON public.project_connectivity_place USING btree (project_id, active, deleted, sort_order, id);


--
-- Name: idx_project_connectivity_place_project_category; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_project_connectivity_place_project_category ON public.project_connectivity_place USING btree (project_id, category, active, deleted, sort_order);


--
-- Name: idx_project_connectivity_place_project_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_project_connectivity_place_project_type ON public.project_connectivity_place USING btree (project_id, place_type, active, deleted, sort_order);


--
-- Name: idx_project_connectivity_project_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_project_connectivity_project_active ON public.project_connectivity USING btree (project_id, active, deleted);


--
-- Name: idx_project_construction_stage_project_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_project_construction_stage_project_id ON public.project_construction_stage USING btree (project_id);


--
-- Name: idx_project_coordinates; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_project_coordinates ON public.project USING btree (latitude, longitude);


--
-- Name: idx_project_created_by_dashboard_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_project_created_by_dashboard_user_id ON public.project USING btree (created_by_dashboard_user_id);


--
-- Name: idx_project_floor_plan_project_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_project_floor_plan_project_id ON public.project_floor_plan USING btree (project_id);


--
-- Name: idx_project_highlight_project; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_project_highlight_project ON public.project_highlight USING btree (project_id);


--
-- Name: idx_project_highlight_project_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_project_highlight_project_active ON public.project_highlight USING btree (project_id, active, deleted, sort_order, id);


--
-- Name: idx_project_master_plan_project_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_project_master_plan_project_id ON public.project_master_plan USING btree (project_id);


--
-- Name: idx_project_master_plan_public_lookup; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_project_master_plan_public_lookup ON public.project_master_plan USING btree (project_id, active, deleted);


--
-- Name: idx_project_meter_snapshot_project_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_project_meter_snapshot_project_id ON public.project_meter_snapshot USING btree (project_id);


--
-- Name: idx_project_name_trgm; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_project_name_trgm ON public.project USING gin (lower((name)::text) public.gin_trgm_ops);


--
-- Name: idx_project_payment_milestone_project_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_project_payment_milestone_project_id ON public.project_payment_milestone USING btree (project_id);


--
-- Name: idx_project_price_history_project_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_project_price_history_project_id ON public.project_price_history USING btree (project_id);


--
-- Name: idx_project_property_types_project_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_project_property_types_project_id ON public.project_property_types USING btree (project_id);


--
-- Name: idx_project_provider; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_project_provider ON public.provider_project USING btree (provider_id);


--
-- Name: idx_project_review_display_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_project_review_display_status ON public.project_review USING btree (display_status);


--
-- Name: idx_project_review_project_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_project_review_project_id ON public.project_review USING btree (project_id);


--
-- Name: idx_project_review_project_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_project_review_project_status ON public.project_review USING btree (project_id, verification_status);


--
-- Name: idx_project_review_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_project_review_status ON public.project USING btree (review_status);


--
-- Name: idx_project_review_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_project_review_user_id ON public.project_review USING btree (user_id);


--
-- Name: idx_project_review_user_project; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_project_review_user_project ON public.project_review USING btree (user_id, project_id);


--
-- Name: idx_project_review_user_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_project_review_user_status ON public.project_review USING btree (user_id, verification_status);


--
-- Name: idx_project_reviewed_by_dashboard_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_project_reviewed_by_dashboard_user_id ON public.project USING btree (reviewed_by_dashboard_user_id);


--
-- Name: idx_project_submitted_by_dashboard_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_project_submitted_by_dashboard_user_id ON public.project USING btree (submitted_by_dashboard_user_id);


--
-- Name: idx_promo_slot_category_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_promo_slot_category_id ON public.promo_banner_slot_config USING btree (home_category_id);


--
-- Name: idx_promo_slot_city_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_promo_slot_city_id ON public.promo_banner_slot_config USING btree (city_id);


--
-- Name: idx_promo_slot_screen; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_promo_slot_screen ON public.promo_banner_slot_config USING btree (screen);


--
-- Name: idx_promo_slot_screen_category_active_priority; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_promo_slot_screen_category_active_priority ON public.promo_banner_slot_config USING btree (screen, home_category_id, is_active, priority, id);


--
-- Name: idx_promo_slot_screen_category_city_active_priority; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_promo_slot_screen_category_city_active_priority ON public.promo_banner_slot_config USING btree (screen, home_category_id, city_id, is_active, priority, id);


--
-- Name: idx_provider_category; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_provider_category ON public.provider_profile USING btree (primary_category_id);


--
-- Name: idx_provider_media_provider_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_provider_media_provider_id ON public.provider_media USING btree (provider_id);


--
-- Name: idx_provider_media_provider_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_provider_media_provider_type ON public.provider_media USING btree (provider_id, media_type);


--
-- Name: idx_public_review_place_active_lookup; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_public_review_place_active_lookup ON public.public_review_place USING btree (target_type, target_id, active, deleted);


--
-- Name: idx_public_review_place_google_place_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_public_review_place_google_place_id ON public.public_review_place USING btree (google_place_id);


--
-- Name: idx_public_review_place_target; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_public_review_place_target ON public.public_review_place USING btree (target_type, target_id);


--
-- Name: idx_public_review_place_target_fetch_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_public_review_place_target_fetch_status ON public.public_review_place USING btree (target_type, target_id, fetch_status);


--
-- Name: idx_public_review_sample_place; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_public_review_sample_place ON public.public_review_sample USING btree (review_place_id);


--
-- Name: idx_public_review_sample_public_lookup; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_public_review_sample_public_lookup ON public.public_review_sample USING btree (review_place_id, display_status, rating);


--
-- Name: idx_public_review_sample_rating; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_public_review_sample_rating ON public.public_review_sample USING btree (rating);


--
-- Name: idx_public_review_sample_sentiment; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_public_review_sample_sentiment ON public.public_review_sample USING btree (sentiment);


--
-- Name: idx_public_review_sample_target; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_public_review_sample_target ON public.public_review_sample USING btree (target_type, target_id);


--
-- Name: idx_public_review_summary_place; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_public_review_summary_place ON public.public_review_summary USING btree (review_place_id);


--
-- Name: idx_public_review_summary_target; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_public_review_summary_target ON public.public_review_summary USING btree (target_type, target_id);


--
-- Name: idx_refresh_tokens_user_device_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_refresh_tokens_user_device_active ON public.refresh_tokens USING btree (user_id, device_id, revoked, expires_at);


--
-- Name: idx_refresh_tokens_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_refresh_tokens_user_id ON public.refresh_tokens USING btree (user_id);


--
-- Name: idx_scrape_cand_builder_cid; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_scrape_cand_builder_cid ON public.dashboard_scrape_candidate_builder USING btree (candidate_id);


--
-- Name: idx_scrape_cand_compliance_cid; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_scrape_cand_compliance_cid ON public.dashboard_scrape_candidate_compliance_item USING btree (candidate_id);


--
-- Name: idx_scrape_cand_document_cid; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_scrape_cand_document_cid ON public.dashboard_scrape_candidate_document USING btree (candidate_id);


--
-- Name: idx_scrape_cand_field_result_cid; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_scrape_cand_field_result_cid ON public.dashboard_scrape_candidate_field_result USING btree (candidate_id);


--
-- Name: idx_scrape_cand_project_cid; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_scrape_cand_project_cid ON public.dashboard_scrape_candidate_project USING btree (candidate_id);


--
-- Name: idx_scrape_cand_raw_value_cid; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_scrape_cand_raw_value_cid ON public.dashboard_scrape_candidate_raw_value USING btree (candidate_id);


--
-- Name: idx_scrape_candidate_linked_builder; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_scrape_candidate_linked_builder ON public.dashboard_scrape_candidate USING btree (linked_builder_id);


--
-- Name: idx_scrape_candidate_linked_project; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_scrape_candidate_linked_project ON public.dashboard_scrape_candidate USING btree (linked_project_id);


--
-- Name: idx_scrape_candidate_rera_number; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_scrape_candidate_rera_number ON public.dashboard_scrape_candidate USING btree (rera_number);


--
-- Name: idx_scrape_candidate_source_code; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_scrape_candidate_source_code ON public.dashboard_scrape_candidate USING btree (source_code);


--
-- Name: idx_scrape_candidate_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_scrape_candidate_status ON public.dashboard_scrape_candidate USING btree (status);


--
-- Name: idx_sd_city; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sd_city ON public.stamp_duty_rule USING btree (city_name);


--
-- Name: idx_sd_effective; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sd_effective ON public.stamp_duty_rule USING btree (effective_from, effective_to);


--
-- Name: idx_sd_lookup; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sd_lookup ON public.stamp_duty_rule USING btree (state_name, city_name, buyer_type, property_category, active);


--
-- Name: idx_sr_customer; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sr_customer ON public.service_request USING btree (customer_user_id);


--
-- Name: idx_sr_pincode; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sr_pincode ON public.service_request USING btree (pincode);


--
-- Name: idx_sr_status_city; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sr_status_city ON public.service_request USING btree (status, city_id);


--
-- Name: idx_src_category_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_src_category_id ON public.service_request_category USING btree (category_id);


--
-- Name: idx_sri_provider; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sri_provider ON public.service_request_interest USING btree (provider_id);


--
-- Name: idx_sri_request; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sri_request ON public.service_request_interest USING btree (service_request_id);


--
-- Name: idx_sri_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sri_status ON public.service_request_interest USING btree (status);


--
-- Name: idx_user_favorite_target; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_favorite_target ON public.user_favorite USING btree (target_type, target_id);


--
-- Name: idx_user_favorite_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_favorite_user_id ON public.user_favorite USING btree (user_id);


--
-- Name: idx_users_onboarding_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_users_onboarding_status ON public.users USING btree (onboarding_status);


--
-- Name: ix_brand_media_brand_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_brand_media_brand_active ON public.brand_media USING btree (brand_id, active, deleted);


--
-- Name: ix_brand_media_brand_place; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_brand_media_brand_place ON public.brand_media USING btree (brand_id, placement);


--
-- Name: ix_brand_media_brand_sort; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_brand_media_brand_sort ON public.brand_media USING btree (brand_id, sort_order);


--
-- Name: ix_featured_carousel_lookup; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_featured_carousel_lookup ON public.featured_carousel_config USING btree (category_id, city_id, active, priority, "position");


--
-- Name: ix_home_section_config_cat_enabled_sort; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_home_section_config_cat_enabled_sort ON public.home_section_config USING btree (home_category_id, enabled, sort_order);


--
-- Name: ix_home_section_item_lookup; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_home_section_item_lookup ON public.home_section_item USING btree (home_category_id, section_type, active, deleted, sort_order, id);


--
-- Name: ix_home_section_item_ref; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_home_section_item_ref ON public.home_section_item USING btree (item_type, ref_id);


--
-- Name: ix_project_analytics_cat_builder; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_project_analytics_cat_builder ON public.project_analytics USING btree (category_id, builder_id, active, deleted, priority);


--
-- Name: ix_project_analytics_cat_only; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_project_analytics_cat_only ON public.project_analytics USING btree (category_id, active, deleted, priority);


--
-- Name: ix_project_plan_cat_builder; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_project_plan_cat_builder ON public.project_plan USING btree (category_id, builder_id, active, deleted, priority);


--
-- Name: ix_project_plan_cat_only; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_project_plan_cat_only ON public.project_plan USING btree (category_id, active, deleted, priority);


--
-- Name: uk_brand_collaboration_builder; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uk_brand_collaboration_builder ON public.brand_collaboration USING btree (brand_id, builder_id) WHERE ((builder_id IS NOT NULL) AND (deleted = false));


--
-- Name: uk_brand_collaboration_business; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uk_brand_collaboration_business ON public.brand_collaboration USING btree (brand_id, business_id) WHERE ((business_id IS NOT NULL) AND (deleted = false));


--
-- Name: uk_brand_collaboration_company; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uk_brand_collaboration_company ON public.brand_collaboration USING btree (brand_id, company_id) WHERE ((company_id IS NOT NULL) AND (deleted = false));


--
-- Name: uk_brand_collaboration_company_project; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uk_brand_collaboration_company_project ON public.brand_collaboration USING btree (brand_id, company_project_id) WHERE ((company_project_id IS NOT NULL) AND (deleted = false));


--
-- Name: uk_brand_collaboration_project; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uk_brand_collaboration_project ON public.brand_collaboration USING btree (brand_id, project_id) WHERE ((project_id IS NOT NULL) AND (deleted = false));


--
-- Name: uk_brand_product_category_brand_slug_active; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uk_brand_product_category_brand_slug_active ON public.brand_product_category USING btree (brand_id, slug) WHERE (deleted = false);


--
-- Name: uk_category_slug; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uk_category_slug ON public.category USING btree (slug);


--
-- Name: uk_city_name_state; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uk_city_name_state ON public.city USING btree (lower((name)::text), lower((COALESCE(state, ''::character varying))::text));


--
-- Name: uk_city_slug; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uk_city_slug ON public.city USING btree (lower((slug)::text));


--
-- Name: uk_instagram_reel_media_id_active; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uk_instagram_reel_media_id_active ON public.instagram_reel USING btree (instagram_media_id) WHERE ((deleted = false) AND (instagram_media_id IS NOT NULL));


--
-- Name: uk_instagram_reel_url_active; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uk_instagram_reel_url_active ON public.instagram_reel USING btree (instagram_url) WHERE ((deleted = false) AND (instagram_url IS NOT NULL));


--
-- Name: uq_dashboard_active_field_issue; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_dashboard_active_field_issue ON public.dashboard_field_review_issues USING btree (entity_type, entity_id, field_key) WHERE (active = true);


--
-- Name: uq_project_master_plan_project_not_deleted; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_project_master_plan_project_not_deleted ON public.project_master_plan USING btree (project_id) WHERE (deleted = false);


--
-- Name: ux_builder_improvement_profile_builder_level; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX ux_builder_improvement_profile_builder_level ON public.builder_improvement_profile USING btree (builder_id) WHERE ((project_id IS NULL) AND (deleted = false));


--
-- Name: ux_builder_improvement_profile_project_level; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX ux_builder_improvement_profile_project_level ON public.builder_improvement_profile USING btree (builder_id, project_id) WHERE ((project_id IS NOT NULL) AND (deleted = false));


--
-- Name: ux_featured_carousel_cat_variant; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX ux_featured_carousel_cat_variant ON public.featured_carousel_config USING btree (city_id, category_id, variant) WHERE (active = true);


--
-- Name: ux_refresh_tokens_token_hash; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX ux_refresh_tokens_token_hash ON public.refresh_tokens USING btree (token);


--
-- Name: ux_users_email; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX ux_users_email ON public.users USING btree (email) WHERE (email IS NOT NULL);


--
-- Name: ux_users_phone_number_normalized; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX ux_users_phone_number_normalized ON public.users USING btree (phone_number) WHERE (phone_number IS NOT NULL);


--
-- Name: ux_users_single_admin; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX ux_users_single_admin ON public.users USING btree (is_admin) WHERE (is_admin = true);


--
-- Name: brand_distributor brand_distributor_brand_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brand_distributor
    ADD CONSTRAINT brand_distributor_brand_id_fkey FOREIGN KEY (brand_id) REFERENCES public.brand(id);


--
-- Name: brand_distributor brand_distributor_distributor_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brand_distributor
    ADD CONSTRAINT brand_distributor_distributor_id_fkey FOREIGN KEY (distributor_id) REFERENCES public.distributor(id);


--
-- Name: builder builder_city_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.builder
    ADD CONSTRAINT builder_city_id_fkey FOREIGN KEY (city_id) REFERENCES public.city(id);


--
-- Name: business_event business_event_business_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.business_event
    ADD CONSTRAINT business_event_business_id_fkey FOREIGN KEY (business_id) REFERENCES public.business(id);


--
-- Name: business_event business_event_category_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.business_event
    ADD CONSTRAINT business_event_category_id_fkey FOREIGN KEY (category_id) REFERENCES public.category(id);


--
-- Name: business_event business_event_city_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.business_event
    ADD CONSTRAINT business_event_city_id_fkey FOREIGN KEY (city_id) REFERENCES public.city(id);


--
-- Name: company_award company_award_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_award
    ADD CONSTRAINT company_award_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.company(id);


--
-- Name: company_certificate company_certificate_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_certificate
    ADD CONSTRAINT company_certificate_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.company(id);


--
-- Name: company company_city_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company
    ADD CONSTRAINT company_city_id_fkey FOREIGN KEY (city_id) REFERENCES public.city(id);


--
-- Name: company_media company_media_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_media
    ADD CONSTRAINT company_media_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.company(id);


--
-- Name: company_pricing_plan company_pricing_plan_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_pricing_plan
    ADD CONSTRAINT company_pricing_plan_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.company(id);


--
-- Name: company_project company_project_city_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_project
    ADD CONSTRAINT company_project_city_id_fkey FOREIGN KEY (city_id) REFERENCES public.city(id);


--
-- Name: company_project company_project_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_project
    ADD CONSTRAINT company_project_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.company(id);


--
-- Name: company_stat company_stat_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_stat
    ADD CONSTRAINT company_stat_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.company(id);


--
-- Name: dashboard_scrape_candidate_builder dashboard_scrape_candidate_builder_candidate_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dashboard_scrape_candidate_builder
    ADD CONSTRAINT dashboard_scrape_candidate_builder_candidate_id_fkey FOREIGN KEY (candidate_id) REFERENCES public.dashboard_scrape_candidate(id) ON DELETE CASCADE;


--
-- Name: dashboard_scrape_candidate_compliance_item dashboard_scrape_candidate_compliance_item_candidate_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dashboard_scrape_candidate_compliance_item
    ADD CONSTRAINT dashboard_scrape_candidate_compliance_item_candidate_id_fkey FOREIGN KEY (candidate_id) REFERENCES public.dashboard_scrape_candidate(id) ON DELETE CASCADE;


--
-- Name: dashboard_scrape_candidate_cost_breakdown dashboard_scrape_candidate_cost_breakdown_candidate_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dashboard_scrape_candidate_cost_breakdown
    ADD CONSTRAINT dashboard_scrape_candidate_cost_breakdown_candidate_id_fkey FOREIGN KEY (candidate_id) REFERENCES public.dashboard_scrape_candidate(id) ON DELETE CASCADE;


--
-- Name: dashboard_scrape_candidate_document dashboard_scrape_candidate_document_candidate_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dashboard_scrape_candidate_document
    ADD CONSTRAINT dashboard_scrape_candidate_document_candidate_id_fkey FOREIGN KEY (candidate_id) REFERENCES public.dashboard_scrape_candidate(id) ON DELETE CASCADE;


--
-- Name: dashboard_scrape_candidate_field_result dashboard_scrape_candidate_field_result_candidate_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dashboard_scrape_candidate_field_result
    ADD CONSTRAINT dashboard_scrape_candidate_field_result_candidate_id_fkey FOREIGN KEY (candidate_id) REFERENCES public.dashboard_scrape_candidate(id) ON DELETE CASCADE;


--
-- Name: dashboard_scrape_candidate_land_utilization dashboard_scrape_candidate_land_utilization_candidate_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dashboard_scrape_candidate_land_utilization
    ADD CONSTRAINT dashboard_scrape_candidate_land_utilization_candidate_id_fkey FOREIGN KEY (candidate_id) REFERENCES public.dashboard_scrape_candidate(id) ON DELETE CASCADE;


--
-- Name: dashboard_scrape_candidate_project dashboard_scrape_candidate_project_candidate_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dashboard_scrape_candidate_project
    ADD CONSTRAINT dashboard_scrape_candidate_project_candidate_id_fkey FOREIGN KEY (candidate_id) REFERENCES public.dashboard_scrape_candidate(id) ON DELETE CASCADE;


--
-- Name: dashboard_scrape_candidate_raw_value dashboard_scrape_candidate_raw_value_candidate_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dashboard_scrape_candidate_raw_value
    ADD CONSTRAINT dashboard_scrape_candidate_raw_value_candidate_id_fkey FOREIGN KEY (candidate_id) REFERENCES public.dashboard_scrape_candidate(id) ON DELETE CASCADE;


--
-- Name: distributor_media distributor_media_distributor_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.distributor_media
    ADD CONSTRAINT distributor_media_distributor_id_fkey FOREIGN KEY (distributor_id) REFERENCES public.distributor(id);


--
-- Name: feed_section_item feed_section_item_config_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.feed_section_item
    ADD CONSTRAINT feed_section_item_config_id_fkey FOREIGN KEY (config_id) REFERENCES public.feed_section_config(id);


--
-- Name: provider_service_area fk_area_city; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.provider_service_area
    ADD CONSTRAINT fk_area_city FOREIGN KEY (city_id) REFERENCES public.city(id);


--
-- Name: provider_service_area fk_area_provider; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.provider_service_area
    ADD CONSTRAINT fk_area_provider FOREIGN KEY (provider_id) REFERENCES public.provider_profile(id);


--
-- Name: promo_banner fk_banner_category; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.promo_banner
    ADD CONSTRAINT fk_banner_category FOREIGN KEY (category_id) REFERENCES public.category(id);


--
-- Name: brand fk_brand_category; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brand
    ADD CONSTRAINT fk_brand_category FOREIGN KEY (category_id) REFERENCES public.category(id);


--
-- Name: brand_category_link fk_brand_category_link_brand; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brand_category_link
    ADD CONSTRAINT fk_brand_category_link_brand FOREIGN KEY (brand_id) REFERENCES public.brand(id);


--
-- Name: brand_category_link fk_brand_category_link_category; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brand_category_link
    ADD CONSTRAINT fk_brand_category_link_category FOREIGN KEY (category_id) REFERENCES public.category(id);


--
-- Name: brand_certificate fk_brand_certificate_brand; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brand_certificate
    ADD CONSTRAINT fk_brand_certificate_brand FOREIGN KEY (brand_id) REFERENCES public.brand(id);


--
-- Name: brand_collaboration fk_brand_collaboration_brand; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brand_collaboration
    ADD CONSTRAINT fk_brand_collaboration_brand FOREIGN KEY (brand_id) REFERENCES public.brand(id);


--
-- Name: brand_collaboration fk_brand_collaboration_builder; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brand_collaboration
    ADD CONSTRAINT fk_brand_collaboration_builder FOREIGN KEY (builder_id) REFERENCES public.builder(id);


--
-- Name: brand_collaboration fk_brand_collaboration_business; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brand_collaboration
    ADD CONSTRAINT fk_brand_collaboration_business FOREIGN KEY (business_id) REFERENCES public.business(id);


--
-- Name: brand_collaboration fk_brand_collaboration_company; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brand_collaboration
    ADD CONSTRAINT fk_brand_collaboration_company FOREIGN KEY (company_id) REFERENCES public.company(id);


--
-- Name: brand_collaboration fk_brand_collaboration_company_project; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brand_collaboration
    ADD CONSTRAINT fk_brand_collaboration_company_project FOREIGN KEY (company_project_id) REFERENCES public.company_project(id);


--
-- Name: brand_collaboration fk_brand_collaboration_project; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brand_collaboration
    ADD CONSTRAINT fk_brand_collaboration_project FOREIGN KEY (project_id) REFERENCES public.project(id);


--
-- Name: brand_faq fk_brand_faq_brand; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brand_faq
    ADD CONSTRAINT fk_brand_faq_brand FOREIGN KEY (brand_id) REFERENCES public.brand(id);


--
-- Name: brand_media fk_brand_media_brand; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brand_media
    ADD CONSTRAINT fk_brand_media_brand FOREIGN KEY (brand_id) REFERENCES public.brand(id);


--
-- Name: brand_media fk_brand_media_sku; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brand_media
    ADD CONSTRAINT fk_brand_media_sku FOREIGN KEY (brand_sku_id) REFERENCES public.brand_sku(id);


--
-- Name: brand_product_category fk_brand_product_category_brand; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brand_product_category
    ADD CONSTRAINT fk_brand_product_category_brand FOREIGN KEY (brand_id) REFERENCES public.brand(id);


--
-- Name: brand_sku fk_brand_sku_brand; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brand_sku
    ADD CONSTRAINT fk_brand_sku_brand FOREIGN KEY (brand_id) REFERENCES public.brand(id);


--
-- Name: brand_sku fk_brand_sku_category; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brand_sku
    ADD CONSTRAINT fk_brand_sku_category FOREIGN KEY (category_id) REFERENCES public.category(id);


--
-- Name: brand_sku fk_brand_sku_product_category; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brand_sku
    ADD CONSTRAINT fk_brand_sku_product_category FOREIGN KEY (product_category_id) REFERENCES public.brand_product_category(id);


--
-- Name: builder_after_sales_upgrade fk_builder_after_sales_upgrade_profile; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.builder_after_sales_upgrade
    ADD CONSTRAINT fk_builder_after_sales_upgrade_profile FOREIGN KEY (profile_id) REFERENCES public.builder_improvement_profile(id) ON DELETE CASCADE;


--
-- Name: builder_highlight_item fk_builder_highlight_item_approved_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.builder_highlight_item
    ADD CONSTRAINT fk_builder_highlight_item_approved_by FOREIGN KEY (approved_by) REFERENCES public.dashboard_users(id) ON DELETE SET NULL;


--
-- Name: builder_highlight_item fk_builder_highlight_item_builder; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.builder_highlight_item
    ADD CONSTRAINT fk_builder_highlight_item_builder FOREIGN KEY (builder_id) REFERENCES public.builder(id);


--
-- Name: builder_highlight_item fk_builder_highlight_item_city; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.builder_highlight_item
    ADD CONSTRAINT fk_builder_highlight_item_city FOREIGN KEY (city_id) REFERENCES public.city(id);


--
-- Name: builder_highlight_item fk_builder_highlight_item_created_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.builder_highlight_item
    ADD CONSTRAINT fk_builder_highlight_item_created_by FOREIGN KEY (created_by) REFERENCES public.dashboard_users(id) ON DELETE SET NULL;


--
-- Name: builder_highlight_item fk_builder_highlight_item_project; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.builder_highlight_item
    ADD CONSTRAINT fk_builder_highlight_item_project FOREIGN KEY (project_id) REFERENCES public.project(id);


--
-- Name: builder_highlight_item fk_builder_highlight_item_updated_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.builder_highlight_item
    ADD CONSTRAINT fk_builder_highlight_item_updated_by FOREIGN KEY (updated_by) REFERENCES public.dashboard_users(id) ON DELETE SET NULL;


--
-- Name: builder_highlight_point fk_builder_highlight_point_item; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.builder_highlight_point
    ADD CONSTRAINT fk_builder_highlight_point_item FOREIGN KEY (highlight_item_id) REFERENCES public.builder_highlight_item(id) ON DELETE CASCADE;


--
-- Name: builder_improvement_action fk_builder_improvement_action_profile; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.builder_improvement_action
    ADD CONSTRAINT fk_builder_improvement_action_profile FOREIGN KEY (profile_id) REFERENCES public.builder_improvement_profile(id) ON DELETE CASCADE;


--
-- Name: builder_improvement_issue fk_builder_improvement_issue_profile; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.builder_improvement_issue
    ADD CONSTRAINT fk_builder_improvement_issue_profile FOREIGN KEY (profile_id) REFERENCES public.builder_improvement_profile(id) ON DELETE CASCADE;


--
-- Name: builder_improvement_profile fk_builder_improvement_profile_builder; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.builder_improvement_profile
    ADD CONSTRAINT fk_builder_improvement_profile_builder FOREIGN KEY (builder_id) REFERENCES public.builder(id);


--
-- Name: builder_improvement_profile fk_builder_improvement_profile_created_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.builder_improvement_profile
    ADD CONSTRAINT fk_builder_improvement_profile_created_by FOREIGN KEY (created_by_dashboard_user_id) REFERENCES public.dashboard_users(id) ON DELETE SET NULL;


--
-- Name: builder_improvement_profile fk_builder_improvement_profile_project; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.builder_improvement_profile
    ADD CONSTRAINT fk_builder_improvement_profile_project FOREIGN KEY (project_id) REFERENCES public.project(id);


--
-- Name: builder_improvement_profile fk_builder_improvement_profile_published_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.builder_improvement_profile
    ADD CONSTRAINT fk_builder_improvement_profile_published_by FOREIGN KEY (published_by_dashboard_user_id) REFERENCES public.dashboard_users(id) ON DELETE SET NULL;


--
-- Name: builder_improvement_profile fk_builder_improvement_profile_reviewed_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.builder_improvement_profile
    ADD CONSTRAINT fk_builder_improvement_profile_reviewed_by FOREIGN KEY (reviewed_by_dashboard_user_id) REFERENCES public.dashboard_users(id) ON DELETE SET NULL;


--
-- Name: builder_improvement_profile fk_builder_improvement_profile_submitted_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.builder_improvement_profile
    ADD CONSTRAINT fk_builder_improvement_profile_submitted_by FOREIGN KEY (submitted_by_dashboard_user_id) REFERENCES public.dashboard_users(id) ON DELETE SET NULL;


--
-- Name: builder_improvement_profile fk_builder_improvement_profile_updated_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.builder_improvement_profile
    ADD CONSTRAINT fk_builder_improvement_profile_updated_by FOREIGN KEY (updated_by_dashboard_user_id) REFERENCES public.dashboard_users(id) ON DELETE SET NULL;


--
-- Name: builder_improvement_timeline fk_builder_improvement_timeline_profile; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.builder_improvement_timeline
    ADD CONSTRAINT fk_builder_improvement_timeline_profile FOREIGN KEY (profile_id) REFERENCES public.builder_improvement_profile(id) ON DELETE CASCADE;


--
-- Name: business fk_business_category; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.business
    ADD CONSTRAINT fk_business_category FOREIGN KEY (category_id) REFERENCES public.category(id);


--
-- Name: business fk_business_city; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.business
    ADD CONSTRAINT fk_business_city FOREIGN KEY (city_id) REFERENCES public.city(id);


--
-- Name: business fk_business_owner_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.business
    ADD CONSTRAINT fk_business_owner_user FOREIGN KEY (owner_user_id) REFERENCES public.users(id);


--
-- Name: category fk_category_parent; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.category
    ADD CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES public.category(id);


--
-- Name: company_brand_link fk_company_brand_link_brand; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_brand_link
    ADD CONSTRAINT fk_company_brand_link_brand FOREIGN KEY (brand_id) REFERENCES public.brand(id);


--
-- Name: company_brand_link fk_company_brand_link_company; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_brand_link
    ADD CONSTRAINT fk_company_brand_link_company FOREIGN KEY (company_id) REFERENCES public.company(id);


--
-- Name: company fk_company_city; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company
    ADD CONSTRAINT fk_company_city FOREIGN KEY (city_id) REFERENCES public.city(id);


--
-- Name: dashboard_field_help fk_dashboard_field_help_created_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dashboard_field_help
    ADD CONSTRAINT fk_dashboard_field_help_created_by FOREIGN KEY (created_by_dashboard_user_id) REFERENCES public.dashboard_users(id) ON DELETE SET NULL;


--
-- Name: dashboard_field_help fk_dashboard_field_help_updated_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dashboard_field_help
    ADD CONSTRAINT fk_dashboard_field_help_updated_by FOREIGN KEY (updated_by_dashboard_user_id) REFERENCES public.dashboard_users(id) ON DELETE SET NULL;


--
-- Name: dashboard_field_review_issues fk_dashboard_field_review_fixed_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dashboard_field_review_issues
    ADD CONSTRAINT fk_dashboard_field_review_fixed_by FOREIGN KEY (fixed_by) REFERENCES public.dashboard_users(id) ON DELETE SET NULL;


--
-- Name: dashboard_field_review_issues fk_dashboard_field_review_marked_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dashboard_field_review_issues
    ADD CONSTRAINT fk_dashboard_field_review_marked_by FOREIGN KEY (marked_by) REFERENCES public.dashboard_users(id) ON DELETE RESTRICT;


--
-- Name: dashboard_login_audit fk_dashboard_login_audit_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dashboard_login_audit
    ADD CONSTRAINT fk_dashboard_login_audit_user FOREIGN KEY (dashboard_user_id) REFERENCES public.dashboard_users(id) ON DELETE SET NULL;


--
-- Name: dashboard_refresh_tokens fk_dashboard_refresh_tokens_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dashboard_refresh_tokens
    ADD CONSTRAINT fk_dashboard_refresh_tokens_user FOREIGN KEY (dashboard_user_id) REFERENCES public.dashboard_users(id) ON DELETE CASCADE;


--
-- Name: dashboard_content_review_history fk_dashboard_review_history_action_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dashboard_content_review_history
    ADD CONSTRAINT fk_dashboard_review_history_action_by FOREIGN KEY (action_by) REFERENCES public.dashboard_users(id) ON DELETE SET NULL;


--
-- Name: favorites fk_favorites_business; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.favorites
    ADD CONSTRAINT fk_favorites_business FOREIGN KEY (business_id) REFERENCES public.business(id) ON DELETE CASCADE;


--
-- Name: favorites fk_favorites_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.favorites
    ADD CONSTRAINT fk_favorites_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: home_section_item fk_home_section_item_config; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.home_section_item
    ADD CONSTRAINT fk_home_section_item_config FOREIGN KEY (config_id) REFERENCES public.home_section_config(id);


--
-- Name: interior_cost_addon_rule fk_interior_cost_addon_rule_company; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.interior_cost_addon_rule
    ADD CONSTRAINT fk_interior_cost_addon_rule_company FOREIGN KEY (company_id) REFERENCES public.company(id);


--
-- Name: interior_cost_rule fk_interior_cost_rule_company; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.interior_cost_rule
    ADD CONSTRAINT fk_interior_cost_rule_company FOREIGN KEY (company_id) REFERENCES public.company(id);


--
-- Name: provider_project_media fk_media_project; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.provider_project_media
    ADD CONSTRAINT fk_media_project FOREIGN KEY (project_id) REFERENCES public.provider_project(id);


--
-- Name: project_amenity_progress fk_project_amenity_progress_project; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_amenity_progress
    ADD CONSTRAINT fk_project_amenity_progress_project FOREIGN KEY (project_id) REFERENCES public.project(id);


--
-- Name: provider_project fk_project_category; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.provider_project
    ADD CONSTRAINT fk_project_category FOREIGN KEY (category_id) REFERENCES public.category(id);


--
-- Name: provider_project fk_project_city; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.provider_project
    ADD CONSTRAINT fk_project_city FOREIGN KEY (city_id) REFERENCES public.city(id);


--
-- Name: project_compliance_item fk_project_compliance_item_project; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_compliance_item
    ADD CONSTRAINT fk_project_compliance_item_project FOREIGN KEY (project_id) REFERENCES public.project(id);


--
-- Name: project_connectivity_place fk_project_connectivity_place_connectivity; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_connectivity_place
    ADD CONSTRAINT fk_project_connectivity_place_connectivity FOREIGN KEY (connectivity_id) REFERENCES public.project_connectivity(id);


--
-- Name: project_connectivity_place fk_project_connectivity_place_project; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_connectivity_place
    ADD CONSTRAINT fk_project_connectivity_place_project FOREIGN KEY (project_id) REFERENCES public.project(id);


--
-- Name: project_connectivity fk_project_connectivity_project; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_connectivity
    ADD CONSTRAINT fk_project_connectivity_project FOREIGN KEY (project_id) REFERENCES public.project(id);


--
-- Name: project_construction_stage fk_project_construction_stage_project; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_construction_stage
    ADD CONSTRAINT fk_project_construction_stage_project FOREIGN KEY (project_id) REFERENCES public.project(id);


--
-- Name: project_cost_breakdown fk_project_cost_breakdown_project; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_cost_breakdown
    ADD CONSTRAINT fk_project_cost_breakdown_project FOREIGN KEY (project_id) REFERENCES public.project(id);


--
-- Name: project fk_project_created_by_dashboard_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project
    ADD CONSTRAINT fk_project_created_by_dashboard_user FOREIGN KEY (created_by_dashboard_user_id) REFERENCES public.dashboard_users(id) ON DELETE SET NULL;


--
-- Name: project_floor_plan fk_project_floor_plan_project; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_floor_plan
    ADD CONSTRAINT fk_project_floor_plan_project FOREIGN KEY (project_id) REFERENCES public.project(id);


--
-- Name: project_highlight fk_project_highlight_project; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_highlight
    ADD CONSTRAINT fk_project_highlight_project FOREIGN KEY (project_id) REFERENCES public.project(id);


--
-- Name: project_land_utilization fk_project_land_utilization_project; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_land_utilization
    ADD CONSTRAINT fk_project_land_utilization_project FOREIGN KEY (project_id) REFERENCES public.project(id);


--
-- Name: project_location_score fk_project_location_score_project; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_location_score
    ADD CONSTRAINT fk_project_location_score_project FOREIGN KEY (project_id) REFERENCES public.project(id);


--
-- Name: project_meter_snapshot fk_project_meter_snapshot_project; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_meter_snapshot
    ADD CONSTRAINT fk_project_meter_snapshot_project FOREIGN KEY (project_id) REFERENCES public.project(id);


--
-- Name: project_payment_milestone fk_project_payment_milestone_project; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_payment_milestone
    ADD CONSTRAINT fk_project_payment_milestone_project FOREIGN KEY (project_id) REFERENCES public.project(id);


--
-- Name: project_price_history fk_project_price_history_project; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_price_history
    ADD CONSTRAINT fk_project_price_history_project FOREIGN KEY (project_id) REFERENCES public.project(id);


--
-- Name: provider_project fk_project_provider; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.provider_project
    ADD CONSTRAINT fk_project_provider FOREIGN KEY (provider_id) REFERENCES public.provider_profile(id);


--
-- Name: project fk_project_reviewed_by_dashboard_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project
    ADD CONSTRAINT fk_project_reviewed_by_dashboard_user FOREIGN KEY (reviewed_by_dashboard_user_id) REFERENCES public.dashboard_users(id) ON DELETE SET NULL;


--
-- Name: project fk_project_submitted_by_dashboard_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project
    ADD CONSTRAINT fk_project_submitted_by_dashboard_user FOREIGN KEY (submitted_by_dashboard_user_id) REFERENCES public.dashboard_users(id) ON DELETE SET NULL;


--
-- Name: provider_profile fk_provider_business; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.provider_profile
    ADD CONSTRAINT fk_provider_business FOREIGN KEY (business_id) REFERENCES public.business(id);


--
-- Name: provider_profile fk_provider_category; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.provider_profile
    ADD CONSTRAINT fk_provider_category FOREIGN KEY (primary_category_id) REFERENCES public.category(id);


--
-- Name: provider_media fk_provider_media_provider; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.provider_media
    ADD CONSTRAINT fk_provider_media_provider FOREIGN KEY (provider_id) REFERENCES public.provider_profile(id) ON DELETE CASCADE;


--
-- Name: provider_profile fk_provider_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.provider_profile
    ADD CONSTRAINT fk_provider_user FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: public_review_sample fk_public_review_sample_place; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.public_review_sample
    ADD CONSTRAINT fk_public_review_sample_place FOREIGN KEY (review_place_id) REFERENCES public.public_review_place(id) ON DELETE CASCADE;


--
-- Name: public_review_summary fk_public_review_summary_place; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.public_review_summary
    ADD CONSTRAINT fk_public_review_summary_place FOREIGN KEY (review_place_id) REFERENCES public.public_review_place(id) ON DELETE CASCADE;


--
-- Name: service_request fk_sr_city; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.service_request
    ADD CONSTRAINT fk_sr_city FOREIGN KEY (city_id) REFERENCES public.city(id);


--
-- Name: service_request fk_sr_customer; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.service_request
    ADD CONSTRAINT fk_sr_customer FOREIGN KEY (customer_user_id) REFERENCES public.users(id);


--
-- Name: service_request_category fk_src_cat; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.service_request_category
    ADD CONSTRAINT fk_src_cat FOREIGN KEY (category_id) REFERENCES public.category(id);


--
-- Name: service_request_category fk_src_req; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.service_request_category
    ADD CONSTRAINT fk_src_req FOREIGN KEY (service_request_id) REFERENCES public.service_request(id) ON DELETE CASCADE;


--
-- Name: service_request_interest fk_sri_provider; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.service_request_interest
    ADD CONSTRAINT fk_sri_provider FOREIGN KEY (provider_id) REFERENCES public.provider_profile(id) ON DELETE CASCADE;


--
-- Name: service_request_interest fk_sri_request; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.service_request_interest
    ADD CONSTRAINT fk_sri_request FOREIGN KEY (service_request_id) REFERENCES public.service_request(id) ON DELETE CASCADE;


--
-- Name: login_history fk_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.login_history
    ADD CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: user_favorite fk_user_favorite_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_favorite
    ADD CONSTRAINT fk_user_favorite_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: guest_sessions guest_sessions_linked_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.guest_sessions
    ADD CONSTRAINT guest_sessions_linked_user_id_fkey FOREIGN KEY (linked_user_id) REFERENCES public.users(id);


--
-- Name: home_project_analytics home_project_analytics_category_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.home_project_analytics
    ADD CONSTRAINT home_project_analytics_category_id_fkey FOREIGN KEY (category_id) REFERENCES public.category(id);


--
-- Name: home_section_config home_section_config_home_category_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.home_section_config
    ADD CONSTRAINT home_section_config_home_category_id_fkey FOREIGN KEY (home_category_id) REFERENCES public.category(id);


--
-- Name: home_section_item home_section_item_home_category_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.home_section_item
    ADD CONSTRAINT home_section_item_home_category_id_fkey FOREIGN KEY (home_category_id) REFERENCES public.category(id);


--
-- Name: otps otps_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.otps
    ADD CONSTRAINT otps_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: project_analytics project_analytics_builder_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_analytics
    ADD CONSTRAINT project_analytics_builder_id_fkey FOREIGN KEY (builder_id) REFERENCES public.builder(id);


--
-- Name: project_analytics project_analytics_category_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_analytics
    ADD CONSTRAINT project_analytics_category_id_fkey FOREIGN KEY (category_id) REFERENCES public.category(id);


--
-- Name: project project_builder_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project
    ADD CONSTRAINT project_builder_id_fkey FOREIGN KEY (builder_id) REFERENCES public.builder(id);


--
-- Name: project project_city_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project
    ADD CONSTRAINT project_city_id_fkey FOREIGN KEY (city_id) REFERENCES public.city(id);


--
-- Name: project_floor_plan_insight project_floor_plan_insight_floor_plan_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_floor_plan_insight
    ADD CONSTRAINT project_floor_plan_insight_floor_plan_id_fkey FOREIGN KEY (floor_plan_id) REFERENCES public.project_floor_plan(id);


--
-- Name: project_floor_plan_room_dimension project_floor_plan_room_dimension_floor_plan_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_floor_plan_room_dimension
    ADD CONSTRAINT project_floor_plan_room_dimension_floor_plan_id_fkey FOREIGN KEY (floor_plan_id) REFERENCES public.project_floor_plan(id);


--
-- Name: project_master_plan project_master_plan_project_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_master_plan
    ADD CONSTRAINT project_master_plan_project_id_fkey FOREIGN KEY (project_id) REFERENCES public.project(id);


--
-- Name: project_media project_media_project_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_media
    ADD CONSTRAINT project_media_project_id_fkey FOREIGN KEY (project_id) REFERENCES public.project(id) ON DELETE CASCADE;


--
-- Name: project_plan project_plan_builder_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_plan
    ADD CONSTRAINT project_plan_builder_id_fkey FOREIGN KEY (builder_id) REFERENCES public.builder(id);


--
-- Name: project_plan project_plan_category_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_plan
    ADD CONSTRAINT project_plan_category_id_fkey FOREIGN KEY (category_id) REFERENCES public.category(id);


--
-- Name: project_property_types project_property_types_project_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_property_types
    ADD CONSTRAINT project_property_types_project_id_fkey FOREIGN KEY (project_id) REFERENCES public.project(id) ON DELETE CASCADE;


--
-- Name: project_review project_review_project_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_review
    ADD CONSTRAINT project_review_project_id_fkey FOREIGN KEY (project_id) REFERENCES public.project(id);


--
-- Name: project_review project_review_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_review
    ADD CONSTRAINT project_review_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: refresh_tokens refresh_tokens_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--
