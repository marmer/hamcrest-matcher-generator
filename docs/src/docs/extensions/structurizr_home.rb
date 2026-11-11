# frozen_string_literal: true

# Names the Structurizr home for asciidoctor-diagram (#382).
#
# The structurizr converter of asciidoctor-diagram 3.2.1 reads DIAGRAM_STRUCTURIZRCLI_HOME once, when
# asciidoctor-diagram is required, from the JRuby ENV first and from the JVM system properties second. ENV is this
# Asciidoctor runtime's own map: setting it touches no JVM system property, so no concurrent plugin execution
# (surefire and failsafe restore a snapshot of the system properties) can remove it. The Maven build requires this
# file before 'asciidoctor-diagram' (docs/pom.xml). A version bump of asciidoctorj-diagram must re-check both
# behaviours (ENV read first, read once at require time).
#
# The home is docs/target/structurizr; the converter reads the Structurizr jars from its lib/ folder, which the
# structurizr-home execution of the docs pom fills. The path derives from this file's location, so it works from any
# working directory and in any absolute path, spaces included. Keep it in step with the structurizr.home property of
# docs/pom.xml. A non-empty value the user set in the environment wins.
if ENV['DIAGRAM_STRUCTURIZRCLI_HOME'].to_s.empty?
  home = File.expand_path('../../../target/structurizr', __dir__)
  raise "Structurizr home #{home}/lib does not exist: the structurizr-home execution of docs/pom.xml has not run" unless File.directory?(File.join(home, 'lib'))

  ENV['DIAGRAM_STRUCTURIZRCLI_HOME'] = home
end
