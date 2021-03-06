package org.nohope.jaxb2.plugin;

import org.junit.Test;
import org.jvnet.jaxb2.maven2.AbstractXJC2Mojo;
import org.jvnet.jaxb2.maven2.ResourceEntry;
import org.jvnet.jaxb2.maven2.test.RunXJC2Mojo;
import org.nohope.logging.Logger;
import org.nohope.logging.LoggerFactory;
import org.nohope.test.ResourceUtils;
import org.nohope.test.runner.TestLifecycleListener;
import org.nohope.typetools.collection.CollectionUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import javax.tools.*;
import javax.tools.JavaCompiler.CompilationTask;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.Assert.assertNotNull;

/**
 * @author <a href="mailto:ketoth.xupack@gmail.com">Ketoth Xupack</a>
 * @since 2013-10-16 14:50
 */
public abstract class Jaxb2PluginTestSupport implements TestLifecycleListener {
    static final Logger LOG = LoggerFactory.getLogger("jaxb2-testing");
    private static final Slf4jToMavenLogAdapter MVN_LOGGER = new Slf4jToMavenLogAdapter(LOG);
    private static final String TEST_RESOURCES_PATH = "src/test/resources/";
    private static final Pattern SLASH = Pattern.compile("/", Pattern.LITERAL);

    static {
        System.setProperty("javax.xml.accessExternalStylesheet", "all");
        System.setProperty("javax.xml.accessExternalSchema", "all");
        System.setProperty("javax.xml.accessExternalDTD", "all");
    }

    private final String[] args;
    private final String resources;
    private final String classpath;
    private final List<String> bindings = new ArrayList<>();

    public Jaxb2PluginTestSupport(final String arg,
                                  final String resources,
                                  final String classpath,
                                  final String... bindings) {
        this(new String[] {arg}, resources, classpath, bindings);
    }

    public Jaxb2PluginTestSupport(final String arg,
                                  final String resources,
                                  final String... bindings) {
        this(new String[] {arg}, resources, bindings);
    }

    public Jaxb2PluginTestSupport(final String[] args,
                                  final String resources,
                                  final String classpath,
                                  final String... bindings) {
        this.args = args.clone();
        this.resources = resources;
        this.classpath = classpath;
        this.bindings.addAll(Arrays.asList(bindings));
    }

    public Jaxb2PluginTestSupport(final String[] args,
                                  final String resources,
                                  final String... bindings) {
        this.args = args.clone();

        this.resources = TEST_RESOURCES_PATH + resources;
        this.classpath = null;
        this.bindings.addAll(Arrays.asList(bindings));
    }

    private String getClasspath() {
        return classpath != null
                ? classpath
                : resources.replace(TEST_RESOURCES_PATH, "").replace("/", ".");
    }

    private String getClasspathPath() {
        return classpath != null
                ? classpath
                : resources.replace(TEST_RESOURCES_PATH, "");
    }

    @Override
    public void runBeforeAllTests() throws Exception {
        try {
            new RunMetadataPlugin().testExecute();
        } catch (final Exception e) {
            LOG.error(e);
            validateBuildFailure(e);
            return;
        }

        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

        final String root = getClass().getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .getFile();

        final ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        final Collection<JavaSourceFromString> units = new ArrayList<>();
        final List<String> classFiles = new ArrayList<>();
        final Collection<String> classes = new ArrayList<>();
        for (final Resource resource : resolver.getResources(
                "classpath*:" + getClasspathPath() + "/**/*.java")) {
            final String file = resource.getFile().getAbsolutePath().replace(root, "");
            if (file.endsWith(".java")) {
                final String prefix = file.substring(0, file.length() - 5);
                final String code = ResourceUtils.getResourceAsString("/" + file);
                assertNotNull(code);
                final String fqdn = SLASH.matcher(prefix).replaceAll(".");
                units.add(new JavaSourceFromString(fqdn, code));
                classFiles.add("/" + prefix + ".class");
                if (!prefix.endsWith("package-info")) {
                    classes.add(fqdn);
                }
            }
        }

        final CompilationTask task = compiler.getTask(null, null, diagnostics, Arrays.asList("-d", root), null, units);

        if (!task.call()) {
            final StringBuilder builder = new StringBuilder("Unable to compile generated source files: ");
            int i = 0;
            for (final Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                builder.append("\n ").append(++i).append(") ").append(diagnostic.getMessage(null)).append('\n');
            }

            throw new IllegalStateException(builder.toString());
        }

        final URLClassLoader ucl = new URLClassLoader(
                CollectionUtils.mapArray(classFiles.toArray(new String[classFiles.size()]), URL.class,
                        source -> {
                            final URL resource = ResourceUtils.getResource(source);
                            assertNotNull(resource);
                            return resource;
                        }));

        for (final String fqdn : classes) {
            ucl.loadClass(fqdn);
        }
    }

    @Test
    public void setupCorrectness() {
    }

    @Override
    public void runAfterAllTests() {
    }

    /**
     * A file object used to represent source coming from a string.
     */
    public class JavaSourceFromString extends SimpleJavaFileObject {
        /**
         * The source code of this "file".
         */
        private final String code;

        /**
         * Constructs a new JavaSourceFromString.
         * @param name the name of the compilation unit represented by this file object
         * @param code the source code for the compilation unit represented by this file object
         */
        public JavaSourceFromString(final String name,
                                    final String code) {
            super(URI.create(
                    "string:///"
                    + name.replace('.', '/')
                    + Kind.SOURCE.extension),
                    Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(final boolean ignoreEncodingErrors) {
            return code;
        }
    }

    private class RunMetadataPlugin extends RunXJC2Mojo {

        @Override
        public File getSchemaDirectory() {
            return new File(getBaseDir(), resources);
        }

        @Override
        protected File getGeneratedDirectory() {
            return new File(getBaseDir(), "target/test-classes");
        }

        @Override
        protected void configureMojo(final AbstractXJC2Mojo mojo) {
            super.configureMojo(mojo);
            mojo.setForceRegenerate(true);
            mojo.setAddCompileSourceRoot(true);
            mojo.setExtension(true);
            mojo.setLog(MVN_LOGGER);

            if (classpath == null) {
                mojo.setGeneratePackage(getClasspath());
            }

            mojo.setVerbose(false);
            mojo.setDebug(false);
            if (LOG.isDebugEnabled()) {
                mojo.setVerbose(true);
                mojo.setDebug(true);
            }

            if (!bindings.isEmpty()) {
                final List<ResourceEntry> entries = new ArrayList<>();
                for (final String binding : bindings) {
                    final ResourceEntry entry = new ResourceEntry();
                    entry.setUrl("file://" + getBaseDir().getAbsolutePath() + "/" + resources + "/" + binding);
                    entries.add(entry);
                }
                mojo.setBindings(entries.toArray(new ResourceEntry[entries.size()]));
            }
        }

        @Override
        public List<String> getArgs() {
            final List<String> args = new ArrayList<>(super.getArgs());
            for (final String arg : Jaxb2PluginTestSupport.this.args) {
                args.add("-X" + arg);
            }
            return args;
        }
    }

    protected void validateBuildFailure(final Exception e) throws Exception {
        throw e;
    }

    public static void assertBuildFailure(final String arg,
                                          final String resources,
                                          final BuildFailureValidator validator) {
        assertBuildFailure(new String[] {arg}, resources, validator);
    }

    public static void assertBuildFailure(final String[] args,
                                          final String resources,
                                          final BuildFailureValidator validator) {
        final Jaxb2PluginTestSupport support = new Jaxb2PluginTestSupport(args, resources) {
            @Override
            protected void validateBuildFailure(final Exception e) throws Exception {
                validator.validate(e);
            }
        };

        try {
            support.runBeforeAllTests();
        } catch (final Exception e) {
            throw new AssertionError(e);
        }
    }

    public interface BuildFailureValidator {
        void validate(final Exception e);
    }
}
