// PROTOTYPE. K4: generic Kotlin layer, no per-type code. Getter/property references instead of generated
// methods; reuses the 6.0.0 runtime (PropertyMatcher) for paths and messages, but not the generated matchers.
package proto.dsl.k4

import java.beans.Introspector
import java.util.function.Function
import kotlin.reflect.KCallable
import kotlin.reflect.KFunction1
import kotlin.reflect.KProperty1
import org.hamcrest.Matcher
import org.hamcrest.Matchers.equalTo
import proto.hamcrest.PropertyMatcher

@DslMarker
annotation class PropsDsl

inline fun <reified T : Any> matches(noinline block: Props<T>.() -> Unit): Props<T> = Props(T::class.java).apply(block)

@PropsDsl
class Props<T : Any>(private val type: Class<T>) : PropertyMatcher<T, Props<T>>(type) {

    // Java POJOs and records need getter references (Person::getName, Address::city): references to Java
    // synthetic properties (Person::name) are still unsupported/experimental in Kotlin 2.2.
    // Kotlin classes give KProperty1 (Person::name). Both are KCallable + (T) -> V, hence the overload pairs.

    @PublishedApi
    internal fun addCheck(callable: KCallable<*>, getter: (T) -> Any?, matcher: Matcher<*>?) {
        val name = propertyName(callable.name)
        property(name, Function { getter(it) })
        if (matcher == null) ignore(name) else check(name, matcher)
    }

    infix fun <V> KProperty1<T, V>.eq(value: V) = addCheck(this, this, equalTo(value))
    infix fun <V> KFunction1<T, V>.eq(value: V) = addCheck(this, this, equalTo(value))

    infix fun <V> KProperty1<T, V>.matches(matcher: Matcher<in V>) = addCheck(this, this, matcher)
    infix fun <V> KFunction1<T, V>.matches(matcher: Matcher<in V>) = addCheck(this, this, matcher)

    /** Nested type: Order::getCustomer has { Customer::getName eq "Ada" }. */
    inline infix fun <reified V : Any> KProperty1<T, V?>.has(noinline block: Props<V>.() -> Unit) =
        addCheck(this, this, proto.dsl.k4.matches<V>(block))
    inline infix fun <reified V : Any> KFunction1<T, V?>.has(noinline block: Props<V>.() -> Unit) =
        addCheck(this, this, proto.dsl.k4.matches<V>(block))

    fun KProperty1<T, *>.ignored() = addCheck(this, this, null)
    fun KFunction1<T, *>.ignored() = addCheck(this, this, null)

    /**
     * The completeness check needs every property of T, which only the generator knows. Here it falls back to
     * java.beans introspection: that finds getX/isX getters, but not record components or Kotlin isX: Boolean?,
     * i.e. it would duplicate (and drift from) the processor's property rules at runtime.
     */
    override fun checkingAllProperties(): Props<T> {
        Introspector.getBeanInfo(type, Any::class.java).propertyDescriptors
            .forEach { d -> property(d.name, Function { d.readMethod.invoke(it) }) }
        return super.checkingAllProperties()
    }
}

/** getName -> name, isActive -> active, city (record component) -> city. */
@PublishedApi
internal fun propertyName(callable: String): String {
    val prefix = listOf("get", "is").firstOrNull { callable.startsWith(it) && callable.length > it.length && callable[it.length].isUpperCase() }
    return if (prefix == null) callable else callable.substring(prefix.length).replaceFirstChar { it.lowercase() }
}
