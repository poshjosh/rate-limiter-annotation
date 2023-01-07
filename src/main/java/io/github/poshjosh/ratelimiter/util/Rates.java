package io.github.poshjosh.ratelimiter.util;

import io.github.poshjosh.ratelimiter.BandwidthFactory;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public final class Rates {

    public static Rates of(Rate... limits) {
        return ofDefaults().limits(limits);
    }

    public static Rates ofDefaults() {
        return new Rates();
    }

    public static Rates of(Rates rates) {
        return new Rates(rates);
    }

    public static Rates or(Rate... rates) {
        return of(Operator.OR, rates);
    }

    public static Rates and(Rate... rates) {
        return of(Operator.AND, rates);
    }

    public static Rates of(Operator operator, Rate... rates) {
        return of(operator, rates == null ? Collections.emptyList() : Arrays.asList(rates));
    }

    public static Rates of(Operator operator, List<Rate> rates) {
        return new Rates(operator, rates);
    }

    private Operator operator = Operator.OR;

    /**
     * Multiple limits. Either set this or {@link #limit} but not both.
     * @see #limit
     */
    // The naming of this variable is part of this class'  contract. Do not arbitrarily rename
    // Always access this throw it's getter. A small inconvenience to pay for adding
    // an additional single limit field.
    //
    private List<Rate> limits = Collections.emptyList();

    /**
     * A single limit. Added for convenience. Either set this or {@link #limits} but not both.
     * @see #limits
     */
    // The naming of this variable is part of this class'  contract. Do not arbitrarily rename
    //
    private Rate limit;

    // A public no-argument constructor is required
    public Rates() { }

    Rates(Rates rates) {
        this(rates.operator, rates.limits);
    }

    Rates(Operator operator, List<Rate> limits) {
        this.operator = operator;
        this.limits = limits == null ? Collections.emptyList() : limits.stream()
                .map(Rate::new).collect(Collectors.toList());
    }

    public boolean hasLimits() {
        return size() > 0;
    }

    public int size() {
        return getLimits() == null ? 0 : getLimits().size();
    }

    public Rates operator(Operator operator) {
        setOperator(operator);
        return this;
    }

    public Operator getOperator() {
        return operator;
    }

    public void setOperator(Operator operator) {
        this.operator = operator;
    }

    public Rates limits(Rate... limits) {
        setLimits(Arrays.asList(limits));
        return this;
    }

    public Rates limits(List<Rate> limits) {
        setLimits(limits);
        return this;
    }

    public List<Rate> getLimits() {
        if (limit != null) {

            // We wrap any possibly unmodifiable instance in our own modifiable wrapper
            limits = limits == null ? new ArrayList<>() : new ArrayList<>(limits);

            // In springframework, the single limit was added twice to the limits array.
            // To prevent this, we check if the limits array already contains the single limit.
            if (!limits.contains(limit)) {
                limits.add(limit);
            }
        }
        return limits;
    }

    public void setLimits(List<Rate> limits) {
        this.limits = limits;
    }


    // Rate related properties
    //
    public long getPermits() {
        return limit == null ? 0 : limit.getPermits();
    }

    public void setPermits(long permits) {
        if (limit == null) {
            limit = Rate.of(permits, Duration.ZERO);
            return;
        }
        limit.setPermits(permits);
    }

    public Duration getDuration() {
        return limit == null ? null : limit.getDuration();
    }

    public void setDuration(Duration duration) {
        if(limit == null) {
            limit = Rate.of(0, duration);
            return;
        }
        limit.setDuration(duration);
    }

    public Class<? extends BandwidthFactory> getFactoryClass() {
        return limit == null ? null : limit.getFactoryClass();
    }

    public void setFactoryClass(Class<? extends BandwidthFactory> factoryClass) {
        if(limit == null) {
            limit = Rate.of(0, Duration.ZERO, factoryClass);
            return;
        }
        limit.setFactoryClass(factoryClass);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Rates that = (Rates) o;
        return operator == that.operator && Objects.equals(getLimits(), that.getLimits());
    }

    @Override
    public int hashCode() {
        return Objects.hash(operator, getLimits());
    }

    @Override
    public String toString() {
        return "Rates{" + "operator=" + operator + ", limits=" + getLimits() + '}';
    }
}
