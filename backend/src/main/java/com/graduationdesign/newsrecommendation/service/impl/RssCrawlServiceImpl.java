package com.graduationdesign.newsrecommendation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduationdesign.newsrecommendation.common.RemoteUrlValidator;
import com.graduationdesign.newsrecommendation.entity.CrawlConfig;
import com.graduationdesign.newsrecommendation.entity.News;
import com.graduationdesign.newsrecommendation.entity.NewsTag;
import com.graduationdesign.newsrecommendation.entity.Tag;
import com.graduationdesign.newsrecommendation.mapper.CrawlConfigMapper;
import com.graduationdesign.newsrecommendation.mapper.NewsMapper;
import com.graduationdesign.newsrecommendation.mapper.NewsTagMapper;
import com.graduationdesign.newsrecommendation.mapper.TagMapper;
import com.graduationdesign.newsrecommendation.service.CacheInvalidationService;
import com.graduationdesign.newsrecommendation.service.CrawlService;
import com.graduationdesign.newsrecommendation.vo.CrawlRunResultVO;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

@Service
public class RssCrawlServiceImpl implements CrawlService {

    private static final List<DateTimeFormatter> RSS_DATE_FORMATTERS = List.of(
        DateTimeFormatter.RFC_1123_DATE_TIME,
        DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm Z", Locale.ENGLISH),
        DateTimeFormatter.ISO_ZONED_DATE_TIME
    );

    private final HttpClient httpClient;
    private final NewsMapper newsMapper;
    private final NewsTagMapper newsTagMapper;
    private final TagMapper tagMapper;
    private final CrawlConfigMapper crawlConfigMapper;
    private final CacheInvalidationService cacheInvalidationService;

    public RssCrawlServiceImpl(
        NewsMapper newsMapper,
        NewsTagMapper newsTagMapper,
        TagMapper tagMapper,
        CrawlConfigMapper crawlConfigMapper,
        CacheInvalidationService cacheInvalidationService
    ) {
        this.httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(Duration.ofSeconds(15))
            .build();
        this.newsMapper = newsMapper;
        this.newsTagMapper = newsTagMapper;
        this.tagMapper = tagMapper;
        this.crawlConfigMapper = crawlConfigMapper;
        this.cacheInvalidationService = cacheInvalidationService;
    }

    @Override
    public CrawlRunResultVO runRssCrawl(CrawlConfig crawlConfig) {
        LocalDateTime now = LocalDateTime.now();
        try {
            CrawlRunResultVO result = doRunRssCrawl(crawlConfig);
            crawlConfig.setLastCrawlTime(now);
            crawlConfig.setLastCrawlCount(result.getInsertedCount());
            crawlConfig.setLastStatus("SUCCESS");
            crawlConfig.setLastError(null);
            crawlConfigMapper.updateById(crawlConfig);

            result.setLastStatus("SUCCESS");
            return result;
        } catch (Exception exception) {
            crawlConfig.setLastCrawlTime(now);
            crawlConfig.setLastCrawlCount(0);
            crawlConfig.setLastStatus("FAILED");
            crawlConfig.setLastError(truncate(exception.getMessage(), 500));
            crawlConfigMapper.updateById(crawlConfig);
            throw new IllegalArgumentException("RSS crawl failed: " + normalizeMessage(exception));
        }
    }

    private CrawlRunResultVO doRunRssCrawl(CrawlConfig crawlConfig) throws Exception {
        URI sourceUri = RemoteUrlValidator.validatePublicHttpUrl(crawlConfig.getSourceUrl());

        HttpRequest request = HttpRequest.newBuilder()
            .uri(sourceUri)
            .timeout(Duration.ofSeconds(20))
            .header("User-Agent", "NewsRecommendationBot/1.0")
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Remote RSS responded with status " + response.statusCode());
        }

        List<RssItemData> items = parseRssItems(response.body());
        Set<Long> defaultTagIds = loadDefaultTagIds(crawlConfig.getCategoryId());

        int insertedCount = 0;
        int duplicateCount = 0;

        for (RssItemData item : items) {
            if (!StringUtils.hasText(item.link()) || !StringUtils.hasText(item.title())) {
                continue;
            }

            if (existsBySourceUrl(item.link().trim())) {
                duplicateCount++;
                continue;
            }

            try {
                persistNews(crawlConfig, item, defaultTagIds);
                insertedCount++;
            } catch (DuplicateKeyException duplicateKeyException) {
                duplicateCount++;
            }
        }

        if (insertedCount > 0) {
            cacheInvalidationService.evictDiscoveryCaches();
        }

        CrawlRunResultVO vo = new CrawlRunResultVO();
        vo.setCrawlConfigId(crawlConfig.getId());
        vo.setSourceName(crawlConfig.getSourceName());
        vo.setInsertedCount(insertedCount);
        vo.setDuplicateCount(duplicateCount);
        vo.setMessage("Crawl completed: inserted " + insertedCount + " news item(s), skipped " + duplicateCount + " duplicate(s)");
        return vo;
    }

    private List<RssItemData> parseRssItems(String xmlContent) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

        Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xmlContent)));
        NodeList itemNodes = document.getElementsByTagName("item");
        List<RssItemData> items = new ArrayList<>();

        for (int index = 0; index < itemNodes.getLength(); index++) {
            Node itemNode = itemNodes.item(index);
            if (!(itemNode instanceof Element itemElement)) {
                continue;
            }

            String title = textOf(itemElement, "title");
            String description = textOf(itemElement, "description");
            String link = textOf(itemElement, "link");
            String pubDate = textOf(itemElement, "pubDate");
            String coverImage = findCoverImage(itemElement);

            String plainDescription = stripHtml(description);
            String summary = truncate(plainDescription, 300);
            String content = truncate(StringUtils.hasText(plainDescription) ? plainDescription : title, 4000);

            items.add(new RssItemData(
                safeText(title),
                safeText(summary),
                safeText(content),
                safeText(link),
                safeText(coverImage),
                parsePublishTime(pubDate)
            ));
        }

        return items;
    }

    private String textOf(Element element, String tagName) {
        NodeList nodeList = element.getElementsByTagName(tagName);
        if (nodeList.getLength() == 0 || nodeList.item(0) == null) {
            return "";
        }
        return nodeList.item(0).getTextContent();
    }

    private String findCoverImage(Element itemElement) {
        NodeList childNodes = itemElement.getChildNodes();
        for (int index = 0; index < childNodes.getLength(); index++) {
            Node childNode = childNodes.item(index);
            if (!(childNode instanceof Element childElement)) {
                continue;
            }

            String nodeName = childElement.getNodeName().toLowerCase(Locale.ROOT);
            String url = childElement.getAttribute("url");

            if ("enclosure".equals(nodeName) && StringUtils.hasText(url)) {
                String type = childElement.getAttribute("type");
                if (!StringUtils.hasText(type) || type.startsWith("image/")) {
                    return url;
                }
            }

            if ((nodeName.endsWith("thumbnail") || nodeName.endsWith("content")) && StringUtils.hasText(url)) {
                return url;
            }
        }
        return "";
    }

    private LocalDateTime parsePublishTime(String value) {
        if (!StringUtils.hasText(value)) {
            return LocalDateTime.now();
        }

        for (DateTimeFormatter formatter : RSS_DATE_FORMATTERS) {
            try {
                ZonedDateTime zonedDateTime = ZonedDateTime.parse(value.trim(), formatter);
                return zonedDateTime.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
            } catch (DateTimeParseException ignored) {
                // try next formatter
            }
        }

        return LocalDateTime.now();
    }

    private Set<Long> loadDefaultTagIds(Long categoryId) {
        List<Tag> tags = tagMapper.selectList(
            new LambdaQueryWrapper<Tag>()
                .eq(Tag::getCategoryId, categoryId)
                .eq(Tag::getStatus, 1)
                .orderByAsc(Tag::getSortOrder)
                .orderByAsc(Tag::getId)
        );

        return tags.stream()
            .map(Tag::getId)
            .filter(Objects::nonNull)
            .collect(LinkedHashSet::new, Set::add, Set::addAll);
    }

    private boolean existsBySourceUrl(String sourceUrl) {
        return newsMapper.selectCount(
            new LambdaQueryWrapper<News>().eq(News::getSourceUrl, sourceUrl)
        ) > 0;
    }

    @Transactional
    protected void persistNews(CrawlConfig crawlConfig, RssItemData item, Set<Long> defaultTagIds) {
        News news = new News();
        news.setTitle(truncate(item.title(), 255));
        news.setSummary(truncate(item.summary(), 1000));
        news.setContent(item.content());
        news.setSourceName(crawlConfig.getSourceName());
        news.setSourceUrl(item.link().trim());
        news.setCoverImage(truncate(item.coverImage(), 500));
        news.setCategoryId(crawlConfig.getCategoryId());
        news.setPublishTime(item.publishTime());
        news.setCrawlTime(LocalDateTime.now());
        news.setStatus(1);
        news.setViewCount(0);
        news.setLikeCount(0);
        news.setFavoriteCount(0);
        news.setCommentCount(0);
        news.setHeatScore(calculateInitialHeatScore(item.publishTime()));
        newsMapper.insert(news);

        for (Long tagId : defaultTagIds) {
            NewsTag newsTag = new NewsTag();
            newsTag.setNewsId(news.getId());
            newsTag.setTagId(tagId);
            newsTagMapper.insert(newsTag);
        }
    }

    private double calculateInitialHeatScore(LocalDateTime publishTime) {
        LocalDateTime now = LocalDateTime.now();
        if (publishTime.isAfter(now.minusDays(3))) {
            return 70.0;
        }
        if (publishTime.isAfter(now.minusDays(7))) {
            return 55.0;
        }
        if (publishTime.isAfter(now.minusDays(30))) {
            return 40.0;
        }
        return 25.0;
    }

    private String stripHtml(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value
            .replaceAll("<!\\[CDATA\\[", "")
            .replaceAll("]]>", "")
            .replaceAll("<[^>]+>", " ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replaceAll("\\s+", " ")
            .trim();
    }

    private String truncate(String value, int maxLength) {
        String safeValue = safeText(value);
        if (safeValue.length() <= maxLength) {
            return safeValue;
        }
        return safeValue.substring(0, maxLength);
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeMessage(Exception exception) {
        String message = exception.getMessage();
        return StringUtils.hasText(message) ? message : "Unknown crawl error";
    }

    private record RssItemData(
        String title,
        String summary,
        String content,
        String link,
        String coverImage,
        LocalDateTime publishTime
    ) {
    }
}
