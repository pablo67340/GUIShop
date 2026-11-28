package com.pablo67340.guishop.neoforge.lib;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * The third party libraries the mod carries, kept out of the module system.
 * <p>
 * A mod jar becomes a module, and a module exports every package it contains.
 * sqlite-jdbc and snakeyaml are both common enough that another mod, or a
 * hybrid server such as Arclight, is likely to supply them too, and two modules
 * exporting one package to a module that reads both is a boot failure for the
 * whole server rather than a problem for us.
 * <p>
 * So the classes are stored under {@value #PREFIX} instead of at the root. FML
 * derives packages from the directories holding class files, so it sees
 * {@code guishoplibs.org.sqlite} and nothing else can collide with that. The
 * bytes are untouched and still name themselves {@code org.sqlite}, which
 * matters for sqlite-jdbc: its native entry points are called
 * {@code Java_org_sqlite_core_NativeDB_*}, so genuinely renaming the package
 * would leave the driver unable to bind to its own native library.
 * <p>
 * Development runs load the mod from its class directories, where nothing is
 * prefixed. The loader falls through to its parent when a class is not bundled,
 * so those runs pick the libraries up from the runtime classpath as before.
 */
public final class BundledLibraries {

    /** Must stay a valid Java identifier, or FML cannot derive a package name. */
    private static final String PREFIX = "guishoplibs/";

    private static final ClassLoader LOADER = new Loader(BundledLibraries.class.getClassLoader());

    private BundledLibraries() {
    }

    /**
     * Loads a bundled library class.
     *
     * @param name the original binary name, such as {@code org.sqlite.SQLiteDataSource}
     */
    public static Class<?> load(String name) throws ClassNotFoundException {
        return Class.forName(name, true, LOADER);
    }

    private static final class Loader extends ClassLoader {

        static {
            registerAsParallelCapable();
        }

        private final ClassLoader source;

        Loader(ClassLoader source) {
            super("guishop-libs", source);
            this.source = source;
        }

        /**
         * Bundled classes are taken from here first, so a copy the server
         * happens to have elsewhere cannot be mixed with ours halfway through a
         * library. Everything else, the JDK included, goes to the parent.
         */
        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            synchronized (getClassLoadingLock(name)) {
                Class<?> found = findLoadedClass(name);
                if (found == null) {
                    byte[] bytes = read(name.replace('.', '/') + ".class");
                    found = bytes == null
                            ? super.loadClass(name, false)
                            : defineClass(name, bytes, 0, bytes.length);
                }
                if (resolve) {
                    resolveClass(found);
                }
                return found;
            }
        }

        @Override
        public URL getResource(String name) {
            URL own = source.getResource(PREFIX + name);
            return own != null ? own : super.getResource(name);
        }

        @Override
        public InputStream getResourceAsStream(String name) {
            InputStream own = source.getResourceAsStream(PREFIX + name);
            return own != null ? own : super.getResourceAsStream(name);
        }

        @Override
        public Enumeration<URL> getResources(String name) throws IOException {
            List<URL> all = new ArrayList<>(Collections.list(source.getResources(PREFIX + name)));
            all.addAll(Collections.list(super.getResources(name)));
            return Collections.enumeration(all);
        }

        private byte[] read(String path) {
            try (InputStream stream = source.getResourceAsStream(PREFIX + path)) {
                return stream == null ? null : stream.readAllBytes();
            } catch (IOException exception) {
                return null;
            }
        }
    }
}
