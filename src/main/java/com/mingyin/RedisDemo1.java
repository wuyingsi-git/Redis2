package com.mingyin;

import redis.clients.jedis.Jedis;

import java.util.*;

public class RedisDemo1 {

    private static final int ARTICLES_PER_PAGE = 5;

    public static void main(String[] args) {
        new RedisDemo1().run();
    }

    public void run() {
        //1、初始化redis连接
        Jedis conn = new Jedis("localhost");
        conn.select(10);
        //发布文章
//        String articleId = postArticle(
//                conn, "username2", "A title2", "http://www.google.com");
//        System.out.println("We posted a new article with id: " + articleId);

        String articleId = "1";
        articleVote(conn, "other_user", "article:" + articleId);

        List<Map<String,String>> articles = getArticles(conn, 1);
        printArticles(articles);

    }

    public String postArticle(Jedis conn, String user, String title, String link) {
        //1、生成一个新的文章ID
        String articleId = String.valueOf(conn.incr("article:"));//String.valueOf(int i) : 将 int 变量 i 转换成字符串

        String voted = "voted:" + articleId;
        //2、添加到记录文章已投用户名单中
        conn.sadd(voted, user);

        long now = System.currentTimeMillis() / 1000;
        String article = "article:" + articleId;
        //3、创建一个HashMap容器。
        HashMap<String,String> articleData = new HashMap<String,String>();
        articleData.put("title", title);
        articleData.put("link", link);
        articleData.put("user", user);
        articleData.put("now", String.valueOf(now));
        articleData.put("votes", "1");
        //4、将文章信息存储到一个散列里面。
        //HMSET key field value [field value ...]
        //同时将多个 field-value (域-值)对设置到哈希表 key 中。
        //此命令会覆盖哈希表中已存在的域。
        conn.hmset(article, articleData);
        //5、将文章添加到更具发布时间排序的有序集合。
        conn.zadd("time:", now, article);
        conn.zadd("vote:", now, article);

        return articleId;
    }

    public void articleVote(Jedis conn, String user, String article) {
        //1、从articleId标识符里面取出文章ID。
        //nt indexOf(int ch,int fromIndex)函数：就是字符ch在字串fromindex位后出现的第一个位置.没有找到返加-1
        //String.Substring (Int32)    从此实例检索子字符串。子字符串从指定的字符位置开始。
        String articleId = article.substring(article.indexOf(':') + 1);
        //2、检查用户是否第一次为这篇文章投票，如果是第一次，则在增加这篇文章的投票数量和评分。
        if (conn.sadd("voted:" + articleId, user) == 1) {
            //为哈希表 key 中的域 field 的值加上增量 increment 。
            //增量也可以为负数，相当于对给定域进行减法操作。
            //HINCRBY counter page_view 200
            conn.hincrBy(article, "votes", 1);
            conn.zincrby("vote:", 1, article);
        }
    }

    public List<Map<String,String>> getArticles(Jedis conn, int page,String order) {
        //1、设置获取文章的起始索引和结束索引。
        int start = (page - 1) * ARTICLES_PER_PAGE;
        int end = start + ARTICLES_PER_PAGE - 1;

        //2、获取多个文章ID
        Set<String> ids = conn.zrevrange(order, start, end);
        List<Map<String,String>> articles = new ArrayList<Map<String,String>>();
        for (String id : ids){
            //3、根据文章ID获取文章的详细信息
            Map<String,String> articleData = conn.hgetAll(id);
            articleData.put("id", id);
            //4、添加到ArrayList容器中
            articles.add(articleData);
        }

        return articles;
    }

    private void printArticles(List<Map<String,String>> articles){
        for (Map<String,String> article : articles){
            System.out.println("  id: " + article.get("id"));
            for (Map.Entry<String,String> entry : article.entrySet()){
                if (entry.getKey().equals("id")){
                    continue;
                }
                System.out.println("    " + entry.getKey() + ": " + entry.getValue());
            }
        }
    }

//    public void addGroups(Jedis conn, String articleId, String[] toAdd) {
//        //1、构建存储文章信息的键名
//        String article = "article:" + articleId;
//        for (String group : toAdd) {
//            //2、将文章添加到它所属的群组里面
//            conn.sadd("group:" + group, article);
//        }
//    }
//
//    public List<Map<String,String>> getGroupArticles(Jedis conn, String group, int page,String order){
//        //1、为每个群组的每种排列顺序都创建一个键
//        String key = order + group;
//        //2、检查是否有已缓存的排序结果，如果没有则进行排序
//        if (!conn.exists(key)){
//            //3、根据评分或者发布时间对群组文章进行排序
//            ZParams params = new ZParams().aggregate(ZParams.Aggregate.MAX);
//            conn.zinterstore(key,params,"group:" +group,order);
//            //让Redis在60秒之后自动删除这个有序集合
//            conn.expire(key,60);
//        }
//        //4、调用之前定义的getArticles()来进行分页并获取文章数据
//        return getArticles(conn, page);
//    }
}
