workspace "Hamcrest Matcher Generator" "Generates type-safe test assertions for Java beans and records at compile time." {
    // The architecture model of hamcrest-matcher-generator. The arc42 chapters 3, 5, 6 and 7 embed its views
    // (structurizr blocks); WorkspaceViewsTest fails when a view is not embedded or an embedded diagram has no view.
    // Labels are two to four words; the detail lives in the chapter text. Every view sets its own autoLayout.


    model {
        developer = person "Test developer" "Wants type-safe assertions for beans and records."
        maintainer = person "Maintainer" "Merges the Release PR."

        hmg = softwareSystem "Hamcrest Matcher Generator" "Annotation processor that generates assertion code." {
            annotations = container "matcher-generator-annotations" "The configuration annotations." "Java, Maven module"
            core = container "matcher-generator-core" "Processor, type model and backend SPI. No assertion library." "Java, Maven module" {
                processor = component "MatcherGenerationProcessor" "Annotation processor, registered through META-INF/services." "Java"
                worker = component "MatcherGenerationProcessorWorker" "Resolves configured types, skips generated ones, hands every backend's source to the source writer." "Java"
                diagnostics = component "ProcessorDiagnostics" "Holds the texts of the messages and places them through the Messager; the worker decides which applies." "Java"
                sourceWriter = component "GeneratedSourceWriter" "Owns the Filer, writes each source once: Java through the Filer, Kotlin into the KAPT directory." "Java"
                typeModelFactory = component "TypeModelFactory" "Reads a type through the element API." "Java"
                nullability = component "NullabilityResolver" "Derives the nullability of a property." "Java"
                propertyModel = component "TypeModel, PropertyModel, Nullability" "The property model that backends work on." "Java"
                spi = component "GeneratorBackend" "SPI with GenerationContext and GeneratedSource." "Java interface"
            }
            hamcrestBackend = container "hamcrest-matcher-generator" "The Hamcrest backend and its runtime library." "Java, JavaPoet, Maven module" {
                backend = component "HamcrestGeneratorBackend" "Implements the SPI." "Java"
                matcherGenerator = component "MatcherGenerator" "Builds the source of a <Type>Matcher class." "JavaPoet"
                referencePresetPlanner = component "ReferencePresetPlanner" "Decides which ReferenceMatching helper compares each property of a reference object." "Java"
                runtimeLibrary = component "BeanPropertyMatcher" "Runtime library that generated matchers call at test time." "Java"
            }
            assertjBackend = container "assertj-assertion-generator" "The AssertJ backend and its AssertionPath runtime." "Java, JavaPoet, Maven module"
            kotlinBackend = container "hamcrest-matcher-generator-kotlin" "Opt-in Kotlin DSL backend: a DSL file per type with properties. Runs under KAPT only." "Java, KotlinPoet, Maven module"
            bridge = container "Bridge jars" "Code-free jars for the 5.x coordinates." "Java, Maven modules"
            endtoend = container "End-to-end modules" "Apply the processor and test the generated code." "Java, Kotlin, Maven modules"
            docs = container "docs" "Renders and checks this documentation." "Asciidoctor, Structurizr, Maven module"
        }

        javac = softwareSystem "Java compiler" "javac, or KAPT for Kotlin." "External"
        hamcrest = softwareSystem "Hamcrest" "Assertion library that the generated matchers extend." "External"
        assertj = softwareSystem "AssertJ" "Assertion library that the generated assertions extend." "External"
        central = softwareSystem "Maven Central" "Artifact repository." "External"
        github = softwareSystem "GitHub Actions" "Builds, releases and deploys the documentation." "External"
        pages = softwareSystem "GitHub Pages" "Hosts the documentation." "External"

        developer -> hmg "Configures types with"
        developer -> central "Downloads artifacts from"
        maintainer -> github "Merges Release PR"
        javac -> hmg "Runs as annotation processor"
        hmg -> javac "Writes generated sources"
        hmg -> hamcrest "Generates matchers for"
        hmg -> assertj "Generates assertions for"
        github -> hmg "Builds and releases"
        github -> central "Publishes signed bundle"
        github -> pages "Deploys documentation"
        github -> maintainer "Announces release"

        developer -> annotations "Configures types with"
        javac -> processor "Invokes"
        core -> annotations "Reads configuration from"
        processor -> hamcrestBackend "Loads via ServiceLoader" "ServiceLoader"
        processor -> assertjBackend "Loads via ServiceLoader" "ServiceLoader"
        processor -> kotlinBackend "Loads via ServiceLoader" "ServiceLoader"
        hamcrestBackend -> core "Implements backend SPI"
        kotlinBackend -> core "Implements backend SPI"
        kotlinBackend -> hamcrestBackend "Reuses matcher naming"
        endtoend -> kotlinBackend "Tests Kotlin DSL"
        assertjBackend -> core "Implements backend SPI"
        assertjBackend -> assertj "Extends AbstractObjectAssert"
        endtoend -> assertjBackend "Tests generated assertions"
        hamcrestBackend -> hamcrest "Uses"
        bridge -> hamcrestBackend "Depends on"
        endtoend -> hamcrestBackend "Tests generated matchers"
        endtoend -> annotations "Configures types with"

        processor -> worker "Hands rounds to"
        worker -> typeModelFactory "Asks for model"
        typeModelFactory -> nullability "Asks for nullability"
        nullability -> propertyModel "Feeds nullability to"
        typeModelFactory -> propertyModel "Creates"
        worker -> spi "Calls backends"
        worker -> sourceWriter "Hands sources to"
        worker -> diagnostics "Reports through"
        sourceWriter -> diagnostics "Reports through"
        sourceWriter -> javac "Writes sources via Filer"
        diagnostics -> javac "Reports through Messager"
        backend -> matcherGenerator "Delegates to"
        backend -> spi "Implements"
        matcherGenerator -> runtimeLibrary "Generates code calling"
        matcherGenerator -> propertyModel "Reads"
        matcherGenerator -> referencePresetPlanner "Asks for the preset of every property of a reference object"
        referencePresetPlanner -> propertyModel "Reads"
        runtimeLibrary -> hamcrest "Extends TypeSafeMatcher"

        release = deploymentEnvironment "Release" {
            deploymentNode "GitHub" {
                deploymentNode "GitHub Actions" {
                    releaseWorkflow = infrastructureNode "Release workflow" "release-please, mvnw verify, mvnw -P release deploy" "GitHub Actions"
                    docsWorkflow = infrastructureNode "Docs workflow" "mvnw -pl docs verify, upload-pages-artifact" "GitHub Actions"
                }
                pagesNode = deploymentNode "GitHub Pages" {
                    containerInstance docs
                }
            }
            centralNode = deploymentNode "Maven Central" "Central Portal" {
                containerInstance annotations
                containerInstance core
                containerInstance hamcrestBackend
                containerInstance assertjBackend
                containerInstance kotlinBackend
                containerInstance bridge
            }
            releaseWorkflow -> centralNode "Uploads one bundle" "central-publishing-maven-plugin"
            docsWorkflow -> pagesNode "Deploys to" "deploy-pages"
        }
    }

    views {
        systemContext hmg "Context" {
            include * maintainer central pages
            autoLayout lr 500 1000
        }

        container hmg "Containers" {
            include *
            exclude endtoend docs bridge
            exclude "core -> javac" "hamcrestBackend -> core" "assertjBackend -> core" "kotlinBackend -> core"
            autoLayout lr 500 1000
        }

        container hmg "BuildModules" {
            include endtoend docs bridge annotations hamcrestBackend assertjBackend kotlinBackend
            autoLayout lr 500 1000
        }

        component core "CoreComponents" {
            include *
            exclude "kotlinBackend -> hamcrestBackend" "hamcrestBackend -> propertyModel"
            autoLayout lr 500 1000
        }

        component hamcrestBackend "HamcrestComponents" {
            include *
            autoLayout tb 500 1000
        }

        dynamic core "Generation" "What happens for a round in which the compiler finds the configuration annotation." {
            javac -> processor "Invokes in every round"
            processor -> worker "Hands the round to"
            worker -> typeModelFactory "Asks for model"
            worker -> spi "Calls backends"
            worker -> sourceWriter "Hands sources to"
            sourceWriter -> javac "Writes sources"
            autoLayout lr 500 1000
        }

        dynamic * "Release" "From merging the Release PR to the announcement." {
            maintainer -> github "Merges Release PR"
            github -> hmg "Builds the tag"
            github -> central "Publishes signed bundle"
            github -> pages "Deploys documentation"
            github -> maintainer "Announces release"
            autoLayout lr 500 1000
        }

        deployment hmg release "Deployment" {
            include *
            exclude "core -> annotations" "hamcrestBackend -> core" "assertjBackend -> core" "kotlinBackend -> core" "kotlinBackend -> hamcrestBackend" "bridge -> hamcrestBackend" "core -> hamcrestBackend" "core -> assertjBackend" "core -> kotlinBackend"
            autoLayout tb 500 1000
        }

        styles {
            element "Person" {
                shape Person
                background #08427b
                color #ffffff
            }
            element "Software System" {
                background #1168bd
                color #ffffff
            }
            element "Container" {
                background #438dd5
                color #ffffff
            }
            element "Component" {
                background #85bbf0
                color #000000
            }
            element "Infrastructure Node" {
                background #ffffff
                color #000000
            }
            element "External" {
                background #8a8a8a
                color #ffffff
            }
            relationship "Relationship" {
                width 300
            }
        }
    }
}
