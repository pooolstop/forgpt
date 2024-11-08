package top.poools.coreproxy.rest;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.poools.coreproxy.service.MinerService;

import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("/api/internal/cache/miner")
@RequiredArgsConstructor
public class MinerCacheController {

    private final MinerService minerService;

    @PutMapping(
            value = "/invalidate/by-name-userid",
            produces = APPLICATION_JSON_VALUE
    )
    ResponseEntity<String> invalidateMinerCacheByNameAndUserId(
            @RequestParam("name") String name,
            @RequestParam("userId") Long userId
    ) {
        minerService.invalidateMinerCacheByNameAndUserId(name, userId);
        return ResponseEntity.ok("cache has invalidated for miner: " + name + " with userId: " + userId);
    }

    @PutMapping(
            value = "/invalidate/all",
            produces = APPLICATION_JSON_VALUE
    )
    ResponseEntity<String> invalidateAllMinerCache() {
        minerService.invalidateAllMinerCache();
        return ResponseEntity.ok("cache has invalidated for all miners");
    }

    @GetMapping(
            value = "/all",
            produces = APPLICATION_JSON_VALUE
    )
    ResponseEntity<Map<Object, Object>> getAllMinerCache(
            @RequestParam("host") String hostName
    ) {
        return ResponseEntity.ok(minerService.getCacheMap());
    }
}