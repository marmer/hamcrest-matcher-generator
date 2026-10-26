# frozen_string_literal: true

# Site layout for the project documentation (design: issue #68, variant D).
#
# Wraps every rendered page in the site chrome without touching page content:
#   * head docinfo:   stylesheet link and theme color
#   * header docinfo: sticky top bar (Guide, Architecture, Javadoc, GitHub) and the generated chapter sidebar
#   * footer docinfo: maintainer card and release information
#   * treeprocessor:  previous/next pager at the end of every guide page
#   * postprocessor:  page kind class on <body> (page-landing, page-guide or page-arch)
#
# The page kind follows the directory of the source file: guide/ is the user guide, architecture/ is arc42, the
# source root is the landing page. The sidebar is generated, never written by hand:
#   * guide pages are the guide/*.adoc files in the order of their ':page-order:' header attribute (then file name),
#     titled by their document title
#   * arc42 chapters are the level 1 sections of the architecture page
# Required attributes (set by the Maven build): site-source-dir, project-version.

require 'asciidoctor/extensions'
require 'cgi'
require 'pathname'

module SiteLayout
  REPOSITORY_URL = 'https://github.com/marmer/hamcrest-matcher-generator'
  JAVADOC_URL = 'https://javadoc.io/doc/io.github.marmer.testutils'
  CHANGELOG_URL = "#{REPOSITORY_URL}/blob/development/CHANGELOG.md"
  LINKEDIN_URL = 'https://de.linkedin.com/in/mariano-mertinat'
  MAINTAINER_URL = 'https://github.com/marmer'
  PAGE_KINDS = { 'guide' => 'guide', 'architecture' => 'arch' }.freeze

  GuidePage = Struct.new(:file, :title, :order)

  # Where a page sits in the site, derived from the location of its source file.
  class Page
    attr_reader :kind, :relroot

    def initialize(document)
      @source_dir = Pathname.new(document.attr('site-source-dir')).cleanpath
      relative = Pathname.new(document.attr('docfile')).relative_path_from(@source_dir)
      directories = relative.dirname.each_filename.reject { |name| name == '.' }
      @kind = PAGE_KINDS.fetch(directories.first, 'landing')
      @relroot = '../' * directories.size
      @file = relative.basename('.adoc').to_s
    end

    def guide_pages
      @guide_pages ||= Dir.glob(File.join(@source_dir, 'guide', '[!_]*.adoc')).map { |path| read_guide_page(path) }
                          .sort_by { |page| [page.order, page.file] }
    end

    def current_guide_page?(guide_page)
      @kind == 'guide' && guide_page.file == @file
    end

    def guide_url
      guide_pages.empty? ? "#{@relroot}index.html" : "#{@relroot}guide/#{guide_pages.first.file}.html"
    end

    def guide_neighbours
      index = guide_pages.index { |page| current_guide_page?(page) }
      index ? [index.positive? ? guide_pages[index - 1] : nil, guide_pages[index + 1]] : [nil, nil]
    end

    private

    def read_guide_page(path)
      header = File.readlines(path, encoding: 'UTF-8').take_while { |line| !line.strip.empty? }
      title = header.map { |line| line[/^=\s+(.+?)\s*$/, 1] }.compact.first
      order = header.map { |line| line[/^:page-order:\s*(\d+)/, 1] }.compact.first
      GuidePage.new(File.basename(path, '.adoc'), title || File.basename(path, '.adoc'), (order || 1000).to_i)
    end
  end

  module Html
    module_function

    def h(text)
      CGI.escapeHTML(text.to_s)
    end

    def head(page)
      <<~HTML
        <link rel="stylesheet" href="#{page.relroot}assets/theme.css">
        <meta name="theme-color" content="#1d2a4d">
      HTML
    end

    def top_bar(page, version)
      <<~HTML
        <header class="site-top">
          <div class="site-top-row">
            <a class="site-brand" href="#{page.relroot}index.html"><span class="site-mark" aria-hidden="true">&#8776;</span> matcher-generator</a>
            <span class="site-version">v#{h(version)}</span>
          </div>
          <nav class="site-nav" aria-label="Site">
            <a class="site-nav-guide" href="#{page.guide_url}">Guide</a>
            <a class="site-nav-arch" href="#{page.relroot}architecture/index.html">Architecture</a>
            <a href="#{JAVADOC_URL}">Javadoc</a>
            <a href="#{REPOSITORY_URL}">GitHub</a>
          </nav>
        </header>
      HTML
    end

    def guide_sidebar(page)
      items = page.guide_pages.map do |guide_page|
        current = page.current_guide_page?(guide_page)
        %(<li><a href="#{page.relroot}guide/#{guide_page.file}.html"#{' aria-current="page"' if current}>#{h(guide_page.title)}</a></li>)
      end
      sidebar('site-side-guide', 'Guide chapters', 'User guide', items,
              %(<li><a href="#{page.relroot}architecture/index.html">Architecture (arc42)</a></li>))
    end

    def arch_sidebar(page, document)
      items = document.sections.map do |section|
        %(<li><a href="##{h(section.id)}">#{h(section.title)}</a></li>)
      end
      sidebar('site-side-arch', 'Architecture chapters', 'Architecture &middot; arc42', items,
              %(<li><a href="#{page.guide_url}">User guide</a></li>))
    end

    def sidebar(css_class, label, heading, items, elsewhere)
      <<~HTML
        <nav class="site-side #{css_class}" aria-label="#{label}">
          <p class="site-side-heading">#{heading}</p>
          <ol>#{items.join}</ol>
          <p class="site-side-heading">Elsewhere</p>
          <ol class="site-side-plain">#{elsewhere}</ol>
        </nav>
      HTML
    end

    def footer(page, version)
      <<~HTML
        <footer class="site-foot">
          <div class="site-foot-inner">
            <div class="site-maintainer">
              <img class="site-avatar" src="#{page.relroot}images/maintainer.jpg" alt="Mariano Mertinat" width="56" height="56">
              <div>
                <p class="site-foot-kicker">Built and maintained by</p>
                <p class="site-foot-name">Mariano Mertinat</p>
                <p class="site-foot-links"><a href="#{LINKEDIN_URL}">LinkedIn</a> <a href="#{MAINTAINER_URL}">GitHub</a></p>
              </div>
            </div>
            <div class="site-foot-meta">
              <p>Docs for release #{h(version)}</p>
              <p><a href="#{CHANGELOG_URL}">Changelog</a> &middot; Apache License 2.0</p>
            </div>
          </div>
        </footer>
      HTML
    end

    def pager(page)
      previous_page, next_page = page.guide_neighbours
      previous_link = if previous_page
                        %(<a rel="prev" href="#{previous_page.file}.html">&larr; #{h(previous_page.title)}</a>)
                      else
                        %(<a rel="prev" href="#{page.relroot}index.html">&larr; Home</a>)
                      end
      next_link = if next_page
                    %(<a rel="next" href="#{next_page.file}.html">#{h(next_page.title)} &rarr;</a>)
                  else
                    %(<a rel="next" href="#{page.relroot}architecture/index.html">Architecture &rarr;</a>)
                  end
      %(<nav class="site-pager" aria-label="Guide pages">#{previous_link}#{next_link}</nav>)
    end
  end

  class HeadDocinfo < Asciidoctor::Extensions::DocinfoProcessor
    use_dsl
    at_location :head

    def process(document)
      Html.head(Page.new(document))
    end
  end

  class HeaderDocinfo < Asciidoctor::Extensions::DocinfoProcessor
    use_dsl
    at_location :header

    def process(document)
      page = Page.new(document)
      html = Html.top_bar(page, document.attr('project-version'))
      case page.kind
      when 'guide' then html + Html.guide_sidebar(page)
      when 'arch' then html + Html.arch_sidebar(page, document)
      else html
      end
    end
  end

  class FooterDocinfo < Asciidoctor::Extensions::DocinfoProcessor
    use_dsl
    at_location :footer

    def process(document)
      Html.footer(Page.new(document), document.attr('project-version'))
    end
  end

  class GuidePager < Asciidoctor::Extensions::Treeprocessor
    def process(document)
      page = Page.new(document)
      return document unless page.kind == 'guide'

      document.blocks << create_pass_block(document, Html.pager(page), {})
      document
    end
  end

  class PageKindClass < Asciidoctor::Extensions::Postprocessor
    def process(document, output)
      kind = Page.new(document).kind
      output.sub(/<body class="([^"]*)"/) { %(<body class="#{Regexp.last_match(1)} page-#{kind}") }
    end
  end
end

Asciidoctor::Extensions.register do
  docinfo_processor SiteLayout::HeadDocinfo
  docinfo_processor SiteLayout::HeaderDocinfo
  docinfo_processor SiteLayout::FooterDocinfo
  treeprocessor SiteLayout::GuidePager
  postprocessor SiteLayout::PageKindClass
end
