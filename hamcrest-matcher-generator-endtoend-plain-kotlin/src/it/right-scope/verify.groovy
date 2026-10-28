// The control must really compile the check, not succeed because it found no sources.
assert new File(basedir, 'target/classes/CheckKt.class').exists() : "The check was not compiled"
