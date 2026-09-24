package com.palliser.backend;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("catalog_product")
public class CatalogProduct {
    @TableId private String slug;
    private String name;
    private String sku;
    private String smallImage;
    private Boolean detailAvailable;
    private String seriesId;
    public String getSlug() { return slug; }
    public void setSlug(String value) { slug=value; }
    public String getName() { return name; }
    public void setName(String value) { name=value; }
    public String getSku() { return sku; }
    public void setSku(String value) { sku=value; }
    public String getSmallImage() { return smallImage; }
    public void setSmallImage(String value) { smallImage=value; }
    public Boolean getDetailAvailable() { return detailAvailable; }
    public void setDetailAvailable(Boolean value) { detailAvailable=value; }
    public String getSeriesId() { return seriesId; }
    public void setSeriesId(String value) { seriesId=value; }
}
