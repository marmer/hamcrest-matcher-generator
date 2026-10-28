package some.other.pck;

import io.github.marmer.matchergenerator.assertj.AssertionPath;
import java.lang.Class;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.annotation.processing.Generated;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.presentation.Representation;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@Generated(
    value = "io.github.marmer.matchergenerator.core.MatcherGenerationProcessor",
    date = "2020-01-01T00:00"
)
@NullMarked
public abstract class AbstractOrderAssert<SELF extends AbstractOrderAssert<SELF, ACTUAL>, ACTUAL extends Order> extends AbstractObjectAssert<SELF, ACTUAL> {
  private static final List<String> PROPERTY_NAMES = List.of("customer");

  private final String path;

  private final Set<String> addressed = new HashSet<>();

  protected AbstractOrderAssert(final @Nullable ACTUAL actual, final Class<?> selfType) {
    super(actual, selfType);
    this.path = AssertionPath.rootedAt("Order");
  }

  public SELF hasCustomer(final Order.@Nullable Customer value) {
    isNotNull();
    return checkProperty("customer", actual.getCustomer(), value);
  }

  public SELF hasCustomerSatisfying(
      final Consumer<? super Order.@Nullable Customer> requirements) {
    isNotNull();
    addressed.add("customer");
    AssertionPath.within(path + ".customer", actual.getCustomer(), requirements);
    return myself;
  }

  public SELF ignoringCustomer() {
    addressed.add("customer");
    return myself;
  }

  @Override
  public SELF isNotNull() {
    if (actual == null) {
      failWithMessage("%nExpecting %s not to be null", path);
    }
    return myself;
  }

  private SELF checkProperty(final String property, final @Nullable Object actualValue,
      final @Nullable Object expectedValue) {
    addressed.add(property);
    if (!Objects.deepEquals(actualValue, expectedValue)) {
      final Representation representation = getWritableAssertionInfo().representation();
      throw failureWithActualExpected(actualValue, expectedValue, "%nExpecting %s.%s to be:%n  %s%nbut was:%n  %s", path, property, representation.toStringOf(expectedValue), representation.toStringOf(actualValue));
    }
    return myself;
  }

  public SELF satisfiesCheckingAllProperties(final Consumer<? super SELF> requirements) {
    isNotNull();
    final Set<String> chain = new HashSet<>(addressed);
    addressed.clear();
    try {
      requirements.accept(myself);
      return hasAllPropertiesChecked();
    } finally {
      addressed.addAll(chain);
    }
  }

  public SELF hasAllPropertiesChecked() {
    isNotNull();
    final List<String> neither = PROPERTY_NAMES.stream().filter(property -> !addressed.contains(property)).collect(Collectors.toList());
    if (!neither.isEmpty()) {
      failWithMessage("%nExpecting every property of %s to be checked or ignored, but these were neither: %s", path, String.join(", ", neither));
    }
    return myself;
  }

  @Generated(
      value = "io.github.marmer.matchergenerator.core.MatcherGenerationProcessor",
      date = "2020-01-01T00:00"
  )
  public abstract static class AbstractCustomerAssert<SELF extends AbstractCustomerAssert<SELF, ACTUAL>, ACTUAL extends Order.Customer> extends AbstractObjectAssert<SELF, ACTUAL> {
    private static final List<String> PROPERTY_NAMES = List.of("name");

    private final String path;

    private final Set<String> addressed = new HashSet<>();

    protected AbstractCustomerAssert(final @Nullable ACTUAL actual, final Class<?> selfType) {
      super(actual, selfType);
      this.path = AssertionPath.rootedAt("Customer");
    }

    public SELF hasName(final @Nullable String value) {
      isNotNull();
      return checkProperty("name", actual.getName(), value);
    }

    public SELF hasNameSatisfying(final Consumer<? super String> requirements) {
      isNotNull();
      addressed.add("name");
      AssertionPath.within(path + ".name", actual.getName(), requirements);
      return myself;
    }

    public SELF ignoringName() {
      addressed.add("name");
      return myself;
    }

    @Override
    public SELF isNotNull() {
      if (actual == null) {
        failWithMessage("%nExpecting %s not to be null", path);
      }
      return myself;
    }

    private SELF checkProperty(final String property, final @Nullable Object actualValue,
        final @Nullable Object expectedValue) {
      addressed.add(property);
      if (!Objects.deepEquals(actualValue, expectedValue)) {
        final Representation representation = getWritableAssertionInfo().representation();
        throw failureWithActualExpected(actualValue, expectedValue, "%nExpecting %s.%s to be:%n  %s%nbut was:%n  %s", path, property, representation.toStringOf(expectedValue), representation.toStringOf(actualValue));
      }
      return myself;
    }

    public SELF satisfiesCheckingAllProperties(final Consumer<? super SELF> requirements) {
      isNotNull();
      final Set<String> chain = new HashSet<>(addressed);
      addressed.clear();
      try {
        requirements.accept(myself);
        return hasAllPropertiesChecked();
      } finally {
        addressed.addAll(chain);
      }
    }

    public SELF hasAllPropertiesChecked() {
      isNotNull();
      final List<String> neither = PROPERTY_NAMES.stream().filter(property -> !addressed.contains(property)).collect(Collectors.toList());
      if (!neither.isEmpty()) {
        failWithMessage("%nExpecting every property of %s to be checked or ignored, but these were neither: %s", path, String.join(", ", neither));
      }
      return myself;
    }

    @Generated(
        value = "io.github.marmer.matchergenerator.core.MatcherGenerationProcessor",
        date = "2020-01-01T00:00"
    )
    public abstract static class AbstractAddressAssert<SELF extends AbstractAddressAssert<SELF, ACTUAL>, ACTUAL extends Order.Customer.Address> extends AbstractObjectAssert<SELF, ACTUAL> {
      private static final List<String> PROPERTY_NAMES = List.of("city");

      private final String path;

      private final Set<String> addressed = new HashSet<>();

      protected AbstractAddressAssert(final @Nullable ACTUAL actual, final Class<?> selfType) {
        super(actual, selfType);
        this.path = AssertionPath.rootedAt("Address");
      }

      public SELF hasCity(final @Nullable String value) {
        isNotNull();
        return checkProperty("city", actual.getCity(), value);
      }

      public SELF hasCitySatisfying(final Consumer<? super String> requirements) {
        isNotNull();
        addressed.add("city");
        AssertionPath.within(path + ".city", actual.getCity(), requirements);
        return myself;
      }

      public SELF ignoringCity() {
        addressed.add("city");
        return myself;
      }

      @Override
      public SELF isNotNull() {
        if (actual == null) {
          failWithMessage("%nExpecting %s not to be null", path);
        }
        return myself;
      }

      private SELF checkProperty(final String property, final @Nullable Object actualValue,
          final @Nullable Object expectedValue) {
        addressed.add(property);
        if (!Objects.deepEquals(actualValue, expectedValue)) {
          final Representation representation = getWritableAssertionInfo().representation();
          throw failureWithActualExpected(actualValue, expectedValue, "%nExpecting %s.%s to be:%n  %s%nbut was:%n  %s", path, property, representation.toStringOf(expectedValue), representation.toStringOf(actualValue));
        }
        return myself;
      }

      public SELF satisfiesCheckingAllProperties(final Consumer<? super SELF> requirements) {
        isNotNull();
        final Set<String> chain = new HashSet<>(addressed);
        addressed.clear();
        try {
          requirements.accept(myself);
          return hasAllPropertiesChecked();
        } finally {
          addressed.addAll(chain);
        }
      }

      public SELF hasAllPropertiesChecked() {
        isNotNull();
        final List<String> neither = PROPERTY_NAMES.stream().filter(property -> !addressed.contains(property)).collect(Collectors.toList());
        if (!neither.isEmpty()) {
          failWithMessage("%nExpecting every property of %s to be checked or ignored, but these were neither: %s", path, String.join(", ", neither));
        }
        return myself;
      }
    }
  }
}
