package top.poools.coreproxy.rest;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.poools.coreproxy.service.UserService;

import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("/api/internal/cache/user")
@RequiredArgsConstructor
public class UserCacheController {

    private final UserService userService;

    @PutMapping(
            value = "/invalidate/by-login",
            produces = APPLICATION_JSON_VALUE
    )
    ResponseEntity<String> invalidateUserCacheByLogin(
            @RequestParam("login") String login
    ) {
        userService.invalidateUserCacheByLogin(login);
        return ResponseEntity.ok("cache has invalidated for user: " + login);
    }

    @PutMapping(
            value = "/invalidate/all",
            produces = APPLICATION_JSON_VALUE
    )
    ResponseEntity<String> invalidateAllUserCache() {
        userService.invalidateAllUserCache();
        return ResponseEntity.ok("cache has invalidated for all users");
    }

    @GetMapping(
            value = "/all",
            produces = APPLICATION_JSON_VALUE
    )
    ResponseEntity<Map<Object, Object>> getAllUserCache(
            @RequestParam("host") String hostName
    ) {
        return ResponseEntity.ok(userService.getCacheMap());
    }
}