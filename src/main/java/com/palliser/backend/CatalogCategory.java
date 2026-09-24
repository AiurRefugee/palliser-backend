package com.palliser.backend;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("catalog_category")
public class CatalogCategory {
    @TableId private String uid;
    private String name;
    private String path;
    private String parentUid;
    private Boolean active;
    public String getUid() { return uid; }
    public void setUid(String value) { uid=value; }
    public String getName() { return name; }
    public void setName(String value) { name=value; }
    public String getPath() { return path; }
    public void setPath(String value) { path=value; }
    public String getParentUid() { return parentUid; }
    public void setParentUid(String value) { parentUid=value; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean value) { active=value; }
}
