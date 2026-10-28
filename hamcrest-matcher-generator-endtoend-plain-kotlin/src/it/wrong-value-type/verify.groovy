// The build must fail for the wrong value type, not for something else.
def log = new File(basedir, 'build.log').text
assert log.contains("fun id(value: Long)") : "Another failure: ${log}"
