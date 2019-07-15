package uk.gov.hmcts.reform.bulkscanprocessor.validation;

import io.vavr.control.Try;
import joptsimple.internal.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.util.Optionals;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.bulkscanprocessor.config.ContainerMappings;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.OcrValidationException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputEnvelope;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client.OcrValidationClient;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client.model.req.FormData;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client.model.req.OcrDataField;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

@Component
@EnableConfigurationProperties(ContainerMappings.class)
public class OcrValidator {

    private static final Logger log = LoggerFactory.getLogger(OcrValidator.class);

    private final OcrValidationClient client;
    private final ContainerMappings containerMappings;
    private final AuthTokenGenerator authTokenGenerator;

    //region constructor
    public OcrValidator(
        OcrValidationClient client,
        ContainerMappings containerMappings,
        AuthTokenGenerator authTokenGenerator
    ) {
        this.client = client;
        this.containerMappings = containerMappings;
        this.authTokenGenerator = authTokenGenerator;
    }
    //endregion

    public void assertIsValid(InputEnvelope envelope) {
        Optionals.ifAllPresent(
            findValidationUrl(envelope.poBox),
            findDocWithOcr(envelope),
            (validationUrl, docWithOcr) ->
                Try.of(() -> client.validate(validationUrl, toFormData(docWithOcr), authTokenGenerator.generate()))
                    .onSuccess(res -> {
                        switch (res.status) {
                            case ERRORS:
                                throw new OcrValidationException(
                                    "Ocr validation failed. "
                                        + "Document OCR: " + docWithOcr.documentControlNumber + ". "
                                        + "Envelope: " + envelope.zipFileName + ". "
                                        + "Errors: " + Strings.join(res.errors, ", ")
                                );
                            case WARNINGS:
                                log.info(
                                    "Validation ended with warnings. File name: {}",
                                    envelope.zipFileName
                                );
                                break;
                            default:
                                break;
                        }
                    })
                    .onFailure(exc -> {
                        log.error(
                            "Error calling validation endpoint. Url: {}, document: {}, envelope: {}",
                            validationUrl,
                            docWithOcr.documentControlNumber,
                            envelope.zipFileName,
                            exc
                        );
                        // log error and proceed
                    })
        );
    }

    private Optional<String> findValidationUrl(String poBox) {
        return containerMappings
            .getMappings()
            .stream()
            .filter(mapping -> Objects.equals(mapping.getPoBox(), poBox))
            .findFirst()
            .map(mapping -> mapping.getOcrValidationUrl())
            .filter(url -> !Strings.isNullOrEmpty(url));
    }

    private Optional<InputScannableItem> findDocWithOcr(InputEnvelope envelope) {
        List<InputScannableItem> docsWithOcr =
            envelope
                .scannableItems
                .stream()
                .filter(it -> it.ocrData != null)
                .collect(toList());
        if (docsWithOcr.size() > 1) {
            log.warn("Multiple documents with OCR in envelope. File name: {}", envelope.zipFileName);
        }
        return docsWithOcr.stream().findFirst();
    }

    private FormData toFormData(InputScannableItem doc) {
        return new FormData(
            doc.documentSubtype,
            doc
                .ocrData
                .getFields()
                .stream()
                .map(it -> new OcrDataField(it.name.asText(), it.value.textValue()))
                .collect(toList())
        );
    }
}