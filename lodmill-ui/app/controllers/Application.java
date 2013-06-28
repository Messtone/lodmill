/* Copyright 2012-2013 Fabian Steeg, hbz. Licensed under the Eclipse Public License 1.0 */

package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import models.Document;
import models.Index;
import models.Parameter;
import models.Search;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;

import play.Logger;
import play.api.http.MediaRange;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * Main application controller.
 * 
 * @author Fabian Steeg (fsteeg)
 */
public final class Application extends Controller {

	private Application() { // NOPMD
		/* No instantiation */
	}

	/**
	 * @return The main page.
	 */
	public static Result index() {
		return ok(views.html.index.render());
	}

	/**
	 * Search enpoint for actual queries.
	 * 
	 * @param indexParameter The index to search (see {@link Index}).
	 * @param parameter The search parameter type (see {@link Parameter}).
	 * @param queryParameter The search query
	 * @param formatParameter The result format
	 * @return The results, in the format specified
	 */
	static Result search(final Index index, final Parameter parameter,
			final String queryParameter, final String formatParameter) {
		List<Document> docs = new ArrayList<>();
		try {
			docs = Search.documents(queryParameter, index, parameter);
		} catch (IllegalArgumentException e) {
			Logger.error(e.getMessage(), e);
			return badRequest(e.getMessage());
		}
		final ImmutableMap<ResultFormat, Result> results =
				results(queryParameter, docs, index);
		try {
			return results.get(ResultFormat.valueOf(formatParameter.toUpperCase()));
		} catch (IllegalArgumentException e) {
			Logger.error(e.getMessage(), e);
			return badRequest("Invalid 'format' parameter, use one of: "
					+ Joiner.on(", ").join(results.keySet()).toLowerCase());
		}
	}

	private static Function<Document, JsonNode> jsonFull =
			new Function<Document, JsonNode>() {
				@Override
				public JsonNode apply(final Document doc) {
					return Json.parse(doc.getSource());
				}
			};

	private static Function<Document, String> jsonShort =
			new Function<Document, String>() {
				@Override
				public String apply(final Document doc) {
					return doc.getMatchedField();
				}
			};

	private static Function<Document, JsonNode> jsonLabelValue =
			new Function<Document, JsonNode>() {
				@Override
				public JsonNode apply(final Document doc) {
					final ObjectNode object = Json.newObject();
					object.put("label", doc.getMatchedField());
					object.put("value", doc.getId());
					return object;
				}
			};

	private static ImmutableMap<ResultFormat, Result> results(final String query,
			final List<Document> documents, final Index selectedIndex) {
		/* JSONP callback support for remote server calls with JavaScript: */
		final String[] callback =
				request() == null || request().queryString() == null ? null : request()
						.queryString().get("callback");
		final JsonNode shortJson =
				Json.toJson(sortStrings(Lists.transform(documents, jsonShort)));
		final JsonNode labelAndValue =
				Json.toJson(sortNodes(Lists.transform(documents, jsonLabelValue)));
		final ImmutableMap<ResultFormat, Result> results =
				new ImmutableMap.Builder<ResultFormat, Result>()
						.put(ResultFormat.PAGE,
								ok(views.html.docs.render(documents, selectedIndex, query)))
						.put(ResultFormat.FULL,
								negotiateContent(documents, selectedIndex, query))
						.put(
								ResultFormat.SHORT,
								callback != null ? ok(String.format("%s(%s)", callback[0],
										shortJson)) : ok(shortJson))
						.put(
								ResultFormat.IDS,
								callback != null ? ok(String.format("%s(%s)", callback[0],
										labelAndValue)) : ok(labelAndValue)).build();
		return results;
	}

	private static ImmutableSortedSet<String> sortStrings(List<String> nodes) {
		return ImmutableSortedSet.copyOf(nodes);
	}

	private static List<JsonNode> sortNodes(List<JsonNode> nodes) {
		final List<JsonNode> sorted = new ArrayList<>(nodes);
		Collections.sort(sorted, new Comparator<JsonNode>() {
			@Override
			public int compare(JsonNode o1, JsonNode o2) {
				return o1.get("label").asText().compareTo(o2.get("label").asText());
			}
		});
		return sorted;
	}

	private static Result negotiateContent(List<Document> documents,
			Index selectedIndex, String query) {
		if (accepted(Serialization.JSON_LD)) {
			return ok(Json.toJson(ImmutableSet.copyOf(Lists.transform(documents,
					jsonFull))));
		} else if (accepted(Serialization.RDF_A)) {
			return ok(views.html.docs.render(documents, selectedIndex, query));
		}
		for (final Serialization serialization : Serialization.values()) {
			if (accepted(serialization)) {
				return ok(Joiner.on("\n").join(transform(documents, serialization)));
			}
		}
		return status(406, "Not acceptable: unsupported content type requested\n");
	}

	private static boolean accepted(Serialization serialization) {
		return request() != null
		/* Any of the types associated with the serialization... */
		&& Iterables.any(serialization.types, new Predicate<String>() {
			@Override
			public boolean apply(final String mediaType) {
				/* ...is accepted by any of the accepted types of the request: */
				return Iterables.any(request().acceptedTypes(),
						new Predicate<MediaRange>() {
							@Override
							public boolean apply(MediaRange media) {
								return media.accepts(mediaType);
							}
						});
			}
		});
	}

	private static List<String> transform(List<Document> documents,
			final Serialization serialization) {
		List<String> transformed =
				Lists.transform(documents, new Function<Document, String>() {
					@Override
					public String apply(final Document doc) {
						return doc.as(serialization.format);
					}
				});
		return transformed;
	}
}