package io.github.marmer.matchergenerator.hamcrest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Value;
import org.junit.jupiter.api.Test;

class PropertyDescriptionTest {
    private static final Item ITEM_1 = new Item("A-1");
    private static final Item ITEM_2 = new Item("B-2");

    static {
        // a generated type is known to print from its properties once its matcher exists
        new BeanPropertyMatcher<>(Item.class, "sku");
    }

    @Test
    void testDescribeValue_Null_ShouldPrintNull() {
        assertEquals("null", PropertyDescription.describeValue(null));
    }

    @Test
    void testDescribeValue_ValueOfAGeneratedType_ShouldPrintFromItsProperties() {
        assertEquals("Item {sku \"A-1\"}", PropertyDescription.describeValue(ITEM_1));
    }

    @Test
    void testDescribeValue_PlainValuesAndContainersWithoutGeneratedTypes_ShouldPrintLikeHamcrest() {
        assertEquals("<[a]>", PropertyDescription.describeValue(List.of("a")));
        assertEquals("<{a=b}>", PropertyDescription.describeValue(Map.of("a", "b")));
        assertEquals("<a=b>", PropertyDescription.describeValue(Map.entry("a", "b")));
        assertEquals("<42>", PropertyDescription.describeValue(42));
    }

    @Test
    void testDescribeValue_CollectionOfGeneratedTypes_ShouldPrintEachFromItsProperties() {
        assertEquals("[Item {sku \"A-1\"}, Item {sku \"B-2\"}]",
            PropertyDescription.describeValue(List.of(ITEM_1, ITEM_2)));
    }

    @Test
    void testDescribeValue_CollectionWithNullAndGeneratedType_ShouldPrintTheNull() {
        assertEquals("[null, Item {sku \"A-1\"}]", PropertyDescription.describeValue(Arrays.asList(null, ITEM_1)));
    }

    @Test
    void testDescribeValue_MapWithGeneratedValues_ShouldPrintEntriesFromTheirProperties() {
        final Map<String, Item> map = new LinkedHashMap<>();
        map.put("first", ITEM_1);
        map.put("second", ITEM_2);

        assertEquals("{first=Item {sku \"A-1\"}, second=Item {sku \"B-2\"}}", PropertyDescription.describeValue(map));
    }

    @Test
    void testDescribeValue_MapWithGeneratedKey_ShouldPrintTheKeyFromItsProperties() {
        assertEquals("{Item {sku \"A-1\"}=plain}", PropertyDescription.describeValue(Map.of(ITEM_1, "plain")));
    }

    @Test
    void testDescribeValue_EntryWithGeneratedValue_ShouldPrintFromItsProperties() {
        assertEquals("<key=Item {sku \"A-1\"}>", PropertyDescription.describeValue(Map.entry("key", ITEM_1)));
    }

    @Test
    void testDescribeValue_EntryWithGeneratedKey_ShouldPrintFromItsProperties() {
        assertEquals("<Item {sku \"A-1\"}=value>", PropertyDescription.describeValue(Map.entry(ITEM_1, "value")));
    }

    @Test
    void testDescribeValue_CollectionOfEntriesWithGeneratedTypes_ShouldPrintTheEntryParts() {
        assertEquals("[key=Item {sku \"A-1\"}]", PropertyDescription.describeValue(List.of(Map.entry("key", ITEM_1))));
    }

    @Test
    void testDescribeValue_MapInsideACollection_ShouldPrintItsGeneratedTypes() {
        assertEquals("[{key=Item {sku \"A-1\"}}]", PropertyDescription.describeValue(List.of(Map.of("key", ITEM_1))));
    }

    @Test
    void testDescribeValue_NestedDeeperThanTheLimit_ShouldPrintTheInnerValuesLikeHamcrest() {
        final Object tooDeep = List.of(List.of(List.of(List.of(List.of(List.of(ITEM_1))))));

        assertEquals("[[[[[<[PropertyDescriptionTest.Item(sku=A-1)]>]]]]]", PropertyDescription.describeValue(tooDeep));
    }

    @Test
    void testDescribeValue_GeneratedTypeWithPropertyTheObjectLacks_ShouldSkipThatProperty() {
        new BeanPropertyMatcher<>(Sparse.class, "ghost", "id");

        assertEquals("Sparse {id \"x\"}", PropertyDescription.describeValue(new Sparse("x")));
    }

    @Test
    void testRegisterGeneratedType_PropertiesEveryObjectHas_ShouldNotBeRegistered() {
        PropertyAccess.registerGeneratedType(Registered.class, List.of("id", "toString", "hashCode", "class"));

        assertEquals(List.of("id"), PropertyAccess.propertiesOfGeneratedType(Registered.class));
    }

    @Test
    void testRegisterGeneratedType_SecondRegistration_ShouldKeepTheFirst() {
        PropertyAccess.registerGeneratedType(Twice.class, List.of("id"));
        final List<String> first = PropertyAccess.propertiesOfGeneratedType(Twice.class);

        PropertyAccess.registerGeneratedType(Twice.class, List.of("id", "other"));

        assertSame(first, PropertyAccess.propertiesOfGeneratedType(Twice.class));
    }

    @Test
    void testRead_PropertyWithoutReadMethod_ShouldBeNoSuchProperty() {
        assertSame(PropertyAccess.NO_SUCH_PROPERTY, PropertyAccess.read(new WriteOnly(), "only"));
    }

    @Value
    public static class Item {
        String sku;
    }

    @Value
    public static class Sparse {
        String id;
    }

    public static class Registered {
    }

    public static class Twice {
    }

    public static class WriteOnly {
        @SuppressWarnings("unused")
        public void setOnly(final String only) {
        }
    }
}
