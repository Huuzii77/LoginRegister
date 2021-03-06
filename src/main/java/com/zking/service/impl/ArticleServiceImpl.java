 /*
  * Copyright 2019 tuhu.cn All right reserved. This software is the
  * confidential and proprietary information of tuhu.cn ("Confidential
  * Information"). You shall not disclose such Confidential Information and shall
  * use it only in accordance with the terms of the license agreement you entered
  * into with Tuhu.cn
  */
 package com.zking.service.impl;

 import com.alibaba.fastjson.JSONObject;
 import com.github.pagehelper.PageHelper;
 import com.github.pagehelper.PageInfo;
 import com.zking.component.StringAndArray;
 import com.zking.entity.Article;
 import com.zking.mapper.ArticleMapper;
 import com.zking.mapper.CategoriesMapper;
 import com.zking.mapper.TagsMapper;
 import com.zking.service.*;
 import com.zking.util.TimeUtil;

 import net.sf.json.JSONArray;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.stereotype.Service;

 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;


 /**
  * @auther chendesheng
  * @date 2019/1/29
  */
 @Service
 public class ArticleServiceImpl implements ArticleService {
     
     private Logger logger = LoggerFactory.getLogger(ArticleServiceImpl.class);
     @Autowired
     ArticleMapper articleMapper;
     @Autowired
     ArchiveService archiveService;
     @Autowired
     ArticleService articleService;
     @Autowired
     VisitorService visitorService;
     @Autowired
     CategoriesService categoriesService;
     @Autowired
     TagService tagService;
     @Autowired
     CategoriesMapper categoriesMapper;
     @Autowired
     TagsMapper tagsMapper;
    
     
     @Override
     public int insert(Article article) {
         
         return  articleMapper.insertSelective(article);
     }
    
     
     @Override
     public JSONObject insertArticle(Article article) {
         JSONObject articleReturn = new JSONObject();
         try {
             if ("".equals(article.getOriginalauthor())){
                 article.setOriginalauthor(article.getAuthor());
             }
             if ("".equals(article.getArticleurl())){
                 String url = "https://www.hellotiger.top/findArticle?articleId=" + article.getArticleid() + "&originalAuthor=" + article.getOriginalauthor();
                 article.setArticleurl(url);
             }
             Article endArticleId = articleMapper.findEndArticleId();
             //设置文章的上一篇文章id
             if (endArticleId != null){
                 article.setLastarticleid(endArticleId.getArticleid());
             }
             articleMapper.insertArticle(article);
            
             //判断发表文章的归档日期是否存在，不存在则插入归档日期
             TimeUtil timeUtil = new TimeUtil();
             String archiveName = timeUtil.timeWhippletreeToYear(article.getPublishdate().substring(0, 7));
             archiveService.addArchiveName(archiveName);
             //设置上一篇文章的下一篇文章id
             if(endArticleId != null){
                 articleService.updateArticleLastOrNextId("nextArticleId", article.getArticleid(), endArticleId.getArticleid());
             }
             
             articleReturn.put("status",200);
             articleReturn.put("articleTitle",article.getArticletitle());
             articleReturn.put("updateDate",article.getUpdatedate());
             articleReturn.put("author",article.getOriginalauthor());
             //本博客中的URL
             articleReturn.put("articleUrl","/findArticle?articleId=" + article.getArticleid() + "&originalAuthor=" + article.getOriginalauthor());
             return articleReturn;
             
         }catch (Exception e){
             articleReturn.put("status",500);
             logger.error("用户 " + article.getAuthor() + " 保存文章 " + article.getArticletitle() + " 失败");
             e.printStackTrace();
             return articleReturn;
         }
         
     }
    
     
     
     @Override
     public void updateArticleLastOrNextId(String lastOrNext, long lastOrNextArticleId, long articleId) {
         
         if("lastArticleId".equals(lastOrNext)){
             articleMapper.updateArticleLastId(lastOrNextArticleId, articleId);
         }
         if("nextArticleId".equals(lastOrNext)){
             articleMapper.updateArticleNextId(lastOrNextArticleId, articleId);
         }
     }
    
     @Override
     public JSONObject updateArticleById(Article article) {
         Article a = articleMapper.getArticleUrlById(article.getId());
         if("原创".equals(article.getArticletype())){
             article.setOriginalauthor(article.getAuthor());
             String url = "https://www.hellotiger.top/findArticle?articleId=" + a.getArticleid() + "&originalAuthor=" + a.getOriginalauthor();
             article.setArticleurl(url);
         }
         articleMapper.updateArticleById(article);
         JSONObject articleReturn = new JSONObject();
         articleReturn.put("status",200);
         articleReturn.put("articleTitle",article.getArticletitle());
         articleReturn.put("updateDate",article.getUpdatedate());
         articleReturn.put("author",article.getOriginalauthor());
         //本博客中的URL
         articleReturn.put("articleUrl","/findArticle?articleId=" + a.getArticleid() + "&originalAuthor=" + a.getOriginalauthor());
         return articleReturn;
     }
    
     @Override
     public JSONObject findArticleByCategory(String category, int rows, int pageNum) {
         List<Article> articles;
         PageInfo<Article> pageInfo;
         JSONArray articleJsonArray = new JSONArray();
         PageHelper.startPage(pageNum, rows);
         if("".equals(category)){
             articles = articleMapper.findAllArticlesPartInfo();
             category = "全部分类";
         } else {
             articles = articleMapper.findArticleByCategory(category);
         }
         pageInfo = new PageInfo<>(articles);
         articleJsonArray = timeLineReturn(articleJsonArray, articles);
         JSONObject pageJson = new JSONObject();
         pageJson.put("pageNum",pageInfo.getPageNum());
         pageJson.put("pageSize",pageInfo.getPageSize());
         pageJson.put("total",pageInfo.getTotal());
         pageJson.put("pages",pageInfo.getPages());
         pageJson.put("isFirstPage",pageInfo.isIsFirstPage());
         pageJson.put("isLastPage",pageInfo.isIsLastPage());
    
         JSONObject jsonObject = new JSONObject();
         jsonObject.put("status",200);
         jsonObject.put("result",articleJsonArray);
         jsonObject.put("category",category);
         jsonObject.put("pageInfo",pageJson);
    
         return jsonObject;
     }
    
    
    
    
    
    
     /**
      * 封装时间线中数据成JsonArray形式
      */
     private JSONArray timeLineReturn(JSONArray articleJsonArray, List<Article> articles){
         JSONObject articleJson;
         for(Article article : articles){
             String[] tagsArray = StringAndArray.stringToArray(article.getArticletags());
             articleJson = new JSONObject();
             articleJson.put("id",article.getId());
             articleJson.put("articleId", article.getArticleid());
             articleJson.put("originalAuthor", article.getOriginalauthor());
             articleJson.put("articleTitle", article.getArticletitle());
             articleJson.put("articleCategories", article.getArticlecategories());
             articleJson.put("publishDate", article.getPublishdate());
             articleJson.put("articleTags", tagsArray);
             articleJsonArray.add(articleJson);
         }
         return articleJsonArray;
     }
    
     @Override
     public int countArticleCategoryByCategory(String category) {
         return articleMapper.countArticleCategoryByCategory(category);
     }
    
     @Override
     public JSONObject findArticleByTag(String tag, int rows, int pageNum) {
         
         PageHelper.startPage(pageNum, rows);
         List<Article> articles = articleMapper.findArticleByTag(tag);
         PageInfo<Article> pageInfo = new PageInfo<>(articles);
         JSONObject articleJson;
         JSONArray articleJsonArray = new JSONArray();
         //二次判断标签是否匹配
         for(Article article : articles){
             String[] tagsArray = StringAndArray.stringToArray(article.getArticletags());
             for(String str : tagsArray){
                 if(str.equals(tag)){
                     articleJson = new JSONObject();
                     articleJson.put("articleId", article.getArticleid());
                     articleJson.put("originalAuthor", article.getOriginalauthor());
                     articleJson.put("articleTitle", article.getArticletitle());
                     articleJson.put("articleCategories", article.getArticlecategories());
                     articleJson.put("publishDate", article.getPublishdate());
                     articleJson.put("articleTags", tagsArray);
                     articleJsonArray.add(articleJson);
                 }
             }
         }
    
         JSONObject pageJson = new JSONObject();
         pageJson.put("pageNum",pageInfo.getPageNum());
         pageJson.put("pageSize",pageInfo.getPageSize());
         pageJson.put("total",pageInfo.getTotal());
         pageJson.put("pages",pageInfo.getPages());
         pageJson.put("isFirstPage",pageInfo.isIsFirstPage());
         pageJson.put("isLastPage",pageInfo.isIsLastPage());
    
         JSONObject jsonObject = new JSONObject();
         jsonObject.put("status",201);
         jsonObject.put("result",articleJsonArray);
         jsonObject.put("tag",tag);
         jsonObject.put("pageInfo",pageJson);
         return jsonObject;
     }
    

     @Override
     public List<Article> listArticles() {
         return articleMapper.listArticles();
     }
     
     @Override
     public JSONArray findAllArticles(String rows, String pageNO) {
         int pageNum = Integer.parseInt(pageNO);
         int pageSize = Integer.parseInt(rows);
    
         PageHelper.startPage(pageNum,pageSize);
         List<Article> articles = articleMapper.listArticles();
         PageInfo<Article> pageInfo = new PageInfo<>(articles);
         List<Map<String, Object>> newArticles = new ArrayList<>();
         Map<String, Object> map;
    
         for(Article article : articles){
             map = new HashMap<>();
             map.put("id",article.getId());
             map.put("articleTitle", article.getArticletitle());
             map.put("articleType", article.getArticletype());
             map.put("publishDate", article.getPublishdate());
             map.put("author", article.getAuthor());
             map.put("articleCategories", article.getArticlecategories());
             map.put("articleTabloid", article.getArticletabloid());
            
             newArticles.add(map);
         }
         JSONArray jsonArray = JSONArray.fromObject(newArticles);
         
         Map<String, Object> thisPageInfo = new HashMap<>();
         thisPageInfo.put("pageNum",pageInfo.getPageNum());
         thisPageInfo.put("pageSize",pageInfo.getPageSize());
         thisPageInfo.put("total",pageInfo.getTotal());
         thisPageInfo.put("pages",pageInfo.getPages());
         thisPageInfo.put("isFirstPage",pageInfo.isIsFirstPage());
         thisPageInfo.put("isLastPage",pageInfo.isIsLastPage());
    
         jsonArray.add(thisPageInfo);
         return jsonArray;
     }
     
     
     
     @Override
     public Article getById(int id) {
         return articleMapper.selectByPrimaryKey(id);
     }
    
     @Override
     public int countArticleNum() {
         return articleMapper.countArticle();
     }
    
     @Override
     public int countArticleByCategoryName(String categoryName) {
         return articleMapper.countByCategoryName(categoryName);
     }
    
     @Override
     public int countArticleByTags(String tagsName) {
         return articleMapper.countByTags(tagsName);
     }
    
     
    
     // todo 
     // 每篇文章的浏览量需要pageName来查找
     @Override
     public JSONObject getArticleManagement(int rows, int pageNum) {
         PageHelper.startPage(pageNum, rows);
         List<Article> articles = articleMapper.getArticleManagement();
         PageInfo<Article> pageInfo = new PageInfo<>(articles);
         JSONArray returnJsonArray = new JSONArray();
         JSONObject returnJson = new JSONObject();
         JSONObject articleJson;
         for(Article article : articles){
             articleJson = new JSONObject();
             articleJson.put("id",article.getId());
             articleJson.put("articleTitle",article.getArticletitle());
             articleJson.put("author",article.getAuthor());
             articleJson.put("publishDate",article.getPublishdate());
             articleJson.put("articleType",article.getArticletype());
             articleJson.put("articleTabloid",article.getArticletabloid());
             articleJson.put("articleCategories",article.getArticlecategories());
             
//             String pageName = "findArticle?articleId=" + article.getArticleid() + "&originalAuthor=" + article.getOriginalauthor();
             String pageName = "findArticle?articleId=1533196734&originalAuthor=张海洋";
             articleJson.put("visitorNum",visitorService.getNumByPageName(pageName));
        
             returnJsonArray.add(articleJson);
         }
         returnJson.put("status",200);
         returnJson.put("result",returnJsonArray);
         JSONObject pageJson = new JSONObject();
         pageJson.put("pageNum",pageInfo.getPageNum());
         pageJson.put("pageSize",pageInfo.getPageSize());
         pageJson.put("total",pageInfo.getTotal());
         pageJson.put("pages",pageInfo.getPages());
         pageJson.put("isFirstPage",pageInfo.isIsFirstPage());
         pageJson.put("isLastPage",pageInfo.isIsLastPage());
    
         returnJson.put("pageInfo",pageJson);
    
         return returnJson;
     }
    
     @Override
     public int deleteArticle(Integer id) {
         try {
             
             // TODO: 2019/2/21  删除文章的同时删除分类和标签
             //删除本篇文章的tag和categories,count(categoryName)==1时删除分类或标签   
             Article article = articleMapper.selectByPrimaryKey(id);
             String categoryName = article.getArticlecategories();
             String tagsName = article.getArticletags();
             int countByCategoryName = articleMapper.countByCategoryName(categoryName);
             int countByTagsName = articleMapper.countByTags(tagsName);
             if (countByCategoryName == 1){
                 categoriesMapper.deleteByCategoryName(categoryName);
             }
             if (countByTagsName == 1){
                 tagsMapper.deleteByTagsName(tagsName);
             }
    
             //删除本篇文章
             articleMapper.deleteByPrimaryKey(id);
             
         }catch (Exception e){
             logger.error("删除文章失败，文章id=" + id);
             return 0;
         }
         return 1;
     }
    
     
     @Override
     public int deleteArticleByCategoryName(String categoryName) {
         try {
             //通过分类名删除文章
             articleMapper.deleteByCategoryName(categoryName);
        
         }catch (Exception e){
             logger.error("删除文章失败，文章categoryName=" + categoryName);
             return 0;
         }
         return 1;
     }
    
     
 }
