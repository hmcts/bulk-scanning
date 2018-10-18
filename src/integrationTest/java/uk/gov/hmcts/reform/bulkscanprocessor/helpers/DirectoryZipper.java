package uk.gov.hmcts.reform.bulkscanprocessor.helpers;

import com.google.common.io.Files;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.google.common.io.Resources.getResource;
import static java.util.stream.Collectors.toList;

public final class DirectoryZipper {

    /**
     * Zips files from given directory. Files in resulting archive are NOT wrapped in a directory.
     */
    public static byte[] zipDir(String dirName) throws IOException {

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(outputStream)) {

            // .listFiles() does not guarantee any order.
            // Files need to be sorted as currently mocks rely on files order.
            // TODO: improve mocks in tests so that order does not matter
            List<File> filesToZip =
                Stream.of(new File(getResource(dirName).getPath()).listFiles())
                    .sorted()
                    .collect(toList());

            for (File file : filesToZip) {
                zos.putNextEntry(new ZipEntry(file.getName()));
                zos.write(Files.toByteArray(file));
                zos.closeEntry();
            }
        }

        return outputStream.toByteArray();
    }

    private DirectoryZipper() {
        // util class
    }
}