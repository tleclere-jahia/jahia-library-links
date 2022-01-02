package org.jahia.community.modules.jahialibrarylinks.interceptor;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.util.Text;
import org.jahia.api.Constants;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.content.interceptor.URLInterceptor;
import org.jahia.services.content.interceptor.url.BaseURLReplacer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;

public class RichTextReplacer extends BaseURLReplacer {
    private static final Logger logger = LoggerFactory.getLogger(RichTextReplacer.class);

    public RichTextReplacer(URLInterceptor urlInterceptor) {
        setUrlInterceptor(urlInterceptor);
    }

    @Override
    public String replacePlaceholdersByRefs(String originalValue, Map<Long, String> refs, String workspaceName, Locale locale, JCRNodeWrapper parent) throws RepositoryException {
        String pathPart = originalValue;
        if (logger.isDebugEnabled()) {
            logger.debug("Before replacePlaceholdersByRefs : " + originalValue);
        }
        final boolean isCmsContext;

        if (pathPart.startsWith(URLInterceptor.DOC_CONTEXT_PLACEHOLDER)) {
            // Remove DOC context part
            pathPart = StringUtils.substringAfter(StringUtils.substringAfter(pathPart, URLInterceptor.DOC_CONTEXT_PLACEHOLDER), "/");
            isCmsContext = false;
        } else if (pathPart.startsWith(URLInterceptor.CMS_CONTEXT_PLACEHOLDER)) {
            // Remove CMS context part
            Matcher m = getUrlInterceptor().getCmsPatternWithContextPlaceholder().matcher(pathPart);
            if (!m.matches()) {
                logger.error("Cannot match URL : " + pathPart);
                return originalValue;
            }
            pathPart = m.group(5);
            isCmsContext = true;
        } else {
            return originalValue;
        }

        final String path = "/" + pathPart;
        return JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, workspaceName, null, session -> {
            String value = originalValue;
            try {
                Matcher matcher = getUrlInterceptor().getRefPattern().matcher(path);
                if (!matcher.matches()) {
                    logger.error("Cannot match value, should contain ##ref : " + path);
                    return originalValue;
                }
                String id = matcher.group(1);
                String ext = matcher.group(2);
                String uuid = refs.get(Long.valueOf(id));

                JCRNodeWrapper node = null;
                if (!StringUtils.isEmpty(uuid)) {
                    try {
                        node = session.getNodeByUUID(uuid);
                    } catch (ItemNotFoundException infe) {
                        // Warning is logged below (also if uuid is empty)
                    }
                }
                if (node == null) {
                    logger.warn("Cannot find referenced item : " + parent.getPath() + " -> " + path + " -> " + uuid);
                    return "#";
                }

                if (node.isNodeType(Constants.JAHIANT_EXTERNAL_PAGE_LINK)) {
                    return node.getPropertyAsString(Constants.URL);
                }

                String nodePath = Text.escapePath(node.getPath());
                value = originalValue.replace(path, nodePath + ext);
                if (isCmsContext) {
                    value = URLInterceptor.CMS_CONTEXT_PLACEHOLDER_PATTERN.matcher(value).replaceAll(getUrlInterceptor().getCmsContext());
                } else {
                    value = URLInterceptor.DOC_CONTEXT_PLACEHOLDER_PATTERN.matcher(value).replaceAll(getUrlInterceptor().getDmsContext());
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("After replacePlaceholdersByRefs : " + value);
                }
            } catch (Exception e) {
                logger.error("Exception when transforming placeholder for " + parent.getPath() + " -> " + path, e);
            }
            return value;
        });
    }
}
