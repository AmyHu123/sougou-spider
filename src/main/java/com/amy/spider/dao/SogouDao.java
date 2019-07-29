package com.amy.spider.dao;

import com.amy.spider.model.SogouDict;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

/**
 * @Author: amy
 * @Date: 2019/7/29
 */
public class SogouDao {

    @Autowired
    private MongoTemplate mongoTemplate;

    public void saveSogou(SogouDict sogouDict){
         mongoTemplate.save(sogouDict);
    }

    public long getByName(String word){
        Criteria criteria =Criteria.where("word").is(word);
        Query query = new Query(criteria);
        return mongoTemplate.count(query,SogouDict.class);
    }
}
