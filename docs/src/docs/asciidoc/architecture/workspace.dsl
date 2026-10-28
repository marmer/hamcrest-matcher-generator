workspace "Hamcrest Matcher Generator" "Generates type-safe test assertions for Java beans and records at compile time." {
    // The architecture model of hamcrest-matcher-generator. The arc42 chapters 3, 5, 6 and 7 embed its views
    // (structurizr blocks); WorkspaceViewsTest fails when a view is not embedded or an embedded diagram has no view.


    model {
        developer = person "Test developer" "Wants type-safe assertions for beans and records."
        maintainer = person "Maintainer" "Merges the Release PR."

        hmg = softwareSystem "Hamcrest Matcher Generator" "Annotation processor that generates assertion code." {
            annotations = container "matcher-generator-annotations" "The configuration annotations." "Java, Maven module"
            core = container "matcher-generator-core" "Processor entry point, type discovery, property model, backend SPI. No assertion library on its classpath." "Java, Maven module" {
                processor = component "MatcherGenerationProcessor" "Annotation processor, registered through META-INF/services." "Java"
                worker = component "MatcherGenerationProcessorWorker" "Resolves configured types, skips generated ones, writes every backend's source." "Java"
                typeModelFactory = component "TypeModelFactory" "Reads a type through the element API." "Java"
                propertyModel = component "TypeModel, PropertyModel, Nullability" "The property model that backends work on." "Java"
                spi = component "GeneratorBackend" "SPI with GenerationContext and GeneratedSource." "Java interface"
            }
            hamcrestBackend = container "hamcrest-matcher-generator" "The Hamcrest backend and its runtime library." "Java, Maven module" {
                backend = component "HamcrestGeneratorBackend" "Implements the SPI." "Java"
                matcherGenerator = component "MatcherGenerator" "Builds the source of a <Type>Matcher class." "JavaPoet"
                runtimeLibrary = component "BeanPropertyMatcher" "Runtime library that generated matchers call at test time." "Java"
            }
            assertjBackend = container "assertj-assertion-generator" "The AssertJ backend: an AbstractXAssert and an XAssert per configured type, and the small AssertionPath runtime." "Java, Maven module"
            bridge = container "Bridge jars" "hamcrest-matcher-generator-dependencies and -annotationprocessor are code-free jars that bring 5.x users to the new coordinates." "Java, Maven modules"
            endtoend = container "End-to-end modules" "Apply the processor to real beans and records and exercise the generated matchers." "Java, Kotlin, Maven modules"
            docs = container "docs" "Renders this documentation and checks it. Needs JDK 21 or later." "Asciidoctor, Structurizr, Maven module"
        }

        javac = softwareSystem "Java compiler" "javac, or KAPT for Kotlin." "External"
        hamcrest = softwareSystem "Hamcrest" "Assertion library that the generated matchers extend." "External"
        assertj = softwareSystem "AssertJ" "Assertion library that the generated assertions extend." "External"
        central = softwareSystem "Maven Central" "Artifact repository." "External"
        github = softwareSystem "GitHub Actions" "Builds, releases and deploys the documentation." "External"
        pages = softwareSystem "GitHub Pages" "Hosts the documentation." "External"

        developer -> hmg "Configures types with @GenerateMatchers"
        developer -> central "Takes the artifacts from"
        maintainer -> github "Merges the Release PR in"
        javac -> hmg "Runs as annotation processor in every round"
        hmg -> javac "Writes generated sources through"
        hmg -> hamcrest "Generates matchers for"
        hmg -> assertj "Generates assertions for"
        github -> hmg "Builds and releases"
        github -> central "Publishes the signed bundle to"
        github -> pages "Deploys the documentation to"
        github -> maintainer "Announces the release to"

        developer -> annotations "Annotates a configuration class with"
        javac -> processor "Invokes"
        core -> annotations "Reads the configuration from"
        worker -> hamcrestBackend "Loads every backend at run time through ServiceLoader, without a compile dependency" "ServiceLoader"
        worker -> assertjBackend "Loads every backend at run time through ServiceLoader, without a compile dependency" "ServiceLoader"
        hamcrestBackend -> core "Implements GeneratorBackend of"
        assertjBackend -> core "Implements GeneratorBackend of"
        assertjBackend -> assertj "Generates classes that extend AbstractObjectAssert of"
        endtoend -> assertjBackend "Apply the processor to beans and records"
        hamcrestBackend -> hamcrest "Uses"
        bridge -> hamcrestBackend "Depends on"
        endtoend -> hamcrestBackend "Apply the processor to beans and records"
        endtoend -> annotations "Configure types with"

        processor -> worker "Hands every round to"
        worker -> typeModelFactory "Asks for the model of a type"
        typeModelFactory -> propertyModel "Creates"
        worker -> spi "Calls every backend through"
        worker -> javac "Writes sources through the Filer of"
        backend -> matcherGenerator "Delegates to"
        backend -> spi "Implements"
        matcherGenerator -> runtimeLibrary "Generates code that calls"
        matcherGenerator -> propertyModel "Reads"
        runtimeLibrary -> hamcrest "Extends TypeSafeMatcher of"

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
                containerInstance bridge
            }
            releaseWorkflow -> centralNode "Uploads one bundle to" "central-publishing-maven-plugin"
            docsWorkflow -> pagesNode "Deploys to" "deploy-pages"
        }
    }

    views {
        systemContext hmg "Context" {
            include *
        }

        container hmg "Containers" {
            include *
        }

        component core "CoreComponents" {
            include *
        }

        component hamcrestBackend "HamcrestComponents" {
            include *
        }

        dynamic core "Generation" "What happens for a round in which the compiler finds the configuration annotation." {
            javac -> processor "Invokes in every round"
            processor -> worker "Hands the round to"
            worker -> typeModelFactory "Asks for the model of every configured type"
            worker -> hamcrestBackend "Calls every backend with the model"
            worker -> javac "Writes the returned sources"
        }

        dynamic * "Release" "From merging the Release PR to the announcement." {
            maintainer -> github "Merges the Release PR in"
            github -> hmg "Builds the tag"
            github -> central "Publishes the signed bundle to"
            github -> pages "Deploys the documentation to"
            github -> maintainer "Announces the release to"
        }

        deployment hmg release "Deployment" {
            include *
        }

        styles {
            element "Person" {
                shape Person
            }
            element "External" {
                background #8a8a8a
                color #ffffff
            }
        }
    }
}
