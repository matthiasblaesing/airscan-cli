/*
 * Copyright 2020 Matthias Bläsing
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

package eu.doppel_helix.airscan;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import picocli.CommandLine;

public class VersionProvider implements CommandLine.IVersionProvider {

    private static final Logger LOG = Logger.getLogger(VersionProvider.class.getName());

    private final String[] version;

    public VersionProvider() {
        Properties properties = new Properties();
        try (InputStream is = VersionProvider.class.getResourceAsStream("/META-INF/maven/eu.doppel_helix.cloudscan/cloudscan/pom.properties")) {
            if (is != null) {
                properties.load(is);
            }
        } catch (IOException ex) {
            LOG.log(Level.WARNING, null, ex);
        }
        version = new String[]{properties.getProperty("version", "unknown")};
    }

    @Override
    public String[] getVersion() throws Exception {
        return version;
    }

}
