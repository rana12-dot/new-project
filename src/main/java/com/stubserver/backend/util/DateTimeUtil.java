package com.stubserver.backend.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public final class DateTimeUtil {

    private DateTimeUtil() {}

    private static final DateTimeFormatter ISO_MINUTE = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final ZoneId UTC = ZoneId.of("UTC");

    public static String istToUtcMinuteString(String istNoOffset) {
        LocalDateTime ldt = LocalDateTime.parse(istNoOffset, ISO_MINUTE);
        ZonedDateTime istZdt = ldt.atZone(IST);
        ZonedDateTime utcZdt = istZdt.withZoneSameInstant(UTC);
        return utcZdt.format(ISO_MINUTE);
    }
}
