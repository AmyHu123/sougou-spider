package com.amy.spider.crawlers;

import cn.wanghaomiao.seimi.annotation.Crawler;
import cn.wanghaomiao.seimi.def.BaseSeimiCrawler;
import cn.wanghaomiao.seimi.http.HttpMethod;
import cn.wanghaomiao.seimi.http.SeimiHttpType;
import cn.wanghaomiao.seimi.struct.Request;
import cn.wanghaomiao.seimi.struct.Response;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.seimicrawler.xpath.JXDocument;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * @Author: amy
 * @Date: 2019/7/24
 */
//这里用的是本地的队列，也可以用redis,配置好redis，再下面的注解里加上queue = DefaultRedisQueue.class
@Crawler(name = "pinyin.sogou", httpTimeOut = 20000, httpType = SeimiHttpType.OK_HTTP3)
public class SougouCrawler extends BaseSeimiCrawler {

    private String crawler_start_url = "https://pinyin.sogou.com/dict/cate/index/1";

    private String domain = "https://pinyin.sogou.com";

    private final String[] speArr = { "\\", "$", "(", ")", "*", "+", ".", "[", "]", "?", "^", "{", "}", "|","/",":"};

    private int max_req_count = 5;
    private final String BASE_DIR = "D:\\SougouSpider";
    private static final Map<Integer, String> L1_CATE = new HashMap<>();
    private final String CATE_BASE_URL = "https://pinyin.sogou.com/dict/cate/index/";

    static {
        L1_CATE.put(2, "自然科学");
        L1_CATE.put(76, "社会科学");
        L1_CATE.put(96, "工程应用");
        L1_CATE.put(127, "农林渔畜");
        L1_CATE.put(132, "医学医药");
        L1_CATE.put(436, "电子游戏");
        L1_CATE.put(154, "艺术设计");
        L1_CATE.put(389, "生活百科");
        L1_CATE.put(367, "运动休闲");
        L1_CATE.put(31, "人文科学");
        L1_CATE.put(403, "娱乐休闲");
    }

    @Override
    public String[] startUrls() {
        String[] urls = new String[11];
        int i = 0;
        for (Map.Entry<Integer, String> e : L1_CATE.entrySet()) {
            urls[i] = this.CATE_BASE_URL + e.getKey();
            i++;
        }
        return urls;
    }

    @Override
    public void start(Response response) {
        if (response == null || response.document() == null) {
            throw new RuntimeException("start process error,url=" + response.getUrl());
        }

        String url = response.getUrl();
        int cateId = NumberUtils.toInt(url.substring(url.lastIndexOf("/") + 1));
        String cateName = L1_CATE.get(cateId);


        JXDocument document = response.document();
        childTitles(document, cateName);


    }

    /**
     * 获取二级标题和三级标题
     *
     * @param document
     * @param cateName
     */
    private void childTitles(JXDocument document, String cateName) {
        //获取所有二级目录td
        List<Object> secondLevelList = document.sel("//div[@id='dict_cate_show']/table[@class='cate_words_list']//tr//td");
        secondLevelList.forEach(o -> {
            Element element = (Element) o;
            Element div = element.child(0);
            Element a = div.child(0);
            String secondLevelHref = a.attr("href");
            String secondLevelName = a.textNodes().get(0).toString();
            String secondPath = BASE_DIR + "/" + cateName + "/" + secondLevelName;

            //如果没有三级目录的二级目录，直接产生路径
            if (div.hasClass("cate_no_child")) {
                Map<String, Object> meta = new HashMap<>();
                meta.put("fileDir", secondPath);
                this.push(Request.build(domain + secondLevelHref, "extractDownloadUrl", HttpMethod.GET, null, meta).setMaxReqCount(max_req_count));
            } else if (div.hasClass("cate_has_child")) {
                //含有三级目录的，取出所有a标签
                Element sibling = div.nextElementSibling();
                Elements elements = sibling.select("a");
                Iterator<Element> itr = elements.iterator();

                while (itr.hasNext()) {
                    Element aa = itr.next();
                    String thirdLevelHref = aa.attr("href");
                    String thirdLevelName = aa.textNodes().get(0).toString();
                    Map<String, Object> meta = new HashMap<>();
                    meta.put("fileDir", secondPath + "/" + thirdLevelName);
                    this.push(Request.build(domain + thirdLevelHref, "extractDownloadUrl", HttpMethod.GET, null, meta).setMaxReqCount(max_req_count));
                }
            }

        });
    }

    /**
     * 获取下载的文件路径
     *
     * @param response
     */
    public void extractDownloadUrl(Response response) {
        Map<String, Object> meta = response.getMeta();
        List<Object> downloadList = response.document().sel("//div[@id='dict_detail_list']//div[@class='dict_dl_btn']/a/@href");
        downloadList.forEach(url -> {
            this.push(Request.build(url.toString(), "downFile", HttpMethod.GET, null, meta));
            logger.info("download path to queue:{" + url.toString() + "}");
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
                this.push(Request.build(base+i, "extractDownloadUrl", HttpMethod.GET, null, meta).setMaxReqCount(max_req_count));
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
                FileUtils.writeByteArrayToFile(new File(dir + "/" + name + "_" + id +  ".scel"), response.getData());
            }else{
                logger.error("请求下载路径为空,url:{" + url+ "}");
            }
        } catch (Exception e) {
            logger.error("下载文件异常,url:{" + url + "}",e);
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
