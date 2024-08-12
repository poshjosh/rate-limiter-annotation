package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.model.Rate;
import io.github.poshjosh.ratelimiter.model.Rates;

interface MatchVisitor<MATCH_RESULT> {
    void visit(String match, Rate rate);

    void visit(String match, Rates rates);

    MATCH_RESULT getResult();
}
