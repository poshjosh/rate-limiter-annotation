package com.looseboxes.ratelimiter.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

final class DefaultClassesInPackageFinder implements ClassesInPackageFinder{

    private static final Logger LOG = LoggerFactory.getLogger(DefaultClassesInPackageFinder.class);
    
    private final ClassLoader classLoader;

    DefaultClassesInPackageFinder(ClassLoader classLoader) {
        this.classLoader = Objects.requireNonNull(classLoader);
    }

    @Override
    public List<Class<?>> findClasses(String packageName, ClassFilter classFilter) {
        try{
            return Collections.unmodifiableList(doFindClasses(packageName, classFilter));
        }catch(IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Attempts to collect all the classes in the specified package as determined
     * by the context class loader
     * 
     * @param packageName
     *            the package name to search
     * @param classFilter
     *            the filter for selecting classes to add 
     * @return a list of classes that exist within that package
     * @throws ClassNotFoundException if something went wrong
     * @throws IOException
     */
    private List<Class<?>> doFindClasses(String packageName, ClassFilter classFilter)
            throws ClassNotFoundException, IOException {
        
        final ArrayList<Class<?>> classes = new ArrayList<>();

        final Enumeration<URL> resources = classLoader.getResources(packageName.replace('.', '/'));

        URLConnection connection;

        for (URL url = null; resources.hasMoreElements()
                && ((url = resources.nextElement()) != null);) {

                connection = url.openConnection();

            if (connection instanceof JarURLConnection) {

                checkJarFile((JarURLConnection) connection, packageName, classes, classFilter);

            } else if (connection.getClass().getCanonicalName().equals("sun.net.www.protocol.file.FileURLConnection")) {

                checkDirectory(new File(URLDecoder.decode(url.getPath(), "UTF-8")), packageName, classes, classFilter);
            } else {
                throw new IOException(packageName + " (" + url.getPath() + ") does not appear to be a valid package");
            }    
        }

        LOG.trace("In package: {}, found classes: {}", packageName, classes);
        
        return classes;
    }
    
    /**
     * Add classes from directory
     * 
     * @param directory
     *            The directory to start with
     * @param pckgname
     *            The package name to search for. Will be needed for getting the
     *            Class object.
     * @param classes
     *            if a file isn't loaded but still is in the directory
     * @param classFilter
     *            the filter for selecting classes to add 
     * @throws ClassNotFoundException
     */
    private void checkDirectory(File directory, String pckgname, List<Class<?>> classes, ClassFilter classFilter)
            throws ClassNotFoundException {
        File tmpDirectory;

        if (directory.exists() && directory.isDirectory()) {
            
            final String[] files = directory.list();

            for (final String file : files) {
                if (file.endsWith(".class")) {
                    try {
                        Class clazz = Class.forName(pckgname + '.' + file.substring(0, file.length() - 6));
                        if(classFilter.test(clazz)) {
                            classes.add(clazz);
                        }
                    } catch (final NoClassDefFoundError ignored) {
                        // do nothing. this class hasn't been found by the
                        // loader, and we don't care.
                    }
                } else if ((tmpDirectory = new File(directory, file)).isDirectory()) {
                    checkDirectory(tmpDirectory, pckgname + "." + file, classes, classFilter);
                }
            }
        }
    }

    /**
     * Add classes from jar file
     * 
     * @param connection
     *            the connection to the jar
     * @param pckgname
     *            the package name to search for
     * @param classes
     *            the current ArrayList of all classes. This method will simply
     *            add new classes.
     * @param classFilter
     *            the filter for selecting classes to add 
     * @throws ClassNotFoundException
     *             if a file isn't loaded but still is in the jar file
     * @throws IOException
     *             if it can't correctly read from the jar file.
     */
    private void checkJarFile(JarURLConnection connection, String pckgname, List<Class<?>> classes, ClassFilter classFilter)
            throws ClassNotFoundException, IOException {
        final JarFile jarFile = connection.getJarFile();
        final Enumeration<JarEntry> entries = jarFile.entries();
        String name;

        for (JarEntry jarEntry = null; entries.hasMoreElements()
                && ((jarEntry = entries.nextElement()) != null);) {
            name = jarEntry.getName();

            if (name.contains(".class")) {
                name = name.substring(0, name.length() - 6).replace('/', '.');

                if (name.contains(pckgname)) {
                    Class clazz = Class.forName(name);
                    if(classFilter.test(clazz)) {
                        classes.add(clazz);
                    }
                }
            }
        }
    }
}
