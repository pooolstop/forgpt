package top.poools.coreproxy.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.http.ResponseEntity;
import top.poools.coreproxy.exception.ServiceException;
import top.poools.coreproxy.mapper.UserMapper;
import top.poools.coreproxy.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.poools.msapi.lk.client.CoreLkClient;
import top.poools.msapi.lk.dto.UserDto;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private static final String USER_CACHE_NAME = "users";
    private final CoreLkClient coreLkClient;
    private final UserMapper userMapper;
    private final CaffeineCacheManager cacheManager;

    @Cacheable(value = USER_CACHE_NAME, key = "#userLogin", unless = "#result == null")
    public User getUserByLogin(String userLogin) {
        log.info("get user by login: {}", userLogin);
        try {
            ResponseEntity<UserDto> responseEntity = coreLkClient.getUserByLogin(userLogin, false);
            return Optional.ofNullable(responseEntity.getBody())
                    .map(userMapper::toEntity)
                    .orElse(null);
        } catch (Exception ex) {
            log.error("user service error: ", ex);
            throw new ServiceException("Can not get user by login", ex);
        }
    }

    @CacheEvict(value = USER_CACHE_NAME, key = "#userLogin")
    public void invalidateUserCacheByLogin(String userLogin) {
        log.info("cache has invalidated for user: {}", userLogin);
    }

    @CacheEvict(value = USER_CACHE_NAME, allEntries = true)
    public void invalidateAllUserCache() {
        log.info("cache has invalidated for all users");
    }

    public Map<Object, Object> getCacheMap() {
        log.debug("get user cache map");
        CaffeineCache cache = (CaffeineCache) cacheManager.getCache(USER_CACHE_NAME);
        return cache != null ? cache.getNativeCache().asMap() : Collections.emptyMap();
    }
}
