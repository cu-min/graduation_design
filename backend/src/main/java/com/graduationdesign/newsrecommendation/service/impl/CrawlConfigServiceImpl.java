package com.graduationdesign.newsrecommendation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.graduationdesign.newsrecommendation.dto.CrawlConfigCreateRequest;
import com.graduationdesign.newsrecommendation.dto.CrawlConfigStatusRequest;
import com.graduationdesign.newsrecommendation.dto.CrawlConfigUpdateRequest;
import com.graduationdesign.newsrecommendation.entity.Category;
import com.graduationdesign.newsrecommendation.entity.CrawlConfig;
import com.graduationdesign.newsrecommendation.mapper.CategoryMapper;
import com.graduationdesign.newsrecommendation.mapper.CrawlConfigMapper;
import com.graduationdesign.newsrecommendation.service.CrawlConfigService;
import com.graduationdesign.newsrecommendation.service.CrawlService;
import com.graduationdesign.newsrecommendation.vo.AdminCrawlConfigDetailVO;
import com.graduationdesign.newsrecommendation.vo.AdminCrawlConfigVO;
import com.graduationdesign.newsrecommendation.vo.CrawlRunResultVO;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CrawlConfigServiceImpl extends ServiceImpl<CrawlConfigMapper, CrawlConfig> implements CrawlConfigService {

    private static final Logger log = LoggerFactory.getLogger(CrawlConfigServiceImpl.class);

    private final CategoryMapper categoryMapper;
    private final CrawlService crawlService;
    private final Set<Long> runningConfigIds = ConcurrentHashMap.newKeySet();

    public CrawlConfigServiceImpl(CategoryMapper categoryMapper, CrawlService crawlService) {
        this.categoryMapper = categoryMapper;
        this.crawlService = crawlService;
    }

    @Override
    public List<AdminCrawlConfigVO> listAdminCrawlConfigs() {
        List<CrawlConfig> crawlConfigs = list(
            new LambdaQueryWrapper<CrawlConfig>()
                .orderByDesc(CrawlConfig::getUpdatedAt)
                .orderByDesc(CrawlConfig::getId)
        );
        if (crawlConfigs.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Category> categoryMap = loadCategoryMap(crawlConfigs.stream()
            .map(CrawlConfig::getCategoryId)
            .filter(Objects::nonNull)
            .distinct()
            .toList());

        return crawlConfigs.stream()
            .map(config -> toAdminCrawlConfigVO(config, categoryMap))
            .toList();
    }

    @Override
    public AdminCrawlConfigDetailVO getAdminCrawlConfigDetail(Long id) {
        CrawlConfig crawlConfig = getByIdOrThrow(id);
        Map<Long, Category> categoryMap = loadCategoryMap(List.of(crawlConfig.getCategoryId()));

        AdminCrawlConfigDetailVO vo = new AdminCrawlConfigDetailVO();
        fillCommonFields(vo, crawlConfig, categoryMap);
        return vo;
    }

    @Override
    @Transactional
    public void createAdminCrawlConfig(CrawlConfigCreateRequest request) {
        validateRequest(request.getSourceType(), request.getCategoryId(), request.getEnabled(), request.getCrawlInterval());
        ensureSourceUrlUnique(request.getSourceUrl().trim(), null);

        CrawlConfig crawlConfig = new CrawlConfig();
        fillCrawlConfig(crawlConfig, request);
        crawlConfig.setLastCrawlCount(0);
        save(crawlConfig);
    }

    @Override
    @Transactional
    public void updateAdminCrawlConfig(Long id, CrawlConfigUpdateRequest request) {
        CrawlConfig crawlConfig = getByIdOrThrow(id);
        validateRequest(request.getSourceType(), request.getCategoryId(), request.getEnabled(), request.getCrawlInterval());
        ensureSourceUrlUnique(request.getSourceUrl().trim(), id);

        fillCrawlConfig(crawlConfig, request);
        updateById(crawlConfig);
    }

    @Override
    @Transactional
    public void deleteAdminCrawlConfig(Long id) {
        getByIdOrThrow(id);
        removeById(id);
    }

    @Override
    @Transactional
    public void updateAdminCrawlConfigStatus(Long id, CrawlConfigStatusRequest request) {
        CrawlConfig crawlConfig = getByIdOrThrow(id);
        crawlConfig.setEnabled(request.getEnabled());
        updateById(crawlConfig);
    }

    @Override
    public CrawlRunResultVO runAdminCrawl(Long id) {
        CrawlConfig crawlConfig = getByIdOrThrow(id);
        return executeCrawl(crawlConfig, false);
    }

    @Override
    public void runScheduledCrawls() {
        List<CrawlConfig> enabledConfigs = list(
            new LambdaQueryWrapper<CrawlConfig>()
                .eq(CrawlConfig::getEnabled, 1)
                .orderByAsc(CrawlConfig::getId)
        );

        for (CrawlConfig crawlConfig : enabledConfigs) {
            if (!shouldRunNow(crawlConfig)) {
                continue;
            }

            try {
                executeCrawl(crawlConfig, true);
            } catch (Exception exception) {
                log.warn(
                    "Scheduled crawl failed. crawlConfigId={}, sourceName={}, error={}",
                    crawlConfig.getId(),
                    crawlConfig.getSourceName(),
                    exception.getMessage()
                );
            }
        }
    }

    private CrawlConfig getByIdOrThrow(Long id) {
        CrawlConfig crawlConfig = getById(id);
        if (crawlConfig == null) {
            throw new IllegalArgumentException("Crawl config does not exist");
        }
        return crawlConfig;
    }

    private void validateRequest(String sourceType, Long categoryId, Integer enabled, Integer crawlInterval) {
        if (!StringUtils.hasText(sourceType) || !"RSS".equalsIgnoreCase(sourceType.trim())) {
            throw new IllegalArgumentException("Only RSS source type is supported in this phase");
        }
        if (enabled == null || (enabled != 0 && enabled != 1)) {
            throw new IllegalArgumentException("Enabled status must be 0 or 1");
        }
        if (crawlInterval == null || crawlInterval < 1) {
            throw new IllegalArgumentException("Crawl interval must be greater than 0");
        }
        Category category = categoryMapper.selectById(categoryId);
        if (category == null || !Objects.equals(category.getStatus(), 1)) {
            throw new IllegalArgumentException("Category does not exist or is disabled");
        }
    }

    private boolean shouldRunNow(CrawlConfig crawlConfig) {
        if (!Objects.equals(crawlConfig.getEnabled(), 1)) {
            return false;
        }
        if (crawlConfig.getLastCrawlTime() == null) {
            return true;
        }
        return !crawlConfig.getLastCrawlTime().isAfter(LocalDateTime.now().minusMinutes(crawlConfig.getCrawlInterval()));
    }

    private CrawlRunResultVO executeCrawl(CrawlConfig crawlConfig, boolean scheduled) {
        if (!"RSS".equalsIgnoreCase(crawlConfig.getSourceType())) {
            throw new IllegalArgumentException("Only RSS source type is supported in this phase");
        }

        Long crawlConfigId = crawlConfig.getId();
        if (!runningConfigIds.add(crawlConfigId)) {
            if (scheduled) {
                log.info("Skip scheduled crawl because it is already running. crawlConfigId={}, sourceName={}", crawlConfigId, crawlConfig.getSourceName());
                return null;
            }
            throw new IllegalArgumentException("This crawl config is already running");
        }

        try {
            log.info("Start {} crawl. crawlConfigId={}, sourceName={}, sourceUrl={}",
                scheduled ? "scheduled" : "manual",
                crawlConfigId,
                crawlConfig.getSourceName(),
                crawlConfig.getSourceUrl()
            );

            CrawlRunResultVO result = crawlService.runRssCrawl(crawlConfig);
            log.info(
                "{} crawl succeeded. crawlConfigId={}, sourceName={}, insertedCount={}, duplicateCount={}, lastStatus={}",
                scheduled ? "Scheduled" : "Manual",
                crawlConfigId,
                crawlConfig.getSourceName(),
                result.getInsertedCount(),
                result.getDuplicateCount(),
                result.getLastStatus()
            );
            return result;
        } catch (RuntimeException exception) {
            log.error(
                "{} crawl failed. crawlConfigId={}, sourceName={}, error={}",
                scheduled ? "Scheduled" : "Manual",
                crawlConfigId,
                crawlConfig.getSourceName(),
                exception.getMessage()
            );
            throw exception;
        } finally {
            runningConfigIds.remove(crawlConfigId);
        }
    }

    private void ensureSourceUrlUnique(String sourceUrl, Long excludeId) {
        LambdaQueryWrapper<CrawlConfig> queryWrapper = new LambdaQueryWrapper<CrawlConfig>()
            .eq(CrawlConfig::getSourceUrl, sourceUrl);
        if (excludeId != null) {
            queryWrapper.ne(CrawlConfig::getId, excludeId);
        }
        if (count(queryWrapper) > 0) {
            throw new IllegalArgumentException("Source URL already exists");
        }
    }

    private void fillCrawlConfig(CrawlConfig crawlConfig, CrawlConfigCreateRequest request) {
        crawlConfig.setSourceName(request.getSourceName().trim());
        crawlConfig.setSourceUrl(request.getSourceUrl().trim());
        crawlConfig.setSourceType(request.getSourceType().trim().toUpperCase());
        crawlConfig.setCategoryId(request.getCategoryId());
        crawlConfig.setEnabled(request.getEnabled());
        crawlConfig.setCrawlInterval(request.getCrawlInterval());
    }

    private Map<Long, Category> loadCategoryMap(List<Long> categoryIds) {
        if (categoryIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return categoryMapper.selectBatchIds(categoryIds).stream()
            .collect(Collectors.toMap(Category::getId, Function.identity()));
    }

    private AdminCrawlConfigVO toAdminCrawlConfigVO(CrawlConfig crawlConfig, Map<Long, Category> categoryMap) {
        AdminCrawlConfigVO vo = new AdminCrawlConfigVO();
        fillCommonFields(vo, crawlConfig, categoryMap);
        return vo;
    }

    private void fillCommonFields(AdminCrawlConfigVO vo, CrawlConfig crawlConfig, Map<Long, Category> categoryMap) {
        vo.setId(crawlConfig.getId());
        vo.setSourceName(crawlConfig.getSourceName());
        vo.setSourceUrl(crawlConfig.getSourceUrl());
        vo.setSourceType(crawlConfig.getSourceType());
        vo.setCategoryId(crawlConfig.getCategoryId());
        vo.setCategoryName(categoryMap.get(crawlConfig.getCategoryId()) != null
            ? categoryMap.get(crawlConfig.getCategoryId()).getName()
            : null);
        vo.setEnabled(crawlConfig.getEnabled());
        vo.setCrawlInterval(crawlConfig.getCrawlInterval());
        vo.setLastCrawlTime(crawlConfig.getLastCrawlTime());
        vo.setLastCrawlCount(crawlConfig.getLastCrawlCount());
        vo.setLastStatus(crawlConfig.getLastStatus());
        vo.setLastError(crawlConfig.getLastError());
        vo.setCreatedAt(crawlConfig.getCreatedAt());
        vo.setUpdatedAt(crawlConfig.getUpdatedAt());
    }
}
