package top.poools.coreproxy.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.http.ResponseEntity;
import top.poools.coreproxy.exception.ServiceException;
import top.poools.coreproxy.mapper.MinerMapper;
import top.poools.coreproxy.model.Miner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.poools.msapi.lk.client.CoreLkClient;
import top.poools.msapi.lk.dto.MinerDto;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinerService {

    private static final String MINER_CACHE_NAME = "miners";
    private final CoreLkClient coreLkClient;
    private final MinerMapper minerMapper;
    private final CaffeineCacheManager cacheManager;

    @Cacheable(value = MINER_CACHE_NAME, key = "{#name, #userId}", unless = "#result == null")
    public Miner getMinerByNameAndUserId(String name, Long userId) throws ServiceException {
        log.info("get miner by name: {} and userId: {}", name, userId);
        try {
            ResponseEntity<MinerDto> responseEntity = coreLkClient.getMinerByNameAndUserId(name, userId);
            return Optional.ofNullable(responseEntity.getBody())
                    .map(minerMapper::toMiner)
                    .orElse(null);
        } catch (Exception ex) {
            log.error("miner service error: ", ex);
            throw new ServiceException("Can not get miner by name and userId", ex);
        }
    }

    public List<Miner> getMinersByInetAddress(String inetAddress) throws ServiceException {
        log.info("get miners by inet address: {}", inetAddress);
        try {
            ResponseEntity<List<MinerDto>> responseEntity = coreLkClient.getMinersByInetAddress(inetAddress);
            return Optional.ofNullable(responseEntity.getBody()).stream()
                    .flatMap(Collection::stream)
                    .map(minerMapper::toMiner)
                    .toList();
        } catch (Exception ex) {
            log.error("miner service error: ", ex);
            throw new ServiceException("Can not get miners by inetAddress: " + inetAddress, ex);
        }
    }

    public Miner registerMiner(Long userId, String name, String info, Long poolId, String inetAddress) throws ServiceException {
        try {
            ResponseEntity<MinerDto> responseEntity = coreLkClient.createMiner(
                    new MinerDto()
                            .setName(name)
                            .setInfo(info)
                            .setUserId(userId)
                            .setIsDeleted(false)
                            .setCreateDate(LocalDateTime.now())
                            .setPoolId(poolId)
                            .setInetAddress(inetAddress));
            return Optional.ofNullable(responseEntity.getBody())
                    .map(minerMapper::toMiner)
                    .orElse(null);
        } catch (Exception ex) {
            log.error("miner service error: ", ex);
            throw new ServiceException("Register miner error", ex);
        }
    }

    public Miner updateMiner(Miner miner, String info, String inetAddress, Integer connectAttempt) throws ServiceException {
        try {
            MinerDto minerDto = minerMapper.toDto(miner)
                    .setInfo(info)
                    .setInetAddress(inetAddress)
                    .setConnectAttempt(connectAttempt);
            ResponseEntity<MinerDto> responseEntity = coreLkClient.updateMiner(minerDto);
            return Optional.ofNullable(responseEntity.getBody())
                    .map(minerMapper::toMiner)
                    .orElse(null);
        } catch (Exception ex) {
            log.error("miner service error: ", ex);
            throw new ServiceException("Update miner error", ex);
        }
    }

    @CacheEvict(value = MINER_CACHE_NAME, key = "{#name, #userId}")
    public void invalidateMinerCacheByNameAndUserId(String name, Long userId) {
        log.info("cache has invalidated for miner: {} with userId: {}", name, userId);
    }

    @CacheEvict(value = MINER_CACHE_NAME, allEntries = true)
    public void invalidateAllMinerCache() {
        log.info("cache has invalidated for all miners");
    }

    public Map<Object, Object> getCacheMap() {
        log.debug("get miner cache map");
        CaffeineCache cache = (CaffeineCache) cacheManager.getCache(MINER_CACHE_NAME);
        return cache != null ? cache.getNativeCache().asMap() : Collections.emptyMap();
    }
}
