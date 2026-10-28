// The build must fail with a type mismatch in Check.kt, not for something else. The wording of the mismatch differs
// between Kotlin versions: a single candidate says "Type mismatch", overloads say "None of the following".
def log = new File(basedir, 'build.log').text
assert log.contains('Check.kt') && (log.contains('Type mismatch') || log.contains('None of the following')) : "Another failure: ${log}"
