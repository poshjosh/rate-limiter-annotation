package com.looseboxes.ratelimiter;

import com.looseboxes.ratelimiter.annotation.NodeValue;
import com.looseboxes.ratelimiter.node.Node;
import com.looseboxes.ratelimiter.util.Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

public class PatternMatchingResourceLimiter<R> implements ResourceLimiter<R> {

    private enum RateLimitResult{SUCCESS, FAILURE, NOMATCH}

    private static final Logger log = LoggerFactory.getLogger(PatternMatchingResourceLimiter.class);
    
    public interface MatcherProvider<T>{
        Matcher<T, ?> getMatcher(String nodeName, NodeValue<ResourceLimiter<?>> nodeValue);
    }

    private final MatcherProvider<R> matcherProvider;
    private final Node<NodeValue<ResourceLimiter<?>>> rootNode;
    private final Set<Node<NodeValue<ResourceLimiter<?>>>> leafNodes;
    private final boolean firstMatchOnly;

    public PatternMatchingResourceLimiter(
            Node<NodeValue<ResourceLimiter<?>>> rootNode,
            boolean firstMatchOnly) {
        this((nodeName, nodeData) -> Matcher.identity(), rootNode, firstMatchOnly);
    }

    public PatternMatchingResourceLimiter(MatcherProvider<R> matcherProvider,
                                          Node<NodeValue<ResourceLimiter<?>>> rootNode,
                                          boolean firstMatchOnly) {
        this.matcherProvider = Objects.requireNonNull(matcherProvider);
        this.rootNode = Objects.requireNonNull(rootNode);
        Set<Node<NodeValue<ResourceLimiter<?>>>> set = new LinkedHashSet<>();
        collectLeafNodes(this.rootNode, set::add);
        this.leafNodes = Collections.unmodifiableSet(set);
        this.firstMatchOnly = firstMatchOnly;
    }

    private <T> void collectLeafNodes(Node<T> root, Consumer<Node<T>> collector) {
        new BreadthFirstNodeVisitor<>(Node::isLeaf, collector).accept(root);
    }

    @Override
    public boolean tryConsume(Object context, R request, int permits, long timeout, TimeUnit unit) {
        Function<Node<NodeValue<ResourceLimiter<?>>>, RateLimitResult> consumePermits =
                node -> tryConsume(request, permits, timeout, unit, node);
        return visitNodes(consumePermits);
    }

    public boolean visitNodes(Function<Node<NodeValue<ResourceLimiter<?>>>, RateLimitResult> visitor) {

        int globalFailureCount = 0;

        for(Node<NodeValue<ResourceLimiter<?>>> node : leafNodes) {

            int nodeSuccessCount = 0;

            while(node != rootNode && node != null && node.hasNodeValue()) {

                final RateLimitResult result = visitor.apply(node);

                switch(result) {
                    case SUCCESS: ++nodeSuccessCount; break;
                    case FAILURE: ++globalFailureCount; break;
                    case NOMATCH:
                        break;
                    default: throw new IllegalArgumentException();
                }

                if(!RateLimitResult.SUCCESS.equals(result)) {
                    break;
                }

                node = node.getParentOrDefault(null);
            }

            if(firstMatchOnly && nodeSuccessCount > 0) {
                break;
            }
        }

        return globalFailureCount == 0;
    }

    private RateLimitResult tryConsume(R request, int permits, long timeout, TimeUnit unit, Node<NodeValue<ResourceLimiter<?>>> node) {

        final ResourceLimiter<?> resourceLimiter = getRateLimiter(node);

        if(resourceLimiter == ResourceLimiter.NO_OP) {
            return RateLimitResult.NOMATCH;
        }

        final Object match = matchOrNull(request, node);

        if(match == null) {
            return RateLimitResult.NOMATCH;
        }

        return ((ResourceLimiter<Object>) resourceLimiter).tryConsume(request, match, permits, timeout, unit) ? RateLimitResult.SUCCESS : RateLimitResult.FAILURE;
    }

    private Object matchOrNull(R request, Node<NodeValue<ResourceLimiter<?>>> node) {
        final String nodeName = node.getName();
        final NodeValue<ResourceLimiter<?>> nodeValue = node.getValueOptional().orElseThrow(NullPointerException::new);
        final Matcher<R, ?> matcher = matcherProvider.getMatcher(nodeName, nodeValue);
        final Object match = matcher.matchOrNull(request);
        if(log.isTraceEnabled()) {
            log.trace("Matched: {}, match: {}, name: {}, matcher: {}",  match != null, match, nodeName, matcher);
        }
        return match;
    }

    private ResourceLimiter<?> getRateLimiter(Node<NodeValue<ResourceLimiter<?>>> node) {
        final ResourceLimiter<?> resourceLimiter = node.getValueOrDefault(null).getValue();
        if(log.isTraceEnabled()) {
            log.trace("Name: {}, rate-limiter: {}", node.getName(), resourceLimiter);
        }
        return resourceLimiter;
    }
}
