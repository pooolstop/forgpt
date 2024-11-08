package top.poools.coreproxy.rest;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.poools.coreproxy.service.PoolService;

import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("/api/internal/cache/pool")
@RequiredArgsConstructor
public class PoolCacheController {

    private final PoolService poolService;

    @PutMapping(
            value = "/invalidate/all",
            produces = APPLICATION_JSON_VALUE
    )
    ResponseEntity<String> invalidatePoolCache() {
        poolService.invalidateDefaultPoolCache();
        poolService.invalidatePoolCache();
        return ResponseEntity.ok("cache has invalidated for default pool");
    }

    @GetMapping(
            value = "/default",
            produces = APPLICATION_JSON_VALUE
    )
    ResponseEntity<Map<Object, Object>> getDefaultPoolCache(
            @RequestParam("host") String hostName
    ) {
        return ResponseEntity.ok(poolService.getDefaultPoolCacheMap());
    }

    @GetMapping(
            value = "/all",
            produces = APPLICATION_JSON_VALUE
    )
    ResponseEntity<Map<Object, Object>> getAllPoolCache(
            @RequestParam("host") String hostName
    ) {
        return ResponseEntity.ok(poolService.getAllPoolCacheMap());
    }
}