package org.jahia.community.modules.jahialibrarylinks.interceptor;

import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRStoreService;
import org.jahia.services.content.interceptor.BaseInterceptor;
import org.jahia.services.content.interceptor.URLInterceptor;
import org.jahia.services.content.nodetypes.ExtendedPropertyDefinition;
import org.jahia.services.render.filter.HtmlTagAttributeTraverser;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Value;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component(service = BaseInterceptor.class, immediate = true)
public class RichTextInterceptor extends URLInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(RichTextInterceptor.class);

    private JCRStoreService jcrStoreService;

    public RichTextInterceptor() {
        super(new RichTextTraverser());
        setUrlReplacers(Collections.singletonList(new RichTextReplacer(this)));
    }

    @Reference
    private void setJcrStoreService(JCRStoreService jcrStoreService) {
        this.jcrStoreService = jcrStoreService;
    }

    @Activate
    public void start() throws Exception {
        setRequiredTypes(Collections.singleton("String"));
        setSelectors(Collections.singleton("RichText"));
        jcrStoreService.addInterceptor(this);
        afterPropertiesSet();
    }

    @Deactivate
    public void stop() {
        jcrStoreService.removeInterceptor(this);
    }

    private static class RichTextTraverser extends HtmlTagAttributeTraverser {
        private static final Map<String, Set<String>> ATTRIBUTES_TO_VISIT = Stream.of(
                new AbstractMap.SimpleEntry<>("a", Collections.singleton("href")),
                new AbstractMap.SimpleEntry<>("embed", Collections.singleton("src")),
                new AbstractMap.SimpleEntry<>("form", Collections.singleton("action")),
                new AbstractMap.SimpleEntry<>("img", new HashSet(Arrays.asList("src", "srcset", "data-src", "data-srcset"))),
                new AbstractMap.SimpleEntry<>("source", Collections.singleton("srcset")),
                new AbstractMap.SimpleEntry<>("link", Collections.singleton("href")),
                new AbstractMap.SimpleEntry<>("param", Collections.singleton("value"))
        ).collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));

        public RichTextTraverser() {
            super(ATTRIBUTES_TO_VISIT);
        }
    }

    @Override
    public void beforeRemove(JCRNodeWrapper node, String name, ExtendedPropertyDefinition definition) {
        // Nothing to do
    }

    @Override
    public Value beforeSetValue(JCRNodeWrapper node, String name, ExtendedPropertyDefinition definition, Value originalValue) {
        return originalValue;
    }

    @Override
    public Value[] beforeSetValues(JCRNodeWrapper node, String name, ExtendedPropertyDefinition definition, Value[] originalValues) {
        return originalValues;
    }
}
