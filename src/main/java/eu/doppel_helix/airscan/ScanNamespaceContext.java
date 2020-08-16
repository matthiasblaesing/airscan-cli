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

import java.util.Iterator;
import javax.xml.namespace.NamespaceContext;

public class ScanNamespaceContext implements NamespaceContext {

    public static final String NS_SCAN = "http://schemas.hp.com/imaging/escl/2011/05/03";
    public static final String NS_PWG = "http://www.pwg.org/schemas/2010/12/sm";

    @Override
    public String getNamespaceURI(String prefix) {
        if (prefix.equals("scan")) {
            return NS_SCAN;
        } else if (prefix.equals("pwg")) {
            return NS_PWG;
        }
        return null;
    }

    @Override
    public String getPrefix(String namespaceURI) {
        return null;
    }

    @Override
    public Iterator getPrefixes(String namespaceURI) {
        return null;
    }
}
