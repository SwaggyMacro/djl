/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance
 * with the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package ai.djl.sentencepiece.jni;

import ai.djl.util.Platform;
import ai.djl.util.Utils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities for finding the SentencePiece binary on the System.
 *
 * <p>The binary will be searched for in a variety of locations in the following order:
 *
 * <ol>
 *   <li>In the path specified by the SENTENCEPIECE_LIBRARY_PATH environment variable
 *   <li>In a jar file location in the classpath. These jars can be created with the pytorch-native
 *       module.
 * </ol>
 */
@SuppressWarnings("MissingJavadocMethod")
public final class LibUtils {

    private static final Logger logger = LoggerFactory.getLogger(LibUtils.class);

    private static final String LIB_NAME = "sentencepiece_native";

    private LibUtils() {}

    public static void loadLibrary() {
        if (System.getProperty("os.name").startsWith("Win")) {
            throw new UnsupportedOperationException("Windows is not supported.");
        }

        String libName = copyJniLibraryFromClasspath();
        logger.debug("Loading sentencepiece library from: {}", libName);
        System.load(libName); // NOPMD
    }

    private static String copyJniLibraryFromClasspath() {
        String name = System.mapLibraryName(LIB_NAME);
        Path nativeDir = Utils.getEngineCacheDir("sentencepiece");
        Platform platform = Platform.detectPlatform("sentencepiece");
        String classifier = platform.getClassifier();
        String version = platform.getVersion();
        Path path = nativeDir.resolve(version).resolve(name);
        if (Files.exists(path)) {
            return path.toAbsolutePath().toString();
        }
        Path tmp = null;
        String libPath = "/native/lib/" + classifier + "/" + name;
        logger.info("Extracting {} to cache ...", libPath);
        try (InputStream stream = LibUtils.class.getResourceAsStream(libPath)) {
            if (stream == null) {
                throw new IllegalStateException("SentencePiece library not found: " + libPath);
            }
            Files.createDirectories(nativeDir.resolve(version));
            tmp = Files.createTempFile(nativeDir, "jni", "tmp");
            Files.copy(stream, tmp, StandardCopyOption.REPLACE_EXISTING);
            Utils.moveQuietly(tmp, path);
            return path.toAbsolutePath().toString();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot copy jni files", e);
        } finally {
            if (tmp != null) {
                Utils.deleteQuietly(tmp);
            }
        }
    }
}
