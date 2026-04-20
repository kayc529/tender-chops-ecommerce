package com.kaycheung.order_service.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

public enum TimeFilter {

    LAST_30 {
        @Override
        public FilterDateRange toDateRange(LocalDate start, LocalDate end)
        {
            Instant now = Instant.now();
            return new FilterDateRange(now.minus(30, ChronoUnit.DAYS), now);
        }
    },
    LAST_3_MONTHS {
        @Override
        public FilterDateRange toDateRange(LocalDate start, LocalDate end)
        {
            ZonedDateTime nowUtc = ZonedDateTime.now(ZoneOffset.UTC);
            return new FilterDateRange(nowUtc.minusMonths(3).toInstant(), nowUtc.toInstant());
        }
    },
    LAST_6_MONTHS {
        @Override
        public FilterDateRange toDateRange(LocalDate start, LocalDate end)
        {
            ZonedDateTime nowUtc = ZonedDateTime.now(ZoneOffset.UTC);
            return new FilterDateRange(nowUtc.minusMonths(6).toInstant(), nowUtc.toInstant());
        }
    },
    ALL {
        @Override
        public FilterDateRange toDateRange(LocalDate start, LocalDate end)
        {
            return FilterDateRange.getUnboundedDateRange();
        }
    },
    CUSTOM {
        @Override
        public FilterDateRange toDateRange(LocalDate start, LocalDate end)
        {
            if (start == null || end == null) {
                throw new IllegalArgumentException("Start and end dates are required for custom time filter");
            }

            if(!start.isBefore(end))
            {
                throw new IllegalArgumentException("Start date must not be after end date");
            }

            Instant startInstant = start.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant endInstant = end.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

            return new FilterDateRange(startInstant, endInstant);
        }
    };

    public abstract FilterDateRange toDateRange(LocalDate start, LocalDate end);

    public static TimeFilter fromParam(String timeFilterParam)
    {
        if(timeFilterParam== null || timeFilterParam.isBlank())
        {
            return TimeFilter.LAST_3_MONTHS;
        }

        return switch (timeFilterParam) {
            case "last30" -> TimeFilter.LAST_30;
            case "3months" -> TimeFilter.LAST_3_MONTHS;
            case "6months" -> TimeFilter.LAST_6_MONTHS;
            case "all" -> TimeFilter.ALL;
            case "custom" -> TimeFilter.CUSTOM;
            default -> throw new IllegalArgumentException("Invalid timeFilter: " + timeFilterParam);
        };
    }
}
