package foo.bar.sample.records.model;

import static foo.bar.sample.records.model.AddressRecordMatcher.isAddressRecord;
import static foo.bar.sample.records.model.CustomerRecordMatcher.isCustomerRecord;
import static foo.bar.sample.records.model.OrderRecordMatcher.isOrderRecord;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MismatchMessageTest {

    @Test
    @DisplayName("A nested mismatch lists the failing checks only, with dot paths")
    void aNestedMismatchListsTheFailingChecksOnlyWithDotPaths() {
        final OrderRecord order = new OrderRecord("ada@example.org",
            new CustomerRecord("Ada", new AddressRecord("Paris")));

        final AssertionError error = assertThrows(AssertionError.class, () -> assertThat(order, isOrderRecord()
            .withEmail(startsWith("bob@"))
            .withCustomer(isCustomerRecord()
                .withName("Ada")
                .withAddress(isAddressRecord().withCity("Berlin")))));

        assertEquals("""

            Expected: OrderRecord with 3 property checks
                 but: email: expected a string starting with "bob@" but was "ada@example.org"
                      customer.address.city: expected "Berlin" but was "Paris"
            """, error.getMessage() + "\n");
    }
}
