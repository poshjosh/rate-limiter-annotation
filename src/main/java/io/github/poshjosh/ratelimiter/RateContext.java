package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.model.Rate;
import io.github.poshjosh.ratelimiter.model.RateConfig;
import io.github.poshjosh.ratelimiter.model.RateSource;
import io.github.poshjosh.ratelimiter.model.Rates;
import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.util.Matcher;
import io.github.poshjosh.ratelimiter.util.MatcherProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.GenericDeclaration;
import java.util.*;

final class RateContext<R> {

    private static final Logger LOG = LoggerFactory.getLogger(RateContext.class);

    static boolean isBottomUpTraversal() {
        return true; // Bottom-up traversal performs better, as of the last tests.
    }

    static <K> RateContext<K> of(
            MatcherProvider<K> matcherProvider,
            Node<RateConfig> node) {
        RateConfig rateConfig = node.getValueOrDefault(null);
        LOG.trace("{}", rateConfig);
        if (rateConfig == null) {
            return null;
        }
        Matcher<K> mainMatcher;
        List<Matcher<K>> subMatchers;
        if(!hasLimitsInTree(node) && !rateConfig.shouldDelegateToParent()) {
            LOG.debug("No limits specified for group, so no matcher will be created for: {}",
                    node.getName());
            mainMatcher = Matcher.matchNone();
            subMatchers = Collections.emptyList();
        } else {
            mainMatcher = matcherProvider.createMainMatcher(rateConfig);
            subMatchers = matcherProvider.createSubMatchers(rateConfig);
            // Tag:Rule:number-of-matchers-must-equal-number-of-rates
            if (subMatchers.size() != rateConfig.getRates().subLimitSize()) {
                throw new IllegalStateException(
                        String.format("Number of Matchers: %s is not equal to number of rates: %s",
                                subMatchers.size(), rateConfig.getRates().subLimitSize()));
            }
        }
        final RateContext<K> rateContext =
                new RateContext<>(rateConfig, mainMatcher, subMatchers);
        LOG.trace("{}", rateContext);
        return rateContext;
    }
    private static boolean hasLimitsInTree(Node<RateConfig> node) {
        return hasLimits(node) || (isBottomUpTraversal() ?
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
        return node.requireValue().getRates().hasLimits();
    }

    private final RateConfig rateConfig;

    /**
     * The matcher to apply before applying sub matchers.
     */
    private final Matcher<R> mainMatcher;

    /**
     * Matchers for rate conditions specific to each rate.
     */
    private final List<Matcher<R>> subMatchers;

    private RateContext(RateConfig rateConfig,
            Matcher<R> mainMatcher, List<Matcher<R>> subMatchers) {
        this.rateConfig = Objects.requireNonNull(rateConfig);
        this.mainMatcher = Objects.requireNonNull(mainMatcher);
        this.subMatchers = Collections.unmodifiableList(new ArrayList<>(subMatchers));
    }

    public boolean isGroupSource() {
        return rateConfig.getSource().isGroupType();
    }

    public boolean isGenericDeclarationSource() {
        return rateConfig.getSource().getSource() instanceof GenericDeclaration;
    }

    public String getId() {
        return rateConfig.getId();
    }

    public boolean hasLimits() {
        return rateConfig.getRates().hasLimits();
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

    public List<Matcher<R>> getSubMatchers() {
        return subMatchers;
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
                && subMatchers.equals(that.subMatchers);
    }

    @Override public int hashCode() {
        return Objects.hash(rateConfig, mainMatcher, subMatchers);
    }

    @Override public String toString() {
        return "RateContext{config=" + rateConfig +
                ", mainMatcher=" + mainMatcher + ", subMatchers=" + subMatchers + '}';
    }
}
