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

import static eu.doppel_helix.airscan.ScanNamespaceContext.NS_SCAN;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class Capabilities {
    private int maxWidth;
    private int maxHeight;
    private List<String> colorModes = Collections.EMPTY_LIST;
    private List<String> contentTypes = Collections.EMPTY_LIST;
    private List<String> documentFormats = Collections.EMPTY_LIST;
    private List<Integer> resolutions = Collections.EMPTY_LIST;
    private int maxResolution;
    private Integer defaultResolution;
    private String defaultColorMode;

    public int getMaxWidth() {
        return maxWidth;
    }

    public void setMaxWidth(int maxWidth) {
        this.maxWidth = maxWidth;
    }

    public int getMaxHeight() {
        return maxHeight;
    }

    public void setMaxHeight(int maxHeight) {
        this.maxHeight = maxHeight;
    }

    public List<String> getColorModes() {
        return colorModes;
    }

    public void setColorModes(List<String> colorModes) {
        this.colorModes = colorModes;
    }

    public List<String> getContentTypes() {
        return contentTypes;
    }

    public void setContentTypes(List<String> contentTypes) {
        this.contentTypes = contentTypes;
    }

    public List<String> getDocumentFormats() {
        return documentFormats;
    }

    public void setDocumentFormats(List<String> documentFormats) {
        this.documentFormats = documentFormats;
    }

    public List<Integer> getResolutions() {
        return resolutions;
    }

    public void setResolutions(List<Integer> resolutions) {
        this.resolutions = resolutions;
    }

    public int getMaxResolution() {
        return maxResolution;
    }

    public void setMaxResolution(int maxResolution) {
        this.maxResolution = maxResolution;
    }

    public Integer getDefaultResolution() {
        return defaultResolution;
    }

    public void setDefaultResolution(Integer defaultResolution) {
        this.defaultResolution = defaultResolution;
    }

    public String getDefaultColorMode() {
        return defaultColorMode;
    }

    public void setDefaultColorMode(String defaultColorMode) {
        this.defaultColorMode = defaultColorMode;
    }

    @Override
    public String toString() {
        return "Capabilities{" + "maxWidth=" + maxWidth + ", maxHeight=" + maxHeight + ", colorModes=" + colorModes + ", contentTypes=" + contentTypes + ", documentFormats=" + documentFormats + ", resolutions=" + resolutions + ", maxResolution=" + maxResolution + '}';
    }

    public void parse(InputStream is, boolean debug) throws IOException {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newDefaultInstance();
            dbf.setNamespaceAware(true);
            byte[] data = is.readAllBytes();
            if(debug) {
                System.err.println("Received:");
                System.err.write(data);
            }
            Document doc = dbf.newDocumentBuilder().parse(new ByteArrayInputStream(data));
            maxHeight = getIntegerFromDocument(doc.getDocumentElement(), NS_SCAN, "MaxHeight");
            maxWidth = getIntegerFromDocument(doc.getDocumentElement(), NS_SCAN, "MaxWidth");
            maxResolution = Math.min(
                getIntegerFromDocument(doc.getDocumentElement(), NS_SCAN, "MaxOpticalXResolution"),
                getIntegerFromDocument(doc.getDocumentElement(), NS_SCAN, "MaxOpticalYResolution")
            );
            colorModes = Collections.unmodifiableList(getChildElementsContent(doc.getDocumentElement(), NS_SCAN, "ColorModes"));
            defaultColorMode = getChildElementsDefault(doc.getDocumentElement(), NS_SCAN, "ColorModes");
            contentTypes = Collections.unmodifiableList(getChildElementsContent(doc.getDocumentElement(), NS_SCAN, "ContentTypes"));
            documentFormats = Collections.unmodifiableList(getChildElementsContent(doc.getDocumentElement(), NS_SCAN, "DocumentFormats"));
            List<Integer> resolutionsBuilder = new ArrayList<>();
            NodeList nl = getSingleElement(doc.getDocumentElement(), NS_SCAN, "DiscreteResolutions").getChildNodes();
            for (int i = 0; i < nl.getLength(); i++) {
                Node n = nl.item(i);
                if (n instanceof Element) {
                    Element e = (Element) n;
                    Element xresEl = getSingleElement(e, NS_SCAN, "XResolution");
                    Element yresEl = getSingleElement(e, NS_SCAN, "YResolution");
                    int xres = Integer.parseInt(xresEl.getTextContent());
                    int yres = Integer.parseInt(yresEl.getTextContent());
                    resolutionsBuilder.add(Math.min(xres, yres));
                    if ("true".equalsIgnoreCase(e.getAttributeNS(NS_SCAN, "default"))
                        || "true".equalsIgnoreCase(xresEl.getAttributeNS(NS_SCAN, "default"))
                        || "true".equalsIgnoreCase(yresEl.getAttributeNS(NS_SCAN, "default"))) {
                        defaultResolution = Math.min(xres, yres);
                    }
                }
            }
            resolutions = Collections.unmodifiableList(resolutionsBuilder);
        } catch (ParserConfigurationException | SAXException | NumberFormatException ex) {
            throw new IOException(ex);
        }
    }

    private List<String> getChildElementsContent(Element document, String ns, String tag) throws IOException {
        NodeList nl = getSingleElement(document, ns, tag).getChildNodes();
        List<String> result = new ArrayList<>();
        for(int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);
            if(n instanceof Element) {
                Element e = (Element) n;
                result.add(e.getTextContent());
            }
        }
        return result;
    }

    private String getChildElementsDefault(Element document, String ns, String tag) throws IOException {
        NodeList nl = getSingleElement(document, ns, tag).getChildNodes();
        for(int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);
            if(n instanceof Element) {
                Element e = (Element) n;
                if("true".equalsIgnoreCase(e.getAttributeNS(NS_SCAN, "default"))) {
                    return e.getTextContent();
                }
            }
        }
        return null;
    }

    private int getIntegerFromDocument(Element document, String ns, String tag) throws IOException {
        try {
            return Integer.parseInt(getSingleElement(document, ns, tag).getTextContent());
        } catch (NumberFormatException ex) {
            throw new IOException(ex);
        }
    }

    private Element getSingleElement(Element document, String ns, String tag) throws IOException {
        NodeList nl = document.getElementsByTagNameNS(ns, tag);
        if(nl.getLength() != 1) {
            throw new IOException(String.format("Found %d instances for %s:%s", nl.getLength(), ns, tag));
        }
        return (Element) nl.item(0);
    }

}
