package com.amy.spider.main;

import cn.wanghaomiao.seimi.core.Seimi;

/**
 * @Author: amy
 * @Date: 2019/7/23
 */
public class Boot {

    public static void main(String[] args) {

        Seimi seimi = new Seimi();
        seimi.goRun("pinyin.sogou.info");
    }
}
