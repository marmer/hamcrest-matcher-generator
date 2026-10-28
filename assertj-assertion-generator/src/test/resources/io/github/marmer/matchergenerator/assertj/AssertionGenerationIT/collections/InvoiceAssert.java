package some.other.pck;

import javax.annotation.processing.Generated;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@Generated(
    value = "io.github.marmer.matchergenerator.core.MatcherGenerationProcessor",
    date = "2020-01-01T00:00"
)
@NullMarked
public class InvoiceAssert extends AbstractInvoiceAssert<InvoiceAssert, Invoice> {
  public InvoiceAssert(final @Nullable Invoice actual) {
    super(actual, InvoiceAssert.class);
  }

  public static InvoiceAssert assertThat(final @Nullable Invoice actual) {
    return new InvoiceAssert(actual);
  }
}
