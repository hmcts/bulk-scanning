package uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor;

import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.InvalidZipArchiveException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.InvalidZipFilesException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.google.common.io.ByteStreams.toByteArray;

public class ZipExtractor {

    public static final String DOCUMENTS_ZIP = "envelope.zip";

    private ZipExtractor() {
    }

    public static Function<ZipInputStream, ZipInputStream> getPreprocessor(
        boolean useWrappingZip
    ) {
        if (useWrappingZip) {
            return ZipExtractor::extract;
        } else {
            return zis -> zis;
        }
    }

    /**
     * Extracts the inner zip.
     */
    static ZipInputStream extract(ZipInputStream zipInputStream) {
        Map<String, byte[]> zipEntries = extractZipEntries(zipInputStream);

        if (zipEntries.containsKey(DOCUMENTS_ZIP)) {
            return new ZipInputStream(new ByteArrayInputStream(zipEntries.get(DOCUMENTS_ZIP)));
        } else {
            throw new InvalidZipFilesException(
                "Zip does not contain envelope. Actual zip entries = " + zipEntries.keySet()
            );
        }
    }

    private static Map<String, byte[]> extractZipEntries(ZipInputStream zis) {
        try {
            Map<String, byte[]> zipEntries = new HashMap<>();
            ZipEntry zipEntry;
            while ((zipEntry = zis.getNextEntry()) != null) {
                zipEntries.put(zipEntry.getName(), toByteArray(zis));
            }

            return zipEntries;
        } catch (IOException ioe) {
            throw new InvalidZipArchiveException("Error extracting zip entries", ioe);
        }
    }
}