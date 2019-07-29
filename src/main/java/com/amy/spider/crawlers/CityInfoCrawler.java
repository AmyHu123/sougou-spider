package com.amy.spider.crawlers;

import cn.wanghaomiao.seimi.annotation.Crawler;
import cn.wanghaomiao.seimi.def.BaseSeimiCrawler;
import cn.wanghaomiao.seimi.http.HttpMethod;
import cn.wanghaomiao.seimi.http.SeimiHttpType;
import cn.wanghaomiao.seimi.struct.Request;
import cn.wanghaomiao.seimi.struct.Response;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.seimicrawler.xpath.JXDocument;

import org.jsoup.nodes.Element;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: amy
 * @Date: 2019/7/25
 * 搜狗词库城市信息的爬虫
 */
//这里用的是本地的队列，也可以用redis,配置好redis，再下面的注解里加上queue = DefaultRedisQueue.class
@Crawler(name = "pinyin.sogou.cityInfo", httpTimeOut = 20000, httpType = SeimiHttpType.OK_HTTP3)
public class CityInfoCrawler extends BaseSeimiCrawler {

    private String crawler_start_url = "https://pinyin.sogou.com/dict/cate/index/167";

    //搜狗词库网页的根请求目录
    private String domain = "https://pinyin.sogou.com";

    private final String[] speArr = { "\\", "$", "(", ")", "*", "+", ".", "[", "]", "?", "^", "{", "}", "|","/"};

    //文件下载根目录
    private final String BASE_DIR = "D:\\SougouSpider";

    private int max_req_count = 5;

    @Override
    public String[] startUrls() {
        return new String[]{crawler_start_url};
    }

    @Override
    public void start(Response response) {
        if (response == null || response.document() == null) {
            throw new RuntimeException("start process error,url=" + response.getUrl());
        }

        JXDocument document = response.document();
        childTitles(document, "城市信息");

    }

    /**
     * 获取二级和三级的目录
     */
    public void childTitles(JXDocument document, String firstName) {
        List<Object> secondLevelList = document.sel("//div[@id='city_list_show']/table[@class='city_list']//tr//td//a");

        secondLevelList.forEach(s -> {
            Element element = (Element) s;
            String secondName = element.text();
            String secondHref = element.attr("href");
            if(!secondName.equals("国外地名")){

                Map<String, Object> meta = new HashMap<>();
                meta.put("fileDir", BASE_DIR + "/" + firstName + "/" + secondName );
                this.push(Request.build(domain + secondHref,"thirdLevelName",HttpMethod.GET,null,meta).setMaxReqCount(max_req_count));

            }else{
                Map<String, Object> meta = new HashMap<>();
                meta.put("fileDir", BASE_DIR + "/" + firstName + "/" + secondName);
                this.push(Request.build(domain + secondHref, "preDownload", HttpMethod.GET, null, meta).setMaxReqCount(max_req_count));
            }

        });

    }

    /**
     *  三级名称
     */
    public void thirdLevelName(Response response){
        Map<String, Object> meta = response.getMeta();
        List<Object> thirdLevelList = response.document().sel("//div[@id='dict_cate_show']/table[@class='cate_words_list']//tr//td//div[@class='cate_no_child no_select']//a//span");
        thirdLevelList.forEach(third -> {
                    Element thirdE = (Element) third;
                    Element a = thirdE.parent();
                    String thirdName = a.textNodes().get(0).toString();
                    String href = a.attr("href");
                    Map<String,Object> newMeta = new HashMap<>();
                    newMeta.put("fileDir",meta.get("fileDir") + "/" + thirdName);
                    this.push(Request.build(domain + href, "preDownload", HttpMethod.GET, null, newMeta).setMaxReqCount(max_req_count));
                });

    }

    /**
     * 生成文件路径
     */
    public void preDownload(Response response){
        Map<String, Object> meta = response.getMeta();
        List<Object> downloadList = response.document().sel("//div[@id='dict_detail_list']//div[@class='dict_dl_btn']/a/@href");
        downloadList.forEach(url -> {
            this.push(Request.build(url.toString(), "downFile", HttpMethod.GET, null, meta).setMaxReqCount(max_req_count));
        });

        //获取分页的每一个数字
        List<Object> pages =  response.document().sel("//div[@id='dict_page_list']/ul/li//a");

        if(!CollectionUtils.isEmpty(pages)) {
            //获取最大分页数字元素
            Element element = (Element) pages.get(pages.size()-2);
            String url = element.attr("href");
            int pageSize = Integer.parseInt(element.text());
            String base = domain+url.substring(0,url.lastIndexOf("/")+1);
            for (int i = 1; i <= pageSize; i++) {
                this.push(Request.build(base + i, "preDownload", HttpMethod.GET, null, meta).setMaxReqCount(max_req_count));
                logger.info("next page url add to queue:{" + base + i +"}");
            }
        }
    }

    /**
     * 下载文件
     * @param response
     */
    public void downFile(Response response) {
        String url = StringUtils.EMPTY;
        try {
            url = URLDecoder.decode(response.getUrl(), "utf-8");
            String name = url.substring(url.indexOf("name=") + 5, url.length());

            String id = url.substring(url.lastIndexOf("id=") + 3,url.indexOf("&"));

            //处理特殊字符
            if (StringUtils.isNotBlank(name)) {
                for (String key : speArr) {
                    if (name.contains(key)) {
                        name = name.replace(key, " ");
                    }
                }
            }

            String dir = response.getMeta().get("fileDir").toString();
            if(StringUtils.isNotBlank(dir)){
                FileUtils.writeByteArrayToFile(new File(dir + "/" + name + "_" + id + ".scel"), response.getData());
            }else{
                logger.error("请求下载路径为空,url:{" + url+ "}");
            }

        } catch (Exception e) {
            logger.error("下载文件发生异常,url:{" + url + "}",e);
            throw new RuntimeException(e);
        }
    }



//    @Override
//    public String proxy() {
//        return this.getProxy();
//    }

//    public String getProxy() {
//        String proxy = "";
//        OkHttpClient client = new OkHttpClient();
//        String scheme;
//        String ip_pool_url;
//        if (RandomUtils.nextInt(0, 10) < 8) {
//            scheme = "http";
//            ip_pool_url = crawler_http_pool_url;
//        } else {
//            scheme = "https";
//            ip_pool_url = crawler_https_pool_url;
//        }
//
//        okhttp3.Request request = new okhttp3.Request.Builder()
//                .url(ip_pool_url)
//                .build();
//
//        try (okhttp3.Response response = client.newCall(request).execute()) {
//            proxy = scheme + "://" + response.body().string();
//        } catch (IOException e) {
//            logger.error("{}", e.getMessage());
//        }
//        return proxy;
//    }
}
