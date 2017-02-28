package com.tongji.wangjimin.tongjinews;

import android.annotation.TargetApi;

import com.tongji.wangjimin.tongjinews.net.ImportNews;
import com.tongji.wangjimin.tongjinews.net.NewsContent;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Created by wangjimin on 17/2/25.
 * Test for NewsContent.
 */

public class TestNewsContent {
    @TargetApi(24)
    @Test
    public void loadContent(){
        ImportNews importNews = ImportNews.getInstance();
        importNews.load(()->{
            NewsContent newsContent = new NewsContent(importNews.getNewsList().get(0),
                    ()->{});
//            System.out.println(newsContent.getContent());
            newsContent.getContent().forEach((c)-> assertTrue(c.startsWith("<p")));
        });

    }
}
