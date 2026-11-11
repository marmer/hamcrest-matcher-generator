# frozen_string_literal: true

# Names the Structurizr home for asciidoctor-diagram (#382).
#
# The structurizr converter reads DIAGRAM_STRUCTURIZRCLI_HOME once, when asciidoctor-diagram is required, and
# looks it up in the JRuby ENV first. ENV is this Asciidoctor runtime's own map: setting it touches no JVM system
# property, so no concurrent plugin execution (surefire and failsafe restore a snapshot of the system properties)
# can remove it. The Maven build requires this file before 'asciidoctor-diagram' (docs/pom.xml).
#
# The home is the lib/ folder that the structurizr-home execution of the docs pom fills with the Structurizr jars:
# docs/target/structurizr. This file sits in docs/src/docs/extensions, so the path is relative to it: it works from
# any working directory and in any absolute path, spaces included. Keep it in step with the structurizr.home
# property of docs/pom.xml.
ENV['DIAGRAM_STRUCTURIZRCLI_HOME'] = File.expand_path('../../../target/structurizr', __dir__)
