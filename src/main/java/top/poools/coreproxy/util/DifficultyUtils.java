package top.poools.coreproxy.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;

@Slf4j
@UtilityClass
public class DifficultyUtils {

    public static final Long DEFAULT_DIFFICULTY = 1L;
    private static final BigInteger MAX_LONG = BigInteger.valueOf(Long.MAX_VALUE);

    public static Long parse(String value, int radix) {
        log.debug("try to parse difficulty from value {} using {} radix", value, radix);
        String validValue = toValidValue(value, radix);
        BigInteger bigIntValue = new BigInteger(validValue, radix);
        if (bigIntValue.compareTo(MAX_LONG) > 0) {
            log.info("value {} is too long. Use Long.MAX_VALUE instead", validValue);
            return Long.MAX_VALUE;
        }
        return bigIntValue.longValue();
    }

    private String toValidValue(String value, int radix) {
        if (radix == 10 && (value.endsWith(".0") || value.endsWith(".00"))) {
            return value.substring(0, value.lastIndexOf("."));
        }
        return value;
    }
}