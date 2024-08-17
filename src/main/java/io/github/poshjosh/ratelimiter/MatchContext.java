package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.model.Rate;
import io.github.poshjosh.ratelimiter.model.RateConfig;
import io.github.poshjosh.ratelimiter.model.RateSource;
import io.github.poshjosh.ratelimiter.model.Rates;
import io.github.poshjosh.ratelimiter.util.Matcher;

import java.util.List;

public interface MatchContext<INPUT> {
    boolean matches(INPUT key, MatchVisitor<?> matchVisitor);

    boolean hasMatcher();

    String getId();

    boolean hasSubConditions();

    RateSource getSource();

    Rate getRate(int index);

    Rates getRates();

    Rates getRatesOrParentRates();

    Matcher<INPUT> getMainMatcher();

    List<Matcher<INPUT>> getLimitMatchers();

    RateConfig getRateConfig();
}
