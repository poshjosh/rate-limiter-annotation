package io.github.poshjosh.ratelimiter.util;

import io.github.poshjosh.ratelimiter.model.Rate;
import io.github.poshjosh.ratelimiter.model.RateConfig;
import io.github.poshjosh.ratelimiter.model.RateSource;
import io.github.poshjosh.ratelimiter.model.Rates;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MatcherProviderTest {

    private final MatcherProvider<String> matcherProvider = MatcherProvider.ofDefaults();

    @Test
    void createMainMatcher_givenOnlyNodeName_shouldCreateANodeNameMatcher() {
        String nodeName = "test-node-name";
        RateConfig rateConfig = givenRateConfigWithNoConditions(nodeName);
        Matcher<String> matcher = matcherProvider.createMainMatcher(rateConfig);
        assertTrue(matcher.matches(nodeName));
        assertFalse(matcher.matches("invalid"));
    }

    @Test
    void mainMatcherForNodeName_shouldNotMatchSubText() {
        String nodeName = "test-node-name";
        RateConfig rateConfig = givenRateConfigWithNoConditions(nodeName);
        Matcher<String> matcher = matcherProvider.createMainMatcher(rateConfig);
        assertFalse(matcher.matches(nodeName.substring(0, 3)));
    }

    @Test
    void mainMatcherForNodeName_shouldNotMatchSuperText() {
        String nodeName = "test-node-name";
        RateConfig rateConfig = givenRateConfigWithNoConditions(nodeName);
        Matcher<String> matcher = matcherProvider.createMainMatcher(rateConfig);
        assertFalse(matcher.matches(nodeName + "1"));
    }

    @Test
    void createMainMatcher_givenRateCondition_shouldCreateAConditionMatcher() throws Exception{
        final int millis = 700;
        LocalDateTime time = LocalDateTime.now().plus(millis, ChronoUnit.MILLIS);
        String nodeName = "test-node-name";
        RateConfig rateConfig = givenRateConfigWithConditions(nodeName, "sys.time>"+time, "");
        Matcher<String> matcher = matcherProvider.createMainMatcher(rateConfig);
        assertFalse(matcher.matches(nodeName));
        Thread.sleep(millis);
        assertTrue(matcher.matches(nodeName));
    }

    @Test
    void createSubMatchers_givenNoRateCondition_shouldReturnEmpty() {
        RateConfig rateConfig = RateConfig.of(Rates.empty());
        List<Matcher<String>> matchers = matcherProvider.createSubMatchers(rateConfig);
        assertTrue(matchers.isEmpty());
    }

    @Test
    void createSubMatchers_givenOnlyGlobalRateCondition_shouldReturnEmpty() {
        Rates rates = new Rates();
        rates.setRateCondition("sys.time.elapsed>PT0S");
        RateConfig rateConfig = RateConfig.of(rates);
        List<Matcher<String>> matchers = matcherProvider.createSubMatchers(rateConfig);
        assertTrue(matchers.isEmpty());
    }

    @Test
    void createSubMatchers_shouldReturnValidNumberOfMatchers() {
        List<Rate> rateList = Arrays.asList(Rate.ofDays(1), Rate.ofDays(2));
        Rates rates = Rates.of(Operator.OR, "sys.time.elapsed>PT0S", rateList);
        RateConfig rateConfig = RateConfig.of(rates);
        List<Matcher<String>> matchers = matcherProvider.createSubMatchers(rateConfig);
        assertEquals(rateList.size(), matchers.size());
    }

    @Test
    void createSubMatchers_givenOnlyOneGlobalRate_shouldReturnEmpty() {
        Rates rates = new Rates();
        rates.setPermits(1);
        rates.setDuration(Duration.ofSeconds(1));
        rates.setRateCondition("sys.time.elapsed>PT0S");
        RateConfig rateConfig = RateConfig.of(rates);
        List<Matcher<String>> matchers = matcherProvider.createSubMatchers(rateConfig);
        assertTrue(matchers.isEmpty());
    }

    @Test
    void createSubMatchers_givenOneNonGlobalRateCondition_shouldReturnOne() {
        String nodeName = "test-node-name";
        RateConfig rateConfig = givenRateConfigWithConditions(nodeName, "", "sys.time.elapsed>PT0S");
        List<Matcher<String>> matchers = matcherProvider.createSubMatchers(rateConfig);
        assertEquals(1, matchers.size());
    }

    private RateConfig givenRateConfigWithNoConditions(String nodeName) {
        return givenRateConfigWithConditions(nodeName, "", "");
    }

    private RateConfig givenRateConfigWithConditions(String nodeName, String globalCondition, String condition) {
        Rates rates = Rates.of(globalCondition, Rate.of(1, condition));
        return RateConfig.of(RateSource.of(nodeName, true), rates, null);
    }
}