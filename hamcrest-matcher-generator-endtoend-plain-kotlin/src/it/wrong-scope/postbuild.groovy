// The build must fail for the scope violation in Check.kt, not for something else. The DslMarker wording is the one
// Kotlin has used since 1.x: the outer scope's function "can't be called in this context by implicit receiver".
def log = new File(basedir, 'build.log').text
assert log.contains('Check.kt') && log.contains('implicit receiver') : "Another failure: ${log}"
