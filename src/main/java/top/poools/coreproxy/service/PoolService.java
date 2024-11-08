package top.poools.coreproxy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import top.poools.coreproxy.exception.ServiceException;
import top.poools.coreproxy.mapper.PoolMapper;
import top.poools.coreproxy.model.Pool;
import top.poools.msapi.lk.client.CoreLkClient;
import top.poools.msapi.lk.dto.PoolDto;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PoolService {

    private static final String DEFAULT_POOL_CACHE_NAME = "default_pool";
    private static final String POOL_CACHE_NAME = "pools";
    private final CoreLkClient coreLkClient;
    private final PoolMapper poolMapper;
    private final CaffeineCacheManager cacheManager;

    @Cacheable(value = DEFAULT_POOL_CACHE_NAME, unless = "#result == null")
    public Pool getDefaultPool() {
        log.info("get default pool");
        try {
            ResponseEntity<PoolDto> responseEntity = coreLkClient.getDefaultPool();
            return Optional.ofNullable(responseEntity.getBody())
                    .map(poolMapper::toEntity)
                    .orElse(null);
        } catch (Exception ex) {
            log.error("pool service error: ", ex);
            throw new ServiceException("Can not get default pool", ex);
        }
    }

    @Cacheable(value = POOL_CACHE_NAME, unless = "#result == null")
    public Pool getPoolById(Long id) {
        log.info("get pool by id: {}", id);
        try {
            ResponseEntity<PoolDto> responseEntity = coreLkClient.getPoolById(id);
            return Optional.ofNullable(responseEntity.getBody())
                    .map(poolMapper::toEntity)
                    .orElse(null);
        } catch (Exception ex) {
            log.error("pool service error: ", ex);
            throw new ServiceException("Can not get pool by id: " + id, ex);
        }
    }

    @CacheEvict(value = DEFAULT_POOL_CACHE_NAME, allEntries = true)
    public void invalidateDefaultPoolCache() {
        log.info("cache has invalidated for default pool");
    }

    @CacheEvict(value = POOL_CACHE_NAME, allEntries = true)
    public void invalidatePoolCache() {
        log.info("cache has invalidated for all pools");
    }

    public Map<Object, Object> getDefaultPoolCacheMap() {
        log.debug("get default pool cache map");
        CaffeineCache cache = (CaffeineCache) cacheManager.getCache(DEFAULT_POOL_CACHE_NAME);
        return cache != null ? cache.getNativeCache().asMap() : Collections.emptyMap();
    }

    public Map<Object, Object> getAllPoolCacheMap() {
        log.debug("get all pool cache map");
        CaffeineCache cache = (CaffeineCache) cacheManager.getCache(POOL_CACHE_NAME);
        return cache != null ? cache.getNativeCache().asMap() : Collections.emptyMap();
    }
}