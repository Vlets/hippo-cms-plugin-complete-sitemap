package com.bloomreach.completesitemap.components;

import java.math.BigDecimal;
import java.util.Calendar;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.repository.HippoStdPubWfNodeType;
import org.onehippo.forge.sitemap.components.UrlInformationProvider;
import org.onehippo.forge.sitemap.components.model.ChangeFrequency;

public class DefaultDocumentInformationProvider implements UrlInformationProvider {

    public DefaultDocumentInformationProvider() {
    }

    public BigDecimal getPriority(HippoBean hippoBean) {
        if (hippoBean.isHippoDocumentBean()) {
            return new BigDecimal("0.9");
        }
        return null;
    }

    //Called for landing pages
    public BigDecimal getPriority() {
        return new BigDecimal("1.0");
    }

    public Calendar getLastModified(HippoBean hippoBean) {
        return hippoBean.getProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_DATE);
    }


    public ChangeFrequency getChangeFrequency(HippoBean hippoBean) {
        return null;
    }

    /**
     * Returns the canonical link to the passed {@link HippoBean} by using the default {@link HstLinkCreator} in the
     * passed {@link HstRequestContext}.
     */
    public String getLoc(HippoBean hippoBean, HstRequestContext requestContext) {
        return getLoc(hippoBean, requestContext, requestContext.getResolvedMount().getMount());
    }

    public String getLoc(HstSiteMapItem hstSiteMapItem, HstRequestContext requestContext) {
        HstLinkCreator linkCreator = requestContext.getHstLinkCreator();
        return linkCreator.create(hstSiteMapItem, requestContext.getResolvedMount().getMount()).toUrlForm(requestContext, true);
    }

    public String getLoc(HippoBean hippoBean, HstRequestContext requestContext, Mount mount) {
        HstLinkCreator linkCreator = requestContext.getHstLinkCreator();
        return linkCreator.create(hippoBean.getNode(), mount).toUrlForm(requestContext, true);
    }
}
