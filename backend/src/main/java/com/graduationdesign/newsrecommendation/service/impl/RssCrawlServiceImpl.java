package com.graduationdesign.newsrecommendation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduationdesign.newsrecommendation.common.RemoteUrlValidator;
import com.graduationdesign.newsrecommendation.entity.Category;
import com.graduationdesign.newsrecommendation.entity.CrawlConfig;
import com.graduationdesign.newsrecommendation.entity.News;
import com.graduationdesign.newsrecommendation.entity.NewsTag;
import com.graduationdesign.newsrecommendation.entity.Tag;
import com.graduationdesign.newsrecommendation.mapper.CategoryMapper;
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
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
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
    private static final String FRONTIER_TECH_CODE = "FRONTIER_TECH";
    private static final String GROWTH_LEARNING_CODE = "GROWTH_LEARNING";
    private static final String CAREER_OPPORTUNITY_CODE = "CAREER_OPPORTUNITY";
    private static final String DIGITAL_LIFE_CODE = "DIGITAL_LIFE";
    private static final String HOT_TREND_CODE = "HOT_TREND";
    private static final String FRONTIER_TECH_COVER = "/news-covers/frontier-tech.svg";
    private static final String GROWTH_LEARNING_COVER = "/news-covers/growth-learning.svg";
    private static final String CAREER_OPPORTUNITY_COVER = "/news-covers/career-opportunity.svg";
    private static final String DIGITAL_LIFE_COVER = "/news-covers/digital-life.svg";
    private static final String HOT_TREND_COVER = "/news-covers/hot-trend.svg";
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
    private final CategoryMapper categoryMapper;
    private final CrawlConfigMapper crawlConfigMapper;
    private final CacheInvalidationService cacheInvalidationService;

    public RssCrawlServiceImpl(
        NewsMapper newsMapper,
        NewsTagMapper newsTagMapper,
        TagMapper tagMapper,
        CategoryMapper categoryMapper,
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
        this.categoryMapper = categoryMapper;
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
                persistNews(crawlConfig, enrichItemFromArticlePageIfNeeded(item));
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
            "article img[src], article img[data-src], article img[data-original], "
                + "main img[src], main img[data-src], main img[data-original], "
                + ".article-content img[src], .article-content img[data-src], .article-content img[data-original], "
                + ".post-content img[src], .post-content img[data-src], .post-content img[data-original], "
                + ".entry-content img[src], .entry-content img[data-src], .entry-content img[data-original], "
                + ".content img[src], .content img[data-src], .content img[data-original], "
                + "#content img[src], #content img[data-src], #content img[data-original], "
                + "img[src], img[data-src], img[data-original]"
        );
        if (imageElement == null) {
            return "";
        }
        return imageUrlOf(imageElement);
    }

    @Transactional
    protected void persistNews(CrawlConfig crawlConfig, RssItemData item) {
        Long categoryId = resolveCategoryId(crawlConfig, item);
        Set<Long> tagIds = resolveTagIds(item);

        News news = new News();
        news.setTitle(truncate(item.title(), 255));
        news.setSummary(truncate(item.summary(), 1000));
        news.setContent(item.content());
        news.setSourceName(crawlConfig.getSourceName());
        news.setSourceUrl(item.link().trim());
        news.setCoverImage(truncate(firstNonBlank(item.coverImage(), defaultCoverImage(categoryId)), 500));
        news.setCategoryId(categoryId);
        news.setPublishTime(item.publishTime());
        news.setCrawlTime(LocalDateTime.now());
        news.setStatus(1);
        news.setViewCount(0);
        news.setLikeCount(0);
        news.setFavoriteCount(0);
        news.setCommentCount(0);
        news.setHeatScore(calculateInitialHeatScore(item.publishTime()));
        newsMapper.insert(news);

        for (Long tagId : tagIds) {
            try {
                NewsTag newsTag = new NewsTag();
                newsTag.setNewsId(news.getId());
                newsTag.setTagId(tagId);
                newsTagMapper.insert(newsTag);
            } catch (Exception exception) {
                log.warn("RSS tag binding skipped. newsId={}, tagId={}, error={}", news.getId(), tagId, exception.getMessage());
            }
        }
    }

    private Long resolveCategoryId(CrawlConfig crawlConfig, RssItemData item) {
        if (isActiveCategory(crawlConfig.getCategoryId())) {
            return crawlConfig.getCategoryId();
        }

        Long keywordCategoryId = resolveCategoryIdByKeywords(item);
        if (keywordCategoryId != null) {
            return keywordCategoryId;
        }

        return fallbackCategoryId();
    }

    private boolean isActiveCategory(Long categoryId) {
        if (categoryId == null) {
            return false;
        }
        Category category = categoryMapper.selectById(categoryId);
        return category != null && Objects.equals(category.getStatus(), 1);
    }

    private Long resolveCategoryIdByKeywords(RssItemData item) {
        String text = searchableText(item);
        if (containsAny(text, "ai", "模型", "openai", "智能体", "机器人", "芯片", "开源")) {
            return categoryIdByCode(FRONTIER_TECH_CODE);
        }
        if (containsAny(text, "实习", "就业", "招聘", "职业", "远程办公", "简历", "面试")) {
            return categoryIdByCode(CAREER_OPPORTUNITY_CODE);
        }
        if (containsAny(text, "app", "数码", "手机", "电脑", "工具", "软件", "硬件")) {
            return categoryIdByCode(DIGITAL_LIFE_CODE);
        }
        if (containsAny(text, "学习", "英语", "自学", "效率", "阅读", "写作", "知识管理")) {
            return categoryIdByCode(GROWTH_LEARNING_CODE);
        }
        if (containsAny(text, "热点", "平台", "趋势", "社会", "青年", "创业")) {
            return categoryIdByCode(HOT_TREND_CODE);
        }
        return null;
    }

    private Long fallbackCategoryId() {
        Long hotTrendCategoryId = categoryIdByCode(HOT_TREND_CODE);
        if (hotTrendCategoryId != null) {
            return hotTrendCategoryId;
        }

        Category category = categoryMapper.selectOne(
            new LambdaQueryWrapper<Category>()
                .eq(Category::getStatus, 1)
                .orderByAsc(Category::getSortOrder)
                .orderByAsc(Category::getId)
                .last("LIMIT 1")
        );
        if (category == null) {
            throw new IllegalStateException("No active category is available for crawled news");
        }
        return category.getId();
    }

    private Long categoryIdByCode(String code) {
        Category category = categoryMapper.selectOne(
            new LambdaQueryWrapper<Category>()
                .eq(Category::getCode, code)
                .eq(Category::getStatus, 1)
                .last("LIMIT 1")
        );
        return category == null ? null : category.getId();
    }

    private String defaultCoverImage(Long categoryId) {
        Category category = categoryMapper.selectById(categoryId);
        if (category == null || !StringUtils.hasText(category.getCode())) {
            return HOT_TREND_COVER;
        }
        return switch (category.getCode()) {
            case FRONTIER_TECH_CODE -> FRONTIER_TECH_COVER;
            case GROWTH_LEARNING_CODE -> GROWTH_LEARNING_COVER;
            case CAREER_OPPORTUNITY_CODE -> CAREER_OPPORTUNITY_COVER;
            case DIGITAL_LIFE_CODE -> DIGITAL_LIFE_COVER;
            default -> HOT_TREND_COVER;
        };
    }

    private Set<Long> resolveTagIds(RssItemData item) {
        String text = searchableText(item);
        Map<String, Tag> tagsByCode = tagMapper.selectList(
            new LambdaQueryWrapper<Tag>().eq(Tag::getStatus, 1)
        ).stream().collect(Collectors.toMap(Tag::getCode, Function.identity(), (left, right) -> left));

        Set<Long> tagIds = new LinkedHashSet<>();
        addTagIfMatches(tagIds, tagsByCode, "AI", text, "ai", "openai", "智能体");
        addTagIfMatches(tagIds, tagsByCode, "LLM", text, "模型", "大模型", "llm");
        addTagIfMatches(tagIds, tagsByCode, "ROBOT", text, "机器人");
        addTagIfMatches(tagIds, tagsByCode, "INTERNSHIP_EMPLOYMENT", text, "实习", "就业", "招聘");
        addTagIfMatches(tagIds, tagsByCode, "NEW_CAREER", text, "新职业", "职业");
        addTagIfMatches(tagIds, tagsByCode, "REMOTE_WORK", text, "远程办公", "远程");
        addTagIfMatches(tagIds, tagsByCode, "APP_RECOMMENDATION", text, "app", "应用");
        addTagIfMatches(tagIds, tagsByCode, "DIGITAL_PRODUCT", text, "数码", "手机", "电脑");
        addTagIfMatches(tagIds, tagsByCode, "SOFTWARE_TOOL", text, "工具", "软件");
        addTagIfMatches(tagIds, tagsByCode, "ENGLISH_LEARNING", text, "英语");
        addTagIfMatches(tagIds, tagsByCode, "SELF_LEARNING_METHOD", text, "学习", "自学");
        addTagIfMatches(tagIds, tagsByCode, "EFFICIENCY_TOOL", text, "效率");
        addTagIfMatches(tagIds, tagsByCode, "READING_WRITING", text, "阅读", "写作");
        addTagIfMatches(tagIds, tagsByCode, "SOCIAL_HOT_TOPIC", text, "热点", "社会");
        addTagIfMatches(tagIds, tagsByCode, "YOUTH_TOPIC", text, "青年");
        addTagIfMatches(tagIds, tagsByCode, "PLATFORM_DYNAMIC", text, "平台");
        addTagIfMatches(tagIds, tagsByCode, "BUSINESS_TREND", text, "趋势", "创业");
        return tagIds;
    }

    private void addTagIfMatches(Set<Long> tagIds, Map<String, Tag> tagsByCode, String code, String text, String... keywords) {
        Tag tag = tagsByCode.get(code);
        if (tag != null && tag.getId() != null && containsAny(text, keywords)) {
            tagIds.add(tag.getId());
        }
    }

    private String searchableText(RssItemData item) {
        return (safeText(item.title()) + " " + safeText(item.summary()) + " " + safeText(item.content()))
            .toLowerCase(Locale.ROOT);
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (StringUtils.hasText(keyword) && text.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
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
        document.select("br").forEach(element -> element.after(new org.jsoup.nodes.TextNode("\n")));
        document.select("p, div, section, article, li, h1, h2, h3, h4, h5, h6, blockquote")
            .forEach(element -> element.appendChild(new org.jsoup.nodes.TextNode("\n\n")));
        return normalizeArticleText(document.body().wholeText());
    }

    private String findFirstImageInHtml(String html, String baseUri) {
        if (!StringUtils.hasText(html)) {
            return "";
        }
        org.jsoup.nodes.Document document = Jsoup.parseBodyFragment(html, safeText(baseUri));
        org.jsoup.nodes.Element imageElement = document.selectFirst(
            "p img[src], p img[data-src], p img[data-original], figure img[src], figure img[data-src], "
                + "figure img[data-original], img[src], img[data-src], img[data-original]"
        );
        if (imageElement == null) {
            return "";
        }
        return imageUrlOf(imageElement);
    }

    private String imageUrlOf(org.jsoup.nodes.Element imageElement) {
        return firstNonBlank(
            imageElement.absUrl("src"),
            imageElement.absUrl("data-src"),
            imageElement.absUrl("data-original"),
            imageElement.absUrl("data-lazy-src"),
            imageElement.absUrl("data-original-src"),
            imageElement.absUrl("data-url"),
            imageElement.attr("src"),
            imageElement.attr("data-src"),
            imageElement.attr("data-original"),
            imageElement.attr("data-lazy-src"),
            imageElement.attr("data-original-src"),
            imageElement.attr("data-url")
        );
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
        String cleaned = normalizeArticleText(value);
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
        return normalizeArticleText(cleaned);
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

    private String normalizeArticleText(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = value
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .replace('\u00A0', ' ')
            .replaceAll("[\\t ]+", " ")
            .replaceAll(" *\\n *", "\n")
            .replaceAll("\\n{3,}", "\n\n")
            .trim();
        return Arrays.stream(normalized.split("\\n", -1))
            .map(String::trim)
            .collect(Collectors.joining("\n"))
            .replaceAll("\\n{3,}", "\n\n")
            .trim();
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
