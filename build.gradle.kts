plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
}

allprojects {
    configurations.configureEach {
        resolutionStrategy {
            force(
                "com.google.code.gson:gson:2.14.0",
                "com.google.protobuf:protobuf-java:3.25.5",
                "com.google.protobuf:protobuf-kotlin:3.25.5",
                "org.bouncycastle:bcprov-jdk18on:1.86",
                "org.bouncycastle:bcpkix-jdk18on:1.86",
                "org.bouncycastle:bcpg-jdk18on:1.86",
                "org.apache.commons:commons-compress:1.28.0",
                "io.netty:netty-codec:4.2.18.Final",
                "io.netty:netty-codec-http:4.2.18.Final",
                "io.netty:netty-codec-http2:4.2.18.Final",
                "io.netty:netty-common:4.2.18.Final",
                "io.netty:netty-buffer:4.2.18.Final",
                "io.netty:netty-transport:4.2.18.Final",
                "io.netty:netty-handler:4.2.18.Final",
                "io.netty:netty-handler-proxy:4.2.18.Final",
                "io.netty:netty-resolver:4.2.18.Final",
                "io.netty:netty-transport-native-unix-common:4.2.18.Final",
                "io.opentelemetry:opentelemetry-api:1.62.0",
                "org.bitbucket.b_c:jose4j:0.9.6",
                "org.jdom:jdom2:2.0.6.1"
            )
        }
    }
}
