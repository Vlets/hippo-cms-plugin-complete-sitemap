package com.bloomreach.completesitemap;

import java.math.BigDecimal;

import org.junit.Test;
import org.onehippo.forge.sitemap.components.model.Url;
import org.onehippo.forge.sitemap.components.model.Urlset;

import com.bloomreach.completesitemap.components.CompleteSitemapFeed;

import static org.junit.Assert.assertEquals;


public class StreamsTesting {


    /* This test checks the remove duplicate urls method.
    *  The method should compare url lists of the landing pages
    *  and the documents. It checks for the urlLoc(url name), and
    *  removes all the duplicates after the first occurrence.
    *  The landing pages should be fed as the first parameter in
    *  order to be detected first and thus keep the high priority.
    */
    @Test
    public void removeDuplicatesTest() {
        CompleteSitemapFeed completeSitemapFeed = new CompleteSitemapFeed();


        Urlset expectedUrlset = new Urlset();
        Urlset urlSetLandingPages = new Urlset();
        Urlset urlSetDocuments = new Urlset();


        Url urlAbout = new Url();
        Url urlNews = new Url();
        Url urlAboutWrongPriority = new Url();


        urlAbout.setLoc("site/about");
        urlAbout.setPriority(new BigDecimal("1.0"));

        urlAboutWrongPriority.setLoc("site/about");
        urlAboutWrongPriority.setPriority(new BigDecimal("0.9"));

        urlNews.setLoc("site/news");
        urlNews.setPriority(new BigDecimal("0.9"));

        //Url set that we want to process
        urlSetLandingPages.getUrls().add(urlAbout);
        urlSetLandingPages.getUrls().add(urlNews);

        urlSetDocuments.getUrls().add(urlAboutWrongPriority);

        Urlset finalSet = completeSitemapFeed.removeDuplicateUrls(urlSetLandingPages, urlSetDocuments);

        //What the url set should contain after the operation
        expectedUrlset.getUrls().add(urlAbout);
        expectedUrlset.getUrls().add(urlNews);

        //The final set should contain the About page with the higher priority, and news
        assertEquals(expectedUrlset.getUrls().get(0).getPriority(), finalSet.getUrls().get(0).getPriority());
    }

}
