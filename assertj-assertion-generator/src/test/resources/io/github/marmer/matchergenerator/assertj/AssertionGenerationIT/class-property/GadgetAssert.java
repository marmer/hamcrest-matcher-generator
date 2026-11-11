package some.other.pck;

import javax.annotation.processing.Generated;
import org.assertj.core.api.InstanceOfAssertFactory;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@Generated("io.github.marmer.matchergenerator.core.MatcherGenerationProcessor")
@NullMarked
public class GadgetAssert extends AbstractGadgetAssert<GadgetAssert, Gadget> {
  public static final InstanceOfAssertFactory<Gadget, GadgetAssert> GADGET = new InstanceOfAssertFactory<>(Gadget.class, GadgetAssert::new);

  public GadgetAssert(final @Nullable Gadget actual) {
    super(actual, GadgetAssert.class);
  }

  public static GadgetAssert assertThat(final @Nullable Gadget actual) {
    return new GadgetAssert(actual);
  }
}
