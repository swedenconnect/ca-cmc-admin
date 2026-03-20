/*
 * Copyright (c) 2026.  Agency for Digital Government (DIGG)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package se.swedenconnect.ca.cmcclient.configuration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

@Component
public class LenientResourceResolver {

  private static final Pattern SCHEME = Pattern.compile("^[a-zA-Z][a-zA-Z0-9+.-]*:.*");

  private final ResourceLoader resourceLoader;

  @Autowired
  public LenientResourceResolver(ResourceLoader resourceLoader) {
    this.resourceLoader = resourceLoader;
  }

  public Resource getResource(String location) {
    return resourceLoader.getResource(normalize(location));
  }

  static String normalize(String location) {
    if (location == null) {
      return null;
    }
    String loc = location.trim();
    if (loc.isEmpty()) {
      return loc;
    }

    // Keep explicit schemes as-is: classpath:, file:, http:, etc.
    if (SCHEME.matcher(loc).matches()) {
      return loc;
    }

    // If it looks like an absolute file path and exists, treat as file:
    if (looksLikeAbsolutePath(loc)) {
      Path p = toPath(loc);
      if (p != null && p.isAbsolute() && Files.exists(p)) {
        return toFileLocation(p);
      }
    }

    // Otherwise leave it alone (could be relative, or servlet-context relative by intention)
    return loc;
  }

  private static boolean looksLikeAbsolutePath(String loc) {
    return loc.startsWith("/") || (loc.length() >= 3
        && Character.isLetter(loc.charAt(0))
        && loc.charAt(1) == ':'
        && (loc.charAt(2) == '\\' || loc.charAt(2) == '/'));
  }

  private static Path toPath(String loc) {
    try {
      // Handle Windows paths like C:\dir\file
      if (loc.length() >= 2 && loc.charAt(1) == ':') {
        return Paths.get(loc);
      }
      return Paths.get(loc);
    } catch (Exception e) {
      return null;
    }
  }

  private static String toFileLocation(Path p) {
    // ResourceLoader accepts file:/... for both Unix and Windows (with forward slashes)
    String normalized = p.toAbsolutePath().toString().replace('\\', '/');
    if (!normalized.startsWith("/")) {
      // Windows like C:/...
      normalized = "/" + normalized;
    }
    return "file:" + normalized;
  }
}
