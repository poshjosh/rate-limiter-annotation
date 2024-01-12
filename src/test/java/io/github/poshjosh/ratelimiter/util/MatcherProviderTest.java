package io.github.poshjosh.ratelimiter.util;

import io.github.poshjosh.ratelimiter.model.Rate;
import io.github.poshjosh.ratelimiter.model.RateConfig;
import io.github.poshjosh.ratelimiter.model.RateSource;
import io.github.poshjosh.ratelimiter.model.Rates;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MatcherProviderTest {

    private final MatcherProvider<String> matcherProvider = MatcherProvider.ofDefaults();

    @Test
    void createMainMatcher_givenNoRateCondition_shouldCreateANodeNameMatcher() {
        String nodeName = "test-node-name";
        RateConfig rateConfig = createRateConfig(nodeName);
        Matcher<String> matcher = matcherProvider.createMainMatcher(rateConfig);
        assertTrue(matcher.matches(nodeName));
        assertFalse(matcher.matches(nodeName + "1"));
    }

    @Test
    void createMainMatcher_givenRateCondition_shouldCreateAConditionMatcher() throws Exception{
        final int millis = 700;
        LocalDateTime time = LocalDateTime.now().plus(millis, ChronoUnit.MILLIS);
        String nodeName = "test-node-name";
        RateConfig rateConfig = createRateConfig(nodeName, "sys.time>"+time, "");
        Matcher<String> matcher = matcherProvider.createMainMatcher(rateConfig);
        assertFalse(matcher.matches(nodeName));
        Thread.sleep(millis);
        assertTrue(matcher.matches(nodeName));
    }

    @Test
    void createSubMatchers_givenNoRateCondition_shouldReturnEmpty() {
        String nodeName = "test-node-name";
        RateConfig rateConfig = createRateConfig(nodeName);
        List<Matcher<String>> matchers = matcherProvider.createSubMatchers(rateConfig);
        assertTrue(matchers.isEmpty());
    }

    @Test
    void createSubMatchers_givenOnlyGlobalRateCondition_shouldReturnEmpty() {
        String nodeName = "test-node-name";
        RateConfig rateConfig = createRateConfig(nodeName, "sys.time.elapsed>PT0S", "");
        List<Matcher<String>> matchers = matcherProvider.createSubMatchers(rateConfig);
        assertTrue(matchers.isEmpty());
    }

    @Test
    void createSubMatchers_givenOneNonGlobalRateConditions_shouldReturnOne() {
        String nodeName = "test-node-name";
        RateConfig rateConfig = createRateConfig(nodeName, "", "sys.time.elapsed>PT0S");
        List<Matcher<String>> matchers = matcherProvider.createSubMatchers(rateConfig);
        assertEquals(1, matchers.size());
    }

    private RateConfig createRateConfig(String nodeName) {
        return createRateConfig(nodeName, "", "");
    }

    private RateConfig createRateConfig(String nodeName, String globalCondition, String condition) {
        Rates rates = Rates.of(globalCondition, Rate.of(1, condition));
        return RateConfig.of(RateSource.of(nodeName, true), rates);
    }
}