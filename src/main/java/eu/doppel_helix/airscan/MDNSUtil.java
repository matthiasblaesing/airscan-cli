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

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class MDNSUtil {
    /**
     * Parse a TXT record of a MDNS service into a map of key-value pairs.
     *
     * @param data
     * @return
     */
    public static Map<String,String> parseTxt(byte[] data) {
        Map<String,String> map = new HashMap<>();
        int offset = 0;
        while(offset < data.length) {
            int length = (int) data[offset];
            if(length <= 0) {
                break; // Fishy
            }
            String stringData = new String(data, offset + 1, length, StandardCharsets.UTF_8);
            int eqPos = stringData.indexOf("=");
            if(eqPos >= 0) {
                map.put(stringData.substring(0, eqPos), stringData.substring(eqPos + 1));
            } else {
                map.put(stringData, null);
            }
            offset += length + 1;
        }
        return map;
    }
}
