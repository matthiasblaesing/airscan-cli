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

import static eu.doppel_helix.airscan.MDNSUtil.parseTxt;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.Callable;
import javax.jmdns.JmmDNS;
import javax.jmdns.ServiceInfo;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import picocli.CommandLine;
import picocli.CommandLine.Option;

@CommandLine.Command(
    name = "airscan",
    mixinStandardHelpOptions = true,
    versionProvider = VersionProvider.class,
    description = "Scan from an \"air scan\" scanner"
)
public class SimpleScan implements Callable<Integer> {

    public static void main(String[] argv) throws Exception {
        System.exit(new CommandLine(new SimpleScan()).execute(argv));
    }

    private static final String SEPARATOR = "================================================================================";

    @Option(names = {"-o", "--output"}, description = "Output filename", defaultValue = "output.jpg")
    private File outputFile;

    @Option(names = {"-t", "--timeout"}, description = "Timeout in seconds to search for scanner")
    private int timeout = 1;

    @Option(names = {"-u", "--url"}, description = "URL to directly contact the scanner")
    private String scannerUrl = null;

    @Option(names = {"-i", "--infoonly"}, description = "Only show information about scanner")
    private boolean infoOnly = false;

    @Option(names = {"-d", "--debug"}, description = "Verbose output")
    private boolean debug = false;

    @Option(names = {"-r", "--resolution"}, description = "Resolution (Defaults to scanner default or highest possible resolution)")
    private Integer resolution;

    @Option(names = {"-c", "--colormode"}, description = "Color model to use (Defaults to scanner default, RGB24 if present or the first supported color mode)")
    private String colorMode;

    private final DocumentBuilder db;
    private final XPath xp;

    public SimpleScan() {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            db = dbf.newDocumentBuilder();
            xp = XPathFactory.newInstance().newXPath();
            xp.setNamespaceContext(new ScanNamespaceContext());
        } catch (ParserConfigurationException ex) {
            throw new RuntimeException("Failed to initializes XML", ex);
        }
    }

    public Integer call() throws Exception {
        scanForScanner();

        if (scannerUrl == null || scannerUrl.isEmpty()) {
            System.err.println("No scanner found");
            return 1;
        }

        System.out.println("\nSelected scanner: " + scannerUrl);

        Capabilities c = getCapabilities();

        if(colorMode == null) {
            if(c.getDefaultColorMode() != null && (! c.getDefaultColorMode().isBlank())) {
                colorMode = c.getDefaultColorMode();
            } else {
                colorMode = c.getColorModes().contains("RGB24") ? "RGB24" : c.getColorModes().get(0);
            }
        }

        if(resolution == null) {
            if(c.getDefaultResolution() != null) {
                resolution = c.getDefaultResolution();
            } else {
                resolution = c.getMaxResolution();
            }
        }

        if(! infoOnly) {
            doScan(0, 0, c.getMaxWidth(), c.getMaxHeight(), colorMode, resolution, resolution);
        }

        return 0;
    }

    private Capabilities getCapabilities() throws IOException {
        Capabilities c = new Capabilities();
        try (InputStream is = new URL(scannerUrl + "ScannerCapabilities").openStream()) {
            c.parse(is, debug);
        }

        System.out.println("\nCapabilities");
        System.out.printf("%20s: %d%n", "Max Height", c.getMaxHeight());
        System.out.printf("%20s: %d%n", "Max Width", c.getMaxWidth());
        System.out.printf("%20s: %d%n", "Max Resolution", c.getMaxResolution());
        System.out.printf("%20s: %d%n", "Default Resolution", c.getDefaultResolution());
        System.out.printf("%20s: %s%n", "Resolutions", c.getResolutions());
        System.out.printf("%20s: %s%n", "Color modes", c.getColorModes());
        System.out.printf("%20s: %s%n", "Content types", c.getContentTypes());
        System.out.printf("%20s: %s%n", "Document formats", c.getDocumentFormats());

        return c;
    }

    /**
     * Unit for {@code xoffset}, {@code yoffset}, {@code width} and
     * {@code height} have to be specified in threehundreth of inches.
     *
     * @param xoffset
     * @param yoffset
     * @param width
     * @param height
     * @param colorMode
     * @param xresolution
     * @param yresolution
     */
    private void doScan(int xoffset, int yoffset, int width, int height, String colorMode, int xresolution, int yresolution) throws IOException {
        System.out.printf("%nBeginning scan (%s, %d, %d)%n", colorMode, xresolution, yresolution);

        try {
            // It would be better, if a real DOM implementation could be used
            // here, but at least on a tested canon scanner invalid XML
            // namespace behavior was observed. Instead of building the DOM
            // "correctly", just build from a known working document
            String request = String.format("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                + "<scan:ScanSettings xmlns:scan=\"http://schemas.hp.com/imaging/escl/2011/05/03\" xmlns:pwg=\"http://www.pwg.org/schemas/2010/12/sm\">\n"
                + "    <pwg:Version>2.6</pwg:Version>\n"
                + "    <pwg:ScanRegions>\n"
                + "        <pwg:ScanRegion>\n"
                + "            <pwg:XOffset>%d</pwg:XOffset>\n"
                + "            <pwg:YOffset>%d</pwg:YOffset>\n"
                + "            <pwg:Width>%d</pwg:Width>\n"
                + "            <pwg:Height>%d</pwg:Height>\n"
                + "            <pwg:ContentRegionUnits>escl:ThreeHundredthsOfInches</pwg:ContentRegionUnits>\n"
                + "        </pwg:ScanRegion>\n"
                + "    </pwg:ScanRegions>\n"
                + "    <scan:InputSource>Platten</scan:InputSource>\n"
                + "    <scan:ColorMode>%s</scan:ColorMode>\n"
                + "    <scan:XResolution>%d</scan:XResolution>\n"
                + "    <scan:YResolution>%d</scan:YResolution>\n"
                + "</scan:ScanSettings>",
                xoffset,
                yoffset,
                width,
                height,
                colorMode,
                xresolution,
                yresolution);

            URL url = new URL(scannerUrl + "ScanJobs");
            HttpURLConnection httpUrlConnection = (HttpURLConnection) url.openConnection();

            httpUrlConnection.setDoInput(true);
            httpUrlConnection.setDoOutput(true);
            httpUrlConnection.setRequestMethod("POST");
            httpUrlConnection.setRequestProperty("Content-Type", "text/xml");

            if (debug) {
                System.err.println(SEPARATOR);
                System.err.printf("%20s: %s%n", "URL", url.toExternalForm());
                System.err.printf("%20s: %s%n", "Method", "POST");
                System.err.printf("%20s: %n%s%n%n", "Body", request);
            }

            try(OutputStream os = httpUrlConnection.getOutputStream()) {
                os.write(request.getBytes(StandardCharsets.UTF_8));
            } catch (IOException ex) {
                try(InputStream is = httpUrlConnection.getErrorStream()) {
                    if(is != null) {
                        is.transferTo(System.err);
                    }
                }
                throw ex;
            }

            if (httpUrlConnection.getResponseCode() == 200) {
                try (InputStream is = httpUrlConnection.getInputStream();
                    FileOutputStream fos = new FileOutputStream(outputFile)) {
                    if(debug) {
                       System.out.println("Scan initialization return http status 200");
                    }
                    is.transferTo(fos);
                } catch (IOException ex) {
                    try (InputStream is = httpUrlConnection.getErrorStream()) {
                        if (is != null) {
                            is.transferTo(System.err);
                        }
                    }
                    throw ex;
                }
            } else if (httpUrlConnection.getResponseCode() == 201) {
                if(debug) {
                    System.err.println("Location Header received: " + httpUrlConnection.getHeaderField("Location"));
                }
                try (InputStream is = new URL(httpUrlConnection.getHeaderField("Location") + "/NextDocument").openStream();
                    FileOutputStream fos = new FileOutputStream(outputFile)) {
                    is.transferTo(fos);
                }

                System.out.println("Wrote scan to: " + outputFile);
            } else {
                throw new IOException("Scanner did not send http Status 200 or 201");
            }
        } catch (MalformedURLException ex) {
            throw new IOException(ex);
        }

    }

    private void scanForScanner() throws IOException {
        if (scannerUrl == null || scannerUrl.isBlank()) {
            ServiceInfo[] services;
            try (JmmDNS dns = JmmDNS.Factory.getInstance()) {
                services = dns.list("_scanner._tcp.local.", timeout * 1000);
            }

            System.out.println("Found scanner: ");
            for (ServiceInfo si : services) {
                Map<String, String> txtData = parseTxt(si.getTextBytes());
                if (txtData.containsKey("mdl") && txtData.containsKey("mfg")) {
                    System.out.println("\t" + txtData.get("mfg") + " " + txtData.get("mdl"));
                } else if (txtData.containsKey("ty")) {
                    System.out.println("\t" + txtData.get("ty"));
                } else {
                    System.out.println("\t" + si.getQualifiedName());
                }
                for (String host : si.getHostAddresses()) {
                    String url = String.format("http://%s:%d/eSCL/", host, si.getPort());
                    System.out.println("\t\t" + url);
                    scannerUrl = url;
                }
            }
        }
    }
}
