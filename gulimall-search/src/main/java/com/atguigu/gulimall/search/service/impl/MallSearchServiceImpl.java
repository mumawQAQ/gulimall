package com.atguigu.gulimall.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.common.to.es.SkuEsModel;
import com.atguigu.gulimall.search.config.GulimallElasticSearchConfig;
import com.atguigu.gulimall.search.constant.EsConstant;
import com.atguigu.gulimall.search.service.MallSearchService;
import com.atguigu.gulimall.search.vo.SearchParam;
import com.atguigu.gulimall.search.vo.SearchResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.search.TotalHits;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MallSearchServiceImpl implements MallSearchService {
    @Autowired
    private RestHighLevelClient client;
    @Override
    public SearchResult search(SearchParam searchParam) {
        SearchResult result = null;
        SearchRequest searchRequest = buildSearchRequest(searchParam);
        try {
            SearchResponse response = client.search(searchRequest, GulimallElasticSearchConfig.COMMON_OPTIONS);
            result = buildSearchResult(response, searchParam);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    private SearchRequest buildSearchRequest(SearchParam Param) {
        // 帮我们构建DSL语句的
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        // 1. 模糊匹配 过滤(按照属性、分类、品牌、价格区间、库存) 先构建一个布尔Query
        // 1.1 must
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        if(!StringUtils.isEmpty(Param.getKeyword())){
            boolQuery.must(QueryBuilders.matchQuery("skuTitle",Param.getKeyword()));
        }
        // 1.2 bool - filter Catalog3Id
        if(StringUtils.isEmpty(Param.getCatalog3Id() != null)){
            boolQuery.filter(QueryBuilders.termQuery("catalogId", Param.getCatalog3Id()));
        }
        // 1.2 bool - brandId [集合]
        if(Param.getBrandId() != null && Param.getBrandId().size() > 0){
            boolQuery.filter(QueryBuilders.termsQuery("brandId", Param.getBrandId()));
        }
        // 属性查询
        if(Param.getAttrs() != null && Param.getAttrs().size() > 0){

            for (String attrStr : Param.getAttrs()) {
                BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
                String[] s = attrStr.split("_");
                // 检索的id  属性检索用的值
                String attrId = s[0];
                String[] attrValue = s[1].split(":");
                boolQueryBuilder.must(QueryBuilders.termQuery("attrs.attrId", attrId));
                boolQueryBuilder.must(QueryBuilders.termsQuery("attrs.attrValue", attrValue));
                // 构建一个嵌入式Query 每一个必须都得生成嵌入的 nested 查询
                NestedQueryBuilder attrsQuery = QueryBuilders.nestedQuery("attrs", boolQueryBuilder, ScoreMode.None);
                boolQuery.filter(attrsQuery);
            }
        }
        // 1.2 bool - filter [库存]
        if(Param.getHasStock() != null){
            boolQuery.filter(QueryBuilders.termQuery("hasStock",Param.getHasStock() == 1));
        }
        // 1.2 bool - filter [价格区间]
        if(!StringUtils.isEmpty(Param.getSkuPrice())){
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("skuPrice");
            String[] s = Param.getSkuPrice().split("_");
            if(s.length == 2){
                // 有三个值 就是区间
                rangeQuery.gte(s[0]).lte(s[1]);
            }else if(s.length == 1){
                // 单值情况
                if(Param.getSkuPrice().startsWith("_")){
                    rangeQuery.lte(s[0]);
                }
                if(Param.getSkuPrice().endsWith("_")){
                    rangeQuery.gte(s[0]);
                }
            }
            boolQuery.filter(rangeQuery);
        }

        // 把以前所有条件都拿来进行封装
        sourceBuilder.query(boolQuery);

        // 1.排序
        if(!StringUtils.isEmpty(Param.getSort())){
            String sort = Param.getSort();
            // sort=hotScore_asc/desc
            String[] s = sort.split("_");
            SortOrder order = s[1].equalsIgnoreCase("asc") ? SortOrder.ASC : SortOrder.DESC;
            sourceBuilder.sort(s[0], order);
        }
        // 2.分页 pageSize ： 5
        sourceBuilder.from((Param.getPageNum()-1) * EsConstant.PRODUCT_PAGE_SIZE);
        sourceBuilder.size(EsConstant.PRODUCT_PAGE_SIZE);

        // 3.高亮
        if(!StringUtils.isEmpty(Param.getKeyword())){
            HighlightBuilder builder = new HighlightBuilder();
            builder.field("skuTitle");
            builder.preTags("<b style='color:red'>");
            builder.postTags("</b>");
            sourceBuilder.highlighter(builder);
        }
        // 聚合分析

        TermsAggregationBuilder brand_agg = AggregationBuilders.terms("brand_agg");
        brand_agg.field("brandId").size(50);
        // 品牌聚合的子聚合
        brand_agg.subAggregation(AggregationBuilders.terms("brand_name_agg").field("brandName").size(1));
        brand_agg.subAggregation(AggregationBuilders.terms("brand_img_agg").field("brandImg").size(1));
        // 将品牌聚合加入 sourceBuilder
        sourceBuilder.aggregation(brand_agg);

        TermsAggregationBuilder catalog_agg = AggregationBuilders.terms("catalog_agg").field("catalogId").size(20);
        catalog_agg.subAggregation(AggregationBuilders.terms("catalog_name_agg").field("catalogName").size(1));
        // 将分类聚合加入 sourceBuilder
        sourceBuilder.aggregation(catalog_agg);
        NestedAggregationBuilder attr_agg = AggregationBuilders.nested("attr_agg", "attrs");
        // 3.1 聚合出当前所有的attrId
        TermsAggregationBuilder attrIdAgg = AggregationBuilders.terms("attr_id_agg").field("attrs.attrId");
        // 3.1.1 聚合分析出当前attrId对应的attrName
        attrIdAgg.subAggregation(AggregationBuilders.terms("attr_name_agg").field("attrs.attrName").size(1));
        // 3.1.2 聚合分析出当前attrId对应的所有可能的属性值attrValue	这里的属性值可能会有很多 所以写50
        attrIdAgg.subAggregation(AggregationBuilders.terms("attr_value_agg").field("attrs.attrValue").size(50));
        // 3.2 将这个子聚合加入嵌入式聚合
        attr_agg.subAggregation(attrIdAgg);
        sourceBuilder.aggregation(attr_agg);
        log.info("\n构建语句：->\n" + sourceBuilder.toString());
        SearchRequest searchRequest = new SearchRequest(new String[]{EsConstant.PRODUCT_INDEX}, sourceBuilder);
        return searchRequest;
    }

    private SearchResult buildSearchResult(SearchResponse response, SearchParam searchParam) {
        SearchResult searchResult = new SearchResult();

        // 1.分页信息
        SearchHits hits = response.getHits();
        TotalHits total = hits.getTotalHits();
        searchResult.setTotal(total.value);
        Long pageNum = total.value % EsConstant.PRODUCT_PAGE_SIZE == 0 ? total.value / EsConstant.PRODUCT_PAGE_SIZE : total.value / EsConstant.PRODUCT_PAGE_SIZE + 1;
        searchResult.setTotalPages(pageNum.intValue());
        searchResult.setPageNum(searchParam.getPageNum());


        // 2.查询结果
        List<SkuEsModel> skuEsModels = new ArrayList<>();
        if(hits.getHits()!= null && hits.getHits().length > 0){
            for (SearchHit hit : hits.getHits()) {
                String sourceAsString = hit.getSourceAsString();
                SkuEsModel skuEsModel = JSON.parseObject(sourceAsString, SkuEsModel.class);
                if (!StringUtils.isEmpty(searchParam.getKeyword())) {
                    HighlightField skuTitle = hit.getHighlightFields().get("skuTitle");
                    String string = skuTitle.getFragments()[0].string();
                    skuEsModel.setSkuTitle(string);
                }
                skuEsModels.add(skuEsModel);
            }
        }
        searchResult.setProducts(skuEsModels);


        // 3.聚合结果
        ArrayList<SearchResult.CatalogVo> catalogVos = new ArrayList<>();
        ParsedLongTerms catalog_agg = response.getAggregations().get("catalog_agg");
        List<? extends Terms.Bucket> buckets = catalog_agg.getBuckets();
        for (Terms.Bucket bucket : buckets) {
            SearchResult.CatalogVo catalogVo = new SearchResult.CatalogVo();
            String keyAsString = bucket.getKeyAsString();
            catalogVo.setCatalogId(Long.parseLong(keyAsString));

            ParsedStringTerms catalog_name_agg = bucket.getAggregations().get("catalog_name_agg");
            String keyAsString1 = catalog_name_agg.getBuckets().get(0).getKeyAsString();
            catalogVo.setCatalogName(keyAsString1);
            catalogVos.add(catalogVo);
        }
        searchResult.setCatalogs(catalogVos);

        ArrayList<SearchResult.BrandVo> brandVos = new ArrayList<>();
        ParsedLongTerms brand_agg = response.getAggregations().get("brand_agg");
        for (Terms.Bucket bucket : brand_agg.getBuckets()) {
            SearchResult.BrandVo brandVo = new SearchResult.BrandVo();

            long brandId = bucket.getKeyAsNumber().longValue();
            brandVo.setBrandId(brandId);

            ParsedStringTerms brand_img_agg = bucket.getAggregations().get("brand_img_agg");
            String brandImg = brand_img_agg.getBuckets().get(0).getKeyAsString();
            brandVo.setBrandImg(brandImg);

            ParsedStringTerms brand_name_agg = bucket.getAggregations().get("brand_name_agg");
            String brandName = brand_name_agg.getBuckets().get(0).getKeyAsString();
            brandVo.setBrandName(brandName);

            brandVos.add(brandVo);
        }
        searchResult.setBrands(brandVos);

        ArrayList<SearchResult.AttrVo> attrVos = new ArrayList<>();
        ParsedNested attr_agg = response.getAggregations().get("attr_agg");
        ParsedLongTerms attr_id_agg = attr_agg.getAggregations().get("attr_id_agg");

        for (Terms.Bucket bucket : attr_id_agg.getBuckets()) {
            SearchResult.AttrVo attrVo = new SearchResult.AttrVo();
            long attrId = bucket.getKeyAsNumber().longValue();
            attrVo.setAttrId(attrId);

            ParsedStringTerms attr_name_agg = bucket.getAggregations().get("attr_name_agg");
            String attrName = attr_name_agg.getBuckets().get(0).getKeyAsString();
            attrVo.setAttrName(attrName);

            ParsedStringTerms attr_value_agg = bucket.getAggregations().get("attr_value_agg");
            List<String> attrValues = attr_value_agg.getBuckets().stream().map(b -> b.getKeyAsString()).collect(Collectors.toList());
            attrVo.setAttrValues(attrValues);
            attrVos.add(attrVo);
        }

        searchResult.setAttrs(attrVos);

        return searchResult;
    }
}
