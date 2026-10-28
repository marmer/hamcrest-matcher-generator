// The build must fail for the scope violation, not for something else.
def log = new File(basedir, 'build.log').text
assert log.contains("can't be called in this context by implicit receiver") : "Another failure: ${log}"
