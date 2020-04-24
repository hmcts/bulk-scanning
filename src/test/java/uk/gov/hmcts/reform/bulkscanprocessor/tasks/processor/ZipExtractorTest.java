package uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.InvalidZipFilesException;
import uk.gov.hmcts.reform.bulkscanprocessor.helper.DirectoryZipper.ZipItem;

import java.io.ByteArrayInputStream;
import java.util.zip.ZipInputStream;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.DirectoryZipper.zipDir;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.DirectoryZipper.zipDirAndWrap;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.DirectoryZipper.zipItems;

@ExtendWith(MockitoExtension.class)
public class ZipExtractorTest {
    @Test
    public void should_verify_valid_zip_successfully() throws Exception {
        byte[] zipBytes = zipDirAndWrap("signature/sample_valid_content");

        ZipInputStream zis = ZipExtractor.extract(new ZipInputStream(new ByteArrayInputStream(zipBytes)));
        assertThat(zis).isNotNull();
    }

    @Test
    public void should_throw_exception_if_envelope_is_not_found() throws Exception {
        byte[] innerZip = zipDir("signature/sample_valid_content");
        byte[] outerZip = zipItems(singletonList((new ZipItem("invalid_entry_name", innerZip))));

        assertThatThrownBy(() -> ZipExtractor.extract(new ZipInputStream(new ByteArrayInputStream(outerZip))))
            .isInstanceOf(InvalidZipFilesException.class)
            .hasMessageContaining("Zip does not contain envelope");
    }
}