package com.amy.spider.crawlers;

import cn.wanghaomiao.seimi.annotation.Crawler;
import cn.wanghaomiao.seimi.def.BaseSeimiCrawler;
import cn.wanghaomiao.seimi.http.SeimiHttpType;
import cn.wanghaomiao.seimi.struct.Response;
import com.amy.spider.dao.SogouDao;
import com.amy.spider.model.SogouDict;
import com.amy.spider.utils.ScanFileUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * @Author: amy
 * @Date: 2019/7/29
 */
@Crawler(name = "pinyin.sogou.info", httpTimeOut = 20000, httpType = SeimiHttpType.OK_HTTP3)
public class SogouInfoCrawler extends BaseSeimiCrawler {

    private String crawler_start_url = "https://pinyin.sogou.com/dict/cate/index/167";

    @Autowired
    private SogouDao sogouDao;

    @Override
    public String[] startUrls() {
        return new String[]{crawler_start_url};
    }

    @Override
    public void start(Response response) {
        ArrayList<Object> list = null;
        try {
            list = ScanFileUtil.scanFilesWithRecursion("D:\\搜狗词库\\自然科学");

            list.forEach(o ->{
                String oldPath = o.toString();
                oldPath = oldPath.replaceAll("\\\\","/");
                String newPath = oldPath.substring(oldPath.indexOf("搜狗词库") + 5,oldPath.lastIndexOf("/"));
                System.out.println(newPath);
                readDict(oldPath,newPath);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 读取文章内容
     * @param path
     * @param category
     */
    public void readDict(String path,String category){
        String line = StringUtils.EMPTY;
        File file = new File(path);
        BufferedReader br = null;
        String[] splitted = null;
        String pinyin = StringUtils.EMPTY;
        String word = StringUtils.EMPTY;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));

            while ((line = br.readLine())!= null) {
                if(!line.equals("")){
                    splitted = line.split(" ");
                    pinyin = splitted[0];
                    word = splitted[1];
                    if(sogouDao.getByName(word) == 0l){
                        sogouDao.saveSogou(new SogouDict(category,pinyin,word,System.currentTimeMillis()));
                    }
                }
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
