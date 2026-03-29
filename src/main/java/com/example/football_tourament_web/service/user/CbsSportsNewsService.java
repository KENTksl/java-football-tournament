package com.example.football_tourament_web.service.user;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.example.football_tourament_web.model.dto.NewsItem;

@Service
public class CbsSportsNewsService {
	private static final String RSS_URL = "https://www.cbssports.com/rss/headlines/";
	private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(12);
	private static final Duration CACHE_TTL = Duration.ofMinutes(10);

	private final HttpClient httpClient;

	private volatile CacheEntry cacheEntry;

	public CbsSportsNewsService() {
		this.httpClient = HttpClient.newBuilder()
				.followRedirects(HttpClient.Redirect.NORMAL)
				.connectTimeout(HTTP_TIMEOUT)
				.build();
	}

	public List<NewsItem> getHeadlines() {
		CacheEntry cached = cacheEntry;
		if (cached != null && !cached.isExpired()) {
			return cached.items();
		}

		List<NewsItem> fresh = fetchAndParse(RSS_URL).orElseGet(Collections::emptyList);
		cacheEntry = new CacheEntry(Instant.now(), fresh);
		return fresh;
	}

	private Optional<List<NewsItem>> fetchAndParse(String url) {
		try {
			HttpRequest request = HttpRequest.newBuilder(URI.create(url))
					.timeout(HTTP_TIMEOUT)
					.header("User-Agent", "Mozilla/5.0 (compatible; football-tourament-web/1.0)")
					.GET()
					.build();

			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				return Optional.empty();
			}

			return Optional.of(parseRss(response.body()));
		} catch (Exception ex) {
			return Optional.empty();
		}
	}

	private List<NewsItem> parseRss(String xml) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(false);
		factory.setXIncludeAware(false);
		factory.setExpandEntityReferences(false);
		factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
		factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

		Document document = factory.newDocumentBuilder()
				.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

		NodeList itemNodes = document.getElementsByTagName("item");
		List<NewsItem> items = new ArrayList<>();

		for (int i = 0; i < itemNodes.getLength(); i++) {
			Node node = itemNodes.item(i);
			if (!(node instanceof Element itemEl)) {
				continue;
			}

			String title = textOfFirst(itemEl, "title").orElse("");
			String link = textOfFirst(itemEl, "link").orElse("");
			String pubDate = textOfFirst(itemEl, "pubDate").orElse("");
			String description = textOfFirst(itemEl, "description")
					.map(CbsSportsNewsService::decodeBasicHtmlEntities)
					.map(CbsSportsNewsService::stripHtml)
					.map(s -> truncate(s, 160))
					.orElse("");

			String imageUrl = firstImageUrl(itemEl).orElse(null);

			if (title.isBlank() || link.isBlank()) {
				continue;
			}

			items.add(new NewsItem(title.trim(), link.trim(), description, imageUrl, pubDate.trim()));
		}

		return items;
	}

	private static Optional<String> firstImageUrl(Element itemEl) {
		NodeList children = itemEl.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node node = children.item(i);
			if (!(node instanceof Element el)) {
				continue;
			}

			String tag = el.getTagName();
			if ("enclosure".equalsIgnoreCase(tag)) {
				String url = el.getAttribute("url");
				if (url != null && !url.isBlank()) {
					return Optional.of(url.trim());
				}
			}

			if ("media:content".equalsIgnoreCase(tag) || tag.endsWith(":content")) {
				String url = el.getAttribute("url");
				if (url != null && !url.isBlank()) {
					return Optional.of(url.trim());
				}
			}

			if ("media:thumbnail".equalsIgnoreCase(tag) || tag.endsWith(":thumbnail")) {
				String url = el.getAttribute("url");
				if (url != null && !url.isBlank()) {
					return Optional.of(url.trim());
				}
			}
		}
		return Optional.empty();
	}

	private static Optional<String> textOfFirst(Element parent, String tagName) {
		NodeList nodes = parent.getElementsByTagName(tagName);
		if (nodes.getLength() == 0) {
			return Optional.empty();
		}
		String text = nodes.item(0).getTextContent();
		if (text == null || text.isBlank()) {
			return Optional.empty();
		}
		return Optional.of(text);
	}

	private static String stripHtml(String input) {
		return input.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
	}

	private static String truncate(String input, int maxLen) {
		if (input == null) {
			return "";
		}
		String s = input.trim();
		if (s.length() <= maxLen) {
			return s;
		}
		return s.substring(0, Math.max(0, maxLen - 1)).trim() + "…";
	}

	private static String decodeBasicHtmlEntities(String input) {
		if (input == null || input.isBlank()) {
			return "";
		}
		return input
				.replace("&amp;", "&")
				.replace("&lt;", "<")
				.replace("&gt;", ">")
				.replace("&quot;", "\"")
				.replace("&#39;", "'")
				.replace("&nbsp;", " ");
	}

	private record CacheEntry(Instant fetchedAt, List<NewsItem> items) {
		private boolean isExpired() {
			return fetchedAt == null || fetchedAt.plus(CACHE_TTL).isBefore(Instant.now());
		}
	}
}

