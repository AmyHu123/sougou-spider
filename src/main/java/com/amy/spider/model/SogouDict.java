package com.amy.spider.model;


import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @Author: amy
 * @Date: 2019/7/29
 */
@Document(collection = "sogou_dict")
public class SogouDict {

    private String id;

    /** 类别 **/
    private String category;

    private String pinyin;

    /** 词 **/
    private String word;

    /** 创建时间 **/
    private long createTime;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getPinyin() {
        return pinyin;
    }

    public void setPinyin(String pinyin) {
        this.pinyin = pinyin;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public SogouDict(String category, String pinyin, String word, long createTime) {
        this.category = category;
        this.word = word;
        this.createTime = createTime;
        this.pinyin = pinyin;
    }

    public SogouDict(){

    }
}
