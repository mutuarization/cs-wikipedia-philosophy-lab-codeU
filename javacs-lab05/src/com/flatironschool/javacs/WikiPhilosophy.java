package com.flatironschool.javacs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Deque;
import java.util.ArrayDeque;
import java.util.StringTokenizer;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import org.jsoup.select.Elements;

public class WikiPhilosophy {
	
	final static WikiFetcher wf = new WikiFetcher();
	final static List<String> seenLinks =  new ArrayList<String>();
	final static Deque paranthesisTracker = new ArrayDeque<String>();
    final static String targetUrl = "https://en.wikipedia.org/wiki/Wikipedia:Getting_to_Philosophy";
	/**
	 * Tests a conjecture about Wikipedia and Philosophy.
	 * 
	 * https://en.wikipedia.org/wiki/Wikipedia:Getting_to_Philosophy
	 * 
	 * 1. Clicking on the first non-parenthesized, non-italicized link
     * 2. Ignoring external links, links to the current page, or red links
     * 3. Stopping when reaching "Philosophy", a page with no links or a page
     *    that does not exist, or when a loop occurs
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		String url = "https://en.wikipedia.org/wiki/Java_(programming_language)";
		while(true) {
			if(url == null) {
				System.out.println("Failure: The last page visited has no links");
				break;
			}

			System.out.println("-->" + url);

			if (seenLinks.contains(url)) {
				System.out.println("Failure: A loop was encountered!");
				break;
			} else if(url.equals(targetUrl)){
				System.out.println("Success: Philosophy link found");
				break;
			} else {
				seenLinks.add(url);
				url = firstValidLink(url);
			}

		}
	}

	/**
	 * Given a url to a page, finds the first valid link
	 * Returns null if the page has no links, or if the first valid 
	 * link has been seen before
	 * @param url base Url of the page
	 * @return validLink
	 */
	private static String firstValidLink(String url) throws IOException {
		System.out.println("     ++Fetching: " + url);
		Elements paragraphs = wf.fetchWikipedia(url);
		Iterable<Node> iter;
		for(Element currentPara: paragraphs) {
			iter = new WikiNodeIterable(currentPara);
			for(Node node: iter) {
				//handle paranthesis
				if(node instanceof TextNode) {
					trackParanthesis((TextNode)node);
				}

				if(!(node instanceof Element)) continue;

				Element nodeElem = (Element)node;

				if(isValidLink(nodeElem)) {
					return nodeElem.attr("abs:href");
				}
			}
		}

		return null;
	}

	/**
	 * Processes textnode by updating parathesis tracker with opening 
	 * and closing parathesis
	 * @ param textNode
	 */
	private static void trackParanthesis(TextNode textNode) {
		StringTokenizer tokenizer = new StringTokenizer(textNode.text(), "() ", true);
		while(tokenizer.hasMoreTokens()) {
			String currentToken = tokenizer.nextToken();
			if(currentToken.equals("(")) {
				paranthesisTracker.push(currentToken);
			}
			if(currentToken.equals(")") && !paranthesisTracker.isEmpty()) {
				paranthesisTracker.pop();
			}
		}
	}


	/**
	 * Check whether a given element represents a valid link
	 * Valid link means the link is: not italicises, not paranthesised, 
	 * and not the link to the current page
	 * @param linkElement element representing the link
	 *
	 */
	private static boolean isValidLink(Element linkElement) {
		//Checking tag is <a>
		if(!linkElement.tagName().equals("a")) {
			return false;
		}

		//Checking italics
		if(isInItalics(linkElement)) {
			return false;
		}

		//Checking paranthesis
		if(isInParanthesis(linkElement)) {
			return false;
		}

		//Checking if current page
		String currPageUrl = linkElement.baseUri();
		if(currPageUrl.equals(linkElement.attr("abs:href"))) {
			return false;
		}

		return true;
	}

	/**
	 * Checks if a link is in italics
	 * @param linkElement element representing the link
	 */
	private static  boolean isInItalics(Element linkELement) {
		Elements ancestors = linkELement.parents();
		for(Element element: ancestors) {
			String tag = element.tagName();
			if(tag.equals("i") || tag.equals("em")) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks if a link is in paranthesis
	 * @param linkElement element representing the link
	 */	
	private static boolean isInParanthesis(Element linkElement) {
		return !paranthesisTracker.isEmpty();

	}



}
