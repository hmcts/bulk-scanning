package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

import joptsimple.internal.Strings;

import java.util.Set;

public class DocumentUrlNotRetrievedException extends RuntimeException {

    private static final long serialVersionUID = -6159204377653345638L;

    public DocumentUrlNotRetrievedException(Set<String> missingFiles) {
        super("Error retrieving urls for uploaded files: " + Strings.join(missingFiles, ", "));
    }
}
