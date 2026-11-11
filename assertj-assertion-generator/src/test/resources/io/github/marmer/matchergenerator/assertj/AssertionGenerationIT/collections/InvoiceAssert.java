package some.other.pck;

import javax.annotation.processing.Generated;
import org.assertj.core.api.InstanceOfAssertFactory;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@Generated("io.github.marmer.matchergenerator.core.MatcherGenerationProcessor")
@NullMarked
public class InvoiceAssert extends AbstractInvoiceAssert<InvoiceAssert, Invoice> {
  public static final InstanceOfAssertFactory<Invoice, InvoiceAssert> INVOICE = new InstanceOfAssertFactory<>(Invoice.class, InvoiceAssert::new);

  public InvoiceAssert(final @Nullable Invoice actual) {
    super(actual, InvoiceAssert.class);
  }

  public static InvoiceAssert assertThat(final @Nullable Invoice actual) {
    return new InvoiceAssert(actual);
  }
}
