workspace "HMG" {
  model {
    dev = person "Developer"
    hmg = softwareSystem "Hamcrest Matcher Generator" {
      ap = container "Annotation Processor" "Java/Kotlin" {
        worker = component "Worker"
        gen = component "MatcherGenerator"
      }
      deps = container "Dependencies" "Java"
    }
    javac = softwareSystem "javac" "Compiler" "External"
    dev -> javac "compiles with"
    javac -> worker "invokes"
    worker -> gen "delegates to"
    ap -> deps "generates code using"
    live = deploymentEnvironment "Build" {
      deploymentNode "Developer machine" {
        deploymentNode "JVM" {
          containerInstance ap
        }
      }
    }
  }
  views {
    systemContext hmg "Context" {
      include *
    }
    container hmg "Containers" {
      include *
    }
    component ap "Components" {
      include *
    }
    dynamic ap "Generation" {
      javac -> worker "process()"
      worker -> gen "generate()"
    }
    deployment hmg live "Deployment" {
      include *
    }
  }
}
