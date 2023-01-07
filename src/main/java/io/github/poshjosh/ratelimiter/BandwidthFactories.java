package io.github.poshjosh.ratelimiter;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

final class BandwidthFactories {

    private static WeakReference<Map<String, BandwidthFactory>> bandwidthFactoryMapWeakReference;

    private BandwidthFactories() { }

    static BandwidthFactory createSystemBandwidthFactory() {
        return createBandwidthFactory(getSystemBandwidthFactoryClass());
    }

    private static Class<? extends BandwidthFactory> getSystemBandwidthFactoryClass() {
        final String factoryClassName = System.getProperty("bandwidth-factory-class");
        if (factoryClassName == null) {
            return BandwidthFactory.AllOrNothing.class;
        }
        try {
            return (Class<BandwidthFactory>)Class.forName(factoryClassName);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    static <T extends BandwidthFactory> T getOrCreateBandwidthFactory(Class<T> clazz) {
        if (bandwidthFactoryMapWeakReference == null || bandwidthFactoryMapWeakReference.get() == null) {
            bandwidthFactoryMapWeakReference = new WeakReference<>(new HashMap<>());
        }
        Map<String, BandwidthFactory> bandwidthFactoryMap = bandwidthFactoryMapWeakReference.get();
        if (bandwidthFactoryMap == null) {
            bandwidthFactoryMap = new HashMap<>();
            bandwidthFactoryMapWeakReference = new WeakReference<>(bandwidthFactoryMap);
        }
        final String key = clazz.getName();
        BandwidthFactory bandwidthFactory = bandwidthFactoryMap.get(key);
        if (bandwidthFactory == null) {
            bandwidthFactory = createBandwidthFactory(clazz);
            bandwidthFactoryMap.put(key, bandwidthFactory);
        }
        return (T)bandwidthFactory;
    }

    private static <T extends BandwidthFactory> T createBandwidthFactory(Class<T> clazz) {
        try {
            return clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException e) {
            throw new RuntimeException(e);
        }
    }
}
