package com.bloomreach.completesitemap.components;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.HstQueryResult;
import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.query.filter.Filter;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoBeanIterator;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.onehippo.forge.sitemap.components.model.Url;
import org.onehippo.forge.sitemap.components.model.Urlset;
import org.onehippo.forge.sitemap.components.util.MatcherUtils;
import org.onehippo.forge.sitemap.generator.SitemapGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toCollection;

@SuppressWarnings({"unused"})
@ParametersInfo(type = CompleteSitemapFeed.CompleteSitemapFeedParametersInformation.class)
public class CompleteSitemapFeed extends BaseHstComponent {

    private static final Logger LOG = LoggerFactory.getLogger(CompleteSitemapFeed.class);
    private static final DefaultDocumentInformationProvider defaultDocumentInformationProvider = new DefaultDocumentInformationProvider();

    public interface CompleteSitemapFeedParametersInformation {

        @Parameter(
                name = "documentTypes",
                required = true,
                defaultValue = ""
        )
        String getDocumentTypes();

        @Parameter(
                name = "timezone",
                defaultValue = "UTC"
        )
        String getTimezone();

        @Parameter(
                name = "propertyCriteria",
                defaultValue = ""
        )
        String getPropertyCriteria();

    }

    @Override
    public void doBeforeRender(final HstRequest request, final HstResponse response) {
        super.doBeforeRender(request, response);


        CompleteSitemapFeedParametersInformation parameters = getComponentParametersInfo(request);
        verifyRequiredParametersAreFilled(parameters);

        String[] documentTypes = MatcherUtils.getCommaSeparatedValues(parameters.getDocumentTypes());

        Map<String, String> propertyCriteria = parsePropertyCriteria(parameters.getPropertyCriteria());

        HstQuery query = createQuery(
                request,
                documentTypes,
                parameters.getTimezone(),
                propertyCriteria);

        HstQueryResult result, resultLandingPages;

        try {
            result = query.execute();
        } catch (QueryException e) {
            LOG.error("Domain sitemap cannot be created que to a problem in the query", e);
            throw new IllegalArgumentException("passed query resulted in an Exception", e);
        }

        List<HstSiteMapItem> hstSiteMapItems = request.getRequestContext().getResolvedMount().getMount().getHstSite().getSiteMap().getSiteMapItems();

        Urlset urlSetLandingPages = createSitemapUrlSet(request, hstSiteMapItems);
        Urlset urlSetDocuments = createSitemapUrlSet(request, result);
        Urlset urlSetToSitemap = removeDuplicateUrls(urlSetLandingPages, urlSetDocuments);

        String sitemap = SitemapGenerator.toString(urlSetToSitemap);
        request.setAttribute("sitemap", sitemap);
    }

    public Urlset removeDuplicateUrls(Urlset landingPagesSet, Urlset lowPrioritySet){
        Urlset urlSetCombined = new Urlset();
        Urlset urlSetWithoutDuplicates = new Urlset();
        urlSetCombined.getUrls().addAll(landingPagesSet.getUrls());
        urlSetCombined.getUrls().addAll(lowPrioritySet.getUrls());

        //Filter out duplicate pages. Give priority to first occurrence.
        List<Url> urlList = urlSetCombined.getUrls().stream().collect(collectingAndThen(toCollection(() -> new TreeSet<>(Comparator.comparing(Url::getLoc))),
                ArrayList::new));

        urlSetWithoutDuplicates.getUrls().addAll(urlList);
        return urlSetWithoutDuplicates;
    }

    // Creates URL set from all document beans that resulted from a query
    private Urlset createSitemapUrlSet(HstRequest request, HstQueryResult result) {
        Url documentUrl;
        HstRequestContext requestContext = request.getRequestContext();
        Urlset urlSet = new Urlset();

        final HippoBeanIterator hippoBeanIterator = result.getHippoBeans();

        while (hippoBeanIterator.hasNext()) {
            final HippoBean hippoBean = hippoBeanIterator.nextHippoBean();
            if (hippoBean == null) {
                // If this node cannot be mapped to a HippoBean, then jump to the next bean
                LOG.debug("Skipping node, because it cannot be mapped to a hippo bean");
                continue;
            }
            documentUrl = createDocumentUrlForHippoBean(hippoBean, requestContext);
            urlSet.getUrls().add(documentUrl);
        }
        return urlSet;
    }

    private Urlset createSitemapUrlSet(HstRequest request, List<HstSiteMapItem> hstSiteMapItems) {
        Url documentUrl;
        HstRequestContext requestContext = request.getRequestContext();
        Urlset urlSet = new Urlset();

        final ListIterator<HstSiteMapItem> hstSiteMapItemListIterator = hstSiteMapItems.listIterator();

        while (hstSiteMapItemListIterator.hasNext()) {
            final HstSiteMapItem hstSiteMapItem = hstSiteMapItemListIterator.next();
            if (hstSiteMapItem.getPageTitle() == null
                    || ( hstSiteMapItem.getRefId() != null && hstSiteMapItem.getRefId().equals("pagenotfound") ) ){
                LOG.debug("Skipping node, because it is a wild card node or 404 page");
                continue;
            }
            documentUrl = createDocumentUrlForSitemapItem(hstSiteMapItem, requestContext);
            urlSet.getUrls().add(documentUrl);
        }
        return urlSet;
    }

    // Creates URL from the document bean.
    private Url createDocumentUrlForHippoBean(HippoBean hippoBean, HstRequestContext requestContext) {
        Url url = new Url();

        url.setPriority(defaultDocumentInformationProvider.getPriority(hippoBean));
        url.setLastmod(defaultDocumentInformationProvider.getLastModified(hippoBean));
        url.setLoc(defaultDocumentInformationProvider.getLoc(hippoBean, requestContext));
        url.setChangeFrequency(defaultDocumentInformationProvider.getChangeFrequency(hippoBean));

        return url;
    }

    // Create URL from a sitemap node.
    private Url createDocumentUrlForSitemapItem(HstSiteMapItem hstSiteMapItem, HstRequestContext requestContext) {
        Url url = new Url();

        // Priority is set to 1.0 for these.
        url.setPriority(defaultDocumentInformationProvider.getPriority());
        url.setLoc(defaultDocumentInformationProvider.getLoc(hstSiteMapItem, requestContext));

        return url;
    }


    //Creates the Query from the request and parameters
    private HstQuery createQuery(final HstRequest request, final String[] documentTypes, final String timezone,
                                 final Map<String, String> propertyCriteria) {
        HippoBean siteContentBaseBean = request.getRequestContext().getSiteContentBaseBean();

        HstQuery query;
        try {
            query = RequestContextProvider.get().getQueryManager().createQuery(siteContentBaseBean, documentTypes);

            Filter filter = query.createFilter();

            if (!propertyCriteria.isEmpty()) {
                // Add property criteria
                for (Map.Entry<String, String> propertyCriterion : propertyCriteria.entrySet()) {
                    filter.addEqualTo(propertyCriterion.getKey(), propertyCriterion.getValue());
                }
            }

            query.setFilter(filter);
        } catch (QueryException e) {
            throw new HstComponentException("Cannot create HstQuery", e);
        }

        return query;
    }



    private static void verifyRequiredParametersAreFilled(final CompleteSitemapFeedParametersInformation parameters) {

        if (StringUtils.isEmpty(parameters.getDocumentTypes())) {
            throw new HstComponentException("No document types specified, please pass the parameter documentTypes");
        }
    }


    /**
     * Takes an input string with comma separated property criteria (in the format prop1=condition1,prop2=condiion2...)
     * and returns a map with key = property and value = criterion
     *
     * @param propertyCriteria comma separated list of property criteria
     * @return {@link Map} containing the property criteria
     */
    private static Map<String, String> parsePropertyCriteria(final String propertyCriteria) {
        if (StringUtils.isEmpty(propertyCriteria)) {
            return Collections.emptyMap();
        }
        final String[] criteria = MatcherUtils.getCommaSeparatedValues(propertyCriteria);
        final Map<String, String> propertiesWithCriteria = new HashMap<>();
        for (final String criterion : criteria) {

            final String[] propertyValuePair = MatcherUtils.parsePropertyValue(criterion);
            if (propertyValuePair == null) {
                throw new HstComponentException("Criterion '" + criterion + "' doet not have format 'property=value'");
            }
            propertiesWithCriteria.put(propertyValuePair[0], propertyValuePair[1]);
        }

        return propertiesWithCriteria;
    }


}





