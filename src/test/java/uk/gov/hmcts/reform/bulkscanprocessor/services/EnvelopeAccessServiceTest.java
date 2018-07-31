package uk.gov.hmcts.reform.bulkscanprocessor.services;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.config.EnvelopeAccessProperties;
import uk.gov.hmcts.reform.bulkscanprocessor.config.EnvelopeAccessProperties.Mapping;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ForbiddenException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ServiceConfigNotFoundException;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RunWith(MockitoJUnitRunner.class)
public class EnvelopeAccessServiceTest {

    @Mock
    private EnvelopeAccessProperties accessProps;

    private EnvelopeAccessService service;

    @Before
    public void setUp() throws Exception {
        this.service = new EnvelopeAccessService(accessProps);

        BDDMockito
            .given(accessProps.getMappings())
            .willReturn(asList(
                new Mapping("jur_A", "read_A", "update_A"),
                new Mapping("jur_B", "read_B", "update_B")
            ));
    }

    @Test
    public void assertCanUpdate_should_throw_an_exception_if_service_is_not_allowed_to_update_in_given_jurisdiction() {

        assertThatThrownBy(() -> service.assertCanUpdate("jur_A", "update_B"))
            .isNotNull()
            .isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("jur_A")
            .hasMessageContaining("update_B");
    }

    @Test
    public void assertCanUpdate_should_throw_an_exception_if_there_is_not_configuration_for_given_jurisdiction() {

        assertThatThrownBy(() -> service.assertCanUpdate("nonExistingJurisdiction", "update_A"))
            .isNotNull()
            .isInstanceOf(ServiceConfigNotFoundException.class)
            .hasMessageContaining("nonExistingJurisdiction");
    }

    @Test
    public void assertCanUpdate_should_not_throw_an_exception_if_service_can_update_envelopes_in_given_jurisdiction() {
        assertThatCode(() -> service.assertCanUpdate("jur_A", "update_A"))
            .doesNotThrowAnyException();
    }
}