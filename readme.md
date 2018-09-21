# Complete sitemap plugin

This plugin is an extension to the Hippo essentials sitemap.xml plugin.
In addition to adding hippo documents and menu items to the sitemap.xml, it also adds any other **live** pages 
that are the direct children of the **hst:hst/hst:configurations/mysitename/hst:sitemap** and
**hst:hst/hst:configurations/mysitename/hst:workspace/hst:sitemap** nodes. It sets the priority of the aforementioned nodes to 1.0, 
and to the priority of any retrieved hippo documents to 0.9.

This creates a sitemap.xml of the whole website that is callable by default at
**http://localhost:8080/site/complete-sitemap.xml**.

## Installation
Before installing this plugin, you **MUST** have the Sitemap essentials plugin installed.

### POM.xml

In the POM.xml in your *cms* folder, add:

    <dependency>
      <groupId>org.onehippo.cms7</groupId>
      <version>1.0</version>
      <artifactId>hippo-plugin-complete-sitemap-content</artifactId>
    </dependency>
    
In the POM.xml in your *site* folder, add:

    <dependency>
      <groupId>org.onehippo.cms7</groupId>
      <version>1.0</version>
      <artifactId>hippo-plugin-complete-sitemap-component</artifactId>
    </dependency>

## Usage
By default, the plugin will not retrieve any documents. In 
**/hst:hst/hst:configurations/hst:default/hst:sitemap/complete-sitemap.xml** you should add any document types you wish to retrieve.
In the corresponding **hst:parameternames** of documentTypes, add all 
document types you wish to include separated by commas.
E.g. for news documents generated by the essentials News plugin, you add **mysitename:newsdocument**.