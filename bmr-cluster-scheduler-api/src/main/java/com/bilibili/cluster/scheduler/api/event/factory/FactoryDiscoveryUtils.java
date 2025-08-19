
package com.bilibili.cluster.scheduler.api.event.factory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class FactoryDiscoveryUtils {

    private static final Logger LOG = LoggerFactory.getLogger(FactoryDiscoveryUtils.class);

    private FactoryDiscoveryUtils() {
    }

    /**
     * Returns the {@link PipelineFactory} for the given identifier.
     */
    @SuppressWarnings("unchecked")
    public static <T extends PipelineFactory> T getFactoryByIdentifier(
            String identifier, Class<T> factoryClass) {

        final ServiceLoader<PipelineFactory> loader = ServiceLoader.load(PipelineFactory.class);
        final List<PipelineFactory> pipelineFactoryList = new ArrayList<>();

        for (PipelineFactory pipelineFactory : loader) {
            if (pipelineFactory != null
                    && pipelineFactory.identifier().equals(identifier)
                    && factoryClass.isAssignableFrom(pipelineFactory.getClass())) {
                pipelineFactoryList.add(pipelineFactory);
            }
        }

        if (pipelineFactoryList.isEmpty()) {
            throw new RuntimeException(
                    String.format(
                            "Cannot find factory with identifier \"%s\" in the classpath.\n\n"
                                    + "Available factory classes are:\n\n"
                                    + "%s",
                            identifier,
                            StreamSupport.stream(loader.spliterator(), false)
                                    .map(f -> f.getClass().getName())
                                    .sorted()
                                    .collect(Collectors.joining("\n"))));
        }

        if (pipelineFactoryList.size() > 1) {
            throw new RuntimeException(
                    String.format(
                            "Multiple factories found in the classpath.\n\n"
                                    + "Ambiguous factory classes are:\n\n"
                                    + "%s",
                            pipelineFactoryList.stream()
                                    .map(f -> f.getClass().getName())
                                    .sorted()
                                    .collect(Collectors.joining("\n"))));
        }

        return (T) pipelineFactoryList.get(0);
    }

    /**
     * Return the path of the jar file that contains the {@link PipelineFactory} for the given identifier.
     */
    public static <T extends PipelineFactory> Optional<URL> getJarPathByIdentifier(
            String identifier, Class<T> factoryClass) {
        try {
            T factory = getFactoryByIdentifier(identifier, factoryClass);
            URL url = factory.getClass().getProtectionDomain().getCodeSource().getLocation();
            if (Files.isDirectory(Paths.get(url.toURI()))) {
                LOG.warn(
                        "The factory class \"{}\" is contained by directory \"{}\" instead of JAR. "
                                + "This might happen in integration test. Will ignore the directory.",
                        factory.getClass().getCanonicalName(),
                        url);
                return Optional.empty();
            }
            return Optional.of(url);
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format("Failed to search JAR by factory identifier \"%s\"", identifier));
        }
    }
}
