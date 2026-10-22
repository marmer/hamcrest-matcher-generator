# Discoverability of small Java libraries

Research for issue #58 (part of map #52). Date: 2026-10-22.

**Question:** What makes a small Java testing library findable in 2026? Which measures are a one-time effort and which need ongoing care? The project should run on autopilot after 6.0.0 with little owner effort.

**Short answer:** People find a library like this through a few places: the Maven Central search page, GitHub search and topics, Google, and AI assistants. All of them read the same few things: the POM `name`/`description`, the GitHub description and topics, the README, and a crawlable docs site that other pages link to. Almost all of it is a one-time setup. The only recurring work is cutting releases, which keeps the "last published" date fresh, and keeping version numbers in the README and docs current. That can be automated. llms.txt does nothing for Google Search. Its main value is for coding agents, and it costs little.

## 1. Facts (with sources)

### Maven Central (central.sonatype.com)

- Central requires `name`, `description`, `url`, `licenses`, `developers` and `scm` in the POM. Non-`pom` artifacts also need `-sources.jar`, `-javadoc.jar`, checksums and GPG signatures. Source: https://central.sonatype.org/publish/requirements/
- For this artifact, the central.sonatype.com page currently shows: the POM description ("A tool to generate hamcrest matchers based on any model classes (or other classes with property getters)"), the license (Apache 2.0), a "Project URL" and "Source Control" link (both point to the GitHub repo), version 5.1.0, and "Age: 3 years ago". Observed at https://central.sonatype.com/artifact/io.github.marmer.testutils/hamcrest-matcher-generator on 2026-10-22.
- OSSRH reached end-of-life on 2025-06-30. Publishing now goes through the Central Publisher Portal. Sources: https://central.sonatype.org/news/20250326_ossrh_sunset/ and https://central.sonatype.org/publish/publish-portal-guide/
- Repo state: the root `pom.xml` still points `distributionManagement` at `oss.sonatype.org` and has a Travis `ciManagement` URL. The license URL uses `http://`.

### javadoc.io

- javadoc.io serves Javadoc for open-source artifacts on Maven Central that ship a `-javadoc.jar`, and generates badges. Source: https://javadoc.io/
- Observed: `https://javadoc.io/doc/io.github.marmer.testutils/hamcrest-matcher-generator-dependencies` redirects (303) to a versions page, so the artifact is already known there. There is nothing to host yourself. Central already requires the javadoc jar, so this comes free with every release.

### GitHub repository

- Topics let people "explore repositories in a particular subject area … and discover new solutions to a specific problem". A repo can have up to 20 topics, each lowercase letters, digits and hyphens, 50 characters or less. Source: https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/classifying-your-repository-with-topics
- Social preview image: PNG/JPG/GIF under 1 MB, at least 640x320, 1280x640 recommended. Without one, shared links show only basic repo info and the owner's avatar. Source: https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/customizing-your-repositorys-social-media-preview
- GitHub says a README covers what the project does, why it is useful, how to get started, where to get help, and who maintains it. It "should only contain information necessary for developers to get started"; longer docs belong elsewhere. Anything past 500 KiB is truncated. Source: https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/about-readmes
- Repo state on 2026-10-22: **no topics**, **no homepage URL**, 0 GitHub Releases. Description: "A library to generate tools to support faster, more robust and more readable tests." It contains neither "Hamcrest" nor "AssertJ" nor "annotation processor".

### Docs site on GitHub Pages and search engines

- Project sites live at `https://<owner>.github.io/<repo>`. Source: https://docs.github.com/en/pages/getting-started-with-github-pages/about-github-pages
- "Google primarily finds pages through links from other pages it already crawled." A sitemap is optional. Google's advice is to first make sure people know about the site. Titles should be unique and descriptive. Meta descriptions may be used as the snippet. Source: https://developers.google.com/search/docs/fundamentals/seo-starter-guide
- A sitemap is usually unnecessary for sites of "about 500 pages or fewer" that are "comprehensively linked internally". It does help a site that "is new and has few external links to it". Source: https://developers.google.com/search/docs/crawling-indexing/sitemaps/overview
- In Asciidoctor, the `:description:` and `:keywords:` document attributes become `<meta>` elements in the HTML head. Source: https://docs.asciidoctor.org/asciidoc/latest/document/metadata/
- Docs site state on 2026-10-22: `https://marmer.github.io/hamcrest-matcher-generator/` has `<title>hamcrest-matcher-generator</title>`, no meta description, and no `sitemap.xml` or `robots.txt` (both return 404). The content is an arc42 architecture document, not a user guide.

### AI assistants and llms.txt

- llms.txt is a **proposal**, not a standard. It is a Markdown file at `/llms.txt` (or under a subpath such as `/docs/llms.txt`). The only required element is an H1 with the project name. It can add a blockquote summary and H2 lists of links, and an `## Optional` section by convention. Pages can offer `.md` copies, for example `index.html.md`. Source: https://llmstxt.org/
- Google: "Google Search itself doesn't use them … Doing so will neither harm nor help your site's visibility or rankings in Google Search, as Google Search ignores them." What helps is unique, useful content that is crawlable. Source: https://developers.google.com/search/docs/fundamentals/ai-optimization-guide
- Google says sites need no special AI text files or markup to appear in AI Overviews or AI Mode. Source: https://developers.google.com/search/docs/appearance/ai-features
- Secondary sources (not primary) report that Lighthouse 13.3 added an llms.txt audit in an "Agentic Browsing" category, so Google's tooling and Google's search guidance currently disagree. Source: https://www.searchenginejournal.com/googles-llms-txt-guidance-depends-on-which-product-you-ask (treat as unverified)

### Awesome lists (awesome-java)

- awesome-java sets **no star, age or activity threshold**. An entry must meet at least one of these: widely recommended, highly discussed, "unique in its approach and function", or "a niche product that fills a gap". Java must be a primary target. The license must be clearly documented and the docs must be in English. Entries use the format `[LIBRARY](LINK) - DESCRIPTION.` and are sorted A-Z. Descriptions must say what sets the entry apart from overlapping ones. The rules also say "Self-promotion is discouraged." Source: https://github.com/akullpp/awesome-java/blob/master/CONTRIBUTING.md

## 2. Recommendations (opinion, based on the facts above)

1. **Keywords people search for.** A user searches "hamcrest matcher generator", "assertj generator", "annotation processor assertions", or "generate matchers for records/lombok". Put those words in the places that get indexed: the POM `description`, the GitHub description, topics, the README's first paragraph, and the docs site `<title>`/`:description:`. Today the GitHub description has none of them.
2. **Fix the POM once (in the 6.0.0 work).** Write a description that names Hamcrest *and* AssertJ, the annotation processor, and records/Lombok/Kotlin. Set `url` to the docs site, or keep GitHub and add the docs link to the README. Use an `https` license URL. Remove the Travis `ciManagement` (or point it at GitHub Actions) and the OSSRH `distributionManagement`. Every module inherits these values, so set them in the parent POM. Each module's `name`/`description` shows on its own Central page.
3. **GitHub settings once.** Add topics, for example `java`, `testing`, `hamcrest`, `assertj`, `annotation-processor`, `code-generation`, `junit`, `kotlin`, `lombok`, `java-records`, `test-automation`, `matchers`. Set the homepage to the Pages URL. Upload a 1280x640 social preview. Publish GitHub Releases with notes, which can be automated from the release workflow.
4. **README structure.** Use this order: one-sentence pitch, a 5-line before/after code example, the Maven/Gradle dependency with the current version, a link to the docs and Javadoc, the compatibility matrix (JDK / Hamcrest / AssertJ / Kotlin), and the license. Move the long reference and the six-badge Sonar block lower down or into the docs.
5. **Docs site.** The arc42 document is for maintainers. Searchers need a user guide page (getting started, examples, configuration) as the landing page or one level below it, with a descriptive title and meta description. A sitemap is optional at this size. Adding one through the docs build is still cheap and helps a new site with few inbound links. Register the site in Google Search Console once if the owner wants indexing data. That step is optional.
6. **llms.txt: cheap, low value, optional.** Google ignores it. It may help coding agents. If it is added, generate it at build time from the README/user guide (an H1, a summary, and links to the guide, Javadoc and Central) so it never needs manual care. The bigger win for AI assistants is the same as for Google: clear, crawlable examples with the exact artifact coordinates.
7. **Inbound links matter most.** Google finds pages through links. One-time actions that create links: a javadoc.io badge, linking the docs from the README and POM, an awesome-java PR, and possibly a short blog post or Stack Overflow answers where they genuinely fit. For the awesome-java PR, the niche "fills a gap" criterion fits. Expect a self-submitted entry to be judged strictly, so give it a neutral one-line description that states the difference from AssertJ's own assertions generator.

## 3. One-time vs. ongoing

| Measure | Effort | Ongoing care |
|---|---|---|
| POM name/description/url/scm/licenses cleanup | One-time | None (inherited) |
| GitHub description, topics, homepage | One-time | None |
| Social preview image | One-time | Only on rebrand |
| README restructure | One-time | Version numbers per release (automate with a release-time replace or a Central badge) |
| User-guide page + meta description on Pages | One-time | Update when the API changes |
| Sitemap / llms.txt | One-time if generated by the docs build | None if generated |
| javadoc.io | Free with every Central release | None |
| GitHub Releases | Per release | Automate in the release workflow |
| awesome-java PR | One-time | None (an entry can be removed if the project looks dead) |
| Search Console | One-time (optional) | Optional checks |
| Activity signals (recent release, Dependabot merges) | Ongoing | Low if dependency bumps auto-merge and release |

## 4. Prioritised checklist for this repo

1. [ ] Set the GitHub description (mention Hamcrest + AssertJ + annotation processor), the homepage (Pages URL), and 10-15 topics. About 5 minutes, settings only.
2. [ ] Fix the parent POM metadata in 6.0.0: description, https license URL, remove Travis `ciManagement` and OSSRH `distributionManagement`, and give each module a meaningful `name`/`description`.
3. [ ] Restructure the README: pitch, example, dependency snippet, links (docs, Javadoc, Central), compatibility; add a javadoc.io badge.
4. [ ] Add a user-guide/landing page to the Pages site, with `:description:`/`:keywords:` and a descriptive title, and keep arc42 as a sub-page.
5. [ ] Automate GitHub Releases (with notes) in the release workflow, and automate README version bumps.
6. [ ] Upload a 1280x640 social preview image.
7. [ ] Optional: generate `sitemap.xml` and `llms.txt` in the docs build.
8. [ ] Optional: submit an awesome-java PR after 6.0.0 ships with the AssertJ support (the unique-approach/niche criterion).
9. [ ] Optional: verify the Pages site in Google Search Console.
