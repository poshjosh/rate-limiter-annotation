package io.github.poshjosh.ratelimiter.util;

import io.github.poshjosh.ratelimiter.node.Node;
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
        Node<RateConfig> node = createNode(nodeName);
        Matcher<String> matcher = matcherProvider.createMatcher(node);
        assertTrue(matcher.matches(nodeName));
        assertFalse(matcher.matches(nodeName + "1"));
    }

    @Test
    void createMatcher_givenRateCondition_shouldCreateAConditionMatcher() throws Exception{
        final int millis = 700;
        LocalDateTime time = LocalDateTime.now().plus(millis, ChronoUnit.MILLIS);
        String nodeName = "test-node-name";
        Node<RateConfig> node = createNode(nodeName, "sys.time>"+time, "");
        Matcher<String> matcher = matcherProvider.createMatcher(node);
        assertFalse(matcher.matches(nodeName));
        Thread.sleep(millis);
        assertTrue(matcher.matches(nodeName));
    }

    @Test
    void createMatchers_givenNoRateCondition_shouldReturnEmpty() {
        String nodeName = "test-node-name";
        Node<RateConfig> node = createNode(nodeName);
        List<Matcher<String>> matchers = matcherProvider.createMatchers(node);
        assertTrue(matchers.isEmpty());
    }

    @Test
    void createMatchers_givenOnlyGlobalRateCondition_shouldReturnEmpty() {
        String nodeName = "test-node-name";
        Node<RateConfig> node = createNode(nodeName, "sys.time.elapsed>PT0S", "");
        List<Matcher<String>> matchers = matcherProvider.createMatchers(node);
        assertTrue(matchers.isEmpty());
    }

    @Test
    void createMatchers_givenOneNonGlobalRateConditions_shouldReturnOne() {
        String nodeName = "test-node-name";
        Node<RateConfig> node = createNode(nodeName, "", "sys.time.elapsed>PT0S");
        List<Matcher<String>> matchers = matcherProvider.createMatchers(node);
        assertEquals(1, matchers.size());
    }

    private Node createNode(String nodeName) {
        return createNode(nodeName, "", "");
    }

    private Node createNode(String nodeName, String globalCondition, String condition) {
        Rates rates = Rates.of(globalCondition, Rate.of(1, condition));
        RateConfig rateConfig = RateConfig.of(rates);
        return Node.ofDefaultParent(nodeName, rateConfig);
    }
}