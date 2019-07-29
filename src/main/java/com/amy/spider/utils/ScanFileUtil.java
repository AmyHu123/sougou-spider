package com.amy.spider.utils;

import java.io.File;
import java.util.ArrayList;

/**
 * @Author: amy
 * @Date: 2019/7/29
 */
public class ScanFileUtil {
    private static ArrayList<Object> scanFiles = new ArrayList<Object>();

    public static ArrayList<Object> scanFilesWithRecursion(String folderPath) throws Exception{
        ArrayList<String> dirctorys = new ArrayList<String>();
        File directory = new File(folderPath);
        if(!directory.isDirectory()){
            throw new RuntimeException('"' + folderPath + '"' + " input path is not a Directory , please input the right path of the Directory. ^_^...^_^");
        }
        if(directory.isDirectory()){
            File [] filelist = directory.listFiles();
            for(int i = 0; i < filelist.length; i ++){
                /**如果当前是文件夹，进入递归扫描文件夹**/
                if(filelist[i].isDirectory()){
                    //绝对路径名字符串
                    dirctorys.add(filelist[i].getAbsolutePath());
                    /**递归扫描下面的文件夹**/
                    scanFilesWithRecursion(filelist[i].getAbsolutePath());
                }
                /**非文件夹**/
                else{
                    scanFiles.add(filelist[i].getAbsolutePath());
                }
            }
        }

        return scanFiles;
    }
}
