package io.github.marmer.docs;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Checks the HTML the {@code asciidoctor-maven-plugin} rendered before the tests run: internal links, anchors and
 * images must resolve. External links are checked by the weekly lychee workflow, not here.
 */
class RenderedDocsTest {

    private static final Path OUTPUT = Paths.get(System.getProperty("docs.output.directory", "target/generated-docs"));
    private static List<Path> pages;

    @BeforeAll
    static void findRenderedPages() throws IOException {
        try (Stream<Path> files = Files.walk(OUTPUT)) {
            pages = files.filter(file -> file.toString().endsWith(".html")).sorted().collect(Collectors.toList());
        }
    }

    @Test
    void architectureDocumentIsRendered() {
        assertTrue(Files.isRegularFile(OUTPUT.resolve("architecture/index.html")),
            "architecture/index.html is missing in " + OUTPUT);
    }

    @Test
    void chaptersAreNotRenderedAsPagesOfTheirOwn() {
        assertFalse(pages.stream().anyMatch(page -> page.toString().contains("_chapters")),
            "included chapters must not be rendered standalone: " + pages);
    }

    @Test
    void internalLinksAndAnchorsResolve() {
        final List<String> problems = new ArrayList<>();
        for (final Path page : pages) {
            for (final Element link : parse(page).select("a[href]")) {
                checkLink(page, link.attr("href")).ifPresent(problem -> problems.add(OUTPUT.relativize(page) + ": " + problem));
            }
        }
        assertTrue(problems.isEmpty(), "broken internal links:\n" + String.join("\n", problems));
    }

    @Test
    void imagesExistAndAreNotEmpty() {
        final List<String> problems = new ArrayList<>();
        for (final Path page : pages) {
            for (final Element image : parse(page).select("img")) {
                checkImage(page, image.attr("src")).ifPresent(problem -> problems.add(OUTPUT.relativize(page) + ": " + problem));
            }
        }
        assertTrue(problems.isEmpty(), "broken images:\n" + String.join("\n", problems));
    }

    @Test
    void everyPageHasTheTopBarWithItsTabsInOrder() {
        final List<String> problems = new ArrayList<>();
        for (final Path page : pages) {
            final List<String> tabs = parse(page).select("header.site-top nav.site-nav a").eachText();
            if (!List.of("Guide", "Architecture", "Javadoc", "GitHub").equals(tabs)) {
                problems.add(OUTPUT.relativize(page) + ": tabs are " + tabs);
            }
            if (parse(page).select("header.site-top .site-version").text().isEmpty()) {
                problems.add(OUTPUT.relativize(page) + ": brand without version");
            }
        }
        assertTrue(problems.isEmpty(), "top bar problems:\n" + String.join("\n", problems));
    }

    @Test
    void theActiveSectionIsMarkedByThePageKind() {
        assertAll(
            () -> assertTrue(parse(OUTPUT.resolve("index.html")).body().hasClass("page-landing"), "landing"),
            () -> assertTrue(parse(OUTPUT.resolve("guide/getting-started.html")).body().hasClass("page-guide"), "guide"),
            () -> assertTrue(parse(OUTPUT.resolve("architecture/index.html")).body().hasClass("page-arch"), "arc42"));
    }

    @Test
    void architectureSidebarListsEveryChapterAndLinksToItsSection() {
        final Document architecture = parse(OUTPUT.resolve("architecture/index.html"));
        final List<String> chapterIds = architecture.select("#content > .sect1 > h2[id], #content .sect1 > h2[id]").eachAttr("id");
        final List<String> sidebarTargets = architecture.select("nav.site-side-arch ol:not(.site-side-plain) a").eachAttr("href");
        assertAll(
            () -> assertEquals(12, chapterIds.size(), "arc42 has twelve chapters"),
            () -> assertEquals(chapterIds.stream().map(id -> "#" + id).collect(Collectors.toList()), sidebarTargets));
    }

    @Test
    void guideSidebarListsEveryGuidePageAndMarksTheCurrentOne() {
        final long guidePages = pages.stream().filter(page -> page.getParent().endsWith("guide")).count();
        final Document guidePage = parse(OUTPUT.resolve("guide/getting-started.html"));
        final Element current = guidePage.selectFirst("nav.site-side-guide a[aria-current=page]");
        assertAll(
            () -> assertNotNull(current, "current chapter is marked"),
            () -> assertEquals("Getting started", current.text()),
            () -> assertEquals(guidePages, guidePage.select("nav.site-side-guide ol:not(.site-side-plain) li").size(), "every guide page"));
    }

    @Test
    void guidePagesEndWithAPager() {
        final Document guidePage = parse(OUTPUT.resolve("guide/getting-started.html"));
        assertAll(
            () -> assertEquals(1, guidePage.select("#content nav.site-pager").size()),
            () -> assertEquals(2, guidePage.select("#content nav.site-pager a[rel]").size(), "previous and next"));
    }

    @Test
    void landingPageHasTheHeroWithItsCallsToAction() {
        final Document landing = parse(OUTPUT.resolve("index.html"));
        assertAll(
            () -> assertEquals("Annotation processor · Java 17+ · Kotlin · Records", landing.select(".hero .eyebrow").text()),
            () -> assertEquals(List.of("Get started", "How it works"), landing.select(".hero .cta a").eachText()),
            () -> assertEquals(1, landing.select(".pair").size(), "snippet placeholders"),
            () -> assertEquals(3, landing.select(".features li").size(), "features"),
            () -> assertEquals("Install", landing.select("h2").text(), "install section"));
    }

    @Test
    void everyPageHasTheMaintainerFooter() {
        final List<String> problems = new ArrayList<>();
        for (final Path page : pages) {
            final Element footer = parse(page).selectFirst("footer.site-foot");
            if (footer == null) {
                problems.add(OUTPUT.relativize(page) + ": no footer");
                continue;
            }
            for (final String expected : List.of("Mariano Mertinat", "Docs for release", "Apache License 2.0")) {
                if (!footer.text().contains(expected)) {
                    problems.add(OUTPUT.relativize(page) + ": footer lacks '" + expected + "'");
                }
            }
            for (final String link : List.of("LinkedIn", "GitHub", "Changelog")) {
                if (footer.select("a").stream().noneMatch(a -> a.text().equals(link))) {
                    problems.add(OUTPUT.relativize(page) + ": footer lacks the " + link + " link");
                }
            }
            if (footer.select("img.site-avatar[src$=images/maintainer.jpg]").isEmpty()) {
                problems.add(OUTPUT.relativize(page) + ": footer lacks the committed avatar");
            }
        }
        assertTrue(problems.isEmpty(), "footer problems:\n" + String.join("\n", problems));
    }

    @Test
    void stylesheetAndFontsAreLocalAndExist() throws IOException {
        final List<String> problems = new ArrayList<>();
        for (final Path page : pages) {
            final List<Element> stylesheets = parse(page).select("link[rel=stylesheet]");
            if (stylesheets.size() != 1 || isExternal(stylesheets.get(0).attr("href"))) {
                problems.add(OUTPUT.relativize(page) + ": expected exactly the one local site stylesheet, found " + stylesheets);
            } else if (!Files.isRegularFile(resolve(page, stylesheets.get(0).attr("href")))) {
                problems.add(OUTPUT.relativize(page) + ": stylesheet is missing");
            }
        }
        final Path css = OUTPUT.resolve("assets/theme.css");
        final String content = Files.readString(css);
        final Matcher references = Pattern.compile("url\\(\"([^\"]+)\"\\)").matcher(content);
        final List<String> fonts = new ArrayList<>();
        while (references.find()) {
            fonts.add(references.group(1));
            if (isExternal(references.group(1)) || !Files.isRegularFile(resolve(css, references.group(1)))) {
                problems.add("theme.css: '" + references.group(1) + "' is external or missing");
            }
        }
        assertAll(
            () -> assertTrue(problems.isEmpty(), "stylesheet problems:\n" + String.join("\n", problems)),
            () -> assertTrue(fonts.size() >= 4, "Bricolage Grotesque, Public Sans (two) and JetBrains Mono are bundled"));
    }

    @Test
    void noPageContainsAnUnresolvedAttributeReference() {
        final Pattern reference = Pattern.compile("\\{[A-Za-z][\\w-]*}");
        final List<String> problems = new ArrayList<>();
        for (final Path page : pages) {
            final Element body = parse(page).body().clone();
            body.select("pre, code").remove();
            final Matcher matcher = reference.matcher(body.text());
            while (matcher.find()) {
                problems.add(OUTPUT.relativize(page) + ": " + matcher.group());
            }
        }
        assertTrue(problems.isEmpty(), "unresolved attribute references:\n" + String.join("\n", problems));
    }

    @Test
    void linkCheckDetectsBrokenLinks() throws IOException {
        final Path page = OUTPUT.resolve("architecture/index.html");
        assertAll(
            () -> assertTrue(checkLink(page, "does-not-exist.html").isPresent(), "missing file"),
            () -> assertTrue(checkLink(page, "#does-not-exist-anchor").isPresent(), "missing anchor"),
            () -> assertTrue(checkLink(page, "index.html#does-not-exist-anchor").isPresent(), "missing anchor in file"),
            () -> assertFalse(checkLink(page, "https://example.org/").isPresent(), "external links are not checked"),
            () -> assertFalse(checkLink(page, "").isPresent(), "empty href"));
        assertTrue(checkImage(page, "images/does-not-exist.png").isPresent(), "missing image");
        assertTrue(checkImage(page, "").isPresent(), "empty image source");
    }

    private static java.util.Optional<String> checkLink(final Path page, final String href) {
        if (href.isEmpty() || isExternal(href)) {
            return java.util.Optional.empty();
        }
        final URI uri = toUri(href);
        final String path = uri.getPath() == null ? "" : uri.getPath();
        final String fragment = uri.getFragment();
        final Path target = path.isEmpty() ? page : resolve(page, path);
        if (!Files.exists(target)) {
            return java.util.Optional.of("'" + href + "' points to a missing file");
        }
        if (fragment != null && !fragment.isEmpty() && target.toString().endsWith(".html")
            && !hasAnchor(parse(target), fragment)) {
            return java.util.Optional.of("'" + href + "' points to a missing anchor");
        }
        return java.util.Optional.empty();
    }

    private static java.util.Optional<String> checkImage(final Path page, final String src) {
        if (src.isEmpty()) {
            return java.util.Optional.of("image without source");
        }
        if (src.startsWith("data:")) {
            return src.indexOf(',') < 0 || src.indexOf(',') == src.length() - 1
                ? java.util.Optional.of("empty data URI image")
                : java.util.Optional.empty();
        }
        if (isExternal(src)) {
            return java.util.Optional.empty();
        }
        final Path target = resolve(page, toUri(src).getPath());
        try {
            if (!Files.isRegularFile(target)) {
                return java.util.Optional.of("image '" + src + "' is missing");
            }
            if (Files.size(target) == 0) {
                return java.util.Optional.of("image '" + src + "' is empty");
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
        return java.util.Optional.empty();
    }

    private static boolean isExternal(final String reference) {
        return reference.matches("^[a-zA-Z][a-zA-Z0-9+.-]*:.*") || reference.startsWith("//");
    }

    private static URI toUri(final String reference) {
        try {
            return new URI(reference);
        } catch (final URISyntaxException e) {
            throw new IllegalArgumentException("not a valid reference: " + reference, e);
        }
    }

    private static Path resolve(final Path page, final String relativePath) {
        return page.getParent().resolve(URLDecoder.decode(relativePath, StandardCharsets.UTF_8)).normalize();
    }

    private static boolean hasAnchor(final Document document, final String fragment) {
        return document.getElementById(fragment) != null || !document.getElementsByAttributeValue("name", fragment).isEmpty();
    }

    private static Document parse(final Path page) {
        try {
            return Jsoup.parse(page.toFile(), "UTF-8");
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
