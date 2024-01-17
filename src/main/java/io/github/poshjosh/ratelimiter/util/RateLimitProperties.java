package io.github.poshjosh.ratelimiter.util;

import io.github.poshjosh.ratelimiter.RateLimiter;
import io.github.poshjosh.ratelimiter.annotations.Rate;
import io.github.poshjosh.ratelimiter.model.Rates;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The class allows for the customization of {@link RateLimiter}s via properties.
 * The properties defined here will be used to create a {@link RateLimiter} independent
 * of those created from the various {@link Rate} annotations.
 */
public interface RateLimitProperties {

    /**
     * Get the application path. For example the path defined by JAX RS
     * {@link @javax.ws.rs.ApplicationPath} annotation.
     *
     * The default is to return empty text. Applications which define an application path
     * should override this method.
     *
     * <p>
     *     <b>Note:</b> Application paths containing '*' or '?' are not currently supported.
     *     <code>issue #001 Application paths containing asterix or question-mark, not supported</code>
     * </p>
     * @return the application path
     */
    default String getApplicationPath() { return ""; }

    /**
     * List of classes to search for resources annotated with rate limit related annotations.
     *
     * If using annotations, implement either this, {@link #getResourcePackages()}, or both.
     * If not using annotations, simply return an empty list.
     *
     * @return the class resources having rate-limit related annotations.
     */
    List<Class<?>> getResourceClasses();

    /**
     * List of packages to search for resources annotated with rate limit related annotations.
     *
     * If using annotations, implement either this, {@link #getResourceClasses()}, or both.
     * If not using annotations, simply return an empty list.
     *
     * @return the packages containing resources having rate-limit related annotations.
     */
    List<String> getResourcePackages();

    default boolean hasRateSources() {
        return !((getResourcePackages() == null || getResourcePackages().isEmpty())
                && (getResourceClasses() == null || getResourceClasses().isEmpty())
                && (getRateLimitConfigs() == null || getRateLimitConfigs().isEmpty()));
    }

    default boolean isRateLimitingEnabled() {
        final Boolean disabled = getDisabled();
        return disabled == null || Boolean.FALSE.equals(disabled);
    }

    /**
     * Should automatic rate limiting be disable?
     * @return {@code true} if automatic rate limiting should be disabled, otherwise return {@code false}
     */
    default Boolean getDisabled() {
        return Boolean.FALSE;
    }

    /**
     * Rates to apply to rate limiters.
     *
     * The default implementation returns an empty map. Override this to provide custom rates.
     *
     * @return Rates to apply to rate limiters
     */
    default Map<String, Rates> getRateLimitConfigs() { return Collections.emptyMap(); }
}
