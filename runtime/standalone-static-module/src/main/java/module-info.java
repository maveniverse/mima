module eu.maveniverse.maven.mima.runtime.standalonestatic.module {
    requires java.base;

    exports eu.maveniverse.maven.mima.context;

    // resolver-api
    exports org.eclipse.aether;
    exports org.eclipse.aether.artifact;
    exports org.eclipse.aether.collection;
    exports org.eclipse.aether.deployment;
    exports org.eclipse.aether.graph;
    exports org.eclipse.aether.installation;
    exports org.eclipse.aether.metadata;
    exports org.eclipse.aether.repository;
    exports org.eclipse.aether.resolution;
    exports org.eclipse.aether.transfer;
    exports org.eclipse.aether.transform;
    exports org.eclipse.aether.version;

    // resolver-util
    exports org.eclipse.aether.util;
    exports org.eclipse.aether.util.artifact;
    exports org.eclipse.aether.util.concurrency;
    exports org.eclipse.aether.util.filter;
    exports org.eclipse.aether.util.graph.manager;
    exports org.eclipse.aether.util.graph.selector;
    exports org.eclipse.aether.util.graph.transformer;
    exports org.eclipse.aether.util.graph.traverser;
    exports org.eclipse.aether.util.graph.version;
    exports org.eclipse.aether.util.graph.visitor;
    exports org.eclipse.aether.util.listener;
    exports org.eclipse.aether.util.repository;
    exports org.eclipse.aether.util.version;

    // maven-settings
    exports org.apache.maven.settings;
}
