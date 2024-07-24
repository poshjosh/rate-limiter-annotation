package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.model.Rate;
import io.github.poshjosh.ratelimiter.model.RateConfig;
import io.github.poshjosh.ratelimiter.model.RateSource;
import io.github.poshjosh.ratelimiter.model.Rates;
import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.util.Matcher;
import io.github.poshjosh.ratelimiter.util.MatcherProvider;
import io.github.poshjosh.ratelimiter.util.Matchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

final class RateContext<R> {

    private static final Logger LOG = LoggerFactory.getLogger(RateContext.class);

    // Bottom-up traversal performs better, as of the last tests.
    static final boolean IS_BOTTOM_UP_TRAVERSAL = true;

    static <K> RateContext<K> of(
            MatcherProvider<K> matcherProvider,
            Node<RateConfig> node) {
        RateConfig rateConfig = node.getValueOrDefault(null);
        LOG.trace("{}", rateConfig);
        if (rateConfig == null) {
            return null;
        }
        Matcher<K> mainMatcher;
        List<Matcher<K>> limitMatchers;
        if(!hasLimitsInTree(node) && !rateConfig.shouldDelegateToParent()) {
            LOG.debug("No limits specified for group, so no matcher will be created for: {}",
                    node.getName());
            mainMatcher = Matchers.matchNone();
            limitMatchers = Collections.emptyList();
        } else {
            mainMatcher = matcherProvider.createMainMatcher(rateConfig);
            limitMatchers = matcherProvider.createLimitMatchers(rateConfig);
            // Tag:Rule:number-of-matchers-must-equal-number-of-rates
            if (limitMatchers.size() != rateConfig.getRates().subLimitSize()) {
                throw new IllegalStateException(
                        String.format("Number of Matchers: %s is not equal to number of rates: %s",
                                limitMatchers.size(), rateConfig.getRates().subLimitSize()));
            }
        }
        final RateContext<K> rateContext =
                new RateContext<>(rateConfig, mainMatcher, limitMatchers);
        LOG.trace("{}", rateContext);
        return rateContext;
    }
    private static boolean hasLimitsInTree(Node<RateConfig> node) {
        return hasLimits(node) || (IS_BOTTOM_UP_TRAVERSAL ?
                anyParentHasLimits(node) : anyChildHasLimits(node));
    }
    private static boolean anyParentHasLimits(Node<RateConfig> node) {
        return node.getParentOptional()
                .filter(parent -> parent.hasValue() && hasLimitsInTree(parent))
                .isPresent();
    }
    private static boolean anyChildHasLimits(Node<RateConfig> node) {
        return node.getChildren().stream()
                .anyMatch(child -> child.hasValue() && hasLimitsInTree(child));
    }
    private static boolean hasLimits(Node<RateConfig> node) {
        return node.requireValue().getRates().hasLimitsSet();
    }

    private final RateConfig rateConfig;

    /**
     * The matcher to apply before applying sub matchers.
     */
    private final Matcher<R> mainMatcher;

    /**
     * Matchers for rate conditions specific to each rate.
     */
    private final List<Matcher<R>> limitMatchers;

    private RateContext(RateConfig rateConfig,
            Matcher<R> mainMatcher, List<Matcher<R>> limitMatchers) {
        this.rateConfig = Objects.requireNonNull(rateConfig);
        this.mainMatcher = Objects.requireNonNull(mainMatcher);
        this.limitMatchers = Collections.unmodifiableList(new ArrayList<>(limitMatchers));
    }

    public boolean hasMatcher() {
        return !Matchers.matchNone().equals(mainMatcher) ||
                limitMatchers.stream().anyMatch(matcher -> !Matchers.matchNone().equals(matcher));
    }

    public String getId() {
        return rateConfig.getId();
    }

    public boolean hasLimits() {
        return rateConfig.getRates().hasLimitsSet();
    }

    public boolean hasSubConditions() {
        return rateConfig.getRates().hasSubConditions();
    }

    public RateSource getSource() { return rateConfig.getSource(); }

    public Rate getRate(int index) {
        return Rate.of(rateConfig.getRates().getSubLimits().get(index));
    }

    public Rates getRates() {
        return Rates.of(rateConfig.getRates());
    }

    public Rates getRatesWithParentRatesAsFallback() {
        return Rates.of(rateConfig.getRatesWithParentRatesAsFallback());
    }

    public Matcher<R> getMainMatcher() { return mainMatcher; }

    public List<Matcher<R>> getLimitMatchers() {
        return limitMatchers;
    }

    public RateConfig getRateConfig() {
        return rateConfig;
    }

    @Override public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        RateContext<?> that = (RateContext<?>) o;
        return rateConfig.equals(that.rateConfig) && mainMatcher.equals(that.mainMatcher)
                && limitMatchers.equals(that.limitMatchers);
    }

    @Override public int hashCode() {
        return Objects.hash(rateConfig, mainMatcher, limitMatchers);
    }

    @Override public String toString() {
        return "RateContext{config=" + rateConfig +
                ", mainMatcher=" + mainMatcher + ", limitMatchers=" + limitMatchers + '}';
    }
}
