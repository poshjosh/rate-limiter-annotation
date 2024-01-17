package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.bandwidths.RateToBandwidthConverter;
import io.github.poshjosh.ratelimiter.model.Rates;
import io.github.poshjosh.ratelimiter.store.BandwidthsStore;
import io.github.poshjosh.ratelimiter.util.ClassesInPackageFinder;
import io.github.poshjosh.ratelimiter.util.MatcherProvider;
import io.github.poshjosh.ratelimiter.util.RateLimitProperties;
import io.github.poshjosh.ratelimiter.util.Ticker;

import java.util.*;

public class RateLimiterContextImpl<K> implements RateLimiterContext<K> {

    private RateLimitProperties properties;

    private MatcherProvider<K> matcherProvider;

    private RateLimiterProvider rateLimiterProvider;

    private BandwidthsStore<?> store;

    private Ticker ticker;

    private String[] packages;

    private Class<?>[] classes;

    private Map<String, Rates> rates;

    public RateLimiterContextImpl() { }

    public RateLimiterContextImpl with(RateLimiterContextImpl<K> context) {
        this.properties = context.getProperties();
        this.matcherProvider = context.getMatcherProvider();
        this.rateLimiterProvider = context.getRateLimiterProvider();
        this.store = context.getStore();
        this.ticker = context.getTicker();
        this.packages = context.getPackages();
        this.classes = context.getClasses();
        this.rates = context.getRates();
        return this;
    }

    public RateLimiterContext<K> withDefaultsAsFallback() {
        if (!hasRateSources()) {
            throw new IllegalArgumentException(
                    "A source of rates must be defined, either: packages, classes, or rates");
        }
        if (getProperties() == null) {
            setProperties(new RateLimitProperties(){
                @Override public List<Class<?>> getResourceClasses() {
                    return classes == null ? Collections.emptyList() : Arrays.asList(classes);
                }
                @Override public List<String> getResourcePackages() {
                    return packages == null ? Collections.emptyList() : Arrays.asList(packages);
                }
                @Override public Map<String, Rates> getRateLimitConfigs() {
                    return rates == null ? Collections.emptyMap() : rates;
                }
            });
        }

        if (matcherProvider == null) {
            setMatcherProvider(MatcherProvider.ofDefaults());
        }

        if (store == null) {
            setStore(BandwidthsStore.ofDefaults());
        }

        if (ticker == null) {
            // To maintain a synchronized time between distributed services,
            // prefer the time since epoch.
            setTicker(Ticker.SYSTEM_EPOCH_MILLIS);
        }

        if (rateLimiterProvider == null) {
            // We decide to use this as a sensible default.
            // If you want to convert Rate to Bandwidth in a different way, then
            // implement your own RateLimiterProvider and pass it to the builder.
            final RateToBandwidthConverter rateToBandwidthConverter =
                    RateToBandwidthConverter.ofDefaults(ticker);
            setRateLimiterProvider(RateLimiterProvider.of(
                    rateToBandwidthConverter, store, ticker));
        }
        return this;
    }

    @Override
    public boolean hasRateSources() {
        return !((properties == null || !properties.hasRateSources())
                && (packages == null || packages.length == 0)
                && (classes == null || classes.length == 0)
                && (rates == null || rates.isEmpty()));
    }
    @Override public Set<Class<?>> getTargetClasses() {
        Set<Class<?>> classes = new HashSet<>();
        classes.addAll(getProperties().getResourceClasses());
        // TODO - ClassesInPackageFinder is hidden logic, which should be exposed?
        List<Class<?>> classesFromPackages = getClassesInPackageFinder()
                .findClasses(getProperties().getResourcePackages());
        classes.addAll(classesFromPackages);
        return Collections.unmodifiableSet(classes);
    }

    protected ClassesInPackageFinder getClassesInPackageFinder() {
        return ClassesInPackageFinder.ofDefaults();
    }

    @Override public RateLimiterContext<K> withProperties(RateLimitProperties properties) {
        RateLimiterContextImpl result = with(this);
        result.setProperties(properties);
        return result;
    }

    @Override public RateLimiterContext<K> withMatcherProvider(MatcherProvider<K> matcherProvider) {
        RateLimiterContextImpl result = with(this);
        result.setMatcherProvider(matcherProvider);
        return result;
    }

    @Override public RateLimiterContext<K> withRateLimiterProvider(
            RateLimiterProvider rateLimiterProvider) {
        RateLimiterContextImpl result = with(this);
        result.setRateLimiterProvider(rateLimiterProvider);
        return result;
    }

    @Override public RateLimiterContext<K> withStore(BandwidthsStore<K> bandwidthsStore) {
        RateLimiterContextImpl result = with(this);
        result.setStore(store);
        return result;
    }

    @Override public RateLimiterContext<K> withTicker(Ticker ticker) {
        RateLimiterContextImpl result = with(this);
        result.setTicker(ticker);
        return result;
    }

    @Override public RateLimitProperties getProperties() {
        return properties;
    }

    public void setProperties(RateLimitProperties properties) {
        this.properties = properties;
    }

    @Override public MatcherProvider<K> getMatcherProvider() {
        return matcherProvider;
    }

    public void setMatcherProvider(MatcherProvider<K> matcherProvider) {
        this.matcherProvider = matcherProvider;
    }

    @Override public RateLimiterProvider getRateLimiterProvider() {
        return rateLimiterProvider;
    }

    public void setRateLimiterProvider(RateLimiterProvider rateLimiterProvider) {
        this.rateLimiterProvider = rateLimiterProvider;
    }

    @Override public BandwidthsStore<?> getStore() {
        return store;
    }

    public void setStore(BandwidthsStore<?> store) {
        this.store = store;
    }

    @Override public Ticker getTicker() {
        return ticker;
    }

    public void setTicker(Ticker ticker) {
        this.ticker = ticker;
    }

    public String[] getPackages() {
        return packages;
    }

    public void setPackages(String[] packages) {
        this.packages = packages;
    }

    public Class<?>[] getClasses() {
        return classes;
    }

    public void setClasses(Class<?>[] classes) {
        this.classes = classes;
    }

    public Map<String, Rates> getRates() {
        return rates;
    }

    public void setRates(Map<String, Rates> rates) {
        this.rates = rates;
    }

    @Override public String toString() {
        return "RateLimiterContextImpl{" + "properties=" + properties + ", matcherProvider="
                + matcherProvider + ", rateLimiterProvider=" + rateLimiterProvider + ", store="
                + store + ", ticker=" + ticker + ", packages=" + Arrays.toString(packages)
                + ", classes=" + Arrays.toString(classes) + ", rates=" + rates + '}';
    }
}
