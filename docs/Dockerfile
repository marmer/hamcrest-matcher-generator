FROM asciidoctor/docker-asciidoctor:latest

WORKDIR /documents

CMD ["asciidoctor", \
     "-r", "asciidoctor-diagram", \
     "-a", "data-uri", \
     "-a", "allow-uri-read", \
     "-D", "/documents/build", \
     "-o", "index.html", \
     "/documents/arc42.adoc"]
