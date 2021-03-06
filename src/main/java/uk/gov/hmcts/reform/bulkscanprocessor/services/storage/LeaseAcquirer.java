package uk.gov.hmcts.reform.bulkscanprocessor.services.storage;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.models.BlobErrorCode;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.specialized.BlobLeaseClient;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Consumer;

import static com.azure.storage.blob.models.BlobErrorCode.BLOB_NOT_FOUND;
import static com.azure.storage.blob.models.BlobErrorCode.LEASE_ALREADY_PRESENT;
import static com.azure.storage.blob.models.CopyStatusType.SUCCESS;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.reform.bulkscanprocessor.services.storage.LeaseMetaDataChecker.LEASE_EXPIRATION_TIME;

@Component
public class LeaseAcquirer {

    private static final Logger logger = getLogger(LeaseAcquirer.class);

    private final LeaseMetaDataChecker leaseMetaDataChecker;
    public static final String META_DATA_WAIT_COPY =  "waitingCopy";

    public LeaseAcquirer(
        LeaseMetaDataChecker leaseMetaDataChecker
    ) {
        this.leaseMetaDataChecker = leaseMetaDataChecker;
    }

    /**
     * Main wrapper for blobs to be leased by {@link BlobLeaseClient}.
     *
     * @param blobClient Represents blob
     * @param onLeaseSuccess Consumer which takes in {@code leaseId} acquired with {@link BlobLeaseClient}
     * @param onFailure Extra step to execute in case an error occurred
     * @param releaseLease Flag whether to release the lease or not
     */
    public void ifAcquiredOrElse(
        BlobClient blobClient,
        Consumer<String> onLeaseSuccess,
        Consumer<BlobErrorCode> onFailure,
        boolean releaseLease
    ) {
        try {

            var blobProperties  = blobClient.getProperties();
            if (null != blobProperties.getCopyStatus()
                && blobProperties.getCopyStatus() != SUCCESS) {
                logger.warn(
                    "Copy in progress skipping, file {} in container {}, copy status {}",
                    blobClient.getBlobName(),
                    blobClient.getContainerName(),
                    blobClient.getProperties().getCopyStatus()
                );
                return;
            }

            var metaData = blobProperties.getMetadata();
            if (metaData.get(META_DATA_WAIT_COPY) != null) {
                logger.warn(
                    "Copy Source did not clean the meta data, skipping  file {} in container {}",
                    blobClient.getBlobName(),
                    blobClient.getContainerName()
                );
                return;
            }

            boolean isReady = false;
            BlobErrorCode errorCode = LEASE_ALREADY_PRESENT;

            try {
                isReady = leaseMetaDataChecker.isReadyToUse(blobClient);
            } catch (Exception ex) {
                if (ex instanceof BlobStorageException) {
                    errorCode = getErrorCode(blobClient, (BlobStorageException) ex);
                }
                logger.warn(
                    "Could not check meta data for lease expiration on file {} in container {}",
                    blobClient.getBlobName(),
                    blobClient.getContainerName()
                );
            } finally {
                if (!isReady) {
                    //it means lease did not acquired let the failure function decide
                    onFailure.accept(errorCode);
                }
            }

            if (isReady) {
                onLeaseSuccess.accept(null);
                if (releaseLease) {
                    clearMetadataAndReleaseLease(blobClient);
                }
            }
        } catch (BlobStorageException exc) {

            String logContext = "Error acquiring lease for blob. "
                + "File name: " + blobClient.getBlobName()
                + ", Container: " + blobClient.getContainerName();

            if (exc.getErrorCode() != LEASE_ALREADY_PRESENT && exc.getErrorCode() != BLOB_NOT_FOUND) {
                logger.error(logContext, exc);
            } else {
                logger.info(logContext, exc);
            }

            onFailure.accept(exc.getErrorCode());
        }
    }

    private void clearMetadataAndReleaseLease(
        BlobClient blobClient
    ) {
        try {
            Map<String, String> blobMetaData = blobClient.getProperties().getMetadata();
            blobMetaData.remove(LEASE_EXPIRATION_TIME);
            blobClient.setMetadata(blobMetaData);
        } catch (BlobStorageException exc) {
            logger.warn(
                "Could not clear metadata, lBlob: {}, container: {}",
                blobClient.getBlobName(),
                blobClient.getContainerName(),
                exc
            );
        }
    }

    private BlobErrorCode getErrorCode(BlobClient blobClient, BlobStorageException exc) {
        // sometimes there is no error code in blob storage devmode
        BlobErrorCode errorCode = exc.getErrorCode();
        if (errorCode == null) {
            logger.info("Error code is NULL, File name: {}, Container: {}",
                blobClient.getBlobName(),
                blobClient.getContainerName(),
                exc
            );
            if (exc.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                errorCode = BLOB_NOT_FOUND;
            }
        }
        return errorCode;
    }

}
