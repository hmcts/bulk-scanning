package uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableSet;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItem;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

public class Document {

    private static final String DOCUMENT_TYPE_CHERISHED = "Cherished";
    private static final String DOCUMENT_TYPE_OTHER = "Other";
    private static final Set<String> DOCUMENT_TYPES =
        ImmutableSet.of(DOCUMENT_TYPE_CHERISHED, DOCUMENT_TYPE_OTHER);

    @JsonProperty("file_name")
    public final String fileName;

    @JsonProperty("control_number")
    public final String controlNumber;

    @JsonProperty("type")
    public final String type;

    @JsonProperty("scanned_at")
    public final Instant scannedAt;

    @JsonProperty("url")
    public final String url;

    @JsonProperty("ocr_data")
    public final Map<String, String> ocrData;

    // region constructor
    private Document(
        String fileName,
        String controlNumber,
        String type,
        Instant scannedAt,
        String url,
        Map<String, String> ocrData
    ) {
        this.fileName = fileName;
        this.controlNumber = controlNumber;
        this.type = type;
        this.scannedAt = scannedAt;
        this.url = url;
        this.ocrData = ocrData;
    }
    // endregion

    public static Document fromScannableItem(ScannableItem item) {
        return new Document(
            item.getFileName(),
            item.getDocumentControlNumber(),
            mapDocumentType(item.getDocumentType()),
            item.getScanningDate().toInstant(),
            item.getDocumentUrl(),
            item.getOcrData()
        );
    }

    // This mapping will eventually be done during the creation of DB envelope, when
    // document subtype is introduced by the team. Until that happens, we need to store
    // original values in the DB, so that no information is lost. That's why the mapping
    // takes place when putting the message on the queue.
    private static String mapDocumentType(String documentType) {
        if (DOCUMENT_TYPES.contains(documentType)) {
            return documentType.toLowerCase();
        } else {
            return DOCUMENT_TYPE_OTHER.toLowerCase();
        }
    }
}
