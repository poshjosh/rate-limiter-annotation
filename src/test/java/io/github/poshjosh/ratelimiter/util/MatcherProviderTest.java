package io.github.poshjosh.ratelimiter.util;

import io.github.poshjosh.ratelimiter.annotation.RateSource;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MatcherProviderTest {

    private final MatcherProvider<String> matcherProvider = MatcherProvider.ofDefaults();

    @Test
    void createMatcher_givenNoRateCondition_shouldCreateANodeNameMatcher() {
        String nodeName = "test-node-name";
        RateConfig rateConfig = createRateConfig(nodeName);
        Matcher<String> matcher = matcherProvider.createMatcher(rateConfig);
        assertTrue(matcher.matches(nodeName));
        assertFalse(matcher.matches(nodeName + "1"));
    }

    @Test
    void createMatcher_givenRateCondition_shouldCreateAConditionMatcher() throws Exception{
        final int millis = 700;
        LocalDateTime time = LocalDateTime.now().plus(millis, ChronoUnit.MILLIS);
        String nodeName = "test-node-name";
        RateConfig rateConfig = createRateConfig(nodeName, "sys.time>"+time, "");
        Matcher<String> matcher = matcherProvider.createMatcher(rateConfig);
        assertFalse(matcher.matches(nodeName));
        Thread.sleep(millis);
        assertTrue(matcher.matches(nodeName));
    }

    @Test
    void createMatchers_givenNoRateCondition_shouldReturnEmpty() {
        String nodeName = "test-node-name";
        RateConfig rateConfig = createRateConfig(nodeName);
        List<Matcher<String>> matchers = matcherProvider.createMatchers(rateConfig);
        assertTrue(matchers.isEmpty());
    }

    @Test
    void createMatchers_givenOnlyGlobalRateCondition_shouldReturnEmpty() {
        String nodeName = "test-node-name";
        RateConfig rateConfig = createRateConfig(nodeName, "sys.time.elapsed>PT0S", "");
        List<Matcher<String>> matchers = matcherProvider.createMatchers(rateConfig);
        assertTrue(matchers.isEmpty());
    }

    @Test
    void createMatchers_givenOneNonGlobalRateConditions_shouldReturnOne() {
        String nodeName = "test-node-name";
        RateConfig rateConfig = createRateConfig(nodeName, "", "sys.time.elapsed>PT0S");
        List<Matcher<String>> matchers = matcherProvider.createMatchers(rateConfig);
        assertEquals(1, matchers.size());
    }

    private RateConfig createRateConfig(String nodeName) {
        return createRateConfig(nodeName, "", "");
    }

    private RateConfig createRateConfig(String nodeName, String globalCondition, String condition) {
        Rates rates = Rates.of(globalCondition, Rate.of(1, condition));
        return RateConfig.of(RateSource.of(nodeName), rates);
    }
}