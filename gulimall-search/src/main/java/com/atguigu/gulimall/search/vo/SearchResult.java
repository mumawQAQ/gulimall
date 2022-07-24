package com.atguigu.gulimall.search.vo;

import com.atguigu.common.to.es.SkuEsModel;
import lombok.Data;

import java.util.List;

@Data
public class SearchResult {
    private List<SkuEsModel> products;
    private Integer pageNum;
    private Long total;
    private Integer totalPages;
    private List<BrandVo> brands;
    private List<AttrVo> attrs;
    private List<CatalogVo> catalogs;


    @Data
    private static class CatalogVo{
        private Long catalogId;
        private String catalogName;
    }
    @Data
    private static class BrandVo{
        private Long brandId;
        private String brandName;
        private String brandImg;
    }

    @Data
    private static class AttrVo{
        private Long attrId;
        private String attrName;
        private List<String> attrValues;
    }
}
