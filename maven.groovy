// Generates server-side metadata for Ant & Maven
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HtmlAnchor
import com.gargoylesoftware.htmlunit.html.HtmlPage
import hudson.util.VersionNumber
import net.sf.json.JSONObject

import java.util.regex.Pattern

def getHtmlPage(url) {
    def wc = new WebClient()
    wc.javaScriptEnabled = false;
    wc.cssEnabled = false;
    return wc.getPage(url)
}

def listFromURL(url) {
    def HtmlPage p = getHtmlPage(url)
    def pattern = Pattern.compile("maven-([0-9\\.]+)(-bin)?.zip\$")

    return p.getAnchors().collect { HtmlAnchor a ->
        def m = pattern.matcher(a.hrefAttribute)
        if (m.find()) {
            def ver = m.group(1)
            def fqUrl = p.getFullyQualifiedUrl(a.hrefAttribute)
            return ["id": ver, "name": ver, "url": fqUrl.toExternalForm()]
        }
        return null;
    }
}

def listFromOldURL() {
    return listFromURL("http://archive.apache.org/dist/maven/binaries/")
}

def listMavenFoldersFromNewUrl() {
    def HtmlPage p = getHtmlPage("http://archive.apache.org/dist/maven/")
    def pattern = Pattern.compile("maven-([0-9])/\$")

    return p.getAnchors().collect { HtmlAnchor a ->
        def m = pattern.matcher(a.hrefAttribute)
        if (m.matches()) {
            def url = p.getFullyQualifiedUrl(a.hrefAttribute)
            return getHtmlPage(url)
        }
        return null
    }.findAll { it != null }
}

def listFromNewUrl() {
    def pattern1 = Pattern.compile("([0-9\\.])+/\$")
    def pattern2 = Pattern.compile("^binaries/\$")

    return listMavenFoldersFromNewUrl().collect { HtmlPage p1 ->
        p1.getAnchors().collect { HtmlAnchor a1 ->
            def m1 = pattern1.matcher(a1.hrefAttribute)
            if (m1.matches()) {
                def url1 = p1.getFullyQualifiedUrl(a1.hrefAttribute)
                def HtmlPage p2 = getHtmlPage(url1)
                p2.getAnchors().collect { HtmlAnchor a2 ->
                    def url2 = p2.getFullyQualifiedUrl(a2.hrefAttribute)
                    def m2 = pattern2.matcher(a2.hrefAttribute)
                    if (m2.matches()) {
                        return listFromURL(url2)
                    }
                }
            }
        }
    }.flatten()
}

def listAll() {
    return (listFromOldURL() + listFromNewUrl())
            .findAll { it != null }.unique().sort { o1, o2 ->
        try {
            def v1 = new VersionNumber(o1.id)
            try {
                return new VersionNumber(o2.id).compareTo(v1)
            } catch (IllegalArgumentException _2) {
                return -1
            }
        } catch (IllegalArgumentException _1) {
            try {
                new VersionNumber(o2.id)
                return 1
            } catch (IllegalArgumentException _2) {
                return o2.id.compareTo(o1.id)
            }
        }
    }
}

def store(key, o) {
    JSONObject envelope = JSONObject.fromObject(["list": o])
    lib.DataWriter.write(key, envelope)
}

store("hudson.tasks.Maven.MavenInstaller", listAll())
