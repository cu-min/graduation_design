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
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(RssCrawlServiceImpl.class);
    private static final String CRAWLER_USER_AGENT = "NewsRecommendationBot/1.0";
    private static final int RSS_PREVIEW_CONTENT_LENGTH = 1500;
    private static final int ORIGINAL_CONTENT_MIN_GAIN = 300;
    private static final Duration RSS_REQUEST_TIMEOUT = Duration.ofSeconds(20);
    private static final Duration ARTICLE_REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final String IRRELEVANT_ARTICLE_SELECTOR = String.join(", ",
        "script",
        "style",
        "iframe",
        "nav",
        "header",
        "footer",
        "aside",
        "form",
        "button",
        ".comment",
        ".comments",
        ".ad",
        ".ads",
        ".advertisement",
        ".related",
        ".share",
        ".social",
        ".subscribe"
    );
    private static final List<String> ARTICLE_CONTAINER_SELECTORS = List.of(
        "article",
        "main article",
        "main",
        ".article-content",
        ".post-content",
        ".entry-content",
        ".story",
        ".article",
        ".content",
        "#content"
    );
    private static final List<String> PREVIEW_MARKERS = List.of(
        "read full article",
        "continue reading",
        "comments",
        "read more",
        "阅读全文",
        "查看全文",
        "继续阅读"
    );
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
            .timeout(RSS_REQUEST_TIMEOUT)
            .header("User-Agent", CRAWLER_USER_AGENT)
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
                persistNews(crawlConfig, enrichItemFromArticlePageIfNeeded(item), defaultTagIds);
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
            String rssSummary = textOf(itemElement, "summary");
            String link = textOf(itemElement, "link");
            String pubDate = textOf(itemElement, "pubDate");
            String coverImage = findCoverImage(itemElement);
            String contentSource = firstNonBlank(
                textOf(itemElement, "content:encoded"),
                textOf(itemElement, "encodedContent"),
                description,
                rssSummary,
                title
            );

            String plainSummary = cleanHtmlText(firstNonBlank(description, rssSummary, contentSource, title));
            String content = cleanHtmlText(contentSource);
            String bodyImage = findFirstImageInHtml(contentSource, link);
            String summary = truncate(plainSummary, 300);

            items.add(new RssItemData(
                safeText(title),
                safeText(summary),
                safeText(content),
                safeText(link),
                safeText(coverImage),
                safeText(bodyImage),
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
        String enclosureImage = "";
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
                    enclosureImage = url;
                }
            }

            if ((nodeName.endsWith("thumbnail") || nodeName.endsWith("content")) && StringUtils.hasText(url)
                && isImageMedia(childElement)) {
                return url;
            }
        }
        return enclosureImage;
    }

    private boolean isImageMedia(Element element) {
        String type = element.getAttribute("type");
        String medium = element.getAttribute("medium");
        if ("image".equalsIgnoreCase(medium) || (StringUtils.hasText(type) && type.startsWith("image/"))) {
            return true;
        }
        return !StringUtils.hasText(type) && !StringUtils.hasText(medium);
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

    private RssItemData enrichItemFromArticlePageIfNeeded(RssItemData item) {
        String coverImage = firstNonBlank(item.coverImage(), item.bodyImage());
        if (!shouldFetchOriginalArticle(item.content(), item.summary())) {
            return new RssItemData(
                item.title(),
                item.summary(),
                cleanFeedPreviewTail(item.content()),
                item.link(),
                coverImage,
                item.bodyImage(),
                item.publishTime()
            );
        }

        ArticlePageData articlePageData = fetchArticlePage(item.link());
        String content = cleanFeedPreviewTail(item.content());
        if (StringUtils.hasText(articlePageData.content())
            && articlePageData.content().length() > cleanTextLength(content) + ORIGINAL_CONTENT_MIN_GAIN) {
            content = articlePageData.content();
        }
        coverImage = firstNonBlank(item.coverImage(), articlePageData.ogImage(), articlePageData.firstImage(), item.bodyImage());

        return new RssItemData(
            item.title(),
            item.summary(),
            content,
            item.link(),
            coverImage,
            item.bodyImage(),
            item.publishTime()
        );
    }

    private ArticlePageData fetchArticlePage(String sourceUrl) {
        if (!StringUtils.hasText(sourceUrl)) {
            return ArticlePageData.empty();
        }

        try {
            URI articleUri = RemoteUrlValidator.validatePublicHttpUrl(sourceUrl);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(articleUri)
                .timeout(ARTICLE_REQUEST_TIMEOUT)
                .header("User-Agent", CRAWLER_USER_AGENT)
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return ArticlePageData.empty();
            }

            org.jsoup.nodes.Document document = Jsoup.parse(response.body(), sourceUrl);
            document.select(IRRELEVANT_ARTICLE_SELECTOR).remove();
            return new ArticlePageData(
                extractArticleText(document),
                extractOgImage(document),
                extractFirstImage(document)
            );
        } catch (Exception exception) {
            log.debug("Article page extraction skipped. sourceUrl={}, error={}", sourceUrl, exception.getMessage());
            return ArticlePageData.empty();
        }
    }

    private String extractArticleText(org.jsoup.nodes.Document document) {
        ArticleCandidate bestCandidate = ArticleCandidate.empty();
        for (String selector : ARTICLE_CONTAINER_SELECTORS) {
            for (org.jsoup.nodes.Element element : document.select(selector)) {
                ArticleCandidate candidate = buildArticleCandidate(element);
                if (candidate.score() > bestCandidate.score()) {
                    bestCandidate = candidate;
                }
            }
        }
        return bestCandidate.text().length() >= 100 ? bestCandidate.text() : "";
    }

    private ArticleCandidate buildArticleCandidate(org.jsoup.nodes.Element element) {
        LinkedHashSet<String> paragraphs = new LinkedHashSet<>();
        for (org.jsoup.nodes.Element paragraph : element.select("p")) {
            String paragraphText = normalizeWhitespace(paragraph.text());
            if (paragraphText.length() >= 20) {
                paragraphs.add(paragraphText);
            }
        }

        if (!paragraphs.isEmpty()) {
            String text = String.join("\n\n", paragraphs);
            return new ArticleCandidate(text, text.length() + paragraphs.size() * 200);
        }

        String text = normalizeWhitespace(element.text());
        return new ArticleCandidate(text, text.length());
    }

    private String extractOgImage(org.jsoup.nodes.Document document) {
        org.jsoup.nodes.Element imageElement = document.selectFirst("meta[property=\"og:image\"], meta[name=\"og:image\"]");
        if (imageElement == null) {
            return "";
        }
        return firstNonBlank(imageElement.absUrl("content"), imageElement.attr("content"));
    }

    private String extractFirstImage(org.jsoup.nodes.Document document) {
        org.jsoup.nodes.Element imageElement = document.selectFirst(
            "article img[src], main img[src], .article-content img[src], .post-content img[src], "
                + ".entry-content img[src], .content img[src], #content img[src], img[src]"
        );
        if (imageElement == null) {
            return "";
        }
        return firstNonBlank(imageElement.absUrl("src"), imageElement.attr("src"));
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

    private String cleanHtmlText(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        org.jsoup.nodes.Document document = Jsoup.parseBodyFragment(value);
        document.select("script, style, iframe").remove();
        return normalizeWhitespace(document.text());
    }

    private String findFirstImageInHtml(String html, String baseUri) {
        if (!StringUtils.hasText(html)) {
            return "";
        }
        org.jsoup.nodes.Document document = Jsoup.parseBodyFragment(html, safeText(baseUri));
        org.jsoup.nodes.Element imageElement = document.selectFirst("img[src]");
        if (imageElement == null) {
            return "";
        }
        return firstNonBlank(imageElement.absUrl("src"), imageElement.attr("src"));
    }

    private int cleanTextLength(String value) {
        return safeText(value).length();
    }

    private boolean shouldFetchOriginalArticle(String content, String summary) {
        String safeContent = safeText(content);
        if (!StringUtils.hasText(safeContent)) {
            return true;
        }
        if (safeContent.length() < RSS_PREVIEW_CONTENT_LENGTH) {
            return true;
        }
        if (containsPreviewMarker(safeContent)) {
            return true;
        }

        String safeSummary = safeText(summary);
        if (StringUtils.hasText(safeSummary)) {
            String normalizedContent = normalizeForComparison(safeContent);
            String normalizedSummary = normalizeForComparison(safeSummary);
            if (normalizedContent.equals(normalizedSummary)
                || safeContent.length() <= safeSummary.length() + 200) {
                return true;
            }
        }

        return !safeContent.equals(cleanFeedPreviewTail(safeContent));
    }

    private boolean containsPreviewMarker(String value) {
        String normalizedValue = value.toLowerCase(Locale.ROOT);
        for (String marker : PREVIEW_MARKERS) {
            if (normalizedValue.contains(marker.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String cleanFeedPreviewTail(String value) {
        String cleaned = normalizeWhitespace(value);
        if (!StringUtils.hasText(cleaned)) {
            return "";
        }

        cleaned = removeTailFromMarker(cleaned, "Read full article");
        cleaned = removeTailFromMarker(cleaned, "Continue reading");
        cleaned = removeTailFromMarker(cleaned, "Read more");
        cleaned = removeTailFromMarker(cleaned, "阅读全文");
        cleaned = removeTailFromMarker(cleaned, "查看全文");
        cleaned = removeTailFromMarker(cleaned, "继续阅读");
        cleaned = cleaned.replaceAll("(?i)\\s+comments?\\s*$", "");
        return normalizeWhitespace(cleaned);
    }

    private String removeTailFromMarker(String value, String marker) {
        String lowerValue = value.toLowerCase(Locale.ROOT);
        String lowerMarker = marker.toLowerCase(Locale.ROOT);
        int markerIndex = lowerValue.indexOf(lowerMarker);
        if (markerIndex < 0 || markerIndex < Math.max(0, value.length() - 500)) {
            return value;
        }
        return value.substring(0, markerIndex);
    }

    private String normalizeForComparison(String value) {
        return safeText(value).replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    private String normalizeWhitespace(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.replaceAll("\\s+", " ").trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "";
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
        String bodyImage,
        LocalDateTime publishTime
    ) {
    }

    private record ArticlePageData(
        String content,
        String ogImage,
        String firstImage
    ) {
        private static ArticlePageData empty() {
            return new ArticlePageData("", "", "");
        }
    }

    private record ArticleCandidate(
        String text,
        int score
    ) {
        private static ArticleCandidate empty() {
            return new ArticleCandidate("", 0);
        }
    }
}
