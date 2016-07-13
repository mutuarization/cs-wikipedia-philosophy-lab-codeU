# cs-wikipedia-philosophy-lab

## Objectives

1.  Select data structures for an application.
2.  Use Java data structures to write a simple Web crawler.


## Overview

The goal of this lab is to write a Web crawler that tests the "Getting to Philosophy" conjecture, which we presented in the previous lab.  We'll provide some code to help you get started, but you will write more code for this lab than for the previous ones.


## Getting started

When you check out the respository for this lab, you should find a file structure similar to what you saw in previous labs.  The top level directory contains `CONTRIBUTING.md`, `LICENSE.md`, `README.md`, and the directory that contains the code for this lab, `javacs-lab05`.

In the subdirectory `javacs-lab05/src/com/flatironschool/javacs` you'll find the source files you need for this lab:

1.  `WikiNodeExample.java` contains the code from the previous README, demonstrating recursive and iterative implementations of depth-first search (DFS) in a DOM tree.

2.  `WikiNodeIterable.java` contains an `Iterable` class for traversing a DOM tree.  We'll explain this code in the next section.

3.  `WikiFetcher.java` contains a utility class that uses jsoup to download pages from Wikipedia.  To help you comply with Wikipedia's terms of service, this class limits how fast you can download pages; if you request more than one page per second, it sleeps before downloading the next page.

4.  `WikiPhilosophy.java` contains an outline of the code you will write for this lab.  We'll walk you through it below.

Also, in `javacs-lab05`, you'll find the Ant build file `build.xml`.  If you run `ant WikiPhilosophy`, it will run a simple bit of starter code.



## `Iterables` and `Iterators`

In the previous README, we presented an iterative depth-first search (DFS), and suggested that an advantage of the iterative version, compared to the recursive version, is that it is easier to wrap in an `Iterator` object.  In this section we'll see how to do that.

If you are not familiar with the `Iterator` and `Iterable` interfaces, you can read about them [here](https://docs.oracle.com/javase/7/docs/api/java/util/Iterator.html) and [here](https://docs.oracle.com/javase/7/docs/api/java/lang/Iterable.html).

Take a look at the contents of `WikiNodeIterable.java`.  The outer class, `WikiNodeIterable` implements the `Iterable<Node>` interface, so we can use it in an "enchanced for loop" like this:

```java
    Node root = ...
    Iterable<Node> iter = new WikiNodeIterable(root);
    for (Node node: iter) {
        visit(node);
    }
```

Where `root` is the root of the tree we want to traverse and `visit` is a method that does whatever we want when we "visit" a `Node`.

The implementation of WikiNodeIterable follows a conventional formula:

1. The constructor takes and stores a reference to the root `Node`.

2. The `iterator` method creates and returns an `Iterator` object.

Here's what it looks like:

```java
public class WikiNodeIterable implements Iterable<Node> {

	private Node root;

	public WikiNodeIterable(Node root) {
	    this.root = root;
	}

	@Override
	public Iterator<Node> iterator() {
		return new WikiNodeIterator(root);
	}
}
```

The inner class, `WikiNodeIterator`, does all the real work:

```
	private class WikiNodeIterator implements Iterator<Node> {

		Deque<Node> stack;

		public WikiNodeIterator(Node node) {
			stack = new ArrayDeque<Node>();
		    stack.push(root);
		}

		@Override
		public boolean hasNext() {
			return !stack.isEmpty();
		}

		@Override
		public Node next() {
			if (stack.isEmpty()) {
				throw new NoSuchElementException();
			}

			Node node = stack.pop();
			List<Node> nodes = new ArrayList<Node>(node.childNodes());
			Collections.reverse(nodes);
			for (Node child: nodes) {
				stack.push(child);
			}
			return node;
		}
    }
```

This code is almost identical to the iterative version of DFS, but now it's split into three methods:

1.  The constructor initializes the stack (which is implemented using an `ArrayDeque`) and pushes the root node onto it.

2.  `isEmpty` checks whether the stack is empty.

3.  `next` pops the next `Node` off the stack, pushes its children in reverse order, and returns the `Node` it popped.  If someone invokes `next` on an empty `Iterator`, it throws an exception.


## `WikiFetcher`

When you write a Web crawler, it is easy to download too many pages too fast, which might violate the terms of service of the server you are downloading from.  To help you avoid that, we provide a class called `WikiFetcher` that does two things

1.  It encapsulates the code we demonstrated in the previous README for downloading pages from Wikipedia, parsing the HTML, and selecting the content text.

2.  It measures the time between requests and, if we don't leave enough time between requests, it sleeps until a reasonable interval has elapsed.  By default, the interval is one second.

Here's the definition of `WikiFetcher`

```java
public class WikiFetcher {
	private long lastRequestTime = -1;
	private long minInterval = 1000;

	/**
	 * Fetches and parses a URL string, returning a list of paragraph elements.
	 *
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public Elements fetchWikipedia(String url) throws IOException {
		sleepIfNeeded();

		Connection conn = Jsoup.connect(url);
		Document doc = conn.get();
		Element content = doc.getElementById("mw-content-text");
		Elements paras = content.select("p");
		return paras;
	}

	private void sleepIfNeeded() {
		if (lastRequestTime != -1) {
			long currentTime = System.currentTimeMillis();
			long nextRequestTime = lastRequestTime + minInterval;
			if (currentTime < nextRequestTime) {
				try {
					Thread.sleep(nextRequestTime - currentTime);
				} catch (InterruptedException e) {
					System.err.println("Warning: sleep interrupted in fetchWikipedia.");
				}
			}
		}
		lastRequestTime = System.currentTimeMillis();
	}
}
```

The only public method is `fetchWikipedia`, which takes a URL as a String and returns an `Elements` collection that contains one DOM element for each paragraph in the content text.  This code should look familiar.

The new code is in `sleepIfNeeded`, which checks the time since the last request and sleeps if the elapsed time is less than `minInterval`, which is in milliseconds.

That's all there is to `WikiFetcher`.  Here's an example that demonstrates how it's used:

```java
	WikiFetcher wf = new WikiFetcher();

	for (String url: urlList) {
        Elements paragraphs = wf.fetchWikipedia(url);
        processParagraphs(paragraphs);
    }
```

In this example, we assume that `urlList` is a collection of Strings, and `processParagraphs` is a method that does something with the `Elements` object returned by `fetchWikipedia`.

This example demonstrates something important: you should create one `WikiFetcher` object and use it to handle all requests.  If you have multiple instances of `WikiFetcher`, they won't enforce the minimum interval between requests.

NOTE: My implementation of `WikiFetcher` is simple, but it would be easy for someone to mis-use it by creating multiple instances.  You could avoid this problem by making `WikiFetcher` a "singleton", [which you can read about here](https://en.wikipedia.org/wiki/Singleton_pattern).


## Filling in `WikiPhilosophy`

In `WikiPhilosophy.java` you'll find a simple `main` method that shows how to use some of these pieces.  Starting with this code, your job is to write a crawler that:

1.  Takes a URL for a Wikipedia page, downloads it, and parses it.

2.  It should traverse the resulting DOM tree to find the first *valid* link.  We'll explain what "valid" means below.

3.  If the page has no links, or if the first link is a page we have already seen, the program should indicate failure and exit.

4.  If the link matches the URL of the Wikipedia page on philosophy, the program should indicate success and exit.

5.  Otherwise it should go back to Step 1.

The program should build a `List` of the URLs it visits and display the results at the end (whether it succeeds or fails).

So what should we consider a "valid" link?  You have some choices here.  Various versions of the "Getting to Philosophy" conjecture use slightly different rules, but here are some options:

1.  The link should be in the content text of the page, not in a sidebar or boxout.

2.  It should not be in italics or in parentheses.

3.  You should skip external links, links to the current page, and red links.

4.  In some versions, you should skip a link if the text starts with an uppercase letter.

You don't have to enforce all of these rules, but we recommend that you at least handle parentheses, italics, and links to the current page.

If you feel like you have enough information to get started, go ahead.  Or you might want to read these hints:

1.  As you traverse the tree, the two kinds of `Node` you will need to deal with are `TextNode` and `Element`.  If you find an `Element`, you will probably have to typecast it to access the tag and other information.

2.  When you find an `Element` that contains a link, you can check whether it is in italics by following parent links up the tree.  If there is an `i` or `em` tag in the parent chain, the link is in italics.

3.  To check whether a link is in parentheses, you will have to scan through the text as you traverse the tree and keep track of opening and closing parentheses (ideally your solution should be able to handle nested parentheses (like this)).

4.  If you start from [the Java page](https://en.wikipedia.org/wiki/Java_(programming_language)), you should get to [Philosophy](https://en.wikipedia.org/wiki/Philosophy) after following seven links (unless something has changed since we ran the code).

Ok, that's all the help you're going to get from us.  Now it's up to you.  Have fun!


## Resources

[Iterator](https://docs.oracle.com/javase/7/docs/api/java/util/Iterator.html): Java documentation.

[Iterable](https://docs.oracle.com/javase/7/docs/api/java/lang/Iterable.html): Java documentation.

[Singleton pattern](https://en.wikipedia.org/wiki/Singleton_pattern): Wikipedia.

<p class='util--hide'>View <a href='https://learn.co/lessons/cs-wikipedia-philosophy-lab'>Wikipedia Philosophy Lab</a> on Learn.co and start learning to code for free.</p>
